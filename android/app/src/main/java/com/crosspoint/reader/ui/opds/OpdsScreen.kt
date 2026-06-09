package com.crosspoint.reader.ui.opds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.crosspoint.reader.data.opds.OpdsEntry
import com.crosspoint.reader.data.opds.OpdsServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsScreen(
    rootUrl: String? = null,
    onBack: () -> Unit,
    onNavigateToCatalog: (url: String) -> Unit,
    viewModel: OpdsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(rootUrl) {
        rootUrl?.let { viewModel.loadFeed(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = uiState) {
                        is OpdsUiState.Feed -> s.feed.title
                        else -> "OPDS Catalogs"
                    }
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState) {
                            is OpdsUiState.Feed -> viewModel.backToServerList()
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState is OpdsUiState.ServerList) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Outlined.Add, "Add server")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is OpdsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is OpdsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.backToServerList() }) { Text("Back") }
                    }
                }

                is OpdsUiState.Downloading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Downloading "${state.title}"…")
                    }
                }

                is OpdsUiState.ServerList -> {
                    if (state.servers.isEmpty()) {
                        EmptyCatalogHint(onAdd = { showAddDialog = true })
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.servers) { server ->
                                ServerListItem(
                                    server = server,
                                    onClick = { viewModel.loadServer(server) },
                                    onLongClick = { viewModel.removeServer(server) }
                                )
                            }
                        }
                    }
                }

                is OpdsUiState.Feed -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.feed.entries, key = { it.id }) { entry ->
                            OpdsEntryItem(
                                entry = entry,
                                onClick = {
                                    when {
                                        entry.acquisitionUrl != null ->
                                            viewModel.downloadAndOpen(
                                                context, entry.acquisitionUrl, entry.title
                                            )
                                        entry.navigationUrl != null ->
                                            onNavigateToCatalog(entry.navigationUrl)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { server ->
                viewModel.addServer(server)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerListItem(server: OpdsServer, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.onBackground)
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                server.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun OpdsEntryItem(entry: OpdsEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.coverUrl != null) {
            Surface(
                modifier = Modifier.size(width = 44.dp, height = 60.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            val icon = if (entry.isAcquisition) Icons.Outlined.Book else Icons.Outlined.Folder
            Icon(icon, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.author.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(entry.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            if (entry.isAcquisition) Icons.Outlined.Download else Icons.Outlined.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun EmptyCatalogHint(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Language, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("No OPDS servers added", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAdd) { Text("Add server") }
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onAdd: (OpdsServer) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add OPDS server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(OpdsServer(name = name.trim(), url = url.trim(), username = username, password = password)) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
