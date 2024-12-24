package `fun`.coda.app.picturetalk4android.data

import kotlinx.coroutines.flow.Flow

class ImageAnalysisRepository(private val imageAnalysisDao: ImageAnalysisDao) {
    val allAnalyses: Flow<List<ImageAnalysisEntity>> = imageAnalysisDao.getAllAnalyses()

    suspend fun insert(analysis: ImageAnalysisEntity) {
        imageAnalysisDao.insert(analysis)
    }

    suspend fun deleteAll() {
        imageAnalysisDao.deleteAll()
    }
} 