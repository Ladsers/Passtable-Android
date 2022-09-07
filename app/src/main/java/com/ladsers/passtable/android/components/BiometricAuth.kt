package com.ladsers.passtable.android.components

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.containers.RecentFiles
import com.ladsers.passtable.android.dialogs.MsgDialog
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricAuth(
    private val context: Context,
    private val activity: FragmentActivity,

    private val afterActivation: () -> Unit,
    private val authSucceeded: (String) -> Unit,
    private val authFailed: () -> Unit
) {

    private val executor = ContextCompat.getMainExecutor(context)
    private var isActivation = false
    private var strToBiometricPrompt: String? = null

    private val keyName = "EncryptedPrimaryPassword"

    fun startAuth(masterPassEncrypted: String) {
        if (!checkAvailability()) {
            showAuthError(context.getString(R.string.dlg_err_fingerprintSensorNotAvailable))
            return
        }
        if (masterPassEncrypted.isBlank() || masterPassEncrypted == "@") {
            showAuthError(context.getString(R.string.dlg_err_biometricFailure))
            return
        }
        isActivation = false
        val cipher = getCipher()
        val secretKey = getSecretKey()

        val mpeList = masterPassEncrypted.split("@")
        val initVec = Base64.decode(mpeList[0])

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, initVec))
        }
        catch (e: Exception){
            resetAuth()
            showAuthError(context.getString(R.string.dlg_err_biometricNewFingerprints))
            return
        }

        strToBiometricPrompt = mpeList[1]
        biometricPrompt.authenticate(
            promptInfoLogin,
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    fun activateAuth(masterPass: String) {
        if (!checkAvailability()) {
            showActivateError(context.getString(R.string.dlg_err_fingerprintSensorNotAvailable))
            afterActivation()
            return
        }
        isActivation = true
        val cipher = getCipher()
        val secretKey = getSecretKey()

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        }
        catch (e: Exception){
            resetAuth()
            showActivateError(context.getString(R.string.dlg_err_biometricNewFingerprintsEnableFail))
            afterActivation()
            return
        }

        strToBiometricPrompt = masterPass
        biometricPrompt.authenticate(
            promptInfoActivate,
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    fun checkAvailability(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun resetAuth() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(keyName)
        RecentFiles.forgetMpsEncrypted(context)
    }

    private fun encrypt(mp: String, cipher: Cipher) {
        val mpEncrypted = cipher.doFinal(mp.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.toBase64String(cipher.iv)
        val data = Base64.toBase64String(mpEncrypted)
        val outStr = "$iv@$data"
        strToBiometricPrompt = null
        if (RecentFiles.rememberLastMpEncrypted(context, outStr)) {
            Toast.makeText(
                context,
                context.getString(R.string.ui_msg_biometricEnabled),
                Toast.LENGTH_SHORT
            ).show()
        } else showActivateError(context.getString(R.string.dlg_err_biometricEnableFail))
        afterActivation()
    }

    private fun decrypt(mpEncrypted: String, cipher: Cipher) {
        val mp = cipher.doFinal(Base64.decode(mpEncrypted))
        strToBiometricPrompt = null
        authSucceeded(String(mp, StandardCharsets.UTF_8))
    }

    private fun getCipher() = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_GCM + "/"
                + KeyProperties.ENCRYPTION_PADDING_NONE
    )

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.getKey(keyName, null)?.let { return it as SecretKey }

        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(true)
        }
        val paramsForKeyGen = paramsBuilder.build()

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGen.init(paramsForKeyGen)
        return keyGen.generateKey()
    }

    private val promptInfoActivate = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.dlg_title_biometricEnable))
        .setNegativeButtonText(context.getString(R.string.app_bt_cancel))
        .build()

    private val promptInfoLogin = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.dlg_title_biometricAuthentication))
        .setNegativeButtonText(context.getString(R.string.app_bt_enterPassword))
        .build()

    private val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)

                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        if (isActivation) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ui_msg_canceled),
                                Toast.LENGTH_SHORT
                            ).show()
                            afterActivation()
                        } else {
                            if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) authFailed()
                            else activity.finish()
                        }
                    }
                    else -> {
                        if (isActivation) {
                            showActivateError(context.getString(R.string.dlg_err_biometricEnableFail))
                            afterActivation()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ui_msg_biometricNotAvailable),
                                Toast.LENGTH_LONG
                            ).show()
                            authFailed()
                        }
                    }
                }
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                result.cryptoObject?.cipher?.apply {
                    if (isActivation) encrypt(strToBiometricPrompt!!, this)
                    else decrypt(strToBiometricPrompt!!, this)
                }
            }
        })

    private fun showActivateError(reason: String) {
        val msgDialog = MsgDialog(context, activity.window)
        msgDialog.create(context.getString(R.string.dlg_title_operationCanceled), reason)
        msgDialog.addPositiveBtn(
            context.getString(R.string.app_bt_ok),
            R.drawable.ic_accept
        ) {}
        msgDialog.show()
    }

    private fun showAuthError(reason: String) {
        val msgDialog = MsgDialog(context, activity.window)
        msgDialog.create(context.getString(R.string.dlg_title_authenticationError), reason)
        msgDialog.addPositiveBtn(
            context.getString(R.string.app_bt_enterPassword),
            R.drawable.ic_next_arrow
        ) { authFailed() }
        msgDialog.disableSkip()
        msgDialog.show()
    }
}