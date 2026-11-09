package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalRoomRepository
import com.example.data.local.StrideApexLocalDatabase
import com.example.data.repository.TrainingRepository
import com.example.ui.AuthScreen
import com.example.ui.BiomechanicsLabScreen
import com.example.ui.DashboardScreen
import com.example.ui.RacePredictorAndPrScreen
import com.example.ui.StravaOcrScreen
import com.example.ui.signOutUser
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.StrideApexTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.util.configureAppCheck
import com.example.viewmodel.StrideApexViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class StrideApexTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard),
    STRAVA_OCR("strava_ocr", R.string.nav_ocr_scan, Icons.Default.AddAPhoto),
    PREDICT_PRS("predict_prs", R.string.nav_predict_prs, Icons.Default.Speed),
    BIOMECHANICS("biomechanics", R.string.nav_biomechanics, Icons.AutoMirrored.Filled.DirectionsRun)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            StrideApexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    // Default to launching directly into Offline SQLite Mode if not signed in, so login is optional
    var showAuthScreen by rememberSaveable { mutableStateOf(false) }

    if (user == null && showAuthScreen) {
        BackHandler {
            showAuthScreen = false
        }
        AuthScreen(
            onAuthSuccess = { showAuthScreen = false },
            onContinueOffline = { showAuthScreen = false }
        )
    } else {
        val activeKey = user?.uid ?: "offline_local_user"
        key(activeKey) {
            MainFeatureScreen(
                currentUserId = user?.uid,
                userEmail = user?.email ?: user?.displayName ?: "Offline Mode • Local SQLite",
                onOpenSignIn = { showAuthScreen = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String?,
    userEmail: String,
    onOpenSignIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val isOfflineMode = currentUserId.isNullOrBlank()
    val vmKey = currentUserId ?: "offline_local_user"

    val viewModel: StrideApexViewModel = viewModel(
        key = vmKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val localDatabase = StrideApexLocalDatabase.getInstance(app)
                val localRepository = LocalRoomRepository(localDatabase.localTrainingDao())
                val cloudRepository = if (!currentUserId.isNullOrBlank()) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    TrainingRepository(db)
                } else {
                    null
                }
                StrideApexViewModel(
                    localRepository = localRepository,
                    cloudRepository = cloudRepository,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val workoutsState by viewModel.workoutsState.collectAsStateWithLifecycle()
    val prsState by viewModel.prsState.collectAsStateWithLifecycle()
    val goalsState by viewModel.goalsState.collectAsStateWithLifecycle()
    val backupsState by viewModel.backupsState.collectAsStateWithLifecycle()
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val fatigueExponent by viewModel.fatigueExponent.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(StrideApexTab.DASHBOARD) }

    if (currentTab != StrideApexTab.DASHBOARD) {
        BackHandler {
            currentTab = StrideApexTab.DASHBOARD
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateSurface,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isOfflineMode) AeroCyan else SuccessGreen,
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOfflineMode) AeroCyan else VoltLime
                                )
                            }
                        }
                    },
                    actions = {
                        if (isOfflineMode) {
                            OutlinedButton(
                                onClick = onOpenSignIn,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltLime),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("open_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Sign In for Cloud Sync",
                                    tint = VoltLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cloud Sign-In",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VoltLime
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = SlateSurface,
                        tonalElevation = 8.dp
                    ) {
                        StrideApexTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = VoltLime,
                                    indicatorColor = VoltLime,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StrideApexTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = VoltLime,
                                    indicatorColor = VoltLime,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
                        Surface(
                            color = SlateSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(1.dp, VoltLime, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VoltLime,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                    Text("Dismiss", color = AeroCyan)
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            StrideApexTab.DASHBOARD -> {
                                DashboardScreen(
                                    workoutsState = workoutsState,
                                    prsState = prsState,
                                    goalsState = goalsState,
                                    backupsState = backupsState,
                                    isOfflineMode = isOfflineMode,
                                    onOpenSignIn = onOpenSignIn,
                                    onNavigateToOcr = { currentTab = StrideApexTab.STRAVA_OCR },
                                    onNavigateToPredict = { currentTab = StrideApexTab.PREDICT_PRS },
                                    onCreateCloudBackup = { label -> viewModel.createCloudBackupCheckpoint(label) },
                                    onExportBackupJson = { viewModel.exportFullTrainingBackupJson() },
                                    onDeleteWorkout = { id -> viewModel.deleteWorkout(id) }
                                )
                            }
                            StrideApexTab.STRAVA_OCR -> {
                                StravaOcrScreen(
                                    ocrState = ocrState,
                                    onAnalyzeBitmap = { bmp -> viewModel.analyzeStravaScreenshot(bmp) },
                                    onSaveWorkout = { title, dist, dur, hr, cad, elev, splits, insight, src ->
                                        viewModel.saveWorkoutAndCheckPr(
                                            title = title,
                                            distanceKm = dist,
                                            durationSeconds = dur,
                                            avgHeartRate = hr,
                                            cadenceSpm = cad,
                                            elevationGainM = elev,
                                            splitsSummary = splits,
                                            aiCoachingInsight = insight,
                                            sourceType = src,
                                            onComplete = { currentTab = StrideApexTab.DASHBOARD }
                                        )
                                    }
                                )
                            }
                            StrideApexTab.PREDICT_PRS -> {
                                RacePredictorAndPrScreen(
                                    workoutsState = workoutsState,
                                    prsState = prsState,
                                    goalsState = goalsState,
                                    fatigueExponent = fatigueExponent,
                                    onFatigueExponentChange = { exp -> viewModel.updateFatigueExponent(exp) },
                                    onUpsertPrManual = { cat, sec, title ->
                                        viewModel.upsertPersonalRecordManual(cat, sec, title)
                                    },
                                    onAddGoal = { title, cat, sec, wkKm, enabled, hr, min ->
                                        viewModel.addTrainingGoal(title, cat, sec, wkKm, enabled, hr, min)
                                    },
                                    onToggleGoalCompleted = { goal -> viewModel.toggleGoalCompleted(goal) },
                                    onDeleteGoal = { id -> viewModel.deleteGoal(id) }
                                )
                            }
                            StrideApexTab.BIOMECHANICS -> {
                                BiomechanicsLabScreen(
                                    coachState = coachState,
                                    onAskCoach = { question, mod -> viewModel.askBiomechanicsCoach(question, mod) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
