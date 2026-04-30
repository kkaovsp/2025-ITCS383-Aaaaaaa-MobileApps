package com.kkaovsp.boothorganizer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [AppUtils] — pure logic/utility functions.
 * These tests verify status-to-color mapping, color math, filename sanitization,
 * JSON helpers, and error body parsing WITHOUT any Compose UI or Activity involvement.
 */
class AppUtilsTest {

    // ========================================================================
    // reservationColor()
    // ========================================================================

    @Test
    fun reservationColor_pendingPayment_returnsWarning() {
        assertEquals(AppUtils.COLOR_WARNING, AppUtils.reservationColor("PENDING_PAYMENT"))
    }

    @Test
    fun reservationColor_waitingForApproval_returnsSecondary() {
        assertEquals(AppUtils.COLOR_SECONDARY, AppUtils.reservationColor("WAITING_FOR_APPROVAL"))
    }

    @Test
    fun reservationColor_confirmed_returnsSuccess() {
        assertEquals(AppUtils.COLOR_SUCCESS, AppUtils.reservationColor("CONFIRMED"))
    }

    @Test
    fun reservationColor_cancelled_returnsDanger() {
        assertEquals(AppUtils.COLOR_DANGER, AppUtils.reservationColor("CANCELLED"))
    }

    @Test
    fun reservationColor_unknownStatus_returnsMuted() {
        assertEquals(AppUtils.COLOR_TEXT_MUTED, AppUtils.reservationColor("SOME_RANDOM_STATUS"))
    }

    @Test
    fun reservationColor_emptyString_returnsMuted() {
        assertEquals(AppUtils.COLOR_TEXT_MUTED, AppUtils.reservationColor(""))
    }

    // ========================================================================
    // paymentColor()
    // ========================================================================

    @Test
    fun paymentColor_approved_returnsSuccess() {
        assertEquals(AppUtils.COLOR_SUCCESS, AppUtils.paymentColor("APPROVED"))
    }

    @Test
    fun paymentColor_pending_returnsWarning() {
        assertEquals(AppUtils.COLOR_WARNING, AppUtils.paymentColor("PENDING"))
    }

    @Test
    fun paymentColor_rejected_returnsDanger() {
        assertEquals(AppUtils.COLOR_DANGER, AppUtils.paymentColor("REJECTED"))
    }

    @Test
    fun paymentColor_unknownStatus_returnsMuted() {
        assertEquals(AppUtils.COLOR_TEXT_MUTED, AppUtils.paymentColor("UNKNOWN"))
    }

    // ========================================================================
    // approvalColor()
    // ========================================================================

    @Test
    fun approvalColor_approved_returnsSuccess() {
        assertEquals(AppUtils.COLOR_SUCCESS, AppUtils.approvalColor("APPROVED"))
    }

    @Test
    fun approvalColor_rejected_returnsDanger() {
        assertEquals(AppUtils.COLOR_DANGER, AppUtils.approvalColor("REJECTED"))
    }

    @Test
    fun approvalColor_pending_returnsWarning() {
        assertEquals(AppUtils.COLOR_WARNING, AppUtils.approvalColor("PENDING"))
    }

    @Test
    fun approvalColor_unknownStatus_returnsMuted() {
        assertEquals(AppUtils.COLOR_TEXT_MUTED, AppUtils.approvalColor(""))
    }

    // ========================================================================
    // lighten()
    // ========================================================================

    @Test
    fun lighten_black_returnsLightGray() {
        val black = (0xFF shl 24) or 0 // ARGB black
        val result = AppUtils.lighten(black)
        val r = (result shr 16) and 0xFF
        val g = (result shr 8) and 0xFF
        val b = result and 0xFF
        // 0 + (255 - 0) * 0.86 = 219
        assertEquals(219, r)
        assertEquals(219, g)
        assertEquals(219, b)
    }

    @Test
    fun lighten_white_staysWhite() {
        val white = (0xFF shl 24) or (255 shl 16) or (255 shl 8) or 255
        val result = AppUtils.lighten(white)
        val r = (result shr 16) and 0xFF
        val g = (result shr 8) and 0xFF
        val b = result and 0xFF
        assertEquals(255, r)
        assertEquals(255, g)
        assertEquals(255, b)
    }

    @Test
    fun lighten_resultIsLighterThanInput() {
        val color = AppUtils.COLOR_DANGER // rgb(239,68,68)
        val result = AppUtils.lighten(color)
        val origR = (color shr 16) and 0xFF
        val resultR = (result shr 16) and 0xFF
        assertTrue("Lightened red channel should be >= original", resultR >= origR)
    }

    // ========================================================================
    // safeFileName()
    // ========================================================================

    @Test
    fun safeFileName_normalText_returnsLowercase() {
        assertEquals("hello_world", AppUtils.safeFileName("Hello World"))
    }

    @Test
    fun safeFileName_specialCharacters_replacedWithUnderscores() {
        assertEquals("test_file_2024", AppUtils.safeFileName("Test@File#2024"))
    }

