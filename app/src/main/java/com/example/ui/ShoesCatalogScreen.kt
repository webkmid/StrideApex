package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.WorkoutLog
import com.example.domain.RacePredictionEngine
import com.example.domain.ShoeAffiliateLinks
import com.example.domain.ShoeModel
import com.example.domain.ShoeTierState
import com.example.domain.ShoeUnlockEngine
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OnVoltLime
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelemetryMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltLime
import com.example.viewmodel.UiState

private val ShopeeOrange = Color(0xFFEE4D2D)

@Composable
fun ShoesCatalogScreen(
    workoutsState: UiState<List<WorkoutLog>>,
    onSelectShoe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val workouts = (workoutsState as? UiState.Success)?.data ?: emptyList()
    val shoeProgress = remember(workouts) { ShoeUnlockEngine.evaluateProgress(workouts) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTierId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedBrand by rememberSaveable { mutableStateOf<String?>(null) }

    val brands = remember(shoeProgress) {
        shoeProgress.tiers.flatMap { it.tier.shoes.map { shoe -> shoe.brand } }.distinct()
    }

    val filteredTiers = remember(shoeProgress, searchQuery, selectedTierId, selectedBrand) {
        val q = searchQuery.trim().lowercase()
        shoeProgress.tiers
            .filter { ts -> selectedTierId == null || ts.tier.id == selectedTierId }
            .map { ts ->
                val matchingShoes = ts.tier.shoes.filter { shoe ->
                    val brandMatch = selectedBrand == null || shoe.brand.equals(selectedBrand, ignoreCase = true)
                    val textMatch = q.isEmpty() ||
                        shoe.name.lowercase().contains(q) ||
                        shoe.brand.lowercase().contains(q) ||
                        shoe.plateType.lowercase().contains(q) ||
                        shoe.bestFor.lowercase().contains(q)
                    brandMatch && textMatch
                }
                ts to matchingShoes
            }
            .filter { (_, shoes) -> shoes.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Katalog Sepatu & Tier",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Klik sepatu untuk melihat detail spesifikasi lengkap & tautan Shopee.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Runner Pace & Tier Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "PACE TOP 3 (30 HARI)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (shoeProgress.currentAvgPaceSec > 0L) {
                                        RacePredictionEngine.formatPace(shoeProgress.currentAvgPaceSec)
                                    } else {
                                        "--:-- /km"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime,
                                    maxLines = 1
                                )
                            }

                            Surface(
                                color = VoltLime.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, VoltLime.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = shoeProgress.currentTier.badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VoltLime,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (shoeProgress.nextTier != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target: ${shoeProgress.nextTier.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = shoeProgress.nextTier.unlockPaceDisplay,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = TelemetryMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { shoeProgress.progressToNextTier.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = VoltLime,
                                trackColor = SlateBorder
                            )
                            if (shoeProgress.secondsNeededToNextTier > 0L && shoeProgress.currentAvgPaceSec > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tingkatkan pace ${shoeProgress.secondsNeededToNextTier}s/km untuk membuka ${shoeProgress.nextTier.title}!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AeroCyan
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Seluruh kasta sepatu hingga Elite Pinnacle telah terbuka!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen
                                )
                            }
                        }

                        if (shoeProgress.totalWorkoutsEvaluated == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gunakan tombol Scan Run di Dashboard untuk mencatat lari & membuka tier berikutnya.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Search Bar + Tier & Brand Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = {
                            Text("Cari sepatu, brand, atau foam...", fontSize = 13.sp, color = TextSecondary)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VoltLime,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = SlateSurface,
                            unfocusedContainerColor = SlateSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterPillChip(
                                label = "Semua Tier",
                                selected = selectedTierId == null,
                                icon = null,
                                onClick = { selectedTierId = null }
                            )
                        }
                        items(shoeProgress.tiers, key = { it.tier.id }) { tierState ->
                            val paceShort = if (tierState.tier.unlockPaceSecondsPerKm >= 9000L) {
                                "Semua"
                            } else {
                                "<${tierState.tier.unlockPaceSecondsPerKm / 60}:00"
                            }
                            FilterPillChip(
                                label = "Tier ${tierState.tier.id} · $paceShort",
                                selected = selectedTierId == tierState.tier.id,
                                icon = if (tierState.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                onClick = {
                                    selectedTierId = if (selectedTierId == tierState.tier.id) null else tierState.tier.id
                                }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterPillChip(
                                label = "Semua Brand",
                                selected = selectedBrand == null,
                                icon = null,
                                accentColor = AeroCyan,
                                onClick = { selectedBrand = null }
                            )
                        }
                        items(brands, key = { it }) { brand ->
                            FilterPillChip(
                                label = brand,
                                selected = selectedBrand == brand,
                                icon = null,
                                accentColor = AeroCyan,
                                onClick = {
                                    selectedBrand = if (selectedBrand == brand) null else brand
                                }
                            )
                        }
                    }
                }
            }

            // 3. Tier Sections & Clickable Shoe Cards
            if (filteredTiers.isEmpty()) {
                item {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Sepatu tidak ditemukan", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ubah kata kunci atau reset filter pencarian.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                filteredTiers.forEach { (tierState, shoes) ->
                    item(key = "header_${tierState.tier.id}") {
                        TierSectionHeader(tierState = tierState)
                    }
                    items(shoes, key = { "${tierState.tier.id}_${it.slug}" }) { shoe ->
                        CatalogShoeCard(
                            shoe = shoe,
                            isUnlocked = tierState.isUnlocked,
                            onClick = { onSelectShoe(shoe.slug) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    accentColor: Color = VoltLime,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accentColor else SlateSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) accentColor else SlateBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) OnVoltLime else TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) OnVoltLime else TextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TierSectionHeader(tierState: ShoeTierState) {
    val tier = tierState.tier
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = if (tierState.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (tierState.isUnlocked) VoltLime else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = tier.badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = TelemetryMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (tierState.isCurrentHighest) VoltLime else if (tierState.isUnlocked) AeroCyan else TextSecondary
                )
            }

            Surface(
                color = when {
                    tierState.isCurrentHighest -> VoltLime
                    tierState.isUnlocked -> AeroCyan.copy(alpha = 0.16f)
                    else -> SlateSurface
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (tierState.isUnlocked) {
                        if (tierState.isCurrentHighest) "REKOMENDASI" else "TERBUKA"
                    } else {
                        tier.unlockPaceDisplay
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        tierState.isCurrentHighest -> OnVoltLime
                        tierState.isUnlocked -> AeroCyan
                        else -> TextSecondary
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tier.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (tierState.isUnlocked) TextPrimary else TextSecondary
        )
        Text(
            text = tier.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun CatalogShoeCard(
    shoe: ShoeModel,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val extra = remember(shoe.slug) { ShoeAffiliateLinks.getDetailExtra(shoe) }
    var loadFailed by remember(shoe.imageUrl) { mutableStateOf(false) }
    var loadSucceeded by remember(shoe.imageUrl) { mutableStateOf(false) }
    val showRemoteImage = !shoe.imageUrl.isNullOrBlank() && !loadFailed

    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isUnlocked) SlateBorder else SlateBorder.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (loadSucceeded && !loadFailed) Color(0xFFF8FAFC) else ObsidianBackground)
                        .border(0.5.dp, SlateBorder.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!loadSucceeded || !showRemoteImage) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = if (isUnlocked) AeroCyan.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (showRemoteImage) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(shoe.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${shoe.brand} ${shoe.name}",
                            contentScale = ContentScale.Fit,
                            onSuccess = { loadSucceeded = true },
                            onError = { loadFailed = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .alpha(if (isUnlocked) 1f else 0.55f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AeroCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = shoe.brand,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AeroCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = extra.estimatedPriceIdr,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = TelemetryMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = shoe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = shoe.plateType,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TelemetryMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (isUnlocked) VoltLime else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Compact 3-cell specs
            Surface(
                color = ObsidianBackground,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, SlateBorder.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogSpecMini("STACK", shoe.stackHeight, Modifier.weight(1.35f))
                    CatalogSpecMini(
                        "DROP",
                        "${if (shoe.dropMm % 1.0 == 0.0) shoe.dropMm.toInt() else shoe.dropMm} mm",
                        Modifier.weight(0.75f)
                    )
                    CatalogSpecMini("BOBOT", shoe.weightG?.let { "~${it} g" } ?: "--", Modifier.weight(0.9f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = ShopeeOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Tersedia di Shopee",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ShopeeOrange
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Detail & Beli di Shopee",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AeroCyan
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AeroCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSpecMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = TelemetryMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
