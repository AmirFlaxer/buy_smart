package com.amir.buysmart.data.remote

import com.amir.buysmart.BuildConfig
import com.amir.buysmart.domain.model.ShoppingLocation
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiLocationClassifier @Inject constructor() {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0f
                maxOutputTokens = 20
            },
            systemInstruction = content {
                text("ענה במילה אחת בלבד מהרשימה: סופר/ירקניה/מעדניה/מאפייה/בית_מרקחת/אחר")
            }
        )
    }

    suspend fun classify(itemName: String): ShoppingLocation? {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return null
        return try {
            val response = model.generateContent(itemName)
            val text = response.text?.trim()?.lowercase() ?: return null
            when {
                "ירקניה" in text || "ירקן" in text -> ShoppingLocation.GREENGROCER
                "מעדניה" in text || "קצב" in text -> ShoppingLocation.DELI
                "מאפייה" in text                   -> ShoppingLocation.BAKERY
                "בית_מרקחת" in text || "בית מרקחת" in text -> ShoppingLocation.PHARMACY
                "אחר" in text                      -> null
                else                               -> ShoppingLocation.SUPERMARKET
            }
        } catch (e: Exception) {
            null
        }
    }
}
