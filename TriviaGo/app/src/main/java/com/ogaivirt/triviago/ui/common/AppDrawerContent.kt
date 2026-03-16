package com.ogaivirt.triviago.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogaivirt.triviago.domain.model.UserProfile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.AdminPanelSettings

@Composable
fun AppDrawerContent(
    userProfile: UserProfile?,
    onCloseDrawer: () -> Unit,
    onProfileClick: () -> Unit,
    onMyQuizzesClick: () -> Unit,
    onCreateQuizClick: () -> Unit,
    onFindQuizClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToSupportDashboard: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onReportBugClick: () -> Unit,
    onVerifyAccountClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onProfileClick()
                        onCloseDrawer()
                    }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileImage(
                    imageUrl = userProfile?.profilePictureUrl,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = userProfile?.username ?: "Učitavanje...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))


            NavigationDrawerItem(
                label = { Text("Moji Kvizovi") },
                selected = false,
                onClick = { onMyQuizzesClick(); onCloseDrawer() },
                icon = { Icon(Icons.Default.Bookmark, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )


            if (userProfile?.roles?.contains("CREATOR") == true) {
                NavigationDrawerItem(
                    label = { Text("Napravi Kviz") },
                    selected = false,
                    onClick = { onCreateQuizClick(); onCloseDrawer() },
                    icon = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }


            NavigationDrawerItem(
                label = { Text("Pronađi Kviz") },
                selected = false,
                onClick = { onFindQuizClick(); onCloseDrawer() },
                icon = { Icon(Icons.Default.List, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )


            if (userProfile?.roles?.contains("SUPPORT") == true) {
                Divider()
                NavigationDrawerItem(
                    label = { Text("Tabla za podršku") },
                    selected = false,
                    onClick = { onCloseDrawer(); onNavigateToSupportDashboard() },
                    icon = { Icon(Icons.Default.SupportAgent, "Tabla za podršku") }
                )
                Divider()
            }


            if (userProfile?.roles?.contains("ADMIN") == true) {
                Divider()
                NavigationDrawerItem(
                    label = { Text("Admin Panel") },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onNavigateToAdminDashboard()
                    },
                    icon = { Icon(Icons.Default.AdminPanelSettings, "Admin Panel") }
                )
                Divider()
            }

            Spacer(modifier = Modifier.weight(1f))


            if (userProfile?.roles?.contains("VERIFIED") == false) {
                Button(
                    onClick = { onVerifyAccountClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Verifikuj Se")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }



            Button(
                onClick = { onReportBugClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Prijavi grešku")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSignOutClick(); onCloseDrawer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Odjavi se")
            }
        }
    }
}