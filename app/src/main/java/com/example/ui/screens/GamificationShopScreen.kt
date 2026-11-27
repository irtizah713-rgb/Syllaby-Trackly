package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementUnlockItem
import com.example.data.model.GamificationCatalog
import com.example.data.model.RewardTransactionItem
import com.example.data.model.ShopCategory
import com.example.data.model.ShopItem
import com.example.data.model.UserAccount
import com.example.ui.components.MinimalCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalSyllabyColors
import com.example.ui.theme.parseHexColor

@Composable
fun GamificationShopScreen(
    account: UserAccount?,
    unlockedAchievements: List<AchievementUnlockItem>,
    transactions: List<RewardTransactionItem>,
    onBack: () -> Unit,
    onPurchaseItem: (ShopItem) -> Unit,
    onEquipFrame: (String) -> Unit
) {
    val colors = LocalSyllabyColors.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var pendingPurchase by remember { mutableStateOf<ShopItem?>(null) }

    val xp = account?.xp ?: 0L
    val level = account?.level ?: 1L
    val (intoLevel, neededLevel) = GamificationCatalog.xpIntoCurrentLevel(xp)
    val levelProgress = (intoLevel.toFloat() / neededLevel.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val unlockedKeys = unlockedAchievements.map { it.achievementKey }.toSet()
    val ownedShopIds = account?.unlockedShopIds?.toSet() ?: setOf("frame_default")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ranks & Reward Shop", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text("Earn XP & Theme Points through genuine study", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // Level & Theme Points Banner
            item {
                MinimalCard(highlightBorder = colors.accent.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LEVEL $level", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                            Text(GamificationCatalog.rankForLevel(level), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.accent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${account?.themePoints ?: 0} TP",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = colors.accent,
                        trackColor = colors.surfaceElevated
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$intoLevel / $neededLevel XP to Level ${level + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            // Tabs: Achievements / Reward Shop / Ledger
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Achievements", "Reward Shop", "Transactions").forEachIndexed { idx, label ->
                        Surface(
                            onClick = { tabIndex = idx },
                            shape = RoundedCornerShape(10.dp),
                            color = if (tabIndex == idx) colors.accent else colors.surface,
                            border = BorderStroke(1.dp, if (tabIndex == idx) colors.accent else colors.borderSubtle)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (tabIndex == idx) colors.onAccent else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            when (tabIndex) {
                0 -> {
                    items(GamificationCatalog.achievements, key = { it.key }) { def ->
                        val isUnlocked = unlockedKeys.contains(def.key)
                        MinimalCard(highlightBorder = if (isUnlocked) colors.success.copy(alpha = 0.45f) else colors.borderSubtle) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) colors.success.copy(alpha = 0.16f) else colors.surfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUnlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = if (isUnlocked) colors.success else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(def.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(def.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text(
                                        text = "+${def.xpReward} XP • +${def.pointsReward} TP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.accent
                                    )
                                }
                                if (isUnlocked) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "Unlocked", tint = colors.success)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    items(GamificationCatalog.shopItems, key = { it.id }) { item ->
                        val owned = ownedShopIds.contains(item.id)
                        val equipped = account?.selectedFrameId == item.id
                        val swatch = parseHexColor(item.previewHex, colors.accent)
                        MinimalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatch.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = swatch, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text(item.category.label, style = MaterialTheme.typography.labelSmall, color = swatch)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (owned) {
                                    if (item.category == ShopCategory.FRAME) {
                                        Surface(
                                            onClick = { onEquipFrame(item.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (equipped) colors.success.copy(alpha = 0.16f) else colors.surfaceElevated
                                        ) {
                                            Text(
                                                text = if (equipped) "Equipped" else "Equip",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (equipped) colors.success else colors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    } else {
                                        Text("Unlocked", style = MaterialTheme.typography.labelMedium, color = colors.success)
                                    }
                                } else {
                                    Surface(
                                        onClick = { pendingPurchase = item },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.accent
                                    ) {
                                        Text(
                                            text = "${item.costPoints} TP",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            color = colors.onAccent,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (transactions.isEmpty()) {
                        item {
                            MinimalCard {
                                Text("No XP or Theme Points transactions yet.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    } else {
                        items(transactions, key = { it.id }) { tx ->
                            val isPositive = tx.amount >= 0
                            MinimalCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        Text(tx.currency, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                    }
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${tx.amount}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = if (isPositive) colors.success else colors.warning
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingPurchase != null) {
        val item = pendingPurchase!!
        val balance = account?.themePoints ?: 0L
        val canAfford = balance >= item.costPoints
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = { Text("Confirm Purchase") },
            text = {
                Text(
                    if (canAfford) "Unlock ${item.title} for ${item.costPoints} Theme Points? Current balance: $balance TP."
                    else "You need ${item.costPoints} Theme Points to unlock ${item.title}. Current balance: $balance TP."
                )
            },
            confirmButton = {
                if (canAfford) {
                    TextButton(onClick = {
                        onPurchaseItem(item)
                        pendingPurchase = null
                    }) { Text("Unlock") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) { Text("Close") }
            }
        )
    }
}
