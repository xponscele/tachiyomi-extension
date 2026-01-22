package eu.kanade.tachiyomi.lib.mtls

import android.util.Log
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.util.Base64

/**
 * Configuration data for mTLS
 */
data class MtlsConfig(
    val enabled: Boolean = false,
    val certificateFile: File? = null,
    val certificateBase64: String = "",
    val certificatePassword: String = "",
    val caCertificate: String = "",
)

/**
 * Helper class to configure OkHttp client with mTLS support
 */
object MtlsHelper {
    private const val TAG = "MtlsHelper"

    /**
     * Configures the OkHttpClient.Builder with mTLS support based on the provided configuration
     */
    fun OkHttpClient.Builder.configureMtls(config: MtlsConfig): OkHttpClient.Builder {
        Log.i(TAG, "configureMtls called - enabled: ${config.enabled}")

        if (!config.enabled) {
            Log.i(TAG, "mTLS is disabled, skipping configuration")
            return this
        }

        // Try base64 first, then file
        val certData = when {
            config.certificateBase64.isNotEmpty() -> {
                Log.i(TAG, "Using certificate from base64, length=${config.certificateBase64.length}")
                try {
                    Base64.getDecoder().decode(config.certificateBase64)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode base64 certificate", e)
                    return this
                }
            }
            config.certificateFile != null -> {
                if (!SimpleCertificateManager.validateCertificateFile(config.certificateFile)) {
                    Log.e(TAG, "Certificate file validation failed: ${config.certificateFile.absolutePath}")
                    return this
                }
                Log.i(TAG, "Loading certificate from: ${config.certificateFile.absolutePath}")
                config.certificateFile.readBytes()
            }
            else -> {
                Log.w(TAG, "mTLS enabled but no certificate specified")
                return this
            }
        }

        try {
            val sslContext = createSslContext(certData, config.certificatePassword)
            val trustManager = createTrustManager(config)

            sslSocketFactory(sslContext.socketFactory, trustManager)
            Log.i(TAG, "mTLS configured successfully")

            return this
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure mTLS", e)
            throw RuntimeException("Failed to configure mTLS: ${e.message}", e)
        }
    }

    /**
     * Creates an SSLContext configured with client certificates from byte array
     */
    private fun createSslContext(certData: ByteArray, password: String): SSLContext {
        Log.d(TAG, "Loading PKCS12 keystore from byte array")

        val inputStream = ByteArrayInputStream(certData)
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(inputStream, password.toCharArray())

        Log.i(TAG, "PKCS12 keystore loaded successfully")

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, password.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)

        return sslContext
    }

    /**
     * Creates a TrustManager for server certificate verification
     */
    private fun createTrustManager(config: MtlsConfig): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        if (config.caCertificate.isNotEmpty()) {
            // TODO: Implement custom CA certificate loading
            Log.w(TAG, "Custom CA certificates not yet implemented")
        }

        trustManagerFactory.init(null as KeyStore?)

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${trustManagers.contentToString()}"
        }

        return trustManagers[0] as X509TrustManager
    }
}
