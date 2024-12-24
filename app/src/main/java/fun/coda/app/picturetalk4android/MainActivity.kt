package `fun`.coda.app.picturetalk4android

import AnalysisResponse
import FileDetailRequest
import FileMeta
import KimiService
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import `fun`.coda.app.picturetalk4android.data.AppDatabase
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisEntity
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisRepository
import `fun`.coda.app.picturetalk4android.data.SentenceEntity
import `fun`.coda.app.picturetalk4android.data.WordEntity
import `fun`.coda.app.picturetalk4android.ui.theme.PictureTalk4AndroidTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.os.Handler
import android.os.Looper
import android.content.Intent
import androidx.compose.material3.Divider
import android.media.MediaPlayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisWithWords
import androidx.compose.foundation.lazy.items

enum class Screen(val title: String) {
    Home("首页"),
    Camera("开拍"),
    Profile("我的")
}



class MainActivity : ComponentActivity() {
    companion object {
        private const val PREF_NAME = "kimi_config"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }

    val analysisResults = mutableStateListOf<ImageAnalysisWithWords>()
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
            .withAspectRatio(9f, 16f)  // 修改为 9:16 的全屏比例
            .withMaxResultSize(1080, 1920)  // 调整最大尺寸为全屏比例
            .useSourceImageAspectRatio()  // 初始显示时使用原图比例
            .withOptions(UCrop.Options().apply {
                setHideBottomControls(false)
                setFreeStyleCropEnabled(true)  // 允许自由调整裁剪框
                setShowCropGrid(true)  // 显示裁剪网格
                setShowCropFrame(true)  // 显示裁剪框
                setStatusBarColor(getColor(android.R.color.black))
                setToolbarColor(getColor(android.R.color.black))
                setToolbarWidgetColor(getColor(android.R.color.white))
            })

        try {
            cropLauncher.launch(uCrop.getIntent(this))
        } catch (e: Exception) {
            Log.e("MainActivity", "启动裁剪失败", e)
            // 如果裁剪失败，直接处理原图
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
                
                // 获取文件路径
                val filePath = when (uri.scheme) {
                    "file" -> uri.path  // 直接使用文件路径
                    "content" -> getRealPathFromURI(uri)  // 内容 URI 需要转换
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
                    sentence = SentenceEntity(
                        english = analysisResponse.sentence.english,
                        chinese = analysisResponse.sentence.chinese
                    )
                )

                val words = analysisResponse.words.map { word ->
                    WordEntity(
                        image_id = 0, // 临时ID，insert时会被更新
                        word = word.word,
                        phoneticsymbols = word.phoneticsymbols,
                        explanation = word.explanation,
                        location = word.location
                    )
                }

                repository.insert(imageAnalysisEntity, words)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "图片处理失败", e)
                when (e) {
                    is IllegalStateException -> {
                        // KIMI 配置错误
                        showConfigurationDialog()
                    }
                    else -> {
                        Toast.makeText(
                            this@MainActivity,
                            "图片处理失败: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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

    // 添加 MediaPlayer 用于播放音频
    private var mediaPlayer: MediaPlayer? = null
    
    // 添加播放音频���方法
    internal fun playWordAudio(word: String) {
        try {
            // 释放之前的 MediaPlayer
            mediaPlayer?.release()
            
            // 创建新的 MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                val audioUrl = "https://dict.youdao.com/dictvoice?audio=${word.lowercase()}&type=2"
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    mp.reset()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MainActivity", "播放音频失败: what=$what, extra=$extra")
                    Toast.makeText(
                        this@MainActivity,
                        "播放音频失败",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "设置音频源失败", e)
            Toast.makeText(
                this@MainActivity,
                "播放音频失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                
                                Screen.values().forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                when (screen) {
                                                    Screen.Home -> Icons.Default.Home
                                                    Screen.Camera -> Icons.Default.PhotoCamera
                                                    Screen.Profile -> Icons.Default.Person
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
                            composable(Screen.Home.name) { HomeScreen(analysisResults) }
                            composable(Screen.Camera.name) { 
                                CameraScreen(
                                    pickImageLauncher = pickImageLauncher,
                                    activity = this@MainActivity
                                )
                            }
                            composable(Screen.Profile.name) { 
                                ProfileScreen(this@MainActivity)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
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
                    activity = activity
                )
            }
        }
    }
}

@Composable
fun ImageWithWords(
    imageAnalysis: ImageAnalysisWithWords,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { 
        ImageAnalysisRepository(AppDatabase.getDatabase(context).imageAnalysisDao()) 
    }
    
    Box(modifier = modifier) {
        AsyncImage(
            model = Uri.parse(imageAnalysis.analysis.imageUri),
            contentDescription = null,
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
        
        if (imageSize != Size.Zero) {
            for (word in imageAnalysis.words) {
                word.location?.let { location ->
                    var offset by remember { 
                        mutableStateOf(Offset(word.offset_x, word.offset_y))
                    }
                    
                    val coordinates = location.split(",").map { coord -> coord.trim().toFloat() }
                    val xPos = coordinates[0] * imageSize.width + offset.x
                    val yPos = coordinates[1] * imageSize.height + offset.y
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = xPos.roundToInt(),
                                    y = yPos.roundToInt()
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offset = Offset(
                                        offset.x + dragAmount.x,
                                        offset.y + dragAmount.y
                                    )
                                    scope.launch {
                                        repository.updateWordOffsets(
                                            imageAnalysis.analysis.id,
                                            word.word ?: "",
                                            offset.x,
                                            offset.y
                                        )
                                    }
                                }
                            }
                    ) {
                        WordCard(
                            word = word,
                            expanded = selectedWord == word.word,
                            onExpandChange = { 
                                selectedWord = if (selectedWord == word.word) null else word.word 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    expanded: Boolean,
    onExpandChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    
    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable { 
                onExpandChange()
                // 点击时播放音频
                word.word?.let { activity.playWordAudio(it) }
            },
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
fun ProfileScreen(activity: MainActivity) {
    var showDialog by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像区域
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 用户名/ID
            Text(
                text = "Picture Talk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 设置项列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // KIMI 配置项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Column {
                            Text(
                                text = "KIMI Token",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!activity.isKimiConfigured()) {
                                Text(
                                    text = "未配置",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // 版本信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "版本",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // KIMI Token 配置对话框
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("配置 KIMI Token") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = authToken,
                            onValueChange = { authToken = it },
                            label = { Text("Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activity.saveKimiConfig(
                                token = authToken.takeIf { it.isNotBlank() }
                            )
                            showDialog = false
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(analysisResults: List<ImageAnalysisWithWords>) {
    // 使用 VerticalPager 代替 LazyColumn 实现全屏滑动
    val pagerState = rememberPagerState(pageCount = { analysisResults.size })
    
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
            // 图片和单词
            ImageWithWords(
                imageAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
            
            // 底部渐变和句子
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 英文句子
                    Text(
                        text = analysis.analysis.sentence.english ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    // 中文翻译
                    Text(
                        text = analysis.analysis.sentence.chinese ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
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