package com.fotobox.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fotobox.app.ui.viewmodels.SlideShowViewModel

@Composable
fun SlideShowScreen(
    onBack: () -> Unit,
    viewModel: SlideShowViewModel = hiltViewModel()
) {
    val currentPath by viewModel.currentImagePath.collectAsState()
    val hasImages by viewModel.hasImages.collectAsState()

    LaunchedEffect(Unit) { viewModel.start() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { /* tap to advance */ viewModel.next() }
    ) {
        if (hasImages) {
            Crossfade(
                targetState = currentPath,
                animationSpec = tween(1000),
                label = "slideshow"
            ) { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Noch keine Fotos in der Galerie",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

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
