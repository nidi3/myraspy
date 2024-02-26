package guru.nidi.myraspy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.concurrent.thread

class Ip(gitUrl: String, token: String, val repoDir: File, val ipFile: String) {
    val log = LoggerFactory.getLogger(javaClass)

    val creds = UsernamePasswordCredentialsProvider(token, "")
    val git = if (repoDir.exists()) Git.open(repoDir).also {
        it.pull().call()
    }
    else Git.cloneRepository()
        .setCredentialsProvider(creds)
        .setURI(gitUrl)
        .setDirectory(repoDir)
        .call()

    fun checkIpPeriodically(sec: Int) {
        thread(isDaemon = true) {
            while (true) {
                try {
                    runBlocking { saveIp(findMyIp()) }
                } catch (e: Exception) {
                    log.error("Problem checking IP", e)
                }
                Thread.sleep(1000L * sec)
            }
        }
    }

    suspend fun findMyIp(): String = HttpClient(CIO).use {
        val response = it.get("http://checkip.amazonaws.com/")
        return response.bodyAsText()
    }

    fun saveIp(ip: String) {
        val file = File(repoDir, ipFile)
        if (file.readText() != html(ip)) {
            log.info("IP changed to $ip")
            file.writeText(html(ip))
            git.add().addFilepattern(ipFile).call()
            git.commit().setMessage("update my IP").call()
            git.push().setCredentialsProvider(creds).call()
        }
    }

    fun html(ip: String) = """
        <html>
            <head>
                <meta http-equiv="refresh" content="0; url=http://$ip" />
            </head>
            <body>
                redirecting...
            </body>
        </html>
    """.trimIndent()
}
