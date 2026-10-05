package com.example.worldbusiness.data.model

data class MacroIndicator(
    val title: String,
    val value: String,
    val change: String,
    val isPositive: Boolean,
    val category: String, // "CENTRAL_BANKS", "TAX_OECD", "CORRIDOR", "GLOBAL_GDP"
    val description: String
)

data class RegionalHub(
    val code: String,
    val name: String,
    val city: String,
    val coordinatesNormX: Float, // 0f to 1f normalized map X
    val coordinatesNormY: Float, // 0f to 1f normalized map Y
    val activeEntityCount: Int,
    val activeHeadcount: Int,
    val revenueContributionPercent: Int,
    val localStatus: String, // "ONLINE", "ACTIVE", "PEAK_HOURS"
    val localTimeStr: String,
    val activeCurrency: String
)
