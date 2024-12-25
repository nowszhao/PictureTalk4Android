package `fun`.coda.app.picturetalk4android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import `fun`.coda.app.picturetalk4android.data.ImageAnalysisWithWords
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import `fun`.coda.app.picturetalk4android.data.AnalysisStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    analyses: List<ImageAnalysisWithWords>,
    onImageClick: (ImageAnalysisWithWords) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("已完成", "解析中")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("任务列表") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回首页"
                            )
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val filteredAnalyses = when (selectedTab) {
            0 -> analyses.filter { it.analysis.status == AnalysisStatus.COMPLETED }
            1 -> analyses.filter { it.analysis.status == AnalysisStatus.PROCESSING }
            else -> emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredAnalyses.size) { index ->
                val analysis = filteredAnalyses[index]
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clickable { onImageClick(analysis) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = analysis.analysis.imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 添加解析中的进度指示器
                        if (analysis.analysis.status == AnalysisStatus.PROCESSING) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}