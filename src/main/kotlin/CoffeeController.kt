import ApplianceControlServiceGrpc.ApplianceControlServiceImplBase
import de.fhdo.CoffeeProxy.CoffeeProxy
import de.fhdo.CoffeeProxy.Model.CoffeeOption
import de.fhdo.CoffeeProxy.Sse.SseCoffeeCallbackHandler
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import java.time.Instant

class CoffeeController : ApplianceControlServiceImplBase(),
    SseCoffeeCallbackHandler {

    private lateinit var coffeeProxy: CoffeeProxy
    private lateinit var lastUpdate: Instant
    private var currentProgramObserver: StreamObserver<AppliancePlugin.ProgramProgressResponse>? = null
    private var currentProgramStartedAt: Instant? = null

    private var updateChannel = Channel<String>(50)

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
        if (currentProgramStartedAt != null && currentProgramStartedAt!!.isAfter(Instant.now().minusSeconds(60 * 2))) {
            responseObserver.onNext(
                AppliancePlugin.ProgramProgressResponse.newBuilder().setProgress(100.0).setFailure(true)
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
        startProgram(request.programId, request.parametersMap)
        currentProgramObserver = responseObserver
        currentProgramStartedAt = Instant.now()
    }

    private fun startProgram(programId: String, params: Map<String, String>) {
        coffeeProxy.beverage = programId
        Thread.sleep(1000)
        params.entries.forEach {
            when (it.key) {
                "fillQuantity" -> {
                    coffeeProxy.fillQuantity = it.value.toInt()
                }

                "beanAmount" -> {
                    coffeeProxy.beanAmount = it.value
                }

                "temperature" -> {
                    coffeeProxy.coffeeTemperature = it.value
                }
                "intensity" -> {
                    coffeeProxy.flowRate = it.value
                }
                "doubleBeverage" -> {
                    coffeeProxy.multipleBeverages = it.value.toBooleanStrictOrNull() ?: false
                }
                "beanContainer" -> {
                    coffeeProxy.beanContainer = it.value
                }
            }

        }
        Thread.sleep(1000)
       // coffeeProxy.start()
    }


    override fun handleOptionsChanged(options: MutableList<CoffeeOption>) {
        options.forEach {
            try {
                println("${it.key} : ${it.value}")
                if (it.key == "BSH.Common.Option.ProgramProgress") {
                    var progress = it.value.toString().toDouble()
                    if(progress == 0.0) {
                        return
                    }
                    currentProgramObserver?.onNext(
                        AppliancePlugin.ProgramProgressResponse.newBuilder().setProgress(progress)
                            .setFailure(false).build()
                    )
                }
                else if(it.key == "BSH.Common.Root.ActiveProgram" && it.value == null) {
                    currentProgramObserver?.onCompleted()
                    currentProgramStartedAt = null
                    currentProgramObserver = null
                    println("Program Finished")

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