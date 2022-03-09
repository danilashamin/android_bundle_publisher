import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.AppEdit
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        println("Started apk upload script")
        val appId = args[0]
        val apkPath = args[1]
        val credentialsPath = args[2]
        val inAppUpdatePriority = args[3]
        println("Received arguments\nappId: $appId\napkPath: $apkPath\ncredentialsPath: $credentialsPath")
        uploadApk(appId, apkPath, credentialsPath, inAppUpdatePriority)
    } catch (e: Exception) {
        println("${e::class.simpleName} ${e.message}")
        exitProcess(SCRIPT_FAILED_CODE)
    }
}

private const val SCRIPT_FAILED_CODE = -1

private fun setHttpTimeout(requestInitializer: HttpRequestInitializer) = HttpRequestInitializer { request ->
    requestInitializer.initialize(request)
    request.connectTimeout = 3 * 60000
    request.readTimeout = 3 * 60000
}

private fun uploadApk(appId: String, apkPath: String, credentialsPath: String, inAppUpdatePriority: String) {
    val credentials = ServiceAccountCredentials
        .fromStream(FileInputStream(credentialsPath))
        .createScoped(AndroidPublisherScopes.all())

    val publisher = AndroidPublisher.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        setHttpTimeout(HttpCredentialsAdapter(credentials))
    ).setApplicationName("Google Play APK upload").build()

    val edits = publisher.edits()

    val edit: AppEdit = edits.insert(appId, null).execute()
    println("Created edit: ${edit.id}")
    // can use publisher.edits().apks()
    val bundle: Bundle = edits.bundles().upload(
        appId,
        edit.id,
        FileContent("application/octet-stream", File(apkPath))
    ).execute()


    println("Uploaded apk versionCode: ${bundle.versionCode}")

    val bundleVersionCode = mutableListOf(bundle.versionCode.toLong())
    edits.tracks().update(
        appId,
        edit.id,
        "production",
        Track().setReleases(
            mutableListOf(
                TrackRelease()
                    .setVersionCodes(bundleVersionCode)
                    .setStatus("draft")
                    .setInAppUpdatePriority(inAppUpdatePriority.toIntOrNull() ?: 0)
            )
        )
    ).execute()

    edits.commit(appId, edit.id).execute()
    println("Committed edit with a new apk")
}