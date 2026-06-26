package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.nio.CharBuffer
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

class WalletDeviceCrypto(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wallet_device_crypto", Context.MODE_PRIVATE)

    fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    fun ensureWalletKey(alias: String): WalletPublicKey {
        val keyPair = loadOrCreateEd25519(alias)
        return WalletPublicKey(
            publicKeyBase64 = encode(rawEd25519PublicKey(keyPair.public.encoded)),
            keyAlgorithm = "Ed25519"
        )
    }

    fun signWalletMessage(alias: String, message: String): String {
        val keyPair = loadOrCreateEd25519(alias)
        return sign(message, keyPair.private)
    }

    fun signKantinAuthorizeMessage(
        alias: String,
        walletPublicId: String,
        amount: Long,
        kantinUserId: String,
        idempotencyKey: String,
        media: String,
        merchantId: String?,
        outletId: String?,
        nonce: String
    ): String {
        val message = listOf(
            "DOMPET_SANTRI_KANTIN_AUTHORIZE_V1",
            walletPublicId,
            amount.toString(),
            kantinUserId,
            deviceId(),
            idempotencyKey,
            media,
            merchantId.orEmpty(),
            outletId.orEmpty(),
            nonce
        ).joinToString("\n")
        return signWalletMessage(alias, message)
    }

    fun signKantinCardLookupMessage(
        alias: String,
        walletPublicId: String,
        kantinUserId: String,
        nonce: String
    ): String {
        val message = listOf(
            "DOMPET_SANTRI_KANTIN_CARD_LOOKUP_V1",
            walletPublicId,
            kantinUserId,
            deviceId(),
            nonce
        ).joinToString("\n")
        return signWalletMessage(alias, message)
    }

    fun randomNonce(bytes: Int = 24): String {
        val out = ByteArray(bytes)
        SecureRandom().nextBytes(out)
        return encode(out)
    }

    fun createPinVerifier(pin: CharArray): WalletPinMaterial {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val params = WalletPinKdfParams()
        val verifier = deriveArgon2id(pin, salt, params)
        pin.fill('\u0000')
        return WalletPinMaterial(
            saltBase64 = encode(salt),
            verifierBase64 = encode(verifier),
            params = params
        )
    }

    fun createStudentPinProof(
        pin: CharArray,
        saltBase64: String,
        params: WalletPinKdfParams,
        message: String
    ): String {
        val verifier = deriveArgon2id(pin, decode(saltBase64), params)
        pin.fill('\u0000')
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(verifier, "HmacSHA256"))
        verifier.fill(0)
        return encode(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    private fun deriveArgon2id(pin: CharArray, salt: ByteArray, params: WalletPinKdfParams): ByteArray {
        require(pin.size in 4..12) { "PIN harus 4-12 digit." }
        require(pin.all { it.isDigit() }) { "PIN hanya boleh berisi angka." }
        val generator = Argon2BytesGenerator()
        generator.init(
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(params.iterations)
                .withMemoryAsKB(params.memoryKib)
                .withParallelism(params.parallelism)
                .build()
        )
        val output = ByteArray(params.hashLength)
        val encodedPin = Charsets.UTF_8.encode(CharBuffer.wrap(pin))
        val passwordBytes = ByteArray(encodedPin.remaining())
        encodedPin.get(passwordBytes)
        generator.generateBytes(passwordBytes, output)
        passwordBytes.fill(0)
        return output
    }

    private fun loadOrCreateEd25519(alias: String): KeyPair {
        loadSoftwareKey(alias)?.let { return it }
        val generator = KeyPairGenerator.getInstance("Ed25519")
        val pair = generator.generateKeyPair()
        storeSoftwareKey(alias, pair)
        return pair
    }

    private fun sign(message: String, privateKey: PrivateKey): String {
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(privateKey)
        signature.update(message.toByteArray(Charsets.UTF_8))
        return encode(signature.sign())
    }

    private fun loadSoftwareKey(alias: String): KeyPair? {
        val encrypted = prefs.getString("$alias.private", null) ?: return null
        val iv = prefs.getString("$alias.iv", null) ?: return null
        val bytes = decrypt(decode(encrypted), decode(iv))
        val privateKey = KeyFactory.getInstance("Ed25519")
            .generatePrivate(PKCS8EncodedKeySpec(bytes))
        val publicBytes = prefs.getString("$alias.public", null)?.let(::decode)
            ?: return null
        val publicKey = KeyFactory.getInstance("Ed25519")
            .generatePublic(X509EncodedKeySpec(publicBytes))
        return KeyPair(publicKey, privateKey)
    }

    private fun storeSoftwareKey(alias: String, keyPair: KeyPair) {
        val encrypted = encrypt(keyPair.private.encoded)
        prefs.edit()
            .putString("$alias.private", encode(encrypted.ciphertext))
            .putString("$alias.iv", encode(encrypted.iv))
            .putString("$alias.public", encode(keyPair.public.encoded))
            .apply()
    }

    private fun getAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(AES_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plaintext: ByteArray): EncryptedBytes {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey())
        return EncryptedBytes(cipher.doFinal(plaintext), cipher.iv)
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun encode(bytes: ByteArray): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }

    private fun decode(value: String): ByteArray =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getDecoder().decode(value)
        } else {
            android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
        }

    private fun rawEd25519PublicKey(x509: ByteArray): ByteArray =
        x509.takeLast(32).toByteArray()

    private data class EncryptedBytes(val ciphertext: ByteArray, val iv: ByteArray)

    companion object {
        private const val AES_ALIAS = "wallet-device-wrap-key"
    }
}

data class WalletPublicKey(
    val publicKeyBase64: String,
    val keyAlgorithm: String
)

data class WalletPinKdfParams(
    val memoryKib: Int = 19_456,
    val iterations: Int = 3,
    val parallelism: Int = 1,
    val hashLength: Int = 32
)

data class WalletPinMaterial(
    val saltBase64: String,
    val verifierBase64: String,
    val params: WalletPinKdfParams
)
