package com.fixvol.app.audio

data class PlaybackEvent(
    val id: String,
    val packageName: String?,
    val uid: Int?,
    val usage: Int?,
    val contentType: Int?,
    val category: AudioCategory,
    val state: PlaybackState,
    val audioRoute: AudioRoute = AudioRoute.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
)
