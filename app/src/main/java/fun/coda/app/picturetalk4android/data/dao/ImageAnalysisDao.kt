package `fun`.coda.app.picturetalk4android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import `fun`.coda.app.picturetalk4android.data.entities.ImageAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageAnalysisDao {
    @Query("SELECT * FROM image_analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<ImageAnalysisEntity>>

    @Insert
    suspend fun insert(analysis: ImageAnalysisEntity)

    @Query("DELETE FROM image_analyses")
    suspend fun deleteAll()
} 