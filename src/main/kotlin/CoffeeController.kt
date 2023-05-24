import ApplianceControlServiceGrpc.ApplianceControlServiceImplBase
import de.fhdo.CoffeeProxy.CoffeeProxy
import de.fhdo.CoffeeProxy.Model.CoffeeOption
import de.fhdo.CoffeeProxy.Sse.SseCoffeeCallbackHandler
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        if (currentProgramStartedAt != null && currentProgramStartedAt!!.isAfter(Instant.now().minusSeconds(30))) {
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
        while (!updateChannel.isEmpty) {
            runBlocking {
                updateChannel.receive()
            }
        }
        if(coffeeProxy.beverage.lowercase() != programId.lowercase()) {
            coffeeProxy.beverage = programId
            waitForSingleUpdateKey("selectedProgram")
        }
        //Make sure nothing else changes
        runBlocking {
            try {
                withTimeout(300) {
                    updateChannel.receive()
                }
            } catch (_: Exception) {
                //Nothing to do here
            }
        }
        val updateKeys = mutableListOf<String>()
        params.entries.forEach {
            when (it.key) {
                "fillQuantity" -> {
                    val value = it.value.toInt()
                    if(coffeeProxy.fillQuantity != coffeeProxy.computeFillQuantityForCurrentProgram(value)) {
                        coffeeProxy.fillQuantity = it.value.toInt()
                        waitForSingleUpdateKey("fillQuantity")
                    }
                }

                "beanAmount" -> {
                    if(coffeeProxy.beanAmount != it.value) {
                        coffeeProxy.beanAmount = it.value
                        updateKeys.add("BeanAmount")
                    }
                }

                "temperature" -> {
                    coffeeProxy.coffeeTemperature = it.value
                    if(coffeeProxy.coffeeTemperature != it.value) {
                        coffeeProxy.coffeeTemperature = it.value
                        updateKeys.add("coffeeTemperature")
                    }
                }

                "intensity" -> {
                    coffeeProxy.flowRate = it.value
                    if(coffeeProxy.flowRate != it.value) {
                        coffeeProxy.flowRate = it.value
                        updateKeys.add("flowRate")
                    }

                }
                "doubleBeverage" -> {
                    val value = it.value.toBooleanStrictOrNull() ?: false
                    if(coffeeProxy.multipleBeverages != value) {
                        coffeeProxy.multipleBeverages = value
                        updateKeys.add("multipleBeverages")
                    }

                }
                "beanContainer" -> {
                    if (coffeeProxy.beanContainer != it.value) {
                        coffeeProxy.beanContainer = it.value
                        updateKeys.add("beanContainer")
                    }

                }
            }

        }
        //Check every key is updated
        try {
            runBlocking {
                withTimeout(3000) {
                    while (updateKeys.isNotEmpty()) {
                        val receivedKey = updateChannel.receive()
                        updateKeys.removeIf { receivedKey.lowercase().contains(it.lowercase())}
                    }
                }
            }
        } catch (_: Exception) {
            println("Timeout waiting for update keys")
        }
        println("Starting program")
        coffeeProxy.start()
    }

    private fun waitForSingleUpdateKey(literal: String, timeout: Long = 3000) {

        runBlocking {
            try {
                withTimeout(timeout) {
                    while (!updateChannel.receive().lowercase().contains(literal.lowercase())) {
                    }

                }
            } catch (_: Exception) {
                println("Timed out for waiting for $literal")
            }
        }
    }


    override fun handleOptionsChanged(options: MutableList<CoffeeOption>) {
        options.forEach {
            try {
                println("${it.key} : ${it.value}")
                if (it.key == "BSH.Common.Option.ProgramProgress") {
                    var progress = it.value.toString().toDouble()
                    if (progress == 0.0) {
                        return
                    }
                    currentProgramObserver?.onNext(
                        AppliancePlugin.ProgramProgressResponse.newBuilder().setProgress(progress)
                            .setFailure(false).build()
                    )
                } else if (it.key == "BSH.Common.Root.ActiveProgram" && it.value == null) {
                    currentProgramObserver?.onCompleted()
                    currentProgramStartedAt = null
                    currentProgramObserver = null
                    coffeeProxy.multipleBeverages = false
                    println("Program Finished")

                }
                updateChannel.trySend(it.key)
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