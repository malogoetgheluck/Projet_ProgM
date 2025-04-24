package com.example.projet_progm

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class MusicPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    private var sharedPref: SharedPreferences

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        sharedPref = context.getSharedPreferences("BTDPrefs", Context.MODE_PRIVATE)
    }

    /** MUSIC **/

    fun playMusic(resId: Int, looping: Boolean = true) {
        val musicEnabled = sharedPref.getBoolean("music_enabled", true)
        if (musicEnabled) {
            stopMusic()
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.isLooping = looping
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        val musicEnabled = sharedPref.getBoolean("music_enabled", true)
        if (musicEnabled) {
            if (mediaPlayer == null) {
                playMusic(R.raw.homepage)
            } else {
                mediaPlayer?.start()
            }
        }
    }

    /** SFX **/

    fun loadSound(name: String, resId: Int) {
        val soundId = soundPool.load(context, resId, 1)
        soundMap[name] = soundId
    }

    fun playSound(name: String) {
        val sfxEnabled = sharedPref.getBoolean("sound_enabled", true)
        if (sfxEnabled) {
            soundMap[name]?.let { soundId ->
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        soundPool.release()
    }
}
