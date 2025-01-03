package `fun`.coda.app.picturetalk4android.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Date


data class Word(
    val word: String? = null,
    val phoneticsymbols: String? = null,
    val explanation: String? = null,
    val location: String? = null
)

data class Sentence(
    @SerializedName("text")
    val english: String? = null,
    @SerializedName("translation")
    val chinese: String? = null
)

data class AnalysisResponse(
    val words: List<Word>,
    val sentence: Sentence
)


// 添加数据模型
data class FileDetail(
    val id: String,
    val name: String,
    val parent_path: String = "",
    val type: String = "image",
    val size: Int,
    val status: String = "parsed",
    val extra_info: Map<String, Int> = mapOf(
        "width" to 0,
        "height" to 0
    ),
    val created_at: String = "0001-01-01T00:00:00Z",
    val updated_at: String = "0001-01-01T00:00:00Z",
    val content_type: String = "image/jpeg"
)

data class FileMeta(
    val width: String,
    val height: String
)

data class FileRef(
    val id: String,
    val name: String,
    val size: Int,
    val file: Map<String, Any> = emptyMap(),
    val upload_progress: Int = 100,
    val upload_status: String = "success",
    val parse_status: String = "success",
    val detail: FileDetail,
    val file_info: FileDetail,
    val done: Boolean = true
)

data class Message(
    val role: String,
    val content: String
)

data class ImageAnalysisRequest(
    val messages: List<Message>,
    val use_search: Boolean = true,
    val extend: Map<String, Boolean> = mapOf("sidebar" to true),
    val kimiplus_id: String = "kimi",
    val use_research: Boolean = false,
    val use_math: Boolean = false,
    val refs: List<String>,
    val refs_file: List<FileRef>
)

// 添加 PreSignedURLResponse 数据类
data class PreSignedURLResponse(
    val url: String,
    val object_name: String,
    val file_id: String
)

// 修改 FileDetailRequest 数据类
data class FileDetailRequest(
    val type: String = "image",
    val name: String,
    val file_id: String,
    val meta: FileMeta
)

data class FileDetailResponse(
    val id: String,
    val name: String,
    val type: String,
    val meta: FileMeta
)


@Entity(tableName = "image_analyses")
data class ImageAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val sentence: SentenceEntity,
    val createdAt: Date = Date(),
    val status: AnalysisStatus = AnalysisStatus.PROCESSING
)

// 添加状态枚举
enum class AnalysisStatus {
    PROCESSING,  // 解析中
    COMPLETED    // 已完成
}

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
    @ColumnInfo(name = "offset_x")
    val offset_x: Float? = 0f,
    @ColumnInfo(name = "offset_y")
    val offset_y: Float? = 0f
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

// 添加英语等级枚举
enum class EnglishLevel(val title: String, val description: String) {
    JUNIOR_HIGH("初中", "适合初中英语水平的学习者"),
    SENIOR_HIGH("高中", "适合高中英语水平的学习者"),
    CET4("大学四级", "适合大学英语四级水平的学习者"),
    CET6("大学六级", "适合大学英语六级水平的学习者"),
    IELTS("雅思/托福", "适合准备雅思或托福考试的学习者"),
    ADVANCED("专业/GRE", "适合英语专业或准备GRE考试的学习者");

    companion object {
        fun getDefault() = SENIOR_HIGH
    }
}