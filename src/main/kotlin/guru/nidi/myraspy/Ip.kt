package guru.nidi.myraspy

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class Ip(gitUrl: String, token: String, val repoDir: File, val ipFile: String) {
    val creds = UsernamePasswordCredentialsProvider(token, "")
    val git = if (repoDir.exists()) Git.open(repoDir).also {
        it.pull().call()
    }
    else Git.cloneRepository()
        .setCredentialsProvider(creds)
        .setURI(gitUrl)
        .setDirectory(repoDir)
        .call()

    suspend fun findMyIp(): String = HttpClient(CIO).use {
        val response = it.get("http://checkip.amazonaws.com/")
        return response.bodyAsText()
    }

    fun saveIp(ip: String) {
        val file = File(repoDir, ipFile)
        if (ip != file.readText()) {
            file.writeText(ip)
            git.add().addFilepattern(ipFile).call()
            git.commit().setMessage("update my IP").call()
            git.push().setCredentialsProvider(creds).call()
        }
    }
}
