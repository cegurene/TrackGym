package com.example.gimnasio.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SocialBrand(
    val badgeBackground: Color,
    val badgeContent: Color,
    val label: String
) {
    GOOGLE(Color(0xFFFFFFFF), Color(0xFF4285F4), "G"),
    APPLE(Color(0xFF111111), Color(0xFFFFFFFF), "A")
}

@Composable
fun SocialAuthButtonsRow(
    googleEnabled: Boolean,
    appleEnabled: Boolean,
    googleLoading: Boolean,
    appleLoading: Boolean,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SocialBrandButton(
            brand = SocialBrand.GOOGLE,
            enabled = googleEnabled,
            loading = googleLoading,
            onClick = onGoogleClick,
            modifier = Modifier.weight(1f)
        )
        SocialBrandButton(
            brand = SocialBrand.APPLE,
            enabled = appleEnabled,
            loading = appleLoading,
            onClick = onAppleClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SocialBrandButton(
    brand: SocialBrand,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(brand.badgeBackground)
                .border(1.dp, if (brand == SocialBrand.GOOGLE) Color(0xFFE0E0E0) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (loading) "…" else brand.label,
                color = brand.badgeContent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (loading) "Conectando..." else brand.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge
        )
    }
}
