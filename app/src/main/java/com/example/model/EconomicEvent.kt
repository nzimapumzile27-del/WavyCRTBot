package com.example.model

import java.util.UUID

enum class ImpactLevel(val displayName: String) {
    HIGH("High Impact"),      // Red marked markers
    SPEECH("Speaker Speech"),  // Speech/Market-changing Speakers
    MEDIUM("Medium Impact"),  // Orange
    LOW("Low Impact")         // Yellow/Grey
}

enum class EventStatus {
    UPCOMING,
    TRIGGERED,
    RELEASED
}

data class EconomicEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val currency: String,
    val impact: ImpactLevel,
    val scheduledHourSAST: Int,
    val scheduledMinuteSAST: Int = 0,
    val forecast: String,
    val previous: String,
    var actual: String? = null,
    var status: EventStatus = EventStatus.UPCOMING,
    val impactExplanation: String,
    val targetAssetCategory: String // "Crypto", "Forex", "Indices", or "All"
)
