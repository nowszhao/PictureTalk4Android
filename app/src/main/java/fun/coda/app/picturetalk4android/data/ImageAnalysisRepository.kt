package `fun`.coda.app.picturetalk4android.data.repository

import `fun`.coda.app.picturetalk4android.data.dao.ImageAnalysisDao
import `fun`.coda.app.picturetalk4android.data.entities.ImageAnalysisEntity
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