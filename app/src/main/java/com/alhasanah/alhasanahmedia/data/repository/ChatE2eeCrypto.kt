package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.alhasanah.alhasanahmedia.data.model.ChatDeviceKeyDto
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class LocalChatDeviceIdentity(
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val publicKey: String
)

data class E2eeCipherPayload(
    val ciphertext: String,
    val nonce: String
)

class ChatE2eeCrypto(private val context: Context) {
    private val prefs = context.getSharedPreferences("alumni_chat_e2ee", Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun getOrCreateIdentity(userId: String): LocalChatDeviceIdentity {
        val deviceId = activeDeviceId(userId) ?: stableDeviceId()
        val publicKeyKey = prefKey(userId, deviceId, "public")
        val privateKeyKey = prefKey(userId, deviceId, "private")
        val existingPublic = prefs.getString(publicKeyKey, null)
        val existingPrivate = prefs.getString(privateKeyKey, null)
        if (!existingPublic.isNullOrBlank() && !existingPrivate.isNullOrBlank()) {
            return LocalChatDeviceIdentity(userId, deviceId, deviceName(), existingPublic)
        }

        val keyPair = generateExportableKeyPair()
        val publicKey = keyPair.public.encoded.base64()
        val privateKey = encryptLocal(userId, deviceId, keyPair.private.encoded.base64())
        prefs.edit()
            .putString(publicKeyKey, publicKey)
            .putString(privateKeyKey, privateKey)
            .apply()

        return LocalChatDeviceIdentity(userId, deviceId, deviceName(), publicKey)
    }

    fun encryptForDevice(
        senderUserId: String,
        recipientDevice: ChatDeviceKeyDto,
        plainText: String
    ): E2eeCipherPayload {
        val identity = getOrCreateIdentity(senderUserId)
        val privateKey = loadPrivateKey(senderUserId, identity.deviceId)
        val publicKey = decodePublicKey(recipientDevice.publicKey)
        val secret = deriveAesKey(privateKey, publicKey)
        val nonce = ByteArray(GCM_NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val aad = "${recipientDevice.userId}:${recipientDevice.deviceId}:${identity.deviceId}".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)
        return E2eeCipherPayload(
            ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)).base64(),
            nonce = nonce.base64()
        )
    }

    fun decryptFromDevice(
        recipientUserId: String,
        recipientDeviceId: String,
        senderDeviceId: String,
        senderPublicKey: String,
        ciphertext: String,
        nonce: String
    ): String {
        val privateKey = loadPrivateKey(recipientUserId, recipientDeviceId)
        val secret = deriveAesKey(privateKey, decodePublicKey(senderPublicKey))
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(GCM_TAG_BITS, Base64.decode(nonce, Base64.NO_WRAP)))
        val aad = "$recipientUserId:$recipientDeviceId:$senderDeviceId".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)
        return cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    fun hasLocalIdentity(userId: String): Boolean {
        val deviceId = activeDeviceId(userId) ?: stableDeviceId()
        return !prefs.getString(prefKey(userId, deviceId, "private"), null).isNullOrBlank()
    }

    fun deviceId(userId: String? = null): String =
        userId?.let { activeDeviceId(it) } ?: stableDeviceId()

    fun forgetIdentity(userId: String, deviceId: String = stableDeviceId()) {
        prefs.edit()
            .remove(prefKey(userId, deviceId, "public"))
            .remove(prefKey(userId, deviceId, "private"))
            .remove(activeDevicePrefKey(userId))
            .apply()
        runCatching { keyStore.deleteEntry(localWrapKeyAlias(userId, deviceId)) }
    }

    fun createEncryptedPrivateKeyBackup(userId: String, passphrase: CharArray): BackupPayload {
        val identity = getOrCreateIdentity(userId)
        val privateKeyBase64 = decryptLocal(userId, identity.deviceId, prefs.getString(prefKey(userId, identity.deviceId, "private"), null).orEmpty())
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(GCM_NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        val key = javax.crypto.SecretKeyFactory.getInstance(BACKUP_KDF)
            .generateSecret(javax.crypto.spec.PBEKeySpec(passphrase, salt, BACKUP_ITERATIONS, 256))
            .encoded
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return BackupPayload(
            deviceId = identity.deviceId,
            encryptedPrivateKey = cipher.doFinal(privateKeyBase64.toByteArray(Charsets.UTF_8)).base64(),
            salt = salt.base64(),
            nonce = nonce.base64(),
            iterations = BACKUP_ITERATIONS
        )
    }

    fun restoreEncryptedPrivateKeyBackup(
        userId: String,
        deviceId: String,
        publicKey: String,
        encryptedPrivateKey: String,
        salt: String,
        nonce: String,
        iterations: Int,
        passphrase: CharArray
    ): LocalChatDeviceIdentity {
        val key = javax.crypto.SecretKeyFactory.getInstance(BACKUP_KDF)
            .generateSecret(
                javax.crypto.spec.PBEKeySpec(
                    passphrase,
                    Base64.decode(salt, Base64.NO_WRAP),
                    iterations,
                    256
                )
            )
            .encoded
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(nonce, Base64.NO_WRAP))
        )
        val privateKeyBase64 = cipher
            .doFinal(Base64.decode(encryptedPrivateKey, Base64.NO_WRAP))
            .toString(Charsets.UTF_8)

        val privateKey = KeyFactory.getInstance(EC_ALGORITHM)
            .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.NO_WRAP)))
        requireRestoredKeyPairMatches(privateKey, decodePublicKey(publicKey))

        prefs.edit()
            .putString(prefKey(userId, deviceId, "public"), publicKey)
            .putString(prefKey(userId, deviceId, "private"), encryptLocal(userId, deviceId, privateKeyBase64))
            .putString(activeDevicePrefKey(userId), deviceId)
            .apply()
        return LocalChatDeviceIdentity(userId, deviceId, deviceName(), publicKey)
    }

    private fun loadPrivateKey(userId: String, deviceId: String): PrivateKey {
        val encrypted = prefs.getString(prefKey(userId, deviceId, "private"), null)
            ?: error("Kunci chat lokal tidak ditemukan.")
        val privateKeyBase64 = decryptLocal(userId, deviceId, encrypted)
        return KeyFactory.getInstance(EC_ALGORITHM)
            .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.NO_WRAP)))
    }

    private fun generateExportableKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        generator.initialize(ECGenParameterSpec(EC_CURVE), SecureRandom())
        return generator.generateKeyPair()
    }

    private fun decodePublicKey(publicKey: String): PublicKey =
        KeyFactory.getInstance(EC_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(Base64.decode(publicKey, Base64.NO_WRAP)))

    private fun deriveAesKey(privateKey: PrivateKey, publicKey: PublicKey): SecretKey {
        val agreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val shared = agreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256").digest(shared)
        return SecretKeySpec(digest, "AES")
    }

    private fun requireRestoredKeyPairMatches(privateKey: PrivateKey, publicKey: PublicKey) {
        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val signer = Signature.getInstance(ECDSA_SIGNATURE)
        signer.initSign(privateKey)
        signer.update(challenge)
        val signature = signer.sign()

        val verifier = Signature.getInstance(ECDSA_SIGNATURE)
        verifier.initVerify(publicKey)
        verifier.update(challenge)
        require(verifier.verify(signature)) { "Backup kunci tidak cocok dengan perangkat chat." }
    }

    private fun encryptLocal(userId: String, deviceId: String, value: String): String {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateLocalWrapKey(userId, deviceId))
        val nonce = cipher.iv
        return "${nonce.base64()}:${cipher.doFinal(value.toByteArray(Charsets.UTF_8)).base64()}"
    }

    private fun decryptLocal(userId: String, deviceId: String, value: String): String {
        val parts = value.split(":", limit = 2)
        require(parts.size == 2) { "Format kunci lokal tidak valid." }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateLocalWrapKey(userId, deviceId),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateLocalWrapKey(userId: String, deviceId: String): SecretKey {
        val alias = localWrapKeyAlias(userId, deviceId)
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun localWrapKeyAlias(userId: String, deviceId: String): String =
        "alhasanah_chat_e2ee_${(userId + deviceId).sha256()}"

    private fun stableDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return (context.packageName + ":" + androidId).sha256().take(32)
    }

    private fun deviceName(): String = listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Android" }

    private fun prefKey(userId: String, deviceId: String, suffix: String): String =
        "chat_${(userId + ":" + deviceId).sha256()}_$suffix"

    private fun activeDeviceId(userId: String): String? =
        prefs.getString(activeDevicePrefKey(userId), null)?.takeIf { it.isNotBlank() }

    private fun activeDevicePrefKey(userId: String): String =
        "chat_${userId.sha256()}_active_device"

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    companion object {
        const val BACKUP_KDF = "PBKDF2WithHmacSHA256"
        const val BACKUP_ITERATIONS = 210_000
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val EC_ALGORITHM = "EC"
        private const val ECDH_ALGORITHM = "ECDH"
        private const val ECDSA_SIGNATURE = "SHA256withECDSA"
        private const val EC_CURVE = "secp256r1"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val GCM_NONCE_BYTES = 12
    }
}

data class BackupPayload(
    val deviceId: String,
    val encryptedPrivateKey: String,
    val salt: String,
    val nonce: String,
    val iterations: Int
)
