package com.focusbridge.domain.model

data class DistractingApp(
    val packageName: String,
    val displayName: String,
    val iconBase64: String = "",
    val dailyLimitMs: Long,
    val isActive: Boolean = true,
    val cooldownMs: Long = DEFAULT_COOLDOWN_MS
) {
    companion object {
        const val DEFAULT_COOLDOWN_MS = 30 * 60 * 1000L // 30 minutes

        val SUGGESTED_PACKAGES = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",   // TikTok
            "com.ss.android.ugc.trill",   // TikTok (some regions)
            "com.google.android.youtube",
            "com.twitter.android",
            "com.X.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "com.snapchat.android"
        )
    }
}
