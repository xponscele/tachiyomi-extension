package eu.kanade.tachiyomi.lib.mtls

import android.content.SharedPreferences
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen

/**
 * Helper class to add mTLS preference UI to extension settings
 */
object MtlsPreference {

    private const val PREF_MTLS_ENABLED = "mtls_enabled"
    private const val PREF_MTLS_CLIENT_CERT = "mtls_client_cert"
    private const val PREF_MTLS_CLIENT_KEY = "mtls_client_key"
    private const val PREF_MTLS_CERT_PASSWORD = "mtls_cert_password"
    private const val PREF_MTLS_CA_CERT = "mtls_ca_cert"

    /**
     * Adds mTLS preferences to the preference screen
     */
    fun addPreferences(screen: PreferenceScreen) {
        screen.addPreference(
            CheckBoxPreference(screen.context).apply {
                key = PREF_MTLS_ENABLED
                title = "Enable mTLS"
                summary = "Enable mutual TLS authentication with client certificates"
                setDefaultValue(false)
            }
        )

        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_MTLS_CLIENT_CERT
                title = "Client Certificate"
                summary = "Path to PKCS12 (.p12/.pfx) file or base64 encoded certificate"
                setDefaultValue("")
                dialogTitle = "Client Certificate"
                dialogMessage = "Enter the path to your client certificate file (PKCS12 format) or base64 encoded data"
            }
        )

        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_MTLS_CERT_PASSWORD
                title = "Certificate Password"
                summary = "Password for the client certificate (if encrypted)"
                setDefaultValue("")
                dialogTitle = "Certificate Password"
                dialogMessage = "Enter the password to decrypt the client certificate"

                // Make it a password field
                setOnBindEditTextListener { editText ->
                    editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
        )

        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_MTLS_CA_CERT
                title = "CA Certificate (Optional)"
                summary = "Path to custom CA certificate for server verification or base64 encoded certificate"
                setDefaultValue("")
                dialogTitle = "CA Certificate"
                dialogMessage = "Enter the path to a custom CA certificate file or base64 encoded data (optional)"
            }
        )
    }

    /**
     * Retrieves mTLS configuration from SharedPreferences
     */
    fun getConfig(preferences: SharedPreferences): MtlsConfig {
        return MtlsConfig(
            enabled = preferences.getBoolean(PREF_MTLS_ENABLED, false),
            clientCertificate = preferences.getString(PREF_MTLS_CLIENT_CERT, "") ?: "",
            clientKey = preferences.getString(PREF_MTLS_CLIENT_KEY, "") ?: "",
            certificatePassword = preferences.getString(PREF_MTLS_CERT_PASSWORD, "") ?: "",
            caCertificate = preferences.getString(PREF_MTLS_CA_CERT, "") ?: "",
        )
    }
}
