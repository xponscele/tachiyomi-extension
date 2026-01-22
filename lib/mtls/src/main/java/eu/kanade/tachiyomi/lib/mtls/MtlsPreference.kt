package eu.kanade.tachiyomi.lib.mtls

import android.content.SharedPreferences
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import java.io.File

/**
 * Helper class to add mTLS preference UI to extension settings
 */
object MtlsPreference {

    private const val PREF_MTLS_ENABLED = "mtls_enabled"
    private const val PREF_MTLS_CERT_BASE64 = "mtls_cert_base64"
    private const val PREF_MTLS_CERT_FILENAME = "mtls_cert_filename"
    private const val PREF_MTLS_CERT_PASSWORD = "mtls_cert_password"

    /**
     * Adds mTLS preferences to the preference screen
     */
    fun addPreferences(screen: PreferenceScreen) {
        // Main mTLS toggle
        screen.addPreference(
            SwitchPreferenceCompat(screen.context).apply {
                key = PREF_MTLS_ENABLED
                title = "Enable mTLS"
                summary = "Enable mutual TLS authentication with client certificates"
                setDefaultValue(false)
            }
        )

        // Certificate (base64)
        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_MTLS_CERT_BASE64
                title = "Certificate (Base64)"
                summary = "Paste your PKCS12 certificate encoded in Base64"
                setDefaultValue("")
                dialogTitle = "Certificate (Base64)"
                dialogMessage = "Paste your client certificate (.p12/.pfx) encoded in Base64\n\nTo encode: base64 -w 0 client.p12"
            }
        )

        // Certificate password
        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_MTLS_CERT_PASSWORD
                title = "Certificate password"
                summary = "Password for the certificate (leave empty if no password)"
                setDefaultValue("")
                dialogTitle = "Certificate password"
                dialogMessage = "Enter the password for your certificate (if required)"

                // Make it a password field
                setOnBindEditTextListener { editText ->
                    editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
        )
    }

    /**
     * Retrieves mTLS configuration from SharedPreferences
     * The certificateFile parameter should be created by the extension using Context
     */
    fun getConfig(preferences: SharedPreferences, certificateFile: File?): MtlsConfig {
        val enabled = preferences.getBoolean(PREF_MTLS_ENABLED, false)
        val base64 = preferences.getString(PREF_MTLS_CERT_BASE64, "") ?: ""
        val password = preferences.getString(PREF_MTLS_CERT_PASSWORD, "") ?: ""

        return MtlsConfig(
            enabled = enabled,
            certificateFile = if (enabled && base64.isEmpty()) certificateFile else null,
            certificateBase64 = base64,
            certificatePassword = password,
            caCertificate = "", // Not yet implemented
        )
    }
}
