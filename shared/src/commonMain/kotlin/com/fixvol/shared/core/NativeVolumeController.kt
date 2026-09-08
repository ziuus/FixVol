package com.fixvol.shared.core

import com.fixvol.shared.audio.AudioCategory

interface NativeVolumeController {
    /**
     * Requests the OS to show the native volume panel/UI.
     * @return true if successfully requested.
     */
    fun requestVolumeUi(category: AudioCategory): Boolean

    /**
     * Checks if the device/OS allows showing the native panel without changing volume.
     * Some OS versions or OEM skins might not support it perfectly.
     */
    fun testNativeVolumePanel(): Boolean
}
