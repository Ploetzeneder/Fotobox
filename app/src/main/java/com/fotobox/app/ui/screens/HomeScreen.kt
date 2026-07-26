package com.fotobox.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fotobox.app.ui.viewmodels.HomeViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    onStartCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSlideShow: () -> Unit,
    onOpenAudioGuestbook: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val sessionCount by viewModel.sessionCount.collectAsState()
    val audioCount by viewModel.audioCount.collectAsState()
    val eventName by viewModel.eventName.collectAsState()

    // Hintergrund-Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rotate"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Animierter Hintergrund mit rotierenden Kreisen
        AnimatedBackground(rotation = rotation)

        // Dunkler Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // Top-Right Navigation
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NavButton(
                icon = Icons.Default.Collections,
                label = if (sessionCount > 0) "$sessionCount Fotos" else "Galerie",
                onClick = onOpenGallery
            )
            NavButton(
                icon = Icons.Default.Slideshow,
                label = "Slideshow",
                onClick = onStartSlideShow
            )
            NavButton(
                icon = Icons.Default.Mic,
                label = if (audioCount > 0) "$audioCount Stimmen" else "Gästebuch",
                highlight = audioCount > 0,
                onClick = onOpenAudioGuestbook
            )
            NavButton(
                icon = Icons.Default.Settings,
                label = "Einstellungen",
                onClick = onOpenSettings
            )
        }

        // Center Content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (eventName.isNotEmpty()) {
                Text(
                    text = eventName.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                "FOTOBOX",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Das ultimative Foto-Erlebnis",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White.copy(0.55f)
                )
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Großer START-Button
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .clickable(onClick = onStartCamera),
                contentAlignment = Alignment.Center
            ) {
                // Äußerer Ring
                Box(
                    modifier = Modifier
                        .size(166.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "START",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Tippe um Fotos aufzunehmen",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(0.45f)
                ),
                textAlign = TextAlign.Center
            )
        }

        // Bottom Status-Zeile
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusChip(
                text = if (sessionCount > 0) "$sessionCount Aufnahmen" else "Keine Aufnahmen",
                color = Color.White.copy(0.25f)
            )
            StatusChip(text = "Fotobox v1.0", color = Color.White.copy(0.15f))
            StatusChip(
                text = if (audioCount > 0) "$audioCount Stimmen" else "Gästebuch leer",
                color = Color.White.copy(0.25f)
            )
        }
    }
}

@Composable
private fun AnimatedBackground(rotation: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
            // Große rotierende Kreise
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f),
                    radius = 500f
                ),
                radius = 500f,
                center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(0.25f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f),
                    radius = 400f
                ),
                radius = 400f,
                center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f)
            )
        }
        // Hintergrundfarbe
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0A0520), Color(0xFF150A2A))
            )
        )
        // Kreise erneut über den Hintergrund legen
        rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(0.35f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f),
                    radius = 500f
                ),
                radius = 500f,
                center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f),
                    radius = 400f
                ),
                radius = 400f,
                center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f)
            )
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primary.copy(0.25f)
                else Color.Black.copy(0.4f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, label,
            tint = if (highlight) MaterialTheme.colorScheme.primary else Color.White.copy(0.85f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (highlight) MaterialTheme.colorScheme.primary else Color.White.copy(0.6f),
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.7f)))
    }
}
