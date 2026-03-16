package com.ogaivirt.triviago.ui.screens.home

import android.content.Intent
import android.net.Uri // Import Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult // Import rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts // Import ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import com.airbnb.lottie.compose.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ogaivirt.triviago.R
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import com.ogaivirt.triviago.domain.model.UserProfile
import com.ogaivirt.triviago.ui.common.AppDrawerContent
import com.ogaivirt.triviago.ui.common.ProfileImage
import kotlinx.coroutines.flow.collectLatest // Import collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navBackStackEntry: NavBackStackEntry,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizList: () -> Unit,
    onNavigateToMyQuizzes: () -> Unit,
    onMarkerClick: (quizId: String, questionId: String) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSupportDashboard: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val context = LocalContext.current
    val updatedStat = navBackStackEntry.savedStateHandle.get<QuestionStatistic?>("updated_stat")

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onVerificationImageSelected(uri)
    }

    LaunchedEffect(updatedStat) {
        if (updatedStat != null) {
            viewModel.updateQuestionBag(updatedStat)
            navBackStackEntry.savedStateHandle.remove<QuestionStatistic>("updated_stat")
        }
    }

    LaunchedEffect(viewModel.mapState) {
        viewModel.mapState.collect { state ->
            state.infoMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.onInfoMessageShown()
            }
        }
    }

    // Listen for events from ViewModel (like launching image picker)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HomeViewEvent.LaunchImagePicker -> {
                    imagePickerLauncher.launch("image/*") // Launch the image picker
                }
            }
        }
    }


    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var lockedIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var unlockedIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val mapState by viewModel.mapState.collectAsState()

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val locationManager = context.getSystemService(android.location.LocationManager::class.java)
                val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
                if (isGpsEnabled) {
                    viewModel.forceGpsEnabled()
                    viewModel.startLocationUpdates()
                    viewModel.resetAndReloadData()
                    if (locationPermissionsState.allPermissionsGranted) {
                        viewModel.startLocationUpdates()
                        viewModel.resetAndReloadData()
                    }
                } else {
                    viewModel.onGpsDisabled()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TriviaGO") },
                    actions = {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Obaveštenja")
                        }
                        IconButton(onClick = viewModel::openDrawer) {
                            ProfileImage(
                                imageUrl = mapState.profilePictureUrl,
                                size = 32.dp
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (locationPermissionsState.allPermissionsGranted) {
                when {
                    mapState.isLoading && !mapState.isVerificationLoading -> { // Show loading only if not verification loading
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            val composition by rememberLottieComposition(
                                spec = LottieCompositionSpec.RawRes(R.raw.loading_animation)
                            )
                            LottieAnimation(
                                composition = composition,
                                iterations = LottieConstants.IterateForever,
                                modifier = Modifier.size(150.dp)
                            )
                        }
                    }
                    !mapState.isGpsEnabled -> {
                        GpsDisabledScreen(
                            onEnableGpsClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    // Condition changed slightly to ensure it doesn't show briefly during loading
                    mapState.mapMarkers.isEmpty() && !mapState.isLoading && !mapState.isVerificationLoading -> {
                        val canCreateQuiz = mapState.userProfile?.roles?.contains("CREATOR") == true
                        NoQuizSelectedScreen(
                            onFindQuizClick = onNavigateToQuizList,
                            onCreateQuizClick = onNavigateToCreateQuiz,
                            onActivateQuizClick = onNavigateToMyQuizzes,
                            canCreateQuiz = canCreateQuiz,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    else -> { // Show map if markers exist OR if verification is loading
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(LatLng(44.787197, 20.457273), 12f)
                        }

                        LaunchedEffect(mapState.lastKnownLocation) {
                            mapState.lastKnownLocation?.let {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(it, 15f),
                                    durationMs = 1500
                                )
                            }
                        }

                        GoogleMap(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                            onMapLoaded = {
                                if (lockedIcon == null || unlockedIcon == null) {
                                    try {
                                        ContextCompat.getDrawable(context, R.drawable.question_mark_closed)?.let {
                                            val bitmap = it.toBitmap(width = 100, height = 100)
                                            lockedIcon = BitmapDescriptorFactory.fromBitmap(bitmap)
                                        }
                                        ContextCompat.getDrawable(context, R.drawable.question_mark_open)?.let {
                                            val bitmap = it.toBitmap(width = 100, height = 100)
                                            unlockedIcon = BitmapDescriptorFactory.fromBitmap(bitmap)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("HomeScreen", "Error creating marker icons", e)
                                    }
                                }
                            }
                        ) {
                            if (lockedIcon != null && unlockedIcon != null) {
                                mapState.mapMarkers.forEach { marker ->
                                    Marker(
                                        state = MarkerState(position = marker.position),
                                        title = if (marker.isLocked) "Zaključano" else "Pitanje",
                                        snippet = if (marker.isLocked) "Priđite bliže da otključate" else "Klikni da odgovoriš",
                                        icon = if (marker.isLocked) lockedIcon else unlockedIcon,
                                        onClick = {
                                            if (!marker.isLocked) {
                                                val questionToShow = viewModel.onMarkerClick()
                                                if (questionToShow != null) {
                                                    onMarkerClick(questionToShow.quizId, questionToShow.id)
                                                } else {
                                                    Toast.makeText(context, "Nema dostupnih pitanja u blizini.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Moraš prići bliže!", Toast.LENGTH_SHORT).show()
                                            }
                                            true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Dozvola za lokaciju je neophodna", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Da bismo vam prikazali kvizove na mapi, potrebna nam je vaša dozvola za pristup lokaciji.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Text("Dozvoli pristup")
                    }
                    if (!locationPermissionsState.shouldShowRationale && !locationPermissionsState.allPermissionsGranted && !mapState.isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ako ste odbili dozvolu, morate je omogućiti u podešavanjima aplikacije.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }


        AnimatedVisibility(
            visible = mapState.isDrawerVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = viewModel::closeDrawer
                    )
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                AppDrawerContent(
                    userProfile = mapState.userProfile,
                    onCloseDrawer = viewModel::closeDrawer,
                    onProfileClick = onNavigateToProfile,
                    onMyQuizzesClick = onNavigateToMyQuizzes,
                    onCreateQuizClick = onNavigateToCreateQuiz,
                    onFindQuizClick = onNavigateToQuizList,
                    onSignOutClick = onSignOut,
                    onNavigateToSupportDashboard = onNavigateToSupportDashboard,
                    onNavigateToAdminDashboard = onNavigateToAdminDashboard,
                    onReportBugClick = viewModel::openBugReportDialog,
                    onVerifyAccountClick = viewModel::onVerifyAccountClick
                )
            }
        }

        if (mapState.showBugReportDialog) {
            BugReportDialog(
                onDismiss = viewModel::closeBugReportDialog,
                onSubmit = viewModel::onReportBugSubmit
            )
        }

        // Loading overlay for verification process
        if (mapState.isVerificationLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)) // Semi-transparent overlay
                    .clickable(enabled = false, onClick = {}), // Block clicks
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun GpsDisabledScreen(onEnableGpsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search, // Placeholder
            contentDescription = "GPS isključen",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("GPS je isključen", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Da biste koristili mapu, molimo vas, uključite lokaciju na vašem uređaju.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnableGpsClick) {
            Text("Otvori podešavanja")
        }
    }
}

@Composable
fun NoQuizSelectedScreen(
    onFindQuizClick: () -> Unit,
    onCreateQuizClick: () -> Unit,
    onActivateQuizClick: () -> Unit,
    canCreateQuiz: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Pronađi kviz",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Nema selektovanih kvizova",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Trenutno nemate nijedan aktivan kviz za prikaz na mapi.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onFindQuizClick) {
            Text("Pronađi Kviz")
        }

        OutlinedButton(
            onClick = onActivateQuizClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aktiviraj pretplaćene kvizove")
        }

        if (canCreateQuiz) {
            Text("ili", modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(
                onClick = onCreateQuizClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Napravi novi kviz")
            }
        }
    }
}

@Composable
fun BugReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prijavi grešku") },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opišite problem") },
                placeholder = { Text("Npr: Dugme X ne radi kada...") },
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onSubmit(description) }) {
                Text("Pošalji")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži")
            }
        }
    )
}