package com.localpdf.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.localpdf.core.database.DocumentEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentClassification
import java.io.*
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface VaultRepository {
    fun observeVault(): Flow<List<Document>>
    suspend fun moveToVault(documentId: String): Result<Unit>
    suspend fun openAuthenticatedCopy(documentId: String): Result<File>
    suspend fun restoreFromVault(documentId: String): Result<Unit>
    fun clearTemporaryFiles()
}

class KeystoreVaultRepository private constructor(private val context: Context) : VaultRepository {
    private val dao = LocalPdfDatabase.create(context).documentDao()
    override fun observeVault() = dao.observeVault().map { list -> list.map(DocumentEntity::toModel) }

    override suspend fun moveToVault(documentId: String) = runCatching { withContext(Dispatchers.IO) {
        val document = requireNotNull(dao.getById(documentId)); require(!document.isVaulted)
        val source = File(document.filePath); require(source.isFile) { "Only completed document files can enter the vault" }
        val target = File(context.filesDir, "vault/$documentId.lpv").apply { parentFile?.mkdirs() }
        val pending = File(target.parentFile, "$documentId.pending")
        encrypt(source, pending, key(documentId)); check(pending.renameTo(target))
        dao.updateVault(documentId, target.absolutePath, target.length(), true, System.currentTimeMillis())
        source.delete()
        Unit
    } }

    override suspend fun openAuthenticatedCopy(documentId: String) = runCatching { withContext(Dispatchers.IO) {
        val document = requireNotNull(dao.getById(documentId)); require(document.isVaulted)
        val output = File(context.cacheDir, "vault-open/$documentId.pdf").apply { parentFile?.mkdirs(); delete() }
        decrypt(File(document.filePath), output, existingKey(documentId)); output
    } }

    override suspend fun restoreFromVault(documentId: String) = runCatching { withContext(Dispatchers.IO) {
        val document = requireNotNull(dao.getById(documentId)); require(document.isVaulted)
        val source = File(document.filePath)
        val target = File(context.filesDir, "documents/$documentId/searchable.pdf").apply { parentFile?.mkdirs() }
        val pending = File(target.parentFile, "restore.pending")
        decrypt(source, pending, existingKey(documentId)); check(pending.renameTo(target))
        dao.updateVault(documentId, target.absolutePath, target.length(), false, System.currentTimeMillis())
        source.delete(); deleteKey(documentId)
        Unit
    } }

    override fun clearTemporaryFiles() { File(context.cacheDir, "vault-open").deleteRecursively() }

    private fun key(id: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias(id), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256)
            .setUserAuthenticationRequired(true).setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL).build())
        return generator.generateKey()
    }
    private fun existingKey(id: String): SecretKey = requireNotNull(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias(id), null) as? SecretKey) { "Vault key was invalidated and this file cannot be recovered" }
    private fun deleteKey(id: String) = KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias(id)) }
    private fun alias(id: String) = "localpdf.vault.$id"
    private fun encrypt(input: File, output: File, key: SecretKey) { val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }; DataOutputStream(output.outputStream()).use { out -> out.writeInt(MAGIC); out.writeInt(cipher.iv.size); out.write(cipher.iv); CipherOutputStream(out, cipher).use { encrypted -> input.inputStream().use { it.copyTo(encrypted) } } } }
    private fun decrypt(input: File, output: File, key: SecretKey) { DataInputStream(input.inputStream()).use { stream -> require(stream.readInt() == MAGIC) { "Invalid vault file" }; val iv = ByteArray(stream.readInt().also { require(it in 12..16) }); stream.readFully(iv); val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv)) }; CipherInputStream(stream, cipher).use { encrypted -> output.outputStream().use { encrypted.copyTo(it) } } } }
    companion object { private const val MAGIC = 0x4C505631; fun create(context: Context): VaultRepository = KeystoreVaultRepository(context.applicationContext) }
}

private fun DocumentEntity.toModel() = Document(id, title, filePath, fileSize, pageCount, mimeType, DocumentClassification.valueOf(classification), isVaulted, isFavorite, emptyList(), Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt))
