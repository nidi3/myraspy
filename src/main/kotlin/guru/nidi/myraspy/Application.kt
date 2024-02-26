package guru.nidi.myraspy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.net.BindException
import kotlin.system.exitProcess

val log = LoggerFactory.getLogger("main")

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "shutdown") {
        shutdown(args)
        exitProcess(0)
    }
//    Webcam.setDriver(NativeDriver())
//    val webcam = Webcam.getWebcams().first { it.name == "QHD Webcam" }!!
//    val device = webcam.device
//    device.resolution = device.resolutions[8]
//    webcam.open()
//    Thread.sleep(1000)
//    val i = device.image
//    ImageIO.write(i, "jpeg", File("image.jpeg"))
    val ip =
        Ip(
            "https://github.com/nidi3/myraspy.git",
            System.getenv("NIDI_RASPY_GITHUB_TOKEN"),
            File("./repo"),
            "docs/index.html"
        )
    ip.checkIpPeriodically(5 * 60)
    try {
        EngineMain.main(args)
    } catch (e: BindException) {
        if (shutdown(args)) {
            for (i in 0..10) {
                try {
                    EngineMain.main(args)
                    break
                } catch (e: BindException) {
                    //ignore
                }
                Thread.sleep(50)
            }
        }
        log.error("Cannot stop old server")
        exitProcess(1)
    }
}

fun shutdown(args: Array<String>): Boolean {
    val cmd = commandLineEnvironment(args)
    val shutdownUrl = cmd.config.propertyOrNull("ktor.deployment.shutdown.url") ?: return false
    log.info("Shutting down old server")
    HttpClient(CIO).use {
        runBlocking {
            try {
                it.post("http://localhost:${cmd.connectors[0].port}${shutdownUrl.getString()}")
            } catch (e: Exception) {
                //ignore
            }
        }
    }
    return true
}

fun Application.module() {
    routing {
        get("/favicon.ico") {
            call.respondBytes(javaClass.classLoader.getResourceAsStream("favicon.ico")!!.readAllBytes())
        }
        get("/") {
            call.respondText(
                """
                <html>
                <body>
                <a href="https://nidi3.github.io/myraspy/">refresh</a>
                Hello World!
                </body>
                </html>
            """.trimIndent(), ContentType.Text.Html
            )
        }
    }
}
