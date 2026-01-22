package eu.kanade.tachiyomi.lib.mtls

import android.util.Log
import java.io.File

/**
 * Simple certificate manager - utilities for certificate files
 */
object SimpleCertificateManager {
    private const val TAG = "SimpleCertManager"
    private const val CERT_SUBDIR = "certificates"

    /**
     * Gets the certificate directory for the extension
     * Call this from the extension with proper Context
     */
    fun getCertificateDir(baseDir: File): File {
        val certDir = File(baseDir, CERT_SUBDIR)
        if (!certDir.exists()) {
            certDir.mkdirs()
        }
        return certDir
    }

    /**
     * Validates that a certificate file exists and is readable
     */
    fun validateCertificateFile(certFile: File?): Boolean {
        if (certFile == null) return false
        if (!certFile.exists()) {
            Log.w(TAG, "Certificate file does not exist: ${certFile.absolutePath}")
            return false
        }
        if (!certFile.canRead()) {
            Log.w(TAG, "Cannot read certificate file: ${certFile.absolutePath}")
            return false
        }
        Log.i(TAG, "Certificate file validated: ${certFile.absolutePath}")
        return true
    }
}
