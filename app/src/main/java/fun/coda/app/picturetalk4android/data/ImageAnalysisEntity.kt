package `fun`.coda.app.picturetalk4android.data

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "image_analyses")
data class ImageAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val sentence: SentenceEntity,
    val createdAt: Date = Date()
)

@Entity(
    tableName = "word_table",
    foreignKeys = [
        ForeignKey(
            entity = ImageAnalysisEntity::class,
            parentColumns = ["id"],
            childColumns = ["image_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("image_id")]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val image_id: Long,
    val word: String?,
    val phoneticsymbols: String?,
    val explanation: String?,
    val location: String?,
    var offset_x: Float = 0f,
    var offset_y: Float = 0f
)

data class ImageAnalysisWithWords(
    @Embedded val analysis: ImageAnalysisEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "image_id"
    )
    val words: List<WordEntity>
)

data class SentenceEntity(
    val english: String? = null,
    val chinese: String? = null
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSentence(value: SentenceEntity): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSentence(value: String): SentenceEntity {
        return gson.fromJson(value, SentenceEntity::class.java)
    }

    @TypeConverter
    fun fromDate(value: Date): Long {
        return value.time
    }

    @TypeConverter
    fun toDate(value: Long): Date {
        return Date(value)
    }
} 