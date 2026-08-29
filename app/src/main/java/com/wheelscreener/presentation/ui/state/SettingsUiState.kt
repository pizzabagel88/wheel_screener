package com.wheelscreener.presentation.ui.state

import com.wheelscreener.domain.model.StrategyConfig

data class SettingsUiState(
    val config: StrategyConfig = StrategyConfig.default(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
