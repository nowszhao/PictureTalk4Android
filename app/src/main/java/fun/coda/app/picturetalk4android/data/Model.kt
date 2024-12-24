import android.net.Uri
import com.google.gson.annotations.SerializedName

// 在 MainActivity.kt 顶部添加数据模型
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

data class SSEEvent(
    val event: String,
    val data: String? = null
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

// 添加新的数据类
data class ImageAnalysis(
    val uri: Uri,
    val analysis: AnalysisResponse
)