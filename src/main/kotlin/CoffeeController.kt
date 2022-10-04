import ApplianceControlServiceGrpc.ApplianceControlServiceImplBase
import de.fhdo.CoffeeProxy.CoffeeProxy
import de.fhdo.CoffeeProxy.Model.CoffeeOption
import de.fhdo.CoffeeProxy.Sse.SseCoffeeCallbackHandler
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.time.Instant

class CoffeeController : ApplianceControlServiceImplBase(),
    SseCoffeeCallbackHandler {

    private lateinit var coffeeProxy: CoffeeProxy
    private lateinit var lastUpdate: Instant
    private var currentProgramObserver: StreamObserver<AppliancePlugin.ProgramProgressResponse>? = null
    private var currentProgramStartedAt: Instant? = null


    override fun init(
        request: AppliancePlugin.InitCall,
        responseObserver: StreamObserver<AppliancePlugin.EmptyMessage>
    ) {
        coffeeProxy = CoffeeProxy(
            request.parametersMap["haId"],
            request.parametersMap["clientSecret"],
            request.parametersMap["refreshToken"]
        )
        coffeeProxy.setNestedCallbackHandler(this)
        coffeeProxy.update()
        lastUpdate = Instant.now()
        responseObserver.onNext(AppliancePlugin.EmptyMessage.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun executeProgram(
        request: AppliancePlugin.ProgramCall,
        responseObserver: StreamObserver<AppliancePlugin.ProgramProgressResponse>
    ) {

        //Check if there is something running
        if (currentProgramStartedAt != null && currentProgramStartedAt!!.isAfter(Instant.now().minusSeconds(60 * 5))) {
            responseObserver.onNext(
                AppliancePlugin.ProgramProgressResponse.newBuilder().setProgress(0.0).setFailure(true)
                    .setMessage("There is already a program running").build()
            )
            responseObserver.onCompleted()
            return
        }
        //Force close stuck executions
        currentProgramObserver?.onCompleted()

        if (lastUpdate.isBefore(Instant.now().minusSeconds(60 * 60))) {
            coffeeProxy.update()
            lastUpdate = Instant.now()
        }

        coffeeProxy.beverage = request.programId
        Thread.sleep(500)
        request.parametersMap.entries.forEach {
            when (it.key) {
                "fillQuantity" -> {
                    coffeeProxy.fillQuantity = it.value.toInt()
                }
                "beanAmount" -> {
                    coffeeProxy.beanAmount = it.value
                }
            }

        }
        Thread.sleep(1000)
        coffeeProxy.start()
        currentProgramObserver = responseObserver
        currentProgramStartedAt = Instant.now()
    }

    override fun handleOptionsChanged(options: MutableList<CoffeeOption>) {
        options.forEach {
            try {
                println("${it.key} : ${it.value}")
                if (it.key == "BSH.Common.Option.ProgramProgress") {
                    val progress = it.value.toString().toDouble()
                    currentProgramObserver?.onNext(
                        AppliancePlugin.ProgramProgressResponse.newBuilder().setProgress(progress)
                            .setFailure(false).build()
                    )
                    if (progress >= 100) {
                        currentProgramObserver?.onCompleted()
                        currentProgramStartedAt = null
                        currentProgramObserver = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun main() {
    val server = ServerBuilder.forPort(6000)
        .addService(CoffeeController())
        .build()
    server.start()
    server.awaitTermination()
}