package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.StreamWebView
import com.example.ui.MainViewModel

@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    url: String,
    title: String,
    progress: Int,
    isDesktopMode: Boolean,
    isAdBlockEnabled: Boolean,
    blockedCount: Int,
    isPlaying: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember(url) { mutableStateOf(url) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxSize()) {
        // Address Bar Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.activeWebView?.goBack() },
                        enabled = viewModel.activeWebView?.canGoBack() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (viewModel.activeWebView?.canGoBack() == true) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = { viewModel.activeWebView?.goForward() },
                        enabled = viewModel.activeWebView?.canGoForward() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (viewModel.activeWebView?.canGoForward() == true) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                    }

                    IconButton(onClick = { viewModel.activeWebView?.reload() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("browser_address_bar"),
                        placeholder = { Text("Search YouTube or enter URL", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                onNavigate(textInput)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Go",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            onNavigate(textInput)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = { viewModel.toggleDesktopMode() }) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Phonelink,
                            contentDescription = "Desktop mode toggle",
                            tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Launch Shortcuts & AdBlock Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AdBlock Badge Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.toggleAdBlock() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "AdBlock",
                                tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdBlockEnabled) "Ad-Free ($blockedCount)" else "AdBlock Off",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    // YouTube Quick Shortcuts
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickChip("YouTube", "https://m.youtube.com") { onNavigate(it) }
                        QuickChip("Music", "https://music.youtube.com") { onNavigate(it) }
                        QuickChip("Lo-Fi Live", "https://m.youtube.com/results?search_query=lofi+hip+hop+radio") { onNavigate(it) }
                        QuickChip("Podcasts", "https://m.youtube.com/results?search_query=podcasts") { onNavigate(it) }
                    }
                }
            }
        }

        // Loading Bar
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Web Content View
        Box(modifier = Modifier.weight(1f)) {
            StreamWebView(
                url = url,
                isDesktopMode = isDesktopMode,
                isAdBlockEnabled = isAdBlockEnabled,
                onUrlChanged = { newUrl -> viewModel.updateUrl(newUrl) },
                onTitleChanged = { newTitle -> viewModel.updateTitle(newTitle) },
                onProgressChanged = { newProgress -> viewModel.updateProgress(newProgress) },
                onAdBlocked = { viewModel.incrementAdBlockedCount() },
                onMediaStateChanged = { playing, mediaTitle, mediaUrl ->
                    viewModel.onMediaStateChanged(playing, mediaTitle, mediaUrl)
                },
                onWebViewCreated = { webView ->
                    viewModel.activeWebView = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun QuickChip(label: String, targetUrl: String, onClick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick(targetUrl) }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
