package com.seventyfiveperc.app.speech

import java.util.Locale

/**
 * NameDetector — Fuzzy matching engine for detecting names and roll numbers
 * in transcribed speech text.
 *
 * Supports:
 * - Exact match (case-insensitive)
 * - Fuzzy match (Levenshtein distance)
 * - Phonetic match (Soundex)
 * - Number word detection ("twenty three" → 23)
 */
class NameDetector {

    data class DetectionResult(
        val matched: Boolean,
        val matchedKeyword: String,
        val confidence: Float,    // 0.0 to 1.0
        val matchedText: String,  // the part of transcript that matched
        val isPreAlert: Boolean = false // true if it's the person BEFORE the user
    )

    private var keywords: List<String> = emptyList()
    private var preAlertKeywords: List<String> = emptyList()
    private var sensitivity: Float = 0.7f  // 0.0 = very loose, 1.0 = exact only

    // Number words mapping for roll call detection
    private val numberWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3",
        "four" to "4", "five" to "5", "six" to "6", "seven" to "7",
        "eight" to "8", "nine" to "9", "ten" to "10", "eleven" to "11",
        "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
        "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20",
        "thirty" to "30", "forty" to "40", "fifty" to "50",
        "sixty" to "60", "seventy" to "70", "eighty" to "80", "ninety" to "90",
        "hundred" to "100"
    )

    fun setKeywords(name: String, rollNumber: String, extraKeywords: String) {
        val allKeywords = mutableListOf<String>()
        val preKeywords = mutableListOf<String>()

        if (name.isNotBlank()) {
            val normalizedName = name.trim().lowercase(Locale.ROOT)
            allKeywords.add(normalizedName)
            
            // Add individual parts of the name (e.g., "Rahul" from "Rahul Sharma")
            val parts = normalizedName.split(" ")
            if (parts.size > 1) {
                parts.forEach { part ->
                    if (part.length >= 3) allKeywords.add(part)
                }
            }
        }

        if (rollNumber.isNotBlank()) {
            val normalizedRoll = rollNumber.trim().lowercase(Locale.ROOT)
            allKeywords.add(normalizedRoll)
            
            // Also add without leading zeros: "007" → "7"
            val stripped = normalizedRoll.trimStart('0')
            if (stripped.isNotEmpty() && stripped != normalizedRoll) {
                allKeywords.add(stripped)
            }

            // Next Person logic: Detect the person BEFORE the user
            val rollInt = normalizedRoll.toIntOrNull() ?: stripped.toIntOrNull()
            if (rollInt != null && rollInt > 1) {
                val prev = (rollInt - 1).toString()
                preKeywords.add(prev)
                // Add padded version if original was padded (e.g., "022" if user is "023")
                if (normalizedRoll.startsWith("0")) {
                    preKeywords.add(prev.padStart(normalizedRoll.length, '0'))
                }
            }
        }

        if (extraKeywords.isNotBlank()) {
            extraKeywords.split(",").forEach { kw ->
                val trimmed = kw.trim().lowercase(Locale.ROOT)
                if (trimmed.isNotEmpty()) allKeywords.add(trimmed)
            }
        }

        keywords = allKeywords.distinct()
        preAlertKeywords = preKeywords.distinct()
    }

    fun setSensitivity(level: String) {
        sensitivity = when (level.lowercase(Locale.ROOT)) {
            "low" -> 0.5f
            "high" -> 0.9f
            else -> 0.7f  // medium
        }
    }

    /**
     * Analyze transcribed text for any matching keywords.
     */
    fun analyze(transcript: String): DetectionResult {
        if (transcript.isBlank()) {
            return DetectionResult(false, "", 0f, "")
        }

        val normalizedTranscript = transcript.lowercase(Locale.ROOT).trim()
        val transcriptWithNumbers = convertNumberWords(normalizedTranscript)

        // 1. Check main keywords first (Exact & Fuzzy)
        checkMatches(normalizedTranscript, transcriptWithNumbers, keywords, false)?.let { return it }

        // 2. Check phonetic matches for names (more robust to noise)
        if (sensitivity < 0.9f) { // Only do phonetic if not on "High" sensitivity
            checkPhoneticMatches(normalizedTranscript, keywords, false)?.let { return it }
        }

        // 3. Check pre-alert keywords (person before)
        checkMatches(normalizedTranscript, transcriptWithNumbers, preAlertKeywords, true)?.let { return it }

        return DetectionResult(false, "", 0f, "")
    }

    private fun checkMatches(
        normalizedTranscript: String,
        transcriptWithNumbers: String,
        targetKeywords: List<String>,
        isPreAlert: Boolean
    ): DetectionResult? {
        val words = normalizedTranscript.split(Regex("\\s+"))
        val numWords = transcriptWithNumbers.split(Regex("\\s+"))
        val allTranscriptWords = (words + numWords).distinct()

        for (keyword in targetKeywords) {
            // Substring match in full transcript
            if (normalizedTranscript.contains(keyword) || transcriptWithNumbers.contains(keyword)) {
                return DetectionResult(
                    matched = true,
                    matchedKeyword = keyword,
                    confidence = 1.0f,
                    matchedText = keyword,
                    isPreAlert = isPreAlert
                )
            }

            // Fuzzy match word by word
            for (word in allTranscriptWords) {
                if (word.length < 2) continue
                
                val distance = levenshteinDistance(word, keyword)
                val maxLen = maxOf(word.length, keyword.length)
                val similarity = 1.0f - (distance.toFloat() / maxLen.toFloat())

                if (similarity >= sensitivity) {
                    return DetectionResult(
                        matched = true,
                        matchedKeyword = keyword,
                        confidence = similarity,
                        matchedText = word,
                        isPreAlert = isPreAlert
                    )
                }
            }
        }
        return null
    }

    private fun checkPhoneticMatches(
        transcript: String,
        targetKeywords: List<String>,
        isPreAlert: Boolean
    ): DetectionResult? {
        val words = transcript.split(Regex("\\s+"))
        for (keyword in targetKeywords) {
            if (keyword.length < 3) continue 
            
            val keywordSoundex = getSoundex(keyword)
            for (word in words) {
                if (word.length < 3) continue
                if (getSoundex(word) == keywordSoundex) {
                    return DetectionResult(
                        matched = true,
                        matchedKeyword = keyword,
                        confidence = 0.85f,
                        matchedText = word,
                        isPreAlert = isPreAlert
                    )
                }
            }
        }
        return null
    }

    /**
     * Improved Soundex implementation.
     */
    private fun getSoundex(s: String): String {
        if (s.isEmpty()) return ""
        val out = StringBuilder()
        val upper = s.uppercase(Locale.ROOT)
        out.append(upper[0])

        val codes = mapOf(
            'B' to '1', 'F' to '1', 'P' to '1', 'V' to '1',
            'C' to '2', 'G' to '2', 'J' to '2', 'K' to '2', 'Q' to '2', 'S' to '2', 'X' to '2', 'Z' to '2',
            'D' to '3', 'T' to '3',
            'L' to '4',
            'M' to '5', 'N' to '5',
            'R' to '6'
        )
        
        var lastCode = codes[upper[0]] ?: '0'
        
        for (i in 1 until upper.length) {
            val char = upper[i]
            if (char == 'H' || char == 'W') continue
            
            val code = codes[char] ?: '0'
            if (code != '0' && code != lastCode) {
                out.append(code)
            }
            
            // Vowels reset the lastCode block
            if (code != '0' || (char in "AEIOUY")) {
                lastCode = code
            }
            
            if (out.length == 4) break
        }
        
        while (out.length < 4) out.append('0')
        return out.toString()
    }

    /**
     * Convert spoken number words to digits.
     */
    private fun convertNumberWords(text: String): String {
        var result = text
        val tensWords = listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        val onesWords = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

        for (tens in tensWords) {
            for (ones in onesWords) {
                val tensVal = numberWords[tens]?.toIntOrNull() ?: continue
                val onesVal = numberWords[ones]?.toIntOrNull() ?: continue
                val combined = (tensVal + onesVal).toString()
                result = result.replace("$tens $ones", combined)
                result = result.replace("$tens-$ones", combined)
            }
        }

        for ((word, digit) in numberWords) {
            result = result.replace(Regex("\\b$word\\b"), digit)
        }

        return result
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}
