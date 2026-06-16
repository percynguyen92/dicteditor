package com.dicteditor.percynguyen92.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.utils.getFileName
import dev.chrisbanes.haze.HazeState
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import androidx.compose.ui.graphics.Color

@Composable
fun RecentFilesView(
    hazeState: HazeState,
    recentFiles: List<Uri>,
    onFileClick: (Uri) -> Unit,
    onOpenNewClick: () -> Unit
) {
    val context = LocalContext.current
    if (recentFiles.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Info,
            title = "Chưa mở file dữ liệu",
            description = "Hãy nhấn mở file để duyệt, tìm kiếm và chỉnh sửa danh sách từ điển của bạn dưới bộ nhớ máy.",
            buttonText = "Mở file từ điển (.txt)",
            onButtonClick = onOpenNewClick
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mở file gần đây",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentFiles) { uri ->
                    val defaultName = uri.path?.substringAfterLast('/')?.substringAfterLast(':') ?: uri.toString()
                    val fileName by produceState(initialValue = defaultName, key1 = uri) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getFileName(context, uri)
                        }
                    }
                    val displayPath = remember(uri, fileName) {
                        val path = uri.path ?: uri.toString()
                        val decoded = try { Uri.decode(path) } catch (e: Exception) { path }
                        if (decoded.endsWith(fileName)) {
                            decoded.replace(':', '/')
                        } else {
                            if (decoded.contains(':')) {
                                val beforeColon = decoded.substringBeforeLast(':')
                                val cleaned = beforeColon.replace(':', '/')
                                if (cleaned.endsWith("/")) "$cleaned$fileName" else "$cleaned/$fileName"
                            } else {
                                if (decoded.endsWith("/")) "$decoded$fileName" else "$decoded/$fileName"
                            }
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hazeGlassmorphism(hazeState, cornerRadius = 12)
                            .clickable { onFileClick(uri) },
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.elevatedCardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    displayPath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenNewClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mở file khác", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
