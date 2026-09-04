package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewo.model.MediaType
import com.example.viewo.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminMediaScreen(viewModel: AdminViewModel) {
    val mediaItems by viewModel.media.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<com.example.viewo.model.Media?>(null) }
    var mediaToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Media")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Media Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            val isLoading by viewModel.isLoading.collectAsState()
            
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && mediaItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (mediaItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No media found. Click + to add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = mediaItems,
                            key = { it.id }
                        ) { media ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .combinedClickable(
                                        onClick = { selectedMedia = media },
                                        onLongClick = { mediaToDelete = media.id }
                                    )
                            ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (media.type == MediaType.IMAGE) {
                                            val displayUrl = media.url.replace("10.0.2.2", "10.58.187.64").let { if (it.startsWith("http")) it else "http://10.58.187.64:8081/$it" }
                                            coil.compose.AsyncImage(
                                                model = displayUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.VideoFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = media.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = media.type.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var mediaName by remember { mutableStateOf("") }
        var isVideo by remember { mutableStateOf(false) }
        var mediaUrl by remember { mutableStateOf("") }
        var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }

        val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedUri = uri
                val mime = context.contentResolver.getType(uri)
                isVideo = mime?.startsWith("video") == true
                // Auto-fill name if blank
                if (mediaName.isBlank()) {
                    mediaName = "Uploaded File"
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (selectedUri != null) "Upload Local Media" else "Add Media URL") },
            text = {
                Column {
                    OutlinedTextField(
                        value = mediaName,
                        onValueChange = { mediaName = it },
                        label = { Text("Media Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Type:")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = !isVideo, onClick = { isVideo = false }, enabled = selectedUri == null)
                        Text("Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = isVideo, onClick = { isVideo = true }, enabled = selectedUri == null)
                        Text("Video")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (selectedUri == null) {
                        OutlinedTextField(
                            value = mediaUrl,
                            onValueChange = { mediaUrl = it },
                            label = { Text("Direct URL (.jpg, .mp4, etc)") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Or Upload from Device")
                        }
                    } else {
                        Text(
                            text = "File selected for upload",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { selectedUri = null }) {
                            Text("Remove File")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedDuration = 10
                    val type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE

                    if (selectedUri != null && mediaName.isNotBlank()) {
                        viewModel.uploadLocalMedia(context, selectedUri!!, mediaName, type, if (isVideo) null else parsedDuration)
                        showAddDialog = false
                    } else if (mediaName.isNotBlank() && mediaUrl.isNotBlank()) {
                        viewModel.createMedia(mediaName, type, mediaUrl, if (isVideo) null else parsedDuration)
                        showAddDialog = false
                    }
                }) {
                    Text(if (selectedUri != null) "Upload" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    mediaName = ""
                    mediaUrl = ""
                    selectedUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedMedia != null) {
        AlertDialog(
            onDismissRequest = { selectedMedia = null },
            title = { Text(selectedMedia!!.name) },
            text = {
                if (selectedMedia!!.type == MediaType.IMAGE) {
                    val displayUrl = selectedMedia!!.url.replace("10.0.2.2", "10.58.187.64").let { if (it.startsWith("http")) it else "http://10.58.187.64:8081/$it" }
                    coil.compose.AsyncImage(
                        model = displayUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Video preview not available in Admin")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMedia = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (mediaToDelete != null) {
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to delete this media item?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMedia(mediaToDelete!!)
                    mediaToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
