package `fun`.coda.app.picturetalk4android.ui.screens


import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import `fun`.coda.app.picturetalk4android.MainActivity
import `fun`.coda.app.picturetalk4android.data.AppDatabase
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import `fun`.coda.app.picturetalk4android.utils.AudioService
import `fun`.coda.app.picturetalk4android.data.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Badge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    analyses: List<ImageAnalysisWithWords>,
    onTaskListClick: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { analyses.size })
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingWord by remember { mutableStateOf<String?>(null) }
    
    // Reset playing state when page changes
    LaunchedEffect(pagerState.currentPage) {
        isPlaying = false
        currentPlayingWord = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val analysis = analyses[page]
            var isExpanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // 图片和单词
                ImageWithWords(
                    imageAnalysis = analysis,
                    modifier = Modifier.fillMaxSize(),
                    currentPlayingWord = currentPlayingWord
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
                        .clickable { isExpanded = !isExpanded }
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
                            color = Color.White,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 展开状态显示中文翻译
                        if (isExpanded) {
                            Text(
                                text = analysis.analysis.sentence.chinese ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // 展开/收起指示器
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // Control buttons column
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play/Stop button
            FloatingActionButton(
                onClick = { isPlaying = !isPlaying },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止播放" else "开始播放",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Task list button with badge
            Box {
                FloatingActionButton(
                    onClick = onTaskListClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "任务列表",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Badge
                val processingCount = analyses.count { it.analysis.status == AnalysisStatus.PROCESSING }
                if (processingCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text(text = processingCount.toString())
                    }
                }
            }
        }

        // Auto-play logic
        LaunchedEffect(isPlaying, pagerState.currentPage) {
            if (isPlaying && analyses.isNotEmpty()) {
                val currentAnalysis = analyses[pagerState.currentPage]
                val words = currentAnalysis.words
                
                for (word in words) {
                    if (!isPlaying) break // Stop if playing is cancelled
                    currentPlayingWord = word.word
                    word.word?.let { AudioService.playWordAudio(it) }
                    delay(2000) // Wait for 2 seconds before next word
                }
                
                // Reset after playing all words
                currentPlayingWord = null
                isPlaying = false
            }
        }
    }
}


@Composable
fun ImageWithWords(
    imageAnalysis: ImageAnalysisWithWords,
    modifier: Modifier = Modifier,
    currentPlayingWord: String? = null
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
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
                    imageSize = coordinates.size
                },
            contentScale = ContentScale.Crop
        )

        if (imageSize != IntSize.Zero) {
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
                            expanded = selectedWord == word.word || currentPlayingWord == word.word,
                            isPlaying = currentPlayingWord == word.word,
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
    isPlaying: Boolean = false,
    onExpandChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) Color(0xFFFFA500) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable {
                onExpandChange()
                word.word?.let { AudioService.playWordAudio(it) }
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
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

