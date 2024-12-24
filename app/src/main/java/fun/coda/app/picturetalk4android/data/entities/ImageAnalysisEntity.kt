package `fun`.coda.app.picturetalk4android.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "image_analyses")
data class ImageAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val words: List<WordEntity>,
    val sentence: SentenceEntity,
    val createdAt: Date = Date()
)

data class WordEntity(
    val word: String? = null,
    val phoneticsymbols: String? = null,
    val explanation: String? = null,
    val location: String? = null
)

data class SentenceEntity(
    val english: String? = null,
    val chinese: String? = null
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromWordList(value: List<WordEntity>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWordList(value: String): List<WordEntity> {
        val listType = object : TypeToken<List<WordEntity>>() {}.type
        return gson.fromJson(value, listType)
    }

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