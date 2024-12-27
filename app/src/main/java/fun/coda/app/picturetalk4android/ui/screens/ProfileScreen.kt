package `fun`.coda.app.picturetalk4android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import `fun`.coda.app.picturetalk4android.MainActivity
import `fun`.coda.app.picturetalk4android.data.EnglishLevel

@Composable
fun ProfileScreen(activity: MainActivity) {
    var showDialog by remember { mutableStateOf(false) }
    var showEnglishLevelDialog by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf(activity.getKimiToken() ?: "") }
    var showToken by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var currentLevel by remember { mutableStateOf(activity.getEnglishLevel()) }

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
                // 英语水平设置
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEnglishLevelDialog = true }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "英语水平",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = currentLevel.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // KIMI 配置项
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (!activity.isKimiConfigured()) {
                                showDialog = true
                            } else {
                                isExpanded = !isExpanded
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activity.isKimiConfigured()) 
                            MaterialTheme.colorScheme.primaryContainer
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (activity.isKimiConfigured()) 
                                        Icons.Default.Check 
                                    else 
                                        Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (activity.isKimiConfigured())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "KIMI Token",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (activity.isKimiConfigured()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { showToken = !showToken }) {
                                        Icon(
                                            imageVector = if (showToken) 
                                                Icons.Default.VisibilityOff 
                                            else 
                                                Icons.Default.Visibility,
                                            contentDescription = if (showToken) 
                                                "隐藏Token" 
                                            else 
                                                "显示Token"
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            isExpanded = !isExpanded 
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) 
                                                Icons.Default.ExpandLess 
                                            else 
                                                Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) 
                                                "收起" 
                                            else 
                                                "展开"
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 展开时显示 Token
                        if (isExpanded && activity.isKimiConfigured()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (showToken) 
                                    activity.getKimiToken() ?: "" 
                                else 
                                    maskToken(activity.getKimiToken() ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showDialog = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("编辑")
                                }
                            }
                        } else if (!activity.isKimiConfigured()) {
                            Text(
                                text = "未配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                        text = "1.0.1",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 英语水平选择对话框
        if (showEnglishLevelDialog) {
            AlertDialog(
                onDismissRequest = { showEnglishLevelDialog = false },
                title = { Text("选择英语水平") },
                text = {
                    Column {
                        EnglishLevel.values().forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentLevel = level
                                        activity.saveEnglishLevel(level)
                                        showEnglishLevelDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = level.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = level.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (level == currentLevel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEnglishLevelDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // KIMI Token 配置对话框
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("配置 KIMI Token") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = authToken,
                            onValueChange = { authToken = it },
                            label = { Text("Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showToken) 
                                VisualTransformation.None
                            else 
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) 
                                            Icons.Default.VisibilityOff 
                                        else 
                                            Icons.Default.Visibility,
                                        contentDescription = if (showToken) 
                                            "隐藏Token" 
                                        else 
                                            "显示Token"
                                    )
                                }
                            }
                        )
                        if (activity.isKimiConfigured()) {
                            TextButton(
                                onClick = {
                                    activity.clearKimiConfig()
                                    authToken = ""
                                    showDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("清除配置")
                            }
                        }
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

private fun maskToken(token: String): String {
    if (token.length <= 8) return "****"
    return "${token.take(4)}****${token.takeLast(4)}"
}
