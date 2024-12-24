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

class KimiService {
    companion object {
        private const val TAG = "KimiService"
        private const val BASE_URL = "https://kimi.moonshot.cn/api"
        
        private var authToken: String? = null
        private var apiKey: String? = null
        
        fun configure(token: String?, key: String?) {
            authToken = token
            apiKey = key
        }
        
        fun getAuthHeader(): String {
            return when {
                !authToken.isNullOrBlank() -> "Bearer $authToken"
                !apiKey.isNullOrBlank() -> "Bearer $apiKey"
                else -> throw IllegalStateException("Neither auth token nor API key is configured")
            }
        }
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
        Log.d(TAG, "开始分析图片: fileId=$fileId, fileName=$fileName, fileSize=$fileSize, chatId=$chatId")

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
                  - 使用句最简单、确的英语描图片内容。
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