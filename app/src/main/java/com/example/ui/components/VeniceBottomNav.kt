package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VeniceAmber
import com.example.ui.theme.VeniceBorder
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceTextMuted
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.viewmodel.VeniceNavTab

@Composable
fun VeniceBottomNav(
    activeTab: VeniceNavTab,
    onTabSelected: (VeniceNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VeniceSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(width = (0.5).dp, color = VeniceBorder)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VeniceNavItem(
                title = "Chat",
                selected = activeTab == VeniceNavTab.CHAT,
                selectedIcon = Icons.AutoMirrored.Filled.Chat,
                unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
                testTag = "nav_chat",
                onClick = { onTabSelected(VeniceNavTab.CHAT) }
            )

            VeniceNavItem(
                title = "Studio",
                selected = activeTab == VeniceNavTab.IMAGE_STUDIO,
                selectedIcon = Icons.Default.Image,
                unselectedIcon = Icons.Outlined.Image,
                testTag = "nav_studio",
                onClick = { onTabSelected(VeniceNavTab.IMAGE_STUDIO) }
            )

            VeniceNavItem(
                title = "Tools",
                selected = activeTab == VeniceNavTab.INTELLIGENCE,
                selectedIcon = Icons.Default.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_intelligence",
                onClick = { onTabSelected(VeniceNavTab.INTELLIGENCE) }
            )

            VeniceNavItem(
                title = "Files",
                selected = activeTab == VeniceNavTab.LOCAL_FILES,
                selectedIcon = Icons.Default.Folder,
                unselectedIcon = Icons.Outlined.Folder,
                testTag = "nav_files",
                onClick = { onTabSelected(VeniceNavTab.LOCAL_FILES) }
            )

            VeniceNavItem(
                title = "Privacy",
                selected = activeTab == VeniceNavTab.PRIVACY_VAULT,
                selectedIcon = Icons.Default.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                testTag = "nav_privacy",
                onClick = { onTabSelected(VeniceNavTab.PRIVACY_VAULT) }
            )
        }
    }
}

@Composable
private fun VeniceNavItem(
    title: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = title,
            tint = if (selected) VeniceAmber else VeniceTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            color = if (selected) VeniceTextPrimary else VeniceTextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
