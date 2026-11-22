package com.example.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.RacePredictionEngine
import com.example.domain.ShoeAffiliateLinks
import com.example.domain.ShoeModel
import com.example.domain.ShoeTierState
import com.example.domain.ShoeUnlockProgress
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

private val ShopeeOrange = Color(0xFFEE4D2D)

@Composable
fun ShoeDetailScreen(
    shoe: ShoeModel,
    tierState: ShoeTierState,
    shoeProgress: ShoeUnlockProgress,
    onBack: () -> Unit,
    onSelectRelatedShoe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tier = tierState.tier
    val extra = remember(shoe.slug) { ShoeAffiliateLinks.getDetailExtra(shoe) }
    val shopeeUrl = remember(shoe.slug, shoe.shopeeAffiliateUrl) { ShoeAffiliateLinks.getShopeeUrl(shoe) }
    val relatedShoes = remember(tier, shoe.slug) { tier.shoes.filter { it.slug != shoe.slug } }

    var loadFailed by remember(shoe.imageUrl) { mutableStateOf(false) }
    var loadSucceeded by remember(shoe.imageUrl) { mutableStateOf(false) }
    val showRemoteImage = !shoe.imageUrl.isNullOrBlank() && !loadFailed

    val paceGapSec = if (
        !tierState.isUnlocked &&
        shoeProgress.currentAvgPaceSec > 0L &&
        tier.unlockPaceSecondsPerKm < 9000L
    ) {
        (shoeProgress.currentAvgPaceSec - tier.unlockPaceSecondsPerKm).coerceAtLeast(0L)
    } else {
        0L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Header bar with back navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Detail Sepatu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Surface(
                color = VoltLime.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, VoltLime.copy(alpha = 0.4f)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (tierState.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = VoltLime,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = tier.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TelemetryMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VoltLime
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Studio Image Showcase
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (loadSucceeded && !loadFailed) Color(0xFFF8FAFC) else ObsidianBackground)
                                .border(0.5.dp, SlateBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!loadSucceeded || !showRemoteImage) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = AeroCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            if (showRemoteImage) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(shoe.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "${shoe.brand} ${shoe.name}",
                                    contentScale = ContentScale.Fit,
                                    onSuccess = { loadSucceeded = true },
                                    onError = { loadFailed = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .alpha(if (tierState.isUnlocked) 1f else 0.6f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status Tier Pace Anda",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Surface(
                                color = when {
                                    tierState.isCurrentHighest -> VoltLime
                                    tierState.isUnlocked -> AeroCyan.copy(alpha = 0.18f)
                                    else -> ObsidianBackground
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (tierState.isUnlocked) {
                                        if (tierState.isCurrentHighest) "REKOMENDASI UTAMA" else "TERBUKA"
                                    } else {
                                        "TERKUNCI · ${tier.unlockPaceDisplay}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        tierState.isCurrentHighest -> OnVoltLime
                                        tierState.isUnlocked -> AeroCyan
                                        else -> TextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Identity, Price & Prominent Shopee Affiliate CTA
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = AeroCyan.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = shoe.brand,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AeroCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = tier.unlockPaceDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = TelemetryMonoFontFamily,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "${shoe.brand} ${shoe.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = shoe.plateType,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = TelemetryMonoFontFamily,
                                color = VoltLime,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimasi Harga",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = extra.estimatedPriceIdr,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = TelemetryMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = shoe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )

                        Text(
                            text = "Cocok Untuk: ${shoe.bestFor}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AeroCyan
                        )

                        if (!tierState.isUnlocked) {
                            Surface(
                                color = SlateSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.5.dp, SlateBorder)
                            ) {
                                Text(
                                    text = if (shoeProgress.currentAvgPaceSec > 0L) {
                                        "Pace Top 3 Anda ${RacePredictionEngine.formatPace(shoeProgress.currentAvgPaceSec)}. Perlu -${paceGapSec}s/km untuk membuka ${tier.title}."
                                    } else {
                                        "Catat minimal 1 sesi lari (>= 3 km) untuk membuka verifikasi pace ${tier.title}."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shopeeUrl)))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShopeeOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cek Harga & Beli di Shopee",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "Tautan afiliasi Shopee · Pembelian melalui link ini mendukung StrideApex tanpa biaya tambahan.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. 2x3 Technical Specs Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DetailSpecCard("STACK HEIGHT", shoe.stackHeight, Modifier.weight(1f))
                        DetailSpecCard(
                            "HEEL DROP",
                            "${if (shoe.dropMm % 1.0 == 0.0) shoe.dropMm.toInt() else shoe.dropMm} mm",
                            Modifier.weight(1f)
                        )
                        DetailSpecCard("BOBOT LAB", shoe.weightG?.let { "~${it} g" } ?: "--", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DetailSpecCard("BANTALAN", extra.cushionLevel, Modifier.weight(1f))
                        DetailSpecCard("PERMUKAAN", extra.surfaceType, Modifier.weight(1f))
                    }
                }
            }

            // 4. Pros & Sizing Tips
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(17.dp))
                            Text(
                                text = "Keunggulan & Tips Ukuran",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        extra.pros.forEach { pro ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = pro,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Surface(
                            color = ObsidianBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tips Ukuran: ${extra.sizingTip}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 5. Biomechanical Rationale
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = VoltLime, modifier = Modifier.size(17.dp))
                            Text(
                                text = "Mengapa Cocok di ${tier.badgeLabel}?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = tier.biomechanicalRationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 6. Related Shoes in Same Tier
            if (relatedShoes.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Alternatif Lain di ${tier.badgeLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(relatedShoes, key = { it.slug }) { other ->
                                Surface(
                                    color = SlateSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier
                                        .width(190.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(role = Role.Button) { onSelectRelatedShoe(other.slug) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = other.brand,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AeroCyan
                                        )
                                        Text(
                                            text = other.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${other.weightG?.let { "~${it} g · " } ?: ""}Drop ${other.dropMm} mm",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = TelemetryMonoFontFamily,
                                            color = TextSecondary,
                                            maxLines = 1
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
}

@Composable
private fun DetailSpecCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = TelemetryMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
