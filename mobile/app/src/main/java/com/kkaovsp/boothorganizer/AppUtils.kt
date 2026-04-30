package com.kkaovsp.boothorganizer

import org.json.JSONObject
import java.util.Locale

/**
 * Utility and helper functions extracted from the application logic.
 * Contains pure functions for status mapping, string sanitization,
 * color calculations, and JSON parsing helpers.
 *
 * All color values are stored as packed ARGB integers (0xFFRRGGBB)
 * to avoid dependency on android.graphics.Color in unit tests.
 */
object AppUtils {

    // Color constants as ARGB integers (matching MainActivity theme)
    const val COLOR_SUCCESS: Int = 0xFF.shl(24) or (16 shl 16) or (185 shl 8) or 129   // rgb(16,185,129)
    const val COLOR_WARNING: Int = 0xFF.shl(24) or (245 shl 16) or (158 shl 8) or 11   // rgb(245,158,11)
    const val COLOR_DANGER: Int  = 0xFF.shl(24) or (239 shl 16) or (68 shl 8) or 68    // rgb(239,68,68)
    const val COLOR_SECONDARY: Int = 0xFF.shl(24) or (6 shl 16) or (182 shl 8) or 212  // rgb(6,182,212)
    const val COLOR_TEXT_MUTED: Int = 0xFF.shl(24) or (148 shl 16) or (163 shl 8) or 184 // rgb(148,163,184)

    /**
     * Maps a reservation status string to its corresponding color.
     */
    fun reservationColor(status: String): Int {
        return when (status) {
            "PENDING_PAYMENT" -> COLOR_WARNING
            "WAITING_FOR_APPROVAL" -> COLOR_SECONDARY
            "CONFIRMED" -> COLOR_SUCCESS
            "CANCELLED" -> COLOR_DANGER
            else -> COLOR_TEXT_MUTED
        }
    }

    /**
     * Maps a payment status string to its corresponding color.
     */
    fun paymentColor(status: String): Int {
        return when (status) {
            "APPROVED" -> COLOR_SUCCESS
            "PENDING" -> COLOR_WARNING
            "REJECTED" -> COLOR_DANGER
            else -> COLOR_TEXT_MUTED
        }
    }

    /**
     * Maps a merchant approval status string to its corresponding color.
     */
    fun approvalColor(status: String): Int {
        return when (status) {
            "APPROVED" -> COLOR_SUCCESS
            "REJECTED" -> COLOR_DANGER
            "PENDING" -> COLOR_WARNING
            else -> COLOR_TEXT_MUTED
        }
    }

    /**
     * Lightens a given ARGB color by blending it towards white (86% blend factor).
     */
    fun lighten(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val newR = (r + (255 - r) * 0.86).toInt()
        val newG = (g + (255 - g) * 0.86).toInt()
        val newB = (b + (255 - b) * 0.86).toInt()
        return (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    /**
     * Sanitizes a string to produce a safe filename.
     * Converts to lowercase, replaces non-alphanumeric characters (except Thai chars) with underscores,
     * trims leading/trailing underscores, and defaults to "event" if blank.
     */
    fun safeFileName(value: String): String {
        return value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9ก-๙]+"), "_")
            .trim('_')
            .ifBlank { "event" }
    }

    /**
     * Safely retrieves a string value from a JSONObject, returning "" if the key is missing or null.
     */
    fun jsonClean(json: JSONObject, key: String): String {
        if (!json.has(key) || json.isNull(key)) return ""
        return json.optString(key, "")
    }

    /**
     * Retrieves a boolean-like value from a JSONObject, supporting Boolean, Number, and String types.
     */
    fun jsonOptBooleanLike(json: JSONObject, key: String): Boolean {
        if (!json.has(key) || json.isNull(key)) return false
        val value = json.opt(key)
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value == "true" || value == "1"
            else -> false
        }
    }

    /**
     * Parses an error message from a JSON response body.
     * Tries "error", "detail", then "message" fields, falling back to the raw body or a fallback string.
     */
    fun parseErrorBody(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return try {
            val json = JSONObject(body)
            json.optString("error", json.optString("detail", json.optString("message", body)))
        } catch (_: Exception) {
            body
        }
    }
}
