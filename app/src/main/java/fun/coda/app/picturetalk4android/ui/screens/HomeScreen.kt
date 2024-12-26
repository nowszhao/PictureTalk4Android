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

                // 右侧操作按钮
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 60.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 播放按钮
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    isPlaying = !isPlaying
                                    if (!isPlaying) {
                                        currentPlayingWord = null
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                contentColor = if (isPlaying) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "停止播放" else "开始播放",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = if (isPlaying) "停止" else "播放",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // 任务列表按钮
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onTaskListClick,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "任务列表",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "任务",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // 底部句子展示区域
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
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 中文翻译
                        if (isExpanded) {
                            Text(
                                text = analysis.analysis.sentence.chinese ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // More/Less 文本
                        Text(
                            text = if (isExpanded) "Less" else "More",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        )
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

