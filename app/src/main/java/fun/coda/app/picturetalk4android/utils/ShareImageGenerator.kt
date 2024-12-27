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
            val width = 1440  // 增加到 1440
            val height = (width * 16f / 9f).toInt()
            
            // 使用 ARGB_8888 配置创建位图
            val resultBitmap = Bitmap.createBitmap(
                width, 
                height, 
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(resultBitmap)

            // 1. 绘制背景图片
            drawImage(canvas, originalBitmap, width, height)
            
            onProgress(0.4f)

            // 2. 绘制渐变背景(用于句子区域)
            drawGradientBackground(canvas, width, height)
            
            onProgress(0.6f)

            // 3. 绘制单词卡片
            drawWordCards(canvas, analysis, width, height)
            
            onProgress(0.8f)

            // 4. 绘制句子
            drawSentence(canvas, analysis, width, height)

            // 5. 添加水印
            drawWatermark(canvas, width, height)

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
        // 计算缩放比例，保持原始宽高比
        val scale = Math.max(
            width.toFloat() / bitmap.width,
            height.toFloat() / bitmap.height
        )
        
        // 计算居中位置
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        
        // 创建矩阵进行变换
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(left, top)
        }
        
        // 使用高质量的绘制选项
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun drawGradientBackground(canvas: Canvas, width: Int, height: Int) {
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.7f,
                0f, height.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#B3000000")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.7f, width.toFloat(), height.toFloat(), gradientPaint)
    }

    private fun drawWordCards(canvas: Canvas, analysis: ImageAnalysisWithWords, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 40f
        }

        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.YELLOW
            alpha = 230 // 设置透明度
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#20000000"))
        }

        analysis.words.forEach { word ->
            val coordinates = word.location?.split(",")?.map { it.trim().toFloat() } ?: listOf(0f, 0f)
            val xPos = coordinates[0] * width + (word.offset_x ?: 0f)
            val yPos = coordinates[1] * height + (word.offset_y ?: 0f)

            val cardWidth = 300f
            val cardHeight = 160f
            val rect = RectF(
                xPos - cardWidth/2,
                yPos - cardHeight/2,
                xPos + cardWidth/2,
                yPos + cardHeight/2
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
                rect.left + WORD_CARD_PADDING,
                rect.top + 50f,
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
                rect.left + WORD_CARD_PADDING,
                rect.top + 90f,
                paint
            )

            // 绘制解释
            paint.apply {
                color = Color.DKGRAY
                textSize = 36f
            }
            canvas.drawText(
                word.explanation ?: "",
                rect.left + WORD_CARD_PADDING,
                rect.top + 130f,
                paint
            )
        }
    }

    private fun drawSentence(canvas: Canvas, analysis: ImageAnalysisWithWords, width: Int, height: Int) {
        val paint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        // 英文句子
        paint.textSize = 40f
        val englishText = analysis.analysis.sentence.english ?: ""
        val englishLayout = StaticLayout.Builder.obtain(
            englishText,
            0,
            englishText.length,
            paint,
            (width - (PADDING * 2)).toInt()
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        
        canvas.save()
        canvas.translate(PADDING, height - englishLayout.height - 150f)
        englishLayout.draw(canvas)
        canvas.restore()

        // 中文翻译
        paint.textSize = 36f
        paint.alpha = 204 // 80% opacity
        val chineseText = analysis.analysis.sentence.chinese ?: ""
        val chineseLayout = StaticLayout.Builder.obtain(
            chineseText,
            0,
            chineseText.length,
            paint,
            (width - (PADDING * 2)).toInt()
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(PADDING, height - chineseLayout.height - 80f)
        chineseLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
            color = Color.WHITE
            alpha = 128
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
} 