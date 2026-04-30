package com.kkaovsp.boothorganizer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(val statusCode: Int, override val message: String) : Exception(message)

class ApiClient(private val context: Context) {
    companion object {
        const val BASE_URL = "https://uaoufhdysqcivheauwyf.supabase.co/functions/v1/api"
    }

    var accessToken: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(path: String): String = execute(Request.Builder().url(url(path)).get())

    fun delete(path: String): String = execute(Request.Builder().url(url(path)).delete())

    fun postJson(path: String, body: JSONObject): String = json("POST", path, body)

    fun putJson(path: String, body: JSONObject): String = json("PUT", path, body)

    fun patchJson(path: String, body: JSONObject = JSONObject()): String = json("PATCH", path, body)

    fun postForm(path: String, values: Map<String, String>): String {
        val encoded = values.entries.joinToString("&") {
            "${encode(it.key)}=${encode(it.value)}"
        }
        val body = encoded.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        return execute(Request.Builder().url(url(path)).post(body))
    }

    fun uploadSlip(paymentId: String, uri: Uri): String {
        val name = fileName(uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw ApiException(0, "Cannot read selected slip file")
        val fileBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", name, fileBody)
            .build()
        return execute(Request.Builder().url(url("/payments/upload-slip?payment_id=${encode(paymentId)}")).post(body))
    }

    private fun json(method: String, path: String, body: JSONObject): String {
        val requestBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder = Request.Builder().url(url(path)).method(method, requestBody)
        return execute(builder)
    }

    private fun execute(builder: Request.Builder): String {
        builder.header("Accept", "application/json")
        accessToken?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", "Bearer $it")
        }

        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(response.code, parseError(text, response.message))
            }
            return text
        }
    }

    private fun url(path: String): String {
        return if (path.startsWith("http")) path else BASE_URL + path
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun parseError(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return try {
            val json = JSONObject(body)
            json.optString("error", json.optString("detail", json.optString("message", body)))
        } catch (_: Exception) {
            body
        }
    }

    private fun fileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index) ?: "slip.bin"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "slip.bin"
    }
}
