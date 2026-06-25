package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object VeevoTechSmsGateway {
    // --- VeevoTech V3 Live API Configurations ---
    private const val VEEVOTECH_HASH = "3100d3674c6a6d3de27ce74ddbde0188"
    private const val VEEVOTECH_ENDPOINT_URL = "https://api.veevotech.com/v3/sendsms"

    private val client = OkHttpClient()

    /**
     * Sends an OTP SMS via VeevoTech V3 API to Pakistani mobile numbers.
     * @param phone Pakistani phone number in format 03XXXXXXXXX, 92XXXXXXXXXX, or +92XXXXXXXXXX
     * @param otp 6-digit OTP code to send
     */
    suspend fun sendOtpSms(phone: String, otp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Dynamic formatting to +92XXXXXXXXXX format
            val formattedPhone = when {
                phone.startsWith("+92") -> phone
                phone.startsWith("92") -> "+$phone"
                phone.startsWith("03") -> "+92" + phone.substring(1)
                else -> "+92" + phone.trimStart('0')
            }

            val messageText = "Your TaskMallah Verification Code is: $otp. Do not share this code with anyone."

            // Create JSON Payload
            val jsonPayload = JSONObject().apply {
                put("hash", VEEVOTECH_HASH)
                put("receivernum", formattedPhone)
                put("textmessage", messageText)
                put("sendernum", "Default")
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(VEEVOTECH_ENDPOINT_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d("VeevoTechSms", "SMS V3 sent successfully to $formattedPhone. Response: $bodyString")
                    Result.success(true)
                } else {
                    Log.e("VeevoTechSms", "Failed to send SMS V3. Code: ${response.code}, Response: $bodyString")
                    Result.failure(Exception("VeevoTech V3 HTTP error code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("VeevoTechSms", "Exception during VeevoTech V3 SMS dispatch: ${e.message}", e)
            Result.failure(e)
        }
    }
}
