package `fun`.coda.app.picturetalk4android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var analysisToDelete by remember { mutableStateOf<ImageAnalysisWithWords?>(null) }
    
    Scaffold(
        topBar = {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE3F2FD)) // 浅蓝色背景
        ) {
            // 解析中的图片部分
            val processingImages = analyses.filter { it.analysis.status == AnalysisStatus.PROCESSING }
            if (processingImages.isNotEmpty()) {
                Text(
                    text = "解析中的图片",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(
                        (((processingImages.size + 2) / 3) * 120).dp
                    )
                ) {
                    items(processingImages) { analysis ->
                        ImageCard(
                            analysis = analysis,
                            onImageClick = onImageClick,
                            onDeleteClick = {
                                analysisToDelete = analysis
                                showDeleteDialog = true
                            }
                        )
                    }
                }
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }

            // 所有图片部分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "所有图片",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDeleteAllClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除所有任务",
                        tint = Color.Red
                    )
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(analyses) { analysis ->
                    ImageCard(
                        analysis = analysis,
                        onImageClick = onImageClick,
                        onDeleteClick = {
                            analysisToDelete = analysis
                            showDeleteDialog = true
                        }
                    )
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

@Composable
private fun ImageCard(
    analysis: ImageAnalysisWithWords,
    onImageClick: (ImageAnalysisWithWords) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onImageClick(analysis) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = analysis.analysis.imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Processing indicator
            if (analysis.analysis.status == AnalysisStatus.PROCESSING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除任务",
                    tint = Color.Red
                )
            }
        }
    }
}