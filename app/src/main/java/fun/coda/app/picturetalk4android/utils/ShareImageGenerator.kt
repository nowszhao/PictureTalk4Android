package `fun`.coda.app.picturetalk4android.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import `fun`.coda.app.picturetalk4android.R
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisWithWords
import java.io.File
import java.io.FileOutputStream

class ShareImageGenerator(private val context: Context) {
    companion object {
        private const val PADDING = 40f
        private const val WORD_CARD_PADDING = 16f
        private const val WORD_CARD_CORNER = 12f
        private const val TEXT_PADDING = 20f
    }

    fun generateShareImage(
        analysis: ImageAnalysisWithWords,
        onProgress: (Float) -> Unit = {},
        onComplete: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // 创建缓存目录
            val cacheDir = File(context.cacheDir, "shares").apply { 
                if (!exists()) mkdirs() 
            }
            
            // 加载原始图片
            val originalBitmap = context.contentResolver.openInputStream(Uri.parse(analysis.analysis.imageUri))?.use {
                BitmapFactory.decodeStream(it)
            } ?: throw IllegalStateException("无法加载图片")
            
            onProgress(0.2f)

            // 计算布局尺寸
            val width = 1080 // 固定宽度
            val imageHeight = (width * 16f / 9f).toInt() // 16:9 比例
            val wordCardsHeight = calculateWordCardsHeight(analysis, width)
            val sentenceHeight = calculateSentenceHeight(analysis, width)
            val totalHeight = imageHeight + wordCardsHeight + sentenceHeight + (PADDING * 3).toInt()

            // 创建最终位图
            val resultBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 绘制背景
            canvas.drawColor(Color.WHITE)
            
            onProgress(0.4f)

            // 绘制图片
            drawImage(canvas, originalBitmap, width, imageHeight)
            
            onProgress(0.6f)

            // 绘制单词卡片
            var currentY = imageHeight + PADDING
            currentY = drawWordCards(canvas, analysis, width, currentY)
            
            onProgress(0.8f)

            // 绘制句子
            drawSentence(canvas, analysis, width, currentY + PADDING)

            // 添加水印
            drawWatermark(canvas, width, totalHeight)

            onProgress(0.9f)

            // 保存图片
            val outputFile = File(cacheDir, "share_${System.currentTimeMillis()}.png")
            FileOutputStream(outputFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 清理资源
            originalBitmap.recycle()
            resultBitmap.recycle()

            onProgress(1.0f)
            onComplete(outputFile)

        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun drawImage(canvas: Canvas, bitmap: Bitmap, width: Int, height: Int) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        scaledBitmap.recycle()
    }

    private fun drawWordCards(canvas: Canvas, analysis: ImageAnalysisWithWords, width: Int, startY: Float): Float {
        var currentY = startY
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 40f
        }

        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#20000000"))
        }

        analysis.words.forEach { word ->
            val cardHeight = 160f
            val rect = RectF(
                PADDING,
                currentY,
                width - PADDING,
                currentY + cardHeight
            )

            // 绘制卡片背景
            canvas.drawRoundRect(rect, WORD_CARD_CORNER, WORD_CARD_CORNER, cardPaint)

            // 绘制单词内容
            paint.apply {
                color = Color.BLACK
                textSize = 44f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(
                word.word ?: "",
                PADDING + WORD_CARD_PADDING,
                currentY + 50f,
                paint
            )

            // 绘制音标
            paint.apply {
                color = Color.GRAY
                textSize = 32f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(
                word.phoneticsymbols ?: "",
                PADDING + WORD_CARD_PADDING,
                currentY + 90f,
                paint
            )

            // 绘制解释
            paint.apply {
                color = Color.DKGRAY
                textSize = 36f
            }
            canvas.drawText(
                word.explanation ?: "",
                PADDING + WORD_CARD_PADDING,
                currentY + 130f,
                paint
            )

            currentY += cardHeight + PADDING
        }

        return currentY
    }

    private fun drawSentence(canvas: Canvas, analysis: ImageAnalysisWithWords, width: Int, startY: Float) {
        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = 40f
            color = Color.BLACK
        }

        // 英文句子
        val englishText = analysis.analysis.sentence.english ?: ""
        val englishLayout = StaticLayout(
            englishText,
            paint,
            (width - (PADDING * 2)).toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )
        
        canvas.save()
        canvas.translate(PADDING, startY)
        englishLayout.draw(canvas)
        canvas.restore()

        // 中文翻译
        paint.apply {
            textSize = 36f
            color = Color.GRAY
        }
        
        val chineseText = analysis.analysis.sentence.chinese ?: ""
        val chineseLayout = StaticLayout(
            chineseText,
            paint,
            (width - (PADDING * 2)).toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )

        canvas.save()
        canvas.translate(PADDING, startY + englishLayout.height + TEXT_PADDING)
        chineseLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
            color = Color.parseColor("#80000000")
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val watermark = "图语 PictureTalk"
        canvas.drawText(
            watermark,
            width - paint.measureText(watermark) - PADDING,
            height - PADDING,
            paint
        )
    }

    private fun calculateWordCardsHeight(analysis: ImageAnalysisWithWords, width: Int): Int {
        return ((160f + PADDING) * analysis.words.size).toInt()
    }

    private fun calculateSentenceHeight(analysis: ImageAnalysisWithWords, width: Int): Int {
        val paint = TextPaint().apply {
            textSize = 40f
        }

        val englishLayout = StaticLayout(
            analysis.analysis.sentence.english ?: "",
            paint,
            (width - (PADDING * 2)).toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )

        paint.textSize = 36f
        val chineseLayout = StaticLayout(
            analysis.analysis.sentence.chinese ?: "",
            paint,
            (width - (PADDING * 2)).toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )

        return englishLayout.height + chineseLayout.height + (TEXT_PADDING * 2).toInt()
    }
} 