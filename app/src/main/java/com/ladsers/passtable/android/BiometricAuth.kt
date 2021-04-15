package com.ladsers.passtable.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

    fun startAuth(masterPassEncrypted: String) {
        if (!checkAvailability()) {
            showMsgDialog(context.getString(R.string.dlg_err_biometricSensorNotAvailable))
            authFailed()
            return
        }
        if (masterPassEncrypted.isBlank() || masterPassEncrypted == "@") {
            showMsgDialog(context.getString(R.string.dlg_err_encryptedMasterPassNotFound))
            authFailed()
            return
        }
        isActivation = false
        val cipher = getCipher()
        val secretKey = getSecretKey()

        val mpeList = masterPassEncrypted.split("@")
        val initVec = Base64.decode(mpeList[0])
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, initVec))

        strToBiometricPrompt = mpeList[1]
        biometricPrompt.authenticate(
            promptInfoLogin,
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    fun activateAuth(masterPass: String) {
        if (!checkAvailability()) {
            showMsgDialog(context.getString(R.string.dlg_err_biometricSensorNotAvailable))
            afterActivation()
            return
        }
        isActivation = true
        val cipher = getCipher()
        val secretKey = getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

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

    private fun encrypt(mp: String, cipher: Cipher) {
        val mpEncrypted = cipher.doFinal(mp.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.toBase64String(cipher.iv)
        val data = Base64.toBase64String(mpEncrypted)
        val outStr = "$iv@$data"
        strToBiometricPrompt = null
        if (RecentFiles.rememberLastMpEncrypted(context, outStr)) {
            Toast.makeText(
                context,
                context.getString(R.string.ui_msg_fingerprintActivated),
                Toast.LENGTH_SHORT
            ).show()
        } else showMsgDialog(context.getString(R.string.dlg_err_encryptedMasterPassSaveFail))
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
        val keyName = "EncryptingMasterPass"
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
        .setTitle(context.getString(R.string.dlg_title_activateLoginByFingerprint))
        .setNegativeButtonText(context.getString(R.string.app_bt_cancel))
        .build()

    private val promptInfoLogin = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.dlg_title_loginByFingerprint))
        .setNegativeButtonText(context.getString(R.string.app_bt_usePassword))
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
                            showMsgDialog(context.getString(R.string.dlg_err_encryptedMasterPassSaveFail))
                            afterActivation()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ui_msg_fingerprintErr),
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

    private fun showMsgDialog(text: String, title: String = "") {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(text)
        if (title.isNotEmpty()) builder.setTitle(title)
        builder.setPositiveButton(context.getString(R.string.app_bt_ok)) { _, _ -> }
        builder.show()
    }
}