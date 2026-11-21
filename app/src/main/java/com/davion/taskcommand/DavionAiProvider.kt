package com.davion.taskcommand

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DavionAiProvider {
    private const val PREFS = "davion_ai_secure"
    private const val KEY_ALIAS = "davion_ai_api_key_v1"

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun saveApiKey(context: Context, raw: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(raw.trim().toByteArray(Charsets.UTF_8))
        val stored = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("api_key", stored).apply()
    }

    fun hasApiKey(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("api_key", null).isNullOrBlank()

    private fun apiKey(context: Context): String {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("api_key", null) ?: error("Add your API key in AI Studio settings first.")
        val parts = stored.split(":")
        require(parts.size == 2) { "Saved API key is unreadable. Please save it again." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun post(context: Context, path: String, body: JSONObject): JSONObject {
        val connection = (URL("https://api.openai.com/v1/" + path).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.readTimeout = 90000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer " + apiKey(context))
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val json = JSONObject(response)
        if (code !in 200..299) {
            val message = json.optJSONObject("error")?.optString("message")
            error(message?.takeIf { it.isNotBlank() } ?: "AI request failed with HTTP " + code)
        }
        return json
    }

    fun generateText(context: Context, prompt: String): String {
        val request = JSONObject()
            .put("model", "gpt-6.2-sol")
            .put("input", prompt)
        val response = post(context, "responses", request)
        val direct = response.optString("output_text")
        if (direct.isNotBlank()) return direct
        val output = response.optJSONArray("output") ?: JSONArray()
        val pieces = mutableListOf<String>()
        for (i in 0 until output.length()) {
            val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val item = content.optJSONObject(j) ?: continue
                if (item.optString("type") == "output_text") pieces.add(item.optString("text"))
            }
        }
        return pieces.joinToString("\n").ifBlank { "The AI service returned no text." }
    }

    fun generateImageBase64(context: Context, prompt: String): String {
        val request = JSONObject()
            .put("model", "gpt-image-2")
            .put("prompt", prompt)
            .put("size", "1024x1024")
        val response = post(context, "images/generations", request)
        return response.optJSONArray("data")?.optJSONObject(0)?.optString("b64_json")
            ?.takeIf { it.isNotBlank() } ?: error("The image service returned no image data.")
    }
}
