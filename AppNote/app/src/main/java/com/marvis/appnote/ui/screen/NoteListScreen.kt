package com.marvis.appnote.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marvis.appnote.repository.NoteRepository
import com.marvis.appnote.ui.component.NoteCard
import com.marvis.appnote.viewmodel.NoteViewModel
import com.marvis.appnote.data.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModel.Factory(NoteRepository(db.noteDao()))
    )
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
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
                placeholder = { Text("搜索笔记…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {}
        } else {
            TopAppBar(
                title = { Text("笔记") },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )
        }

        // Content
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无笔记，点击右下角新建", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onNoteClick(note.id) })
                }
            }
        }

        // FAB
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = onNewNote,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        }
    }
}