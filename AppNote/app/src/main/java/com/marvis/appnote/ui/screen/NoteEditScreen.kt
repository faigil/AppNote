package com.marvis.appnote.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.marvis.appnote.data.AppDatabase
import com.marvis.appnote.data.entity.Note
import com.marvis.appnote.repository.NoteRepository
import com.marvis.appnote.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModel.Factory(NoteRepository(db.noteDao()))
    )
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(noteId == -1L) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Load existing note
    LaunchedEffect(noteId) {
        if (noteId != -1L) {
            val note = db.noteDao().getNoteById(noteId)
            note?.let {
                title = it.title
                content = it.content
                imagePaths = try {
                    val arr = JSONArray(it.imagePaths)
                    (0 until arr.length()).map { i -> arr.getString(i) }
                } catch (_: Exception) { emptyList() }
                isLoaded = true
            }
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val dest = File(context.cacheDir, "note_img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
            imagePaths = imagePaths + dest.absolutePath
        }
    }

    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                val dest = File(context.cacheDir, "note_img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
                imagePaths = imagePaths + dest.absolutePath
            }
        }
    }

    if (!isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1L) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val note = Note(
                            id = if (noteId == -1L) 0 else noteId,
                            title = title,
                            content = content,
                            imagePaths = JSONArray(imagePaths).toString()
                        )
                        viewModel.save(note) { onBack() }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    if (noteId != -1L) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("输入内容…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Image section
            Text("图片", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Image grid
            if (imagePaths.isNotEmpty()) {
                var columns = 3
                for (i in imagePaths.indices step columns) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (j in i until minOf(i + columns, imagePaths.size)) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                AsyncImage(
                                    model = File(imagePaths[j]),
                                    contentDescription = "笔记图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        imagePaths = imagePaths.toMutableList().also { it.removeAt(j) }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "移除图片",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Add image button
            OutlinedButton(onClick = { showImagePicker = true }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("插入图片")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Image picker dialog
    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("插入图片") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImagePicker = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("从相册选择")
                    }
                    TextButton(
                        onClick = {
                            showImagePicker = false
                            val file = File(context.cacheDir, "camera_photos/photo_${System.currentTimeMillis()}.jpg")
                            file.parentFile?.mkdirs()
                            tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(tempPhotoUri!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("拍照")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePicker = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确认删除这条笔记？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(Note(id = noteId))
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
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