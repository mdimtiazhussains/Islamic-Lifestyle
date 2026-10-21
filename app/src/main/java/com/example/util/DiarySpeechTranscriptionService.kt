package com.example.util

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/**
 * Result of AI voice transcription and categorization.
 */
data class VoiceJournalTranscriptionResult(
    val title: String,
    val mindsetReflections: String,
    val gratitudeHighlights: String,
    val detectedLanguage: String, // "Roman Urdu" or "English"
    val isDemoOrMock: Boolean = false,
    val rawText: String = ""
)

/**
 * DiarySpeechTranscriptionService
 *
 * Uses Gemini AI multimodal capabilities to transcribe audio recordings
 * into Roman Urdu or English and extracts structured journal fields.
 */
object DiarySpeechTranscriptionService {
    private const val TAG = "DiarySpeechTranscribe"
    private const val MODEL_NAME = "gemini-flash-latest"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Transcribes an audio file into Roman Urdu or English and organizes it into:
     * - Reflection Title
     * - Mindset Reflections & Insights
     * - Gratitude & Positive Highlights
     */
    suspend fun transcribeVoiceReflection(
        audioFile: File,
        apiKey: String = BuildConfig.GEMINI_API_KEY
    ): Result<VoiceJournalTranscriptionResult> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Recorded audio file is missing or empty"))
        }

        val effectiveApiKey = apiKey.trim()
        val isKeyPlaceholder = effectiveApiKey.isBlank() ||
                effectiveApiKey == "MY_GEMINI_API_KEY" ||
                effectiveApiKey == "YOUR_GEMINI_API_KEY"

        if (isKeyPlaceholder) {
            Log.w(TAG, "Gemini API key is placeholder or empty. Generating sample Roman Urdu reflection.")
            return@withContext Result.success(createDemoTranscriptionResult())
        }

        try {
            // Read audio file bytes and convert to Base64
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Construct Gemini multimodal request with audio inlineData
            val promptText = """
                You are an expert bilingual speech recognition and daily journal assistant.
                Carefully listen to this voice journal recording and extract user reflections following these strict rules:

                1. CRITICAL LANGUAGE RULE:
                   - If the spoken audio is in Urdu, Hindi, or a mix of Urdu and English (Roman Urdu / Hinglish), transcribe and formulate the text STRICTLY IN ROMAN URDU (using the English/Latin alphabet, e.g. 'Aaj ka din bohot acha raha, subah jaldi uth kar namaz parhi aur deep work pe focus kiya').
                   - If the audio is spoken completely or predominantly in English, transcribe and format the text in English.
                   - NEVER output Arabic/Perso-Arabic Urdu script (اردو) or Devanagari script. Use strictly Latin/English characters (Roman Urdu or English).

                2. FIELD POPULATION:
                   Extract the thoughts into three fields:
                   - "title": A concise reflection title (3 to 6 words) in Roman Urdu or English.
                   - "mindsetReflections": The core thoughts, reflections, activities, mindset insights, emotional state, or lessons described (in Roman Urdu or English).
                   - "gratitudeHighlights": Things the user expressed gratitude, thanks, or appreciation for (e.g. blessings, achievements, support, health, 'Allah ka shukar'). If not explicitly stated, extract a positive takeaway or blessing.
                   - "detectedLanguage": "Roman Urdu" if Urdu/Hindi/mixed, or "English" if English.

                3. OUTPUT FORMAT:
                   Return ONLY a valid JSON object matching this schema without any markdown backticks:
                   {
                     "title": "...",
                     "mindsetReflections": "...",
                     "gratitudeHighlights": "...",
                     "detectedLanguage": "Roman Urdu"
                   }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Audio part
                val audioPart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "audio/m4a")
                        put("data", base64Audio)
                    }
                    put("inlineData", inlineData)
                }
                partsArray.put(audioPart)

                // Prompt part
                val textPart = JSONObject().apply {
                    put("text", promptText)
                }
                partsArray.put(textPart)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // Generation config for JSON output
                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$effectiveApiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext Result.failure(
                    Exception("AI transcription service error (${response.code}): ${response.message}")
                )
            }

            val parsedResult = parseGeminiResponse(responseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during voice transcription", e)
            Result.failure(e)
        }
    }

    /**
     * Parses the JSON response returned from Gemini API.
     */
    fun parseGeminiResponse(responseJsonStr: String): VoiceJournalTranscriptionResult {
        return try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            // Clean any potential markdown wrapping
            val cleanedText = textPart.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedJson = JSONObject(cleanedText)
            val title = parsedJson.optString("title", "").trim()
            val mindset = parsedJson.optString("mindsetReflections", "").trim()
            val gratitude = parsedJson.optString("gratitudeHighlights", "").trim()
            val detectedLang = parsedJson.optString("detectedLanguage", "Roman Urdu").trim()

            VoiceJournalTranscriptionResult(
                title = title.ifBlank { "Daily Reflection" },
                mindsetReflections = mindset.ifBlank { cleanedText },
                gratitudeHighlights = gratitude.ifBlank { "Shukar alhamdulillah for today's blessings." },
                detectedLanguage = if (detectedLang.contains("English", ignoreCase = true)) "English" else "Roman Urdu",
                isDemoOrMock = false,
                rawText = cleanedText
            )
        } catch (_: Throwable) {
            VoiceJournalTranscriptionResult(
                title = "Voice Reflection",
                mindsetReflections = responseJsonStr.take(300),
                gratitudeHighlights = "Allah ka shukar har haal mein.",
                detectedLanguage = "Roman Urdu",
                isDemoOrMock = false,
                rawText = responseJsonStr
            )
        }
    }

    /**
     * Provides a sample demonstration result strictly in Roman Urdu if the API key
     * is not yet configured in the Secrets panel.
     */
    fun createDemoTranscriptionResult(): VoiceJournalTranscriptionResult {
        return VoiceJournalTranscriptionResult(
            title = "Pur-Aman Sham Aur Wazahat",
            mindsetReflections = "Aaj ka din bohot behtareen raha. Subah time par Fajr ada ki aur rozana ke schedule ke mutabiq 15-minute slot blocks par poora amal kiya. Deep work session ke doran project ka core structure mukammal ho gaya. Dimagh bohot mutmain aur pur-sukoon mehsoos kar raha hai.",
            gratitudeHighlights = "Alhamdulillah sehat, waqt ki barkat aur har mushkil mein madad milne par be-hisaab shukar ada karta hoon.",
            detectedLanguage = "Roman Urdu",
            isDemoOrMock = true,
            rawText = "Demo Roman Urdu voice reflection transcription"
        )
    }
}
