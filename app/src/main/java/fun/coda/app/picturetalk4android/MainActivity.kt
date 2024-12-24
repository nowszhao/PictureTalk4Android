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

enum class Screen(val title: String) {
    Home("首页"),
    Camera("开拍"),
    Profile("我的")
}



class MainActivity : ComponentActivity() {
    companion object {
        private const val PREF_NAME = "kimi_config"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_API_KEY = "api_key"
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
        loadKimiConfig()
        
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

    internal fun saveKimiConfig(token: String?, apiKey: String?) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_API_KEY, apiKey)
            apply()
        }
        KimiService.configure(token, apiKey)
    }

    private fun loadKimiConfig() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        val apiKey = prefs.getString(KEY_API_KEY, null)
        KimiService.configure(token, apiKey)
    }

    // 添加 pickImageLauncher
    internal val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
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
                            expanded = selectedWord == word.word,
                            onExpandChange = { selectedWord = if (selectedWord == word.word) null else word.word }
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
    expanded: Boolean,
    onExpandChange: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onExpandChange),
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
    var authToken by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KIMI 配置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "KIMI API 配置",
                    style = MaterialTheme.typography.titleLarge
                )
                
                OutlinedButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("配置 KIMI API")
                }
            }
        }
        
        // 版本信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "版本信息",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("配置 KIMI API") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Auth Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "请至少填写其中一项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activity.saveKimiConfig(
                            token = authToken.takeIf { it.isNotBlank() },
                            apiKey = apiKey.takeIf { it.isNotBlank() }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(analysisResults: List<ImageAnalysisEntity>) {
    val pagerState = rememberPagerState(pageCount = { analysisResults.size })
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 垂直滑动的图��列表
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
                // 图片
                AsyncImage(
                    model = Uri.parse(analysis.imageUri),
                    contentDescription = "分析图片",
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            imageSize = coordinates.size
                        },
                    contentScale = ContentScale.Crop
                )
                
                // 单词卡片
                analysis.words.forEach { word ->
                    word.location?.let { location ->
                        val (x, y) = location.split(",").map { it.trim().toFloat() }
                        Box(
                            modifier = Modifier.offset {
                                IntOffset(
                                    x = (x * imageSize.width).roundToInt(),
                                    y = (y * imageSize.height).roundToInt()
                                )
                            }
                        ) {
                            var isSelected by remember { mutableStateOf(false) }
                            WordCard(
                                word = word,
                                expanded = isSelected,
                                onExpandChange = { isSelected = !isSelected }
                            )
                        }
                    }
                }

                // 句子显示在底部
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    // 渐变背景
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Black.copy(alpha = 0.8f)
                                    ),
                                    startY = 0f,
                                    endY = 300f
                                )
                            )
                            .clickable { expanded = !expanded }
                            .padding(16.dp, 32.dp)
                    ) {
                        analysis.sentence.let { sentence ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sentence.english?.let { englishText -> 
                                    val displayText = if (!expanded && englishText.length > 50) {
                                        englishText.take(50) + "..."
                                    } else {
                                        englishText
                                    }
                                    
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(2f, 2f),
                                                blurRadius = 5f
                                            )
                                        ),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        lineHeight = 26.sp
                                    )
                                }
                                
                                sentence.chinese?.let { chineseText ->
                                    val displayText = if (!expanded && chineseText.length > 30) {
                                        chineseText.take(30) + "..."
                                    } else {
                                        chineseText
                                    }
                                    
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        ),
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp
                                    )
                                }
                                
                                // 展开/收起提示
                                if ((sentence.english?.length ?: 0) > 50 || 
                                    (sentence.chinese?.length ?: 0) > 30) {
                                    Text(
                                        text = if (expanded) "收起" else "更多",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(1f, 1f),
                                                blurRadius = 3f
                                            )
                                        ),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 4.dp)
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