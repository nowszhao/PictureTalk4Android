package `fun`.coda.app.picturetalk4android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import `fun`.coda.app.picturetalk4android.ui.theme.PictureTalk4AndroidTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.yalantis.ucrop.UCrop
import java.util.UUID
import androidx.compose.runtime.Composable
import android.R.drawable as AndroidDrawable
import androidx.compose.ui.res.painterResource
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import okio.Buffer
import okio.buffer
import okio.source
import android.util.Log
import android.provider.MediaStore
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import java.util.concurrent.Executors
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import `fun`.coda.app.picturetalk4android.data.AppDatabase
import `fun`.coda.app.picturetalk4android.data.entities.ImageAnalysisEntity
import `fun`.coda.app.picturetalk4android.data.entities.SentenceEntity
import `fun`.coda.app.picturetalk4android.data.entities.WordEntity
import `fun`.coda.app.picturetalk4android.data.repository.ImageAnalysisRepository
import kotlinx.coroutines.flow.collect
import android.os.Environment
import okhttp3.MultipartBody
import java.io.FileOutputStream
import java.io.OutputStream
import okhttp3.RequestBody.Companion.asRequestBody
import android.graphics.BitmapFactory
import kotlin.math.roundToInt
import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext

enum class Screen(val title: String) {
    Home("首页"),
    Camera("开拍"),
    Profile("我的")
}

// 在 MainActivity.kt 顶部添加数据模型
data class Word(
    val word: String? = null,
    val phoneticsymbols: String? = null,
    val explanation: String? = null,
    val location: String? = null
)

