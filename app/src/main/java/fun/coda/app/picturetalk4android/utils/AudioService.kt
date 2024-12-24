package `fun`.coda.app.picturetalk4android.utils


import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast

class AudioService {
    companion object {
        // 添加 MediaPlayer 用于播放音频
        var mediaPlayer: MediaPlayer? = null

        // 添加播放音频方法
        internal fun playWordAudio(word: String) {
            try {
                // 释放之前的 MediaPlayer
                mediaPlayer?.release()

                // 创建新的 MediaPlayer
                mediaPlayer = MediaPlayer().apply {
                    val audioUrl = "https://dict.youdao.com/dictvoice?audio=${word.lowercase()}&type=2"
                    setDataSource(audioUrl)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                    }
                    setOnCompletionListener { mp ->
                        mp.reset()
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e("MainActivity", "播放音频失败: what=$what, extra=$extra")
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "设置音频源失败", e)

            }
        }
    }
}
