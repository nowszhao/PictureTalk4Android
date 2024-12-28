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
        WHERE id = :wordId
    """)
    suspend fun updateWordOffsets(wordId: Long, offsetX: Float, offsetY: Float)

    @Query("UPDATE image_analyses SET status = :status WHERE id = :analysisId")
    suspend fun updateStatus(analysisId: Long, status: AnalysisStatus)

    @Query("""
        UPDATE image_analyses 
        SET sentence = :sentence, status = :status 
        WHERE id = :analysisId
    """)
    suspend fun updateAnalysis(analysisId: Long, sentence: SentenceEntity, status: AnalysisStatus)

    @Query("DELETE FROM image_analyses WHERE id = :analysisId")
    suspend fun deleteAnalysis(analysisId: Long)
} 