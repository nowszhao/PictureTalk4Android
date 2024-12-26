package `fun`.coda.app.picturetalk4android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
    onBackClick: () -> Unit,
    onDeleteClick: (ImageAnalysisWithWords) -> Unit,
    onDeleteAllClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var analysisToDelete by remember { mutableStateOf<ImageAnalysisWithWords?>(null) }
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
                    },
                    actions = {
                        IconButton(onClick = onDeleteAllClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除所有任务",
                                tint = Color.Red
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
                        
                        // 添加删除按钮
                        IconButton(
                            onClick = {
                                analysisToDelete = analysis
                                showDeleteDialog = true
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除任务",
                                tint = Color.Red
                            )
                        }
                        
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

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("确认删除") },
                text = { Text("您确定要删除这个任务吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        analysisToDelete?.let { onDeleteClick(it) }
                        showDeleteDialog = false
                    }) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}