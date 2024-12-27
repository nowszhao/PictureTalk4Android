package `fun`.coda.app.picturetalk4android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun TaskListScreen(
    analyses: List<ImageAnalysisWithWords>,
    onImageClick: (ImageAnalysisWithWords) -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: (ImageAnalysisWithWords) -> Unit,
    onDeleteAllClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var analysisToDelete by remember { mutableStateOf<ImageAnalysisWithWords?>(null) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMultiSelectMode) "${selectedItems.size} 已选择" else "任务列表") },
                navigationIcon = {
                    IconButton(
                        onClick = if (isMultiSelectMode) {
                            { isMultiSelectMode = false; selectedItems = emptySet() }
                        } else onBackClick
                    ) {
                        Icon(
                            imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isMultiSelectMode) "取消选择" else "返回首页"
                        )
                    }
                },
                actions = {
                    if (isMultiSelectMode && selectedItems.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除选中项",
                                tint = Color.Red
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE3F2FD))
        ) {
            // Pull to refresh
            val refreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { /* 实现刷新逻辑 */ }
            )
            
            Box(modifier = Modifier.pullRefresh(refreshState)) {
                LazyColumn {
                    // 解析中的任务组
                    val processingImages = analyses.filter { it.analysis.status == AnalysisStatus.PROCESSING }
                    if (processingImages.isNotEmpty()) {
                        item {
                            TaskGroupHeader(
                                title = "解析中 (${processingImages.size})",
                                icon = Icons.Default.Refresh
                            )
                        }
                        
                        items(
                            items = processingImages,
                            key = { it.analysis.id }
                        ) { analysis ->
                            TaskItem(
                                analysis = analysis,
                                isSelected = analysis.analysis.id in selectedItems,
                                isMultiSelectMode = isMultiSelectMode,
                                onItemClick = { 
                                    if (isMultiSelectMode) {
                                        selectedItems = if (analysis.analysis.id in selectedItems) {
                                            selectedItems - analysis.analysis.id
                                        } else {
                                            selectedItems + analysis.analysis.id
                                        }
                                    } else {
                                        onImageClick(analysis)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        isMultiSelectMode = true
                                        selectedItems = setOf(analysis.analysis.id)
                                    }
                                }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // 已完成的任务组
                    val completedImages = analyses.filter { it.analysis.status == AnalysisStatus.COMPLETED }
                    if (completedImages.isNotEmpty()) {
                        item {
                            TaskGroupHeader(
                                title = "已完成 (${completedImages.size})",
                                icon = Icons.Default.CheckCircle,
                                showDeleteAll = !isMultiSelectMode,
                                onDeleteAllClick = { showDeleteAllDialog = true }
                            )
                        }
                        
                        items(
                            items = completedImages,
                            key = { it.analysis.id }
                        ) { analysis ->
                            TaskItem(
                                analysis = analysis,
                                isSelected = analysis.analysis.id in selectedItems,
                                isMultiSelectMode = isMultiSelectMode,
                                onItemClick = { 
                                    if (isMultiSelectMode) {
                                        selectedItems = if (analysis.analysis.id in selectedItems) {
                                            selectedItems - analysis.analysis.id
                                        } else {
                                            selectedItems + analysis.analysis.id
                                        }
                                    } else {
                                        onImageClick(analysis)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        isMultiSelectMode = true
                                        selectedItems = setOf(analysis.analysis.id)
                                    }
                                }
                            )
                        }
                    }
                }
                
                PullRefreshIndicator(
                    refreshing = false,
                    state = refreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // Delete confirmation dialogs
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除${if (isMultiSelectMode) "选中的 ${selectedItems.size} 个" else "这个"}任务吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (isMultiSelectMode) {
                                selectedItems.forEach { id ->
                                    analyses.find { it.analysis.id == id }?.let { onDeleteClick(it) }
                                }
                                selectedItems = emptySet()
                                isMultiSelectMode = false
                            } else {
                                analysisToDelete?.let { onDeleteClick(it) }
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("删除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("确认删除全部") },
                text = { Text("确定要删除所有已完成的任务吗？此操作不可恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteAllClick()
                            showDeleteAllDialog = false
                        }
                    ) {
                        Text("删除全部", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun TaskGroupHeader(
    title: String,
    icon: ImageVector,
    showDeleteAll: Boolean = false,
    onDeleteAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        if (showDeleteAll) {
            TextButton(
                onClick = onDeleteAllClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text("清空")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskItem(
    analysis: ImageAnalysisWithWords,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = analysis.analysis.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = analysis.analysis.sentence.english ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "单词数: ${analysis.words.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                if (analysis.analysis.status == AnalysisStatus.PROCESSING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onItemClick() }
                    )
                }
            }
            
            // Selection overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                )
            }
        }
    }
}