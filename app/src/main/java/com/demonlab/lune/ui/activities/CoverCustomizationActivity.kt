package com.demonlab.lune.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

class CoverCustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, PlayerCustomizationActivity::class.java))
        finish()
    }
}

@Composable
fun VinylRecordCover(
    painter: Painter,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .rotate(rotation)
            .clip(CircleShape)
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(0.9f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.8f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.7f).border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.fillMaxSize(0.6f).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))

        Surface(
            shape = CircleShape,
            modifier = Modifier.fillMaxSize(0.55f),
            border = BorderStroke(2.dp, Color(0xFF202020))
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF101010))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
    }
}
