package com.focusbridge.domain.model

data class NextAction(
    val id: Long = 0,
    val goalId: Long,
    val label: String,
    val type: NextActionType,
    val target: String,
    val sortOrder: Int = 0
)

enum class NextActionType {
    URL,        // Generic URL → open in browser
    YOUTUBE,    // YouTube video/playlist → YouTube app with browser fallback
    SPOTIFY,    // Spotify track/playlist → Spotify app with browser fallback
    APP_INTENT, // Launch a specific app by its package name
    NOTES,      // Internal notes screen
    CHECKLIST   // Internal checklist screen
}
