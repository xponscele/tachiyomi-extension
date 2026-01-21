package eu.kanade.tachiyomi.lib.mtls

import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Configuration data for mTLS
 */
data class MtlsConfig(
    val enabled: Boolean = false,
    val clientCertificate: String = "",
    val clientKey: String = "",
    val certificatePassword: String = "",
    val caCertificate: String = "",
)

/**
 * Helper class to configure OkHttp client with mTLS support
 */
object MtlsHelper {

    /**
     * Configures the OkHttpClient.Builder with mTLS support based on the provided configuration
     */
    fun OkHttpClient.Builder.configureMtls(config: MtlsConfig): OkHttpClient.Builder {
        if (!config.enabled || config.clientCertificate.isEmpty()) {
            return this
        }

        try {
            val sslContext = createSslContext(config)
            val trustManager = createTrustManager(config)

            sslSocketFactory(sslContext.socketFactory, trustManager)

            return this
        } catch (e: Exception) {
            throw RuntimeException("Failed to configure mTLS: ${e.message}", e)
        }
    }

    /**
     * Creates an SSLContext configured with client certificates
     */
    private fun createSslContext(config: MtlsConfig): SSLContext {
        val keyStore = loadClientKeyStore(config)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        val password = config.certificatePassword.toCharArray()
        keyManagerFactory.init(keyStore, password)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

        if (config.caCertificate.isNotEmpty()) {
            val trustStore = loadTrustStore(config.caCertificate)
            trustManagerFactory.init(trustStore)
        } else {
            trustManagerFactory.init(null as KeyStore?)
        }

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
            val trustStore = loadTrustStore(config.caCertificate)
            trustManagerFactory.init(trustStore)
        } else {
            trustManagerFactory.init(null as KeyStore?)
        }

        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${trustManagers.contentToString()}"
        }

        return trustManagers[0] as X509TrustManager
    }

    /**
     * Loads the client certificate and key into a KeyStore
     * Supports both PKCS12 (.p12/.pfx) and PEM formats
     */
    private fun loadClientKeyStore(config: MtlsConfig): KeyStore {
        val certData = config.clientCertificate

        // Try to load as PKCS12 first (most common format for client certificates)
        if (certData.startsWith("MIIF") || certData.endsWith(".p12") || certData.endsWith(".pfx")) {
            return loadPkcs12KeyStore(certData, config.certificatePassword)
        }

        // Otherwise treat as PEM format
        return loadPemKeyStore(config)
    }

    /**
     * Loads a PKCS12 keystore from file path or base64 encoded data
     */
    private fun loadPkcs12KeyStore(certData: String, password: String): KeyStore {
        val inputStream = getInputStream(certData)

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(inputStream, password.toCharArray())

        return keyStore
    }

    /**
     * Loads PEM format certificate and key into a KeyStore
     */
    private fun loadPemKeyStore(config: MtlsConfig): KeyStore {
        // For PEM format, we need to parse the certificate and key separately
        // This is a simplified implementation - full PEM parsing would require additional libraries
        throw UnsupportedOperationException(
            "PEM format support requires additional implementation. " +
            "Please use PKCS12 (.p12/.pfx) format for client certificates."
        )
    }

    /**
     * Loads CA certificates for server verification
     */
    private fun loadTrustStore(caCertData: String): KeyStore {
        val inputStream = getInputStream(caCertData)

        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = certificateFactory.generateCertificates(inputStream)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        certificates.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry("ca_$index", certificate as X509Certificate)
        }

        return keyStore
    }

    /**
     * Gets an InputStream from either a file path or base64 encoded data
     */
    private fun getInputStream(data: String): InputStream {
        return when {
            // Check if it's a file path
            data.startsWith("/") || data.contains(":\\") -> {
                FileInputStream(data)
            }
            // Check if it's base64 encoded
            data.matches(Regex("^[A-Za-z0-9+/]+=*$")) -> {
                val decoded = Base64.getDecoder().decode(data)
                ByteArrayInputStream(decoded)
            }
            // Try to read as raw PEM data
            data.startsWith("-----BEGIN") -> {
                ByteArrayInputStream(data.toByteArray())
            }
            else -> {
                throw IllegalArgumentException("Invalid certificate data format. Expected file path, base64, or PEM format.")
            }
        }
    }
}
