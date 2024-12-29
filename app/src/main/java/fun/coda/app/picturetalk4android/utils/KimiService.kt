package `fun`.coda.app.picturetalk4android.utils


import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import `fun`.coda.app.picturetalk4android.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KimiService {
    companion object {
        private const val TAG = "`fun`.coda.app.picturetalk4android.utils.KimiService"
        private const val BASE_URL = "https://kimi.moonshot.cn/api"
        
        private var authToken: String? = null
        
        // 添加任务队列
        private val analysisQueue = Channel<AnalysisTask>(Channel.UNLIMITED)
        private var queueJob: Job? = null
        
        fun configure(token: String?, key: String?) {
            authToken = token
            // 启动队列处理协程
            if (queueJob == null) {
                queueJob = CoroutineScope(Dispatchers.IO).launch {
                    processQueue()
                }
            }
        }

        private suspend fun processQueue() {
            for (task in analysisQueue) {
                try {
                    val result = task.service.executeAnalysis(task)
                    task.onComplete(Result.success(result))
                } catch (e: Exception) {
                    task.onComplete(Result.failure(e))
                }
            }
        }
        
        fun getAuthHeader(): String {
            return when {
                !authToken.isNullOrBlank() -> "Bearer $authToken"
                else -> throw IllegalStateException("KIMI Token not configured")
            }
        }
    }

    // 添加任务数据类
    private data class AnalysisTask(
        val fileId: String,
        val fileName: String,
        val fileSize: Int,
        val fileDetail: FileDetailResponse,
        val englishLevel: EnglishLevel,
        val service: KimiService,
        val onComplete: (Result<AnalysisResponse>) -> Unit
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var chatId: String? = null  // Store the chat ID

    suspend fun getPreSignedURL(fileName: String): PreSignedURLResponse {
        Log.d(TAG, "开始获取预签名URL: $fileName")
        val request = Request.Builder()
            .url("$BASE_URL/pre-sign-url")
            .post("""{"action":"image","name":"$fileName"}""".toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", getAuthHeader())
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

    suspend fun createChat(): String {
        if (chatId != null) {
            Log.d(TAG, "复用现有聊天会话: $chatId")
            return chatId!!
        }

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
            .addHeader("Authorization", getAuthHeader())
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
                chatId = JSONObject(responseBody).getString("id")
                chatId!!
            }
        }
    }

    suspend fun analyzeImage(
        fileId: String,
        fileName: String,
        fileSize: Int,
        fileDetail: FileDetailResponse,
        englishLevel: EnglishLevel
    ): AnalysisResponse {
        return suspendCoroutine { continuation ->
            val task = AnalysisTask(
                fileId = fileId,
                fileName = fileName,
                fileSize = fileSize,
                fileDetail = fileDetail,
                englishLevel = englishLevel,
                service = this
            ) { result ->
                result.fold(
                    onSuccess = { continuation.resume(it) },
                    onFailure = { continuation.resumeWithException(it) }
                )
            }
            
            runBlocking {
                analysisQueue.send(task)
            }
        }
    }

    // 添加实际执行分析的内部方法
    private suspend fun executeAnalysis(task: AnalysisTask): AnalysisResponse {
        chatId = createChat()
        Log.d(TAG, "开始分析图片: fileId=${task.fileId}, fileName=${task.fileName}, fileSize=${task.fileSize}, chatId=$chatId")

        val fileRef = FileRef(
            id = task.fileId,
            name = task.fileName,
            size = task.fileSize,
            detail = FileDetail(
                id = task.fileDetail.id,
                name = task.fileDetail.name,
                size = task.fileSize
            ),
            file_info = FileDetail(
                id = task.fileDetail.id,
                name = task.fileDetail.name,
                size = task.fileSize
            )
        )

        val prompt = """
        我作为一个英语水平为${task.englishLevel.title}的学习者，我想通过图片场景化学习新的英语词块，请分析我提供的图片，提供以下信息：
        1、词块：
          - 图片场景中我可以学习到相对${task.englishLevel.title}水平之上的 Top8英语词块，信息包括词块、音标和中文解释、词块所在图片大致位置（词块指向物品中的一个点表示，x 和 y 坐标，归一化到0~1的范围，精度为后四位小数点，词块之间的位置不要重叠）
          - 英语词块（chunk）是指在语言处理中，作为一个整体来理解和使用的一组词或短语。词块可以是固定搭配、习惯用语、短语动词、常见的表达方式等。它们在语言中频繁出现，具有一定的固定性和连贯性，使得学习者能够更自然地使用语言。
        2、句子
          - 使用一句最简单、准确的英语描述图片内容。
          - 提供地道的中文翻译。
          - 返回格式，请以 标准 JSON 格式 返回结果，示例如下：
            {
                "words": [
                    {
                        "word": "emergency brake",
                        "phoneticsymbols": "/iˈmɜːdʒənsi breɪk/",
                        "explanation": "紧急刹车",
                        "location": "0.55, 0.65"
                    },
                    ...
                ],
                "sentence": {
                    "text": "The subway car is empty, with handrails, safety strips, and overhead lights clearly visible.",
                    "translation": "地铁车厢是空的，扶手、安全条和头顶灯清晰可见。"
                }
            }
        """.trimIndent()

        val analysisRequest = ImageAnalysisRequest(
            messages = listOf(Message("user", prompt)),
            refs = listOf(task.fileId),
            refs_file = listOf(fileRef)
        )

        val requestJson = Gson().toJson(analysisRequest)
        Log.d(TAG, "发送的请求: $requestJson")

        // 创建请求
        val request = Request.Builder()
            .url("$BASE_URL/chat/$chatId/completion/stream")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", getAuthHeader())
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
                    chatId = null.toString()  // Reset chatId on failure
                    throw IOException("分析图片失败: ${response.code}")
                }

                val reader = response.body!!.charStream().buffered()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    totalEvents++
                    if (line?.startsWith("data: ") == true) {
                        val jsonString = line!!.substring(6)

                        try {
                            val event = JSONObject(jsonString)
                            val eventType = event.getString("event")

                            when (eventType) {
                                "cmpl" -> {
                                    val text = event.optString("text", "")
                                    val loading = event.optBoolean("loading")

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

            Log.d(TAG, "SSE流结束，事件数: $totalEvents, JSON事件数: $jsonEvents")
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
            .addHeader("Authorization", getAuthHeader())
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