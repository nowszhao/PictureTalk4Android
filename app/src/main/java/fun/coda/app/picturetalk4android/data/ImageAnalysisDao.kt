package `fun`.coda.app.picturetalk4android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageAnalysisDao {
    @Transaction
    @Query("SELECT * FROM image_analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<ImageAnalysisWithWords>>

    @Insert
    suspend fun insert(analysis: ImageAnalysisEntity): Long

    @Insert
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM image_analyses")
    suspend fun deleteAll()

    @Query("""
        UPDATE word_table 
        SET offset_x = :offsetX, offset_y = :offsetY 
        WHERE image_id = :imageId AND word = :word
    """)
    suspend fun updateWordOffsets(imageId: Long, word: String, offsetX: Float, offsetY: Float)
} 