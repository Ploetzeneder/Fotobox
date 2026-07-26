package com.fotobox.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fotobox.app.ui.viewmodels.SlideShowViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SlideShowScreen(
    onBack: () -> Unit,
    viewModel: SlideShowViewModel = hiltViewModel()
) {
    val currentPath by viewModel.currentImagePath.collectAsState()
    val hasImages by viewModel.hasImages.collectAsState()
    val eventName by viewModel.eventName.collectAsState()
    val photoCount by viewModel.photoCount.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    var currentTime by remember { mutableStateOf(formatTime()) }

    LaunchedEffect(Unit) {
        viewModel.start()
        while (true) {
            delay(30_000L)
            currentTime = formatTime()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.next() }
    ) {
        if (hasImages) {
            Crossfade(
                targetState = currentPath,
                animationSpec = tween(1200),
                label = "slideshow"
            ) { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Top gradient scrim for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent))
                    )
                    .align(Alignment.TopCenter)
            )

            // Bottom gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.65f)))
                    )
                    .align(Alignment.BottomCenter)
            )

            // Event name overlay (top-left)
            if (eventName.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 20.dp)
                ) {
                    Text(
                        eventName.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                    )
                    Text(
                        currentTime,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(0.6f)
                        )
                    )
                }
            } else {
                // Show time even without event name
                Text(
                    currentTime,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 24.dp)
                )
            }

            // Photo counter (bottom-left)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${currentIndex + 1} / $photoCount",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.7f))
                )
            }

            // Tap hint (bottom-center, fades in color palette)
            Text(
                "Tippen für nächstes Foto",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.3f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
            )

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📷",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Noch keine Fotos",
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Aufnahmen erscheinen hier automatisch",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.4f)),
                        modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Close button (top-right, always visible)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, "Schließen", tint = Color.White)
        }
    }
}

private fun formatTime(): String =
    SimpleDateFormat("HH:mm · dd. MMMM", Locale.GERMAN).format(Date())
