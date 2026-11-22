package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalRoomRepository
import com.example.data.local.StrideApexLocalDatabase
import com.example.data.repository.TrainingRepository
import com.example.domain.ShoeUnlockEngine
import com.example.ui.AuthScreen
import com.example.ui.BiomechanicsLabScreen
import com.example.ui.DashboardScreen
import com.example.ui.HistoryScreen
import com.example.ui.ProgressScreen
import com.example.ui.RacePredictorAndPrScreen
import com.example.ui.ShoeDetailScreen
import com.example.ui.ShoesCatalogScreen
import com.example.ui.StravaOcrScreen
import com.example.ui.signOutUser
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OnVoltLime
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StrideApexTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeState
import com.example.ui.theme.VoltLime
import com.example.util.AdMobManager
import com.example.util.configureAppCheck
import com.example.viewmodel.StrideApexViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class StrideApexTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    PROGRESS("progress", R.string.nav_progress, Icons.Default.Insights),
    SHOES("shoes", R.string.nav_shoes, Icons.Default.ShoppingBag),
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard),
    HISTORY("history", R.string.nav_history, Icons.AutoMirrored.Filled.DirectionsRun),
    PREDICT_PRS("predict_prs", R.string.nav_predict_prs, Icons.Default.Speed)
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

        // Initialize Theme preference
        ThemeState.initialize(this)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // Initialize non-intrusive Google AdMob banner + preloaded short completion ad
        AdMobManager.initialize(this)
        AdMobManager.preloadShortAd(this)

        enableEdgeToEdge()
        setContent {
            StrideApexTheme(darkTheme = ThemeState.isDark) {
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
    val aiPlanLoading by viewModel.aiPlanLoading.collectAsStateWithLifecycle()
    val aiPlanCoachingInsight by viewModel.aiPlanCoachingInsight.collectAsStateWithLifecycle()
    val refreshingWorkoutId by viewModel.refreshingWorkoutId.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(StrideApexTab.DASHBOARD) }
    var selectedShoeSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var isScanScreenOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isScanScreenOpen || selectedShoeSlug != null || currentTab != StrideApexTab.DASHBOARD) {
        when {
            isScanScreenOpen -> isScanScreenOpen = false
            selectedShoeSlug != null -> selectedShoeSlug = null
            else -> currentTab = StrideApexTab.DASHBOARD
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
                        containerColor = ObsidianBackground,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Surface(
                                color = SlateSurfaceElevated,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(1.dp, VoltLime.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "SA",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltLime
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (isOfflineMode) AeroCyan else VoltLime,
                                        shape = CircleShape,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOfflineMode) AeroCyan else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { ThemeState.toggle(context) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (ThemeState.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (ThemeState.isDark) "Ganti ke Mode Terang" else "Ganti ke Mode Gelap",
                                tint = if (ThemeState.isDark) AeroCyan else VoltLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (isOfflineMode) {
                            IconButton(
                                onClick = onOpenSignIn,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("open_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Cloud Sign-In",
                                    tint = VoltLime,
                                    modifier = Modifier.size(20.dp)
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
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    Surface(
                        color = SlateSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = SlateBorder.copy(alpha = 0.6f)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(64.dp)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StrideApexTab.entries.forEach { tab ->
                                val selected = !isScanScreenOpen && currentTab == tab
                                val isCenterDashboard = tab == StrideApexTab.DASHBOARD
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(role = Role.Tab) {
                                            selectedShoeSlug = null
                                            isScanScreenOpen = false
                                            currentTab = tab
                                        }
                                        .testTag("nav_tab_${tab.route}")
                                ) {
                                    if (isCenterDashboard) {
                                        Surface(
                                            color = if (selected) VoltLime else AeroCyan.copy(alpha = 0.16f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) VoltLime else AeroCyan.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = stringResource(tab.labelRes),
                                                    tint = if (selected) OnVoltLime else AeroCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (selected) VoltLime else AeroCyan,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) VoltLime else Color.Transparent)
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes),
                                                tint = if (selected) OnVoltLime else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) VoltLime else TextSecondary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
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
                            val selected = !isScanScreenOpen && currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    selectedShoeSlug = null
                                    isScanScreenOpen = false
                                    currentTab = tab
                                },
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
                                    selectedIconColor = OnVoltLime,
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
                    LaunchedEffect(statusBanner) {
                        if (!statusBanner.isNullOrBlank()) {
                            delay(2500L)
                            viewModel.clearStatusBanner()
                        }
                    }

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
                        val activeWorkouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
                        val activeShoeDetail = selectedShoeSlug?.let { slug ->
                            val shoeProgress = remember(activeWorkouts) {
                                ShoeUnlockEngine.evaluateProgress(activeWorkouts)
                            }
                            ShoeUnlockEngine.findShoeBySlug(shoeProgress, slug)?.let { (shoe, tierState) ->
                                Triple(shoe, tierState, shoeProgress)
                            }
                        }

                        if (isScanScreenOpen) {
                            StravaOcrScreen(
                                ocrState = ocrState,
                                onAnalyzeBitmap = { bmp -> viewModel.analyzeStravaScreenshot(bmp) },
                                onSaveWorkout = { title, dist, dur, hr, cad, elev, splits, insight, src, dateEpochMs ->
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
                                        workoutDateEpochMs = dateEpochMs,
                                        onComplete = {
                                            AdMobManager.maybeShowShortAdAfterRunSaved(
                                                activity = context as? Activity,
                                                totalWorkoutsCount = activeWorkouts.size + 1
                                            ) {
                                                isScanScreenOpen = false
                                                currentTab = StrideApexTab.HISTORY
                                            }
                                        }
                                    )
                                },
                                onBack = { isScanScreenOpen = false }
                            )
                        } else if (activeShoeDetail != null) {
                            val (shoe, tierState, shoeProgress) = activeShoeDetail
                            ShoeDetailScreen(
                                shoe = shoe,
                                tierState = tierState,
                                shoeProgress = shoeProgress,
                                onBack = { selectedShoeSlug = null },
                                onSelectRelatedShoe = { relatedSlug -> selectedShoeSlug = relatedSlug }
                            )
                        } else {
                            when (currentTab) {
                                StrideApexTab.PROGRESS -> {
                                    ProgressScreen(
                                        workoutsState = workoutsState,
                                        goalsState = goalsState,
                                        onNavigateToShoes = { currentTab = StrideApexTab.SHOES }
                                    )
                                }
                                StrideApexTab.SHOES -> {
                                    ShoesCatalogScreen(
                                        workoutsState = workoutsState,
                                        onSelectShoe = { slug -> selectedShoeSlug = slug }
                                    )
                                }
                                StrideApexTab.DASHBOARD -> {
                                    DashboardScreen(
                                        workoutsState = workoutsState,
                                        prsState = prsState,
                                        goalsState = goalsState,
                                        backupsState = backupsState,
                                        isOfflineMode = isOfflineMode,
                                        onOpenSignIn = onOpenSignIn,
                                        onNavigateToOcr = { isScanScreenOpen = true },
                                        onNavigateToPredict = { currentTab = StrideApexTab.PREDICT_PRS },
                                        onNavigateToHistory = { currentTab = StrideApexTab.HISTORY },
                                        onNavigateToProgress = { currentTab = StrideApexTab.PROGRESS },
                                        onNavigateToShoes = { currentTab = StrideApexTab.SHOES },
                                        onCreateCloudBackup = { label -> viewModel.createCloudBackupCheckpoint(label) },
                                        onExportBackupJson = { viewModel.exportFullTrainingBackupJson() },
                                        onDeleteWorkout = { id -> viewModel.deleteWorkout(id) },
                                        onUpdateWorkout = { id, title, dist, dur, hr, cad, elev, splits, insight, dateEpochMs ->
                                            viewModel.updateWorkout(id, title, dist, dur, hr, cad, elev, splits, insight, dateEpochMs)
                                        },
                                        refreshingWorkoutId = refreshingWorkoutId,
                                        onRefreshWorkoutAi = { workout -> viewModel.refreshWorkoutAiAndSplits(workout) },
                                        onAddGoal = { title, cat, sec, wkKm, enabled, hr, min ->
                                            viewModel.addTrainingGoal(title, cat, sec, wkKm, enabled, hr, min)
                                        },
                                        onSaveGoalSchedule = { goalId, cleanTitle, cat, targetSec, weeklyKm, startKey, targetKey, daysIso ->
                                            viewModel.saveOrUpdateGoalSchedule(goalId, cleanTitle, cat, targetSec, weeklyKm, startKey, targetKey, daysIso)
                                        },
                                        onToggleGoalCompleted = { goal -> viewModel.toggleGoalCompleted(goal) },
                                        onDeleteGoal = { id -> viewModel.deleteGoal(id) },
                                        onAskGeminiAdvice = { goal -> viewModel.askGeminiForTrainingPlanAdvice(goal) },
                                        geminiAdviceText = aiPlanCoachingInsight,
                                        isGeminiLoading = aiPlanLoading
                                    )
                                }
                                StrideApexTab.HISTORY -> {
                                    HistoryScreen(
                                        workoutsState = workoutsState,
                                        prsState = prsState,
                                        onDeleteWorkout = { id -> viewModel.deleteWorkout(id) },
                                        onUpdateWorkout = { id, title, dist, dur, hr, cad, elev, splits, insight, dateEpochMs ->
                                            viewModel.updateWorkout(id, title, dist, dur, hr, cad, elev, splits, insight, dateEpochMs)
                                        },
                                        onCreateCloudBackup = { label -> viewModel.createCloudBackupCheckpoint(label) },
                                        refreshingWorkoutId = refreshingWorkoutId,
                                        onRefreshWorkoutAi = { workout -> viewModel.refreshWorkoutAiAndSplits(workout) }
                                    )
                                }
                                StrideApexTab.PREDICT_PRS -> {
                                    RacePredictorAndPrScreen(
                                        workoutsState = workoutsState,
                                        prsState = prsState,
                                        fatigueExponent = fatigueExponent,
                                        onFatigueExponentChange = { exp -> viewModel.updateFatigueExponent(exp) },
                                        onDeletePr = { categoryKey -> viewModel.deletePersonalRecord(categoryKey) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
