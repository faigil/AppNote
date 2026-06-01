package com.marvis.appnote.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marvis.appnote.data.AppDatabase
import com.marvis.appnote.data.entity.AppInfo
import com.marvis.appnote.repository.AppInfoRepository
import com.marvis.appnote.viewmodel.AppInfoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: AppInfoViewModel = viewModel(
        factory = AppInfoViewModel.Factory(AppInfoRepository(context, db.appInfoDao()))
    )
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto-refresh if empty
    LaunchedEffect(Unit) {
        if (db.appInfoDao().count() == 0) {
            viewModel.refresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar or top bar
        if (showSearch) {
            SearchBar(
                query = searchText,
                onQueryChange = {
                    searchText = it
                    viewModel.setSearch(it)
                },
                onSearch = { viewModel.setSearch(searchText) },
                active = false,
                onActiveChange = {},
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        searchText = ""
                        viewModel.setSearch("")
                        showSearch = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                    }
                },
                placeholder = { Text("搜索应用…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {}
        } else {
            TopAppBar(
                title = { Text("应用信息") },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        }

        // Tab row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; viewModel.setTab(0) }) {
                Text("全部", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; viewModel.setTab(1) }) {
                Text("系统应用", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2; viewModel.setTab(2) }) {
                Text("用户应用", modifier = Modifier.padding(12.dp))
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppListItem(app = app, onClick = { selectedApp = app })
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    // Detail dialog
    selectedApp?.let { app ->
        AppDetailDialog(app = app, onDismiss = { selectedApp = null }, context = context)
    }
}

@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(
                if (app.isSystemApp) Icons.Default.Android else Icons.Default.Apps,
                contentDescription = null,
                tint = if (app.isSystemApp) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Text(
                app.versionName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun AppDetailDialog(app: AppInfo, onDismiss: () -> Unit, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.appName, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("包名", app.packageName) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("package", app.packageName))
                }
                DetailRow("版本", "${app.versionName} (${app.versionCode})")
                DetailRow("类型", if (app.isSystemApp) "系统应用" else "用户应用")
                DetailRow("安装时间", formatFullTime(app.installTime))
                DetailRow("更新时间", formatFullTime(app.updateTime))
                DetailRow("目标SDK", app.targetSdk.toString())
                DetailRow("最低SDK", app.minSdk.toString())
                DetailRow("APK大小", formatSize(app.apkSize))
                DetailRow("数据大小", formatSize(app.dataSize))
                DetailRow("UID", app.uid.toString())
                if (app.launchActivity.isNotBlank()) {
                    DetailRow("启动页", app.launchActivity)
                }
                if (app.signatures.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("签名 (SHA-256):", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(100.dp))
                        Text(
                            app.signatures.take(64) + if (app.signatures.length > 64) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                    }
                }
                if (app.permissions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("权限列表:", style = MaterialTheme.typography.labelMedium)
                    val perms = try {
                        org.json.JSONArray(app.permissions)
                    } catch (_: Exception) { org.json.JSONArray() }
                    Column {
                        for (i in 0 until perms.length()) {
                            Text(
                                "  ${perms.getString(i)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(100.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        onCopy?.let {
            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(14.dp))
            }
        }
    }
}

fun formatFullTime(millis: Long): String {
    if (millis == 0L) return "未知"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatSize(bytes: Long): String {
    if (bytes == 0L) return "未知"
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}