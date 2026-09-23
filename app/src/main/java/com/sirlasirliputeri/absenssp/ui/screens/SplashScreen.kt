// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/SplashScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sirlasirliputeri.absenssp.R
import com.sirlasirliputeri.absenssp.ui.theme.CreamBg
import com.sirlasirliputeri.absenssp.ui.theme.NavyDark
import com.sirlasirliputeri.absenssp.ui.theme.TextSecondary

/** Ditampilkan singkat saat app baru dibuka (cold start), sebelum masuk ke layar utama. */
@Composable
fun SplashScreen() {
    val bg = Brush.verticalGradient(listOf(CreamBg, MaterialTheme.colorScheme.surface))
    Column(
        modifier = Modifier.fillMaxSize().background(bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_brand),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(56.dp).width(112.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("SSP Super App", style = MaterialTheme.typography.headlineSmall, color = NavyDark, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("PT Sirla Sirli Puteri", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