    @Test
    fun safeFileName_preservesThaiCharacters() {
        val result = AppUtils.safeFileName("ตลาดนัด123")
        assertTrue("Should contain Thai chars", result.contains("ตลาดนัด"))
        assertTrue("Should contain digits", result.contains("123"))
    }

    @Test
    fun safeFileName_emptyString_returnsEvent() {
        assertEquals("event", AppUtils.safeFileName(""))
    }

    @Test
    fun safeFileName_onlySpecialChars_returnsEvent() {
        assertEquals("event", AppUtils.safeFileName("@#\$%^&*"))
    }

    @Test
    fun safeFileName_leadingAndTrailingSpecialChars_trimmed() {
        assertEquals("test", AppUtils.safeFileName("---test---"))
    }

    @Test
    fun safeFileName_mixedInput_producesCleanName() {
        assertEquals("booth_event_2024_งาน", AppUtils.safeFileName("Booth Event 2024 - งาน!"))
    }

    // ========================================================================
    // jsonClean()
    // ========================================================================

    @Test
    fun jsonClean_existingKey_returnsValue() {
        val json = JSONObject().put("name", "Test Event")
        assertEquals("Test Event", AppUtils.jsonClean(json, "name"))
    }

    @Test
    fun jsonClean_missingKey_returnsEmptyString() {
        val json = JSONObject()
        assertEquals("", AppUtils.jsonClean(json, "missing_key"))
    }

    @Test
    fun jsonClean_nullValue_returnsEmptyString() {
        val json = JSONObject().put("name", JSONObject.NULL)
        assertEquals("", AppUtils.jsonClean(json, "name"))
    }

    @Test
    fun jsonClean_emptyStringValue_returnsEmptyString() {
        val json = JSONObject().put("name", "")
        assertEquals("", AppUtils.jsonClean(json, "name"))
    }

    @Test
    fun jsonClean_numericValue_returnsStringRepresentation() {
        val json = JSONObject().put("count", 42)
        assertEquals("42", AppUtils.jsonClean(json, "count"))
    }

    // ========================================================================
    // jsonOptBooleanLike()
    // ========================================================================

    @Test
    fun jsonOptBooleanLike_booleanTrue_returnsTrue() {
        val json = JSONObject().put("active", true)
        assertTrue(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_booleanFalse_returnsFalse() {
        val json = JSONObject().put("active", false)
        assertFalse(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_numberOne_returnsTrue() {
        val json = JSONObject().put("active", 1)
        assertTrue(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_numberZero_returnsFalse() {
        val json = JSONObject().put("active", 0)
        assertFalse(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_stringTrue_returnsTrue() {
        val json = JSONObject().put("active", "true")
        assertTrue(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_stringOne_returnsTrue() {
        val json = JSONObject().put("active", "1")
        assertTrue(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_stringFalse_returnsFalse() {
        val json = JSONObject().put("active", "false")
        assertFalse(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    @Test
    fun jsonOptBooleanLike_missingKey_returnsFalse() {
        val json = JSONObject()
        assertFalse(AppUtils.jsonOptBooleanLike(json, "missing"))
    }

    @Test
    fun jsonOptBooleanLike_nullValue_returnsFalse() {
        val json = JSONObject().put("active", JSONObject.NULL)
        assertFalse(AppUtils.jsonOptBooleanLike(json, "active"))
    }

    // ========================================================================
    // parseErrorBody()
    // ========================================================================

    @Test
    fun parseErrorBody_blankBody_returnsFallback() {
        assertEquals("Server Error", AppUtils.parseErrorBody("", "Server Error"))
    }

    @Test
    fun parseErrorBody_jsonWithError_returnsErrorField() {
        val body = """{"error": "Invalid credentials"}"""
        assertEquals("Invalid credentials", AppUtils.parseErrorBody(body, "fallback"))
    }

    @Test
    fun parseErrorBody_jsonWithDetail_returnsDetailField() {
        val body = """{"detail": "Not found"}"""
        assertEquals("Not found", AppUtils.parseErrorBody(body, "fallback"))
    }

    @Test
    fun parseErrorBody_jsonWithMessage_returnsMessageField() {
        val body = """{"message": "Timeout"}"""
        assertEquals("Timeout", AppUtils.parseErrorBody(body, "fallback"))
    }

    @Test
    fun parseErrorBody_invalidJson_returnsRawBody() {
        val body = "Something went wrong"
        assertEquals("Something went wrong", AppUtils.parseErrorBody(body, "fallback"))
    }

    @Test
    fun parseErrorBody_jsonWithMultipleFields_prefersError() {
        val body = """{"error": "Main error", "detail": "Detail", "message": "Message"}"""
        assertEquals("Main error", AppUtils.parseErrorBody(body, "fallback"))
    }

    // ========================================================================
    // ApiException
    // ========================================================================

    @Test
    fun apiException_holdsStatusCodeAndMessage() {
        val exception = ApiException(404, "Not Found")
        assertEquals(404, exception.statusCode)
        assertEquals("Not Found", exception.message)
    }

    @Test
    fun apiException_isAnException() {
        val exception = ApiException(500, "Server Error")
        assertTrue(exception is Exception)
    }
}
