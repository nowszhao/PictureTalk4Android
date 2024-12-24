package `fun`.coda.app.picturetalk4android.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageAnalysisRepository(private val imageAnalysisDao: ImageAnalysisDao) {
    val allAnalyses: Flow<List<ImageAnalysisWithWords>> = imageAnalysisDao.getAllAnalyses()

    suspend fun insert(analysis: ImageAnalysisEntity, words: List<WordEntity>) {
        val id = imageAnalysisDao.insert(analysis)
        val wordsWithImageId = words.map { it.copy(image_id = id) }
        imageAnalysisDao.insertWords(wordsWithImageId)
    }

    suspend fun deleteAll() {
        imageAnalysisDao.deleteAll()
    }

    suspend fun updateWordOffsets(imageId: Long, word: String, offsetX: Float, offsetY: Float) {
        withContext(Dispatchers.IO) {
            imageAnalysisDao.updateWordOffsets(imageId, word, offsetX, offsetY)
        }
    }
} 