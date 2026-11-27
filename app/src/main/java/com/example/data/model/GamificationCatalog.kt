package com.example.data.model

data class AchievementDefinition(
    val key: String,
    val title: String,
    val description: String,
    val xpReward: Long,
    val pointsReward: Long
)

enum class ShopCategory(val label: String) {
    FRAME("Profile Frames"),
    BADGE("Custom Badges"),
    THEME("Special Themes"),
    EFFECT("Seasonal Effects")
}

data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ShopCategory,
    val costPoints: Long,
    val previewHex: String
)

enum class DashboardWidgetId(val label: String) {
    TODAY_PROGRESS("Today's Progress"),
    SYLLABUS_OVERVIEW("Syllabus Progress"),
    SUBJECT_PROGRESS("Subject Breakdown"),
    TODAY_TASKS("Today's Tasks"),
    FOCUS_SHORTCUT("Focus Timer Shortcut"),
    UPCOMING_EXAMS("Upcoming Exams"),
    WEEKLY_OVERVIEW("Weekly Productivity"),
    RECENT_ACHIEVEMENTS("Recent Achievements")
}

object GamificationCatalog {
    fun levelFromXp(xp: Long): Long {
        // Level 1: 0-99, Level 2: 100-249, etc. Smooth quadratic curve
        var level = 1L
        var needed = 100L
        var remaining = xp.coerceAtLeast(0L)
        while (remaining >= needed && level < 500L) {
            remaining -= needed
            level++
            needed += 50L
        }
        return level
    }

    fun xpIntoCurrentLevel(xp: Long): Pair<Long, Long> {
        var level = 1L
        var needed = 100L
        var remaining = xp.coerceAtLeast(0L)
        while (remaining >= needed && level < 500L) {
            remaining -= needed
            level++
            needed += 50L
        }
        return remaining to needed
    }

    fun rankForLevel(level: Long): String = when {
        level >= 30 -> "Grand Sage"
        level >= 22 -> "Syllabus Sovereign"
        level >= 15 -> "Academic Luminary"
        level >= 10 -> "Deep Strategist"
        level >= 6 -> "Focused Scholar"
        level >= 3 -> "Consistent Apprentice"
        else -> "Initiate"
    }

    val achievements: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            key = "first_step",
            title = "First Step",
            description = "Create your first academic subject and plan your syllabus.",
            xpReward = 40L,
            pointsReward = 25L
        ),
        AchievementDefinition(
            key = "first_chapter",
            title = "First Chapter",
            description = "Complete your first syllabus chapter.",
            xpReward = 60L,
            pointsReward = 35L
        ),
        AchievementDefinition(
            key = "focus_beginner",
            title = "Focus Beginner",
            description = "Finish your first Pomodoro focus session.",
            xpReward = 50L,
            pointsReward = 30L
        ),
        AchievementDefinition(
            key = "task_finisher",
            title = "Momentum Builder",
            description = "Complete 5 academic tasks.",
            xpReward = 80L,
            pointsReward = 40L
        ),
        AchievementDefinition(
            key = "note_taker",
            title = "Knowledge Archivist",
            description = "Author 3 study notes linked to your syllabus.",
            xpReward = 70L,
            pointsReward = 35L
        ),
        AchievementDefinition(
            key = "seven_day_streak",
            title = "Seven-Day Streak",
            description = "Maintain meaningful daily study activity for 7 consecutive days.",
            xpReward = 200L,
            pointsReward = 100L
        ),
        AchievementDefinition(
            key = "thirty_day_streak",
            title = "Thirty-Day Streak",
            description = "Maintain a 30-day unbroken academic streak.",
            xpReward = 600L,
            pointsReward = 300L
        ),
        AchievementDefinition(
            key = "subject_master",
            title = "Subject Master",
            description = "Complete 100% of chapters in any subject (min 3 chapters).",
            xpReward = 250L,
            pointsReward = 120L
        ),
        AchievementDefinition(
            key = "syllabus_champion",
            title = "Syllabus Champion",
            description = "Complete 25 total chapters across your syllabus.",
            xpReward = 500L,
            pointsReward = 250L
        ),
        AchievementDefinition(
            key = "focus_legend",
            title = "Focus Legend",
            description = "Accumulate 10 hours (600 minutes) of Pomodoro deep focus.",
            xpReward = 450L,
            pointsReward = 220L
        )
    )

    val shopItems: List<ShopItem> = listOf(
        ShopItem(
            id = "frame_default",
            title = "Obsidian Ring",
            description = "Clean minimalist border for your student profile.",
            category = ShopCategory.FRAME,
            costPoints = 0L,
            previewHex = "#E6D5B8"
        ),
        ShopItem(
            id = "frame_gold_hourglass",
            title = "Champagne Halo",
            description = "Warm gold rim inspired by the SyllabyTrackly hourglass.",
            category = ShopCategory.FRAME,
            costPoints = 60L,
            previewHex = "#FBBF24"
        ),
        ShopItem(
            id = "frame_frost_crown",
            title = "Winter Frost Rim",
            description = "Icy cyan metallic frame for deep-focus scholars.",
            category = ShopCategory.FRAME,
            costPoints = 90L,
            previewHex = "#38BDF8"
        ),
        ShopItem(
            id = "frame_rose_aura",
            title = "Rose Quartz Crest",
            description = "Soft luminous rose profile border.",
            category = ShopCategory.FRAME,
            costPoints = 90L,
            previewHex = "#F472B6"
        ),
        ShopItem(
            id = "frame_emerald_sage",
            title = "Emerald Laurels",
            description = "Botanical green ring celebrating steady consistency.",
            category = ShopCategory.FRAME,
            costPoints = 120L,
            previewHex = "#34D399"
        ),
        ShopItem(
            id = "badge_night_owl",
            title = "Midnight Scholar Badge",
            description = "Custom profile badge for late-night deep work.",
            category = ShopCategory.BADGE,
            costPoints = 75L,
            previewHex = "#A78BFA"
        ),
        ShopItem(
            id = "badge_numerical_pro",
            title = "Numerical Ace Badge",
            description = "Badge honoring rigorous problem-solving.",
            category = ShopCategory.BADGE,
            costPoints = 100L,
            previewHex = "#FB923C"
        ),
        ShopItem(
            id = "theme_aurora_pack",
            title = "Aurora Theme Accent Pack",
            description = "Unlocks curated Aurora accent swatches in Theme Studio.",
            category = ShopCategory.THEME,
            costPoints = 80L,
            previewHex = "#2DD4BF"
        ),
        ShopItem(
            id = "effect_seasonal_shimmer",
            title = "Seasonal Atmosphere FX",
            description = "Enhanced subtle frost, petal, and ember ambient effects.",
            category = ShopCategory.EFFECT,
            costPoints = 110L,
            previewHex = "#7DD3FC"
        )
    )
}
