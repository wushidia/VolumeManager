package moe.chensi.volume.data

import android.media.AudioPlaybackConfiguration
import java.lang.reflect.Method

data class Player(val config: AudioPlaybackConfiguration, val playerProxy: Any) {
    companion object {
        private val setVolumeMethod: Method =
            Class.forName("android.media.PlayerProxy").getDeclaredMethod(
                "setVolume", Float::class.javaPrimitiveType
            )
    }

    fun applyVolume(value: Float) {
        setVolumeMethod.invoke(playerProxy, value)
    }
}