data class Sentence(
    val english: String? = null,
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

class KimiService {
    companion object {
        private const val TAG = "KimiService"
        private const val AUTH_TOKEN = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ1c2VyLWNlbnRlciIsImV4cCI6MTczOTU0NzQ3OCwiaWF0IjoxNzMxNzcxNDc4LCJqdGkiOiJjc3Nib2xuZDBwODBpaGswYmIwMCIsInR5cCI6ImFjY2VzcyIsImFwcF9pZCI6ImtpbWkiLCJzdWIiOiJjb2ZzamI5a3FxNHR0cmdhaGhxZyIsInNwYWNlX2lkIjoiY29mc2piOWtxcTR0dHJnYWhocGciLCJhYnN0cmFjdF91c2VyX2lkIjoiY29mc2piOWtxcTR0dHJnYWhocDAifQ.fPEyGwA2GNsrBAPoBVJwGde6BSdRViykCodDOwDeyeabxIuAO8dtZZ8x9gsk9kxJyknfWZ1JG2pZOnMQbQmf9w"  // 替换你的实际 token
        private const val BASE_URL = "https://kimi.moonshot.cn/api"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getPreSignedURL(fileName: String): PreSignedURLResponse {
        Log.d(TAG, "开始获取预签名URL: $fileName")
        val request = Request.Builder()
            .url("$BASE_URL/pre-sign-url")
            .post("""{"action":"image","name":"$fileName"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "获取预签名URL失败: $errorBody")
                    throw IOException("获取预签名URL失败: ${response.code}")
                }
                val responseBody = response.body!!.string()
                Log.d(TAG, "获取预签名URL成功: $responseBody")
                Gson().fromJson(responseBody, PreSignedURLResponse::class.java)
            }
        }
    }

    suspend fun uploadImage(url: String, imageData: ByteArray) {
        Log.d(TAG, "开始上传图片: $url")
        val request = Request.Builder()
            .url(url)
            .put(imageData.toRequestBody("image/jpeg".toMediaType()))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "上传图片失败: $errorBody")
                    throw IOException("上传图片失败: ${response.code}")
                }
                Log.d(TAG, "上传图片成功")
            }
        }
    }

    suspend fun createChat(): String {
        Log.d(TAG, "开始创建聊天会话")
        val request = Request.Builder()
            .url("$BASE_URL/chat")
            .post("""
                {
                    "name": "拍单词",
                    "isExample": false,
                    "enterMethod": "new_chat",
                    "kimiplusId": "kimi"
                }
            """.trimIndent().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "创建聊天会话失败: $errorBody")
                    throw IOException("创建聊天会话失败: ${response.code}")
                }
                val responseBody = response.body!!.string()
                Log.d(TAG, "创建聊天会话成功: $responseBody")
                JSONObject(responseBody).getString("id")
            }
        }
    }

    suspend fun analyzeImage(
        fileId: String,
        fileName: String,
        fileSize: Int,
        fileDetail: FileDetailResponse,
        chatId: String
    ): AnalysisResponse {
        Log.d(TAG, "��始分析图片: fileId=$fileId, fileName=$fileName, fileSize=$fileSize, chatId=$chatId")
        
        // 创建 FileRef
        val fileRef = FileRef(
            id = fileId,
            name = fileName,
            size = fileSize,
            detail = FileDetail(
                id = fileDetail.id,
                name = fileDetail.name,
                size = fileSize
            ),
            file_info = FileDetail(
                id = fileDetail.id,
                name = fileDetail.name,
                size = fileSize
            )
        )

        val prompt = """
            我作为一名英语习者。通过片进场化学习英语单词。请根据我提供的图片，分析并返回以下信息：
                1、单词
                  - 从图中提取常用的英语单词。
                  - 提供以下信息：
                        - 单词
                        - 音标，式
                        - 中文解释
                        - 单词所在的图片位置：包括 x 和 y 坐标（归一化到 0~1 范围，保留四位小数点）。
                   - 意：单词指示应标记物品中的一个具，单词之间的位置不要重叠。
                2、句子
                  - 使用一句最简单、确的英语描图片内容。
                  - 提供地道的中文翻译。
                  - 返回格式
                     - 请以 标准 JSON 格式 返回结果，下：
                        {
                            "words": [
                                {
                                    "word": "Stool",
                                    "phoneticsymbols": "/stuːl/",
                                    "explanation": "凳",
                                    "location": "0.55, 0.65"
                                },
                                ...
                            ],
                            "sentence": {
                                "text": "A green plastic stool stands on a wooden floor against a gray wall, near a light switch.",
                                "translation": "一个绿色的塑料凳子放在木地板上，靠色的墙上，一个灯关。"
                            }
                        }
        """.trimIndent()

        val analysisRequest = ImageAnalysisRequest(
            messages = listOf(Message("user", prompt)),
            refs = listOf(fileId),
            refs_file = listOf(fileRef)
        )

        val requestJson = Gson().toJson(analysisRequest)
        Log.d(TAG, "发送的请求体: $requestJson")

        // 创建请求
        val request = Request.Builder()
            .url("$BASE_URL/chat/$chatId/completion/stream")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .build()

        return withContext(Dispatchers.IO) {
            var result = StringBuilder()
            var isJsonStarted = false
            var jsonBraceCount = 0
            var totalEvents = 0
            var jsonEvents = 0
            
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Response: $response")
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "分析图片失败: $errorBody")
                    throw IOException("分析图片失败: ${response.code}")
                }
                
                val reader = response.body!!.charStream().buffered()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    totalEvents++
//                    Log.d(TAG, "收到原始据($totalEvents): $line")
                    
                    if (line?.startsWith("data: ") == true) {
                        val jsonString = line!!.substring(6)
//                        Log.d(TAG, "解析SSE据: $jsonString")
                        
                        try {
                            val event = JSONObject(jsonString)
                            val eventType = event.getString("event")
//                            Log.d(TAG, "事件类型: $eventType")
                            
                            when (eventType) {
                                "cmpl" -> {
                                    val text = event.optString("text", "")
                                    val loading = event.optBoolean("loading")
//                                    Log.d(TAG, "cmpl件: text='$text', loading=$loading")
                                    
                                    if (text.isNotEmpty()) {
                                        if (text.contains("{") && !isJsonStarted) {
                                            isJsonStarted = true
                                            jsonBraceCount = 1
                                            result.append("{")
                                            Log.d(TAG, "检测到JSON始")
                                            continue
                                        }
                                        
                                        if (isJsonStarted) {
                                            for (char in text) {
                                                when (char) {
                                                    '{' -> jsonBraceCount++
                                                    '}' -> jsonBraceCount--
                                                }
                                                result.append(char)
                                                
                                                if (jsonBraceCount == 0) {
                                                    Log.d(TAG, "检测到完整的JSON")
                                                    break
                                                }
                                            }
                                            jsonEvents++
//                                            Log.d(TAG, "当前JSON状态: length=${result.length}, braceCount=$jsonBraceCount")
                                        }
                                    }
                                }
                                "all_done" -> {
                                    Log.d(TAG, "收到完成件")
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "解析SSE事件失败: ${e.message}", e)
                        }
                    }
                }
            }

            Log.d(TAG, "SSE流结束，总事件数: $totalEvents, JSON事件数: $jsonEvents")
            val finalResult = result.toString()
            Log.d(TAG, "最终结果: '$finalResult'")
            
            try {
                if (finalResult.isEmpty()) {
                    Log.e(TAG, "未收到任何JSON数据")
                    throw IOException("未收到用的结果")
                }
                
                val cleanJson = finalResult.trim()
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace("\\", "")
                
                Log.d(TAG, "处理后的JSON: '$cleanJson'")
                
                if (!cleanJson.startsWith("{") || !cleanJson.endsWith("}")) {
                    Log.e(TAG, "JSON格式不完整: $cleanJson")
                    throw IOException("JSON 格式不完整: $cleanJson")
                }
                
                Gson().fromJson(cleanJson, AnalysisResponse::class.java)
                    ?.also { response ->
                        Log.d(TAG, "分析成功: ${response.words.size}个单词, 句: ${response.sentence.english}")
                    }
                    ?: throw IOException("解析结果为空")
            } catch (e: Exception) {
                Log.e(TAG, "JSON解析失败: ${e.message}", e)
                throw IOException("解析JSON失败: ${e.message}")
            }
        }
    }

    suspend fun getFileDetail(fileId: String, fileName: String, width: String, height: String): FileDetailResponse {
        Log.d(TAG, "开始获取文件详: fileId=$fileId, fileName=$fileName")
        val request = Request.Builder()
            .url("$BASE_URL/file")
            .post(
                FileDetailRequest(
                    name = fileName,
                    file_id = fileId,
                    meta = FileMeta(width = width, height = height)
                ).let { 
                    val json = Gson().toJson(it)
                    Log.d(TAG, "件详请求体: $json")
                    json.toRequestBody("application/json".toMediaType())
                }
            )
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "获取文件详情失败: $errorBody")
                    throw IOException("获取文件详情失败: ${response.code}")
                }
                val responseBody = response.body!!.string()
                Log.d(TAG, "获取文件详情成功: $responseBody")
                Gson().fromJson(responseBody, FileDetailResponse::class.java)
            }
        }
    }

    suspend fun uploadFile(file: File, presignedUrl: String) {
        Log.d(TAG, "开始上传文件: ${file.name} 到 $presignedUrl")
        
        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? = "image/jpeg".toMediaType()
            
            override fun contentLength(): Long = file.length()
            
            override fun writeTo(sink: okio.BufferedSink) {
                file.inputStream().use { input ->
                    sink.writeAll(input.source())
                }
            }
        }

        val request = Request.Builder()
            .url(presignedUrl)
            .put(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "上传文件失败: $errorBody")
                    throw IOException("上传文件失败: ${response.code}")
                }
                Log.d(TAG, "文件上传成功")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val AUTH_TOKEN = "your_auth_token_here"
        private const val BASE_URL = "http://47.88.1.12"
    }

    val analysisResults = mutableStateListOf<ImageAnalysisEntity>()
    private lateinit var repository: ImageAnalysisRepository
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // 权限都已授予
        }
    }

    internal val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                processImage(Uri.fromFile(File(path)))
            }
        }
    }

    private var currentPhotoPath: String? = null
    internal var currentChatId: String? = null
    internal var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    internal var imageCapture: ImageCapture? = null
    internal val kimiService = KimiService()
    internal var currentAnalysis: AnalysisResponse? = null
        private set

    // 重添 pickImageLauncher
    internal val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            startCrop(it)  // 选择图片后先裁剪
        }
    }

    // 添加 cropLauncher 定义
    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                processImage(uri)
            }
        }
    }

    // 修改 processImage 方法
    internal fun processImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "开始处理图片: $uri")
                
                // 获取文件路径
                val filePath = when (uri.scheme) {
                    "file" -> uri.path  // 直接使用文件路径
                    "content" -> getRealPathFromURI(uri)  // 内容 URI 需要���换
                    else -> null
                }
                
                Log.d("MainActivity", "获取到文件路径: $filePath")
                
                if (filePath == null) {
                    Log.e("MainActivity", "无法获取文件路径")
                    throw IOException("无法获取文件路径")
                }
                
                val file = File(filePath)
                if (!file.exists()) {
                    Log.e("MainActivity", "文件不存在: $filePath")
                    throw IOException("文件不存在")
                }
                
                // 获图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(filePath, options)
                
                // 创建 FileDetailRequest
                val fileDetailRequest = FileDetailRequest(
                    name = file.name,
                    file_id = UUID.randomUUID().toString(),
                    meta = FileMeta(
                        width = options.outWidth.toString(),
                        height = options.outHeight.toString()
                    )
                )
                
                // 创建聊天会话
                val chatId = kimiService.createChat()
                Log.d("MainActivity", "创建聊天会话成功: $chatId")
                
                // 获取预签名 URL
                val preSignedURL = kimiService.getPreSignedURL(file.name)
                
                // 上传文件到预签名 URL
                kimiService.uploadFile(file, preSignedURL.url)
                
                // 获取文件详情
                val fileDetail = kimiService.getFileDetail(
                    fileId = preSignedURL.file_id,
                    fileName = file.name,
                    width = options.outWidth.toString(),
                    height = options.outHeight.toString()
                )
                
                // 分析图片
                val analysisResponse = kimiService.analyzeImage(
                    fileId = preSignedURL.file_id,
                    fileName = file.name,
                    fileSize = file.length().toInt(),
                    fileDetail = fileDetail,
                    chatId = chatId
                )
                
                Log.d("MainActivity", "分析结果: $analysisResponse")
                
                // 创建并保存 ImageAnalysisEntity
                val imageAnalysisEntity = ImageAnalysisEntity(
                    imageUri = uri.toString(),
                    words = analysisResponse.words.map { word ->
                        WordEntity(
                            word = word.word,
                            phoneticsymbols = word.phoneticsymbols,
                            explanation = word.explanation,
                            location = word.location
                        )
                    },
                    sentence = SentenceEntity(
                        english = analysisResponse.sentence.english,
                        chinese = analysisResponse.sentence.chinese
                    )
                )
                
                Log.d("MainActivity", "保存数据库: $imageAnalysisEntity")
                repository.insert(imageAnalysisEntity)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "图片处理失败", e)
                Log.e("MainActivity", "详细错误: ${e.message}")
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity, 
                    "图片处理失败: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        repository = ImageAnalysisRepository(database.imageAnalysisDao())
        
        // Collect saved analyses
        lifecycleScope.launch {
            repository.allAnalyses.collect { savedAnalyses ->
                analysisResults.clear()
                analysisResults.addAll(savedAnalyses)
            }
        }
        
        setContent {
            PictureTalk4AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (android.os.Build.VERSION.SDK_INT <= 32) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val outputFile = createImageFile()
        inputStream?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outputFile.absolutePath
    }

    private fun copyImageToPrivateStorage(uri: Uri): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        val destFile = File(storageDir, "JPEG_${timeStamp}_.jpg")
        
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // 修改 startCrop 方法，确保正确处理 URI
    private fun startCrop(sourceUri: Uri) {
        try {
            // 确保源文件存在且可访问
            val inputStream = contentResolver.openInputStream(sourceUri)
            inputStream?.close()

            // 创建目标文件
            val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))

            // 创建裁剪意图
            val uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(9f, 16f)
                .withMaxResultSize(1080, 1920)
                .withOptions(UCrop.Options().apply {
                    setCompressionQuality(90)
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                    setShowCropFrame(true)
                    setShowCropGrid(true)
                    setToolbarColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    setStatusBarColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    setToolbarWidgetColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                })
                .getIntent(this)

            // 启动裁剪
            cropLauncher.launch(uCropIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 修改拍照后的处理
    internal fun takePhoto() {
        imageCapture?.let { capture ->
            // 创建临时文件
            val photoFile = File(
                getOutputDirectory(),
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(System.currentTimeMillis()) + ".jpg"
            )

            // 创建输出选项对象
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // 拍照
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        output.savedUri?.let { uri ->
                            startCrop(uri)
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("MainActivity", "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(
                            this@MainActivity,
                            "拍照失败: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    // 加获取真实文件路径的辅助方
    private fun getRealPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        return cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            it.getString(columnIndex)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // 添加 startCamera 方法
    internal suspend fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            cameraProviderFuture.addListener({
                continuation.resume(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(this))
        }
        
        try {
            // 解绑所有用例
            cameraProvider.unbindAll()
            
            // 设置预览
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            // 设置图像捕获
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // 选择后置摄像头
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // 绑定用例到相机
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "相机启动失败", e)
            Toast.makeText(
                this@MainActivity,
                "相机启动失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as MainActivity
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(Screen.Home.title) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.name } == true,
                    onClick = {
                        navController.navigate(Screen.Home.name) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text(Screen.Camera.title) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Camera.name } == true,
                    onClick = {
                        navController.navigate(Screen.Camera.name) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(Screen.Profile.title) },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Profile.name } == true,
                    onClick = {
                        navController.navigate(Screen.Profile.name) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.name) { 
                HomeScreen(analysisResults = activity.analysisResults)
            }
            composable(Screen.Camera.name) { 
                CameraScreen(
                    pickImageLauncher = activity.pickImageLauncher,
                    activity = activity
                )
            }
            composable(Screen.Profile.name) { 
                ProfileScreen(
                    pickImageLauncher = activity.pickImageLauncher,
                    activity = activity
                )
            }
        }
    }
}

@Composable
fun ImageWithWords(
    imageAnalysis: ImageAnalysisEntity,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    var selectedWord by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = modifier) {
        // 图片
        AsyncImage(
            model = Uri.parse(imageAnalysis.imageUri),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    imageSize = Size(
                        coordinates.size.width.toFloat(),
                        coordinates.size.height.toFloat()
                    )
                },
            contentScale = ContentScale.Fit
        )
        
        // 单词卡片
        if (imageSize != Size.Zero) {
            imageAnalysis.words.forEach { word ->
                word.location?.let { location ->
                    val (x, y) = location.split(",").map { it.trim().toFloat() }
                    val xPos = x * imageSize.width
                    val yPos = y * imageSize.height
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = xPos.roundToInt(),
                                    y = yPos.roundToInt()
                                )
                            }
                    ) {
                        WordCard(
                            word = word,
                            isSelected = selectedWord == word.word,
                            onClick = { selectedWord = if (selectedWord == word.word) null else word.word }
                        )
                    }
                }
            }
        }
        
        // 句子显示在底部
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            imageAnalysis.sentence.let { sentence ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sentence.english?.let { 
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                        sentence.chinese?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            word.word?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isSelected) {
                word.phoneticsymbols?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                word.explanation?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    pickImageLauncher: ActivityResultLauncher<String>,
    activity: MainActivity
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("我的 - 版本信息")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(analysisResults: List<ImageAnalysisEntity>) {
    val pagerState = rememberPagerState(pageCount = { analysisResults.size })
    
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val analysis = analysisResults[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                var imageSize by remember { mutableStateOf(Size.Zero) }
                
                // 图片
                AsyncImage(
                    model = Uri.parse(analysis.imageUri),
                    contentDescription = "分析图片",
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            imageSize = Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        },
                    contentScale = ContentScale.Crop
                )
                
                // 添加半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)))
                
                // 单词卡片
                if (imageSize != Size.Zero) {
                    analysis.words.forEach { word ->
                        word.location?.let { location ->
                            val (x, y) = location.split(",").map { it.trim().toFloat() }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (x * imageSize.width).roundToInt(),
                                            y = (y * imageSize.height).roundToInt()
                                        )
                                    }
                            ) {
                                var isSelected by remember { mutableStateOf(false) }
                                WordCard(
                                    word = word,
                                    isSelected = isSelected,
                                    onClick = { isSelected = !isSelected }
                                )
                            }
                        }
                    }
                }
                
                // 句子显示在底部
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    analysis.sentence.let { sentence ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sentence.english?.let { 
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                                sentence.chinese?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(word: WordEntity) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            word.word?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (expanded) {
                word.phoneticsymbols?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                word.explanation?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CameraScreen(
    pickImageLauncher: ActivityResultLauncher<String>,
    activity: MainActivity
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(previewView) {
            previewView?.let { preview ->
                activity.startCamera(preview, lifecycleOwner)
            }
        }

        // Control buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = { pickImageLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "选择图片",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Camera button
                IconButton(
                    onClick = { activity.takePhoto() }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "拍照",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}