package `fun`.coda.app.picturetalk4android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yalantis.ucrop.UCrop
import `fun`.coda.app.picturetalk4android.data.AnalysisStatus
import `fun`.coda.app.picturetalk4android.data.AppDatabase
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisEntity
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisRepository
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisWithWords
import `fun`.coda.app.picturetalk4android.data.SentenceEntity
import `fun`.coda.app.picturetalk4android.data.WordEntity
import `fun`.coda.app.picturetalk4android.ui.screens.CameraScreen
import `fun`.coda.app.picturetalk4android.ui.screens.HomeScreen
import `fun`.coda.app.picturetalk4android.ui.screens.ProfileScreen
import `fun`.coda.app.picturetalk4android.ui.screens.TaskListScreen
import `fun`.coda.app.picturetalk4android.ui.theme.PictureTalk4AndroidTheme
import `fun`.coda.app.picturetalk4android.utils.AudioService
import `fun`.coda.app.picturetalk4android.utils.KimiService
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class Screen(val title: String) {
    Home("首页"),
    Camera("开拍"),
    Profile("我的"),
    TaskList("任务列表");

    companion object {
        fun bottomNavItems() = listOf(Home, Camera, Profile)
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val PREF_NAME = "kimi_config"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    val analysisResults = mutableStateListOf<ImageAnalysisWithWords>()
    private lateinit var repository: ImageAnalysisRepository


    internal var imageCapture: ImageCapture? = null
    internal val kimiService = KimiService()

    // 修改 pickImageLauncher 的定义
    internal val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startCrop(it) }  // 选择图片后先进行裁剪
    }

    // 确保 startCrop 方法正确实现
    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(
            File(
                getOutputDirectory(),
                "cropped_${System.currentTimeMillis()}.jpg"
            )
        )

        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(9f, 16f)  // 固定9:16比例
            .withMaxResultSize(1080, 1920)
            .withOptions(UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)  // 指定输出格式
                setCompressionQuality(95)  // 设置压缩质量
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)  // 禁用自由裁剪
                setShowCropGrid(true)
                setShowCropFrame(true)
                setStatusBarColor(getColor(android.R.color.black))
                setToolbarColor(getColor(android.R.color.black))
                setToolbarWidgetColor(getColor(android.R.color.white))
            })

        try {
            cropLauncher.launch(uCrop.getIntent(this))
        } catch (e: Exception) {
            Log.e("MainActivity", "启动裁剪失败", e)
            Toast.makeText(this, "裁剪功能不可���，将直接处理原图", Toast.LENGTH_SHORT).show()
            processImage(uri)
        }
    }

    // 确保 cropLauncher 正确处理结果
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                processImage(uri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.e("MainActivity", "裁剪失败: ${cropError?.message}")
            Toast.makeText(
                this,
                "图片裁剪失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 添加状态检查方法
    fun isKimiConfigured(): Boolean {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        return !prefs.getString(KEY_AUTH_TOKEN, null).isNullOrBlank()
    }

    // 修改 processImage 方法
    internal fun processImage(uri: Uri) {
        if (!isKimiConfigured()) {
            showConfigurationDialog()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "开始处理图片: $uri")

                // 创建并保存初始的 ImageAnalysisEntity（状态为 PROCESSING）
                val initialAnalysis = ImageAnalysisEntity(
                    imageUri = uri.toString(),
                    sentence = SentenceEntity(),
                    status = AnalysisStatus.PROCESSING
                )
                val analysisId = repository.insert(initialAnalysis)  // 保存初始记录并获取ID

                try {
                    // 获取文件路径
                    val filePath = when (uri.scheme) {
                        "file" -> uri.path
                        "content" -> getRealPathFromURI(uri)
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

                    // 获图尺寸
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(filePath, options)

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

                    // 处理完成后更新状态和内容
                    repository.updateAnalysis(
                        analysisId,
                        SentenceEntity(
                            english = analysisResponse.sentence.english,
                            chinese = analysisResponse.sentence.chinese
                        ),
                        analysisResponse.words.map { word ->
                            WordEntity(
                                image_id = analysisId,
                                word = word.word,
                                phoneticsymbols = word.phoneticsymbols,
                                explanation = word.explanation,
                                location = word.location
                            )
                        },
                        AnalysisStatus.COMPLETED
                    )

                } catch (e: Exception) {
                    Log.e("MainActivity", "处理图片失败", e)
                    // 处理失败时也要更新状态
                    repository.updateStatus(analysisId, AnalysisStatus.COMPLETED)
                    Toast.makeText(
                        this@MainActivity,
                        "处理片失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "创建初始记录失败", e)
                Toast.makeText(
                    this@MainActivity,
                    "创建任务失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showConfigurationDialog() {
        Toast.makeText(
            this,
            "请先配置 KIMI API 密钥",
            Toast.LENGTH_LONG
        ).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
        loadKimiConfig()
        
        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        repository = ImageAnalysisRepository(database.imageAnalysisDao())
        
        // Collect saved analyses
        lifecycleScope.launch {
            repository.allAnalyses.collect { analyses ->
                analysisResults.clear()
                analysisResults.addAll(analyses)
            }
        }
        
        setContent {
            PictureTalk4AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // 修改这部分代码
                    LaunchedEffect(Unit) {
                        if (!isKimiConfigured()) {
                            // 等待导航图设置完成后再导航
                            navController.graph.let {
                                navController.navigate(Screen.Profile.name) {
                                    popUpTo(it.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                
                                Screen.bottomNavItems().forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                when (screen) {
                                                    Screen.Home -> Icons.Default.Home
                                                    Screen.Camera -> Icons.Default.PhotoCamera
                                                    Screen.Profile -> Icons.Default.Person
                                                    Screen.TaskList -> Icons.Default.List
                                                },
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.name } == true,
                                        onClick = {
                                            navController.navigate(screen.name) {
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
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.name,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.name) { 
                                HomeScreen(
                                    analyses = analysisResults,
                                    onTaskListClick = {
                                        navController.navigate(Screen.TaskList.name)
                                    }
                                )
                            }
                            composable(Screen.Camera.name) { 
                                CameraScreen(
                                    pickImageLauncher = pickImageLauncher,
                                    activity = this@MainActivity
                                )
                            }
                            composable(Screen.Profile.name) { 
                                ProfileScreen(this@MainActivity)
                            }
                            composable(Screen.TaskList.name) {
                                TaskListScreen(
                                    analyses = analysisResults,
                                    onImageClick = { analysis ->
                                        navController.navigate(Screen.Home.name) {
                                            popUpTo(Screen.Home.name) {
                                                inclusive = true
                                            }
                                        }
                                    },
                                    onBackClick = {
                                        navController.navigateUp()
                                    },
                                    onDeleteClick = { analysis ->
                                        lifecycleScope.launch {
                                            repository.deleteAnalysis(analysis.analysis.id)
                                            analysisResults.remove(analysis)
                                        }
                                    },
                                    onDeleteAllClick = {
                                        lifecycleScope.launch {
                                            repository.deleteAll()
                                            analysisResults.clear()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioService.mediaPlayer?.release()
        AudioService.mediaPlayer = null
    }

    // 加获取真实文件路径的辅助方法
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


    internal fun saveKimiConfig(token: String?) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            apply()
        }
        KimiService.configure(token, null)
    }

    private fun loadKimiConfig() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        KimiService.configure(token, null)
    }

    // 添加 takePhoto 方法
    internal fun takePhoto() {
        imageCapture?.let { imageCapture ->
            // 创建带时间戳的图片文件
            val photoFile = File(
                getOutputDirectory(),
                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        // 拍照成功后进行裁剪
                        startCrop(savedUri)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("MainActivity", "拍照失败: ${exc.message}", exc)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera setup
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


