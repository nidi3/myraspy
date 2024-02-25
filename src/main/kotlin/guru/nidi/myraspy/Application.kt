package guru.nidi.myraspy

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.eclipse.jgit.api.Git
import java.io.File


fun main() {
    val repo=File("./repo")
    val git=if (repo.exists())      Git.open(repo)
    else {
        Git.cloneRepository()
            .setURI("https://github.com/nidi3/myraspy.git")
            .setDirectory(File("./repo"))
            .call()
    }
    git.pull()
    embeddedServer(Netty, port = 8090) {
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}
