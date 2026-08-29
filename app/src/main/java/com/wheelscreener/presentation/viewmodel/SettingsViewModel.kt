package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.entity.SettingsEntity
import com.wheelscreener.data.scheduler.ScanScheduler
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.presentation.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val scanScheduler: ScanScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            try {
                val entity = settingsDao.getSetting(CONFIG_KEY)
                val config = if (entity != null) {
                    configAdapter.fromJson(entity.value) ?: StrategyConfig.default()
                } else {
                    StrategyConfig.default()
                }
                _uiState.value = SettingsUiState(config = config)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState(error = "Failed to load configuration: ${e.message}")
            }
        }
    }

    fun updateConfig(newConfig: StrategyConfig) {
        _uiState.value = _uiState.value.copy(config = newConfig, isSaved = false)
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val json = configAdapter.toJson(_uiState.value.config)
                settingsDao.insertSetting(
                    SettingsEntity(
                        key = CONFIG_KEY,
                        value = json,
                        updatedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
                scanScheduler.schedule(_uiState.value.config)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save configuration: ${e.message}"
                )
            }
        }
    }

    fun resetToDefault() {
        _uiState.value = _uiState.value.copy(config = StrategyConfig.default(), isSaved = false)
    }

    companion object {
        private const val CONFIG_KEY = "strategy_config"
    }
}
