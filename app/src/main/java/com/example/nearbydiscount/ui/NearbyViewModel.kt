package com.example.nearbydiscount.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nearbydiscount.core.AppConfig
import com.example.nearbydiscount.core.AppResult
import com.example.nearbydiscount.core.ComplianceCheck
import com.example.nearbydiscount.core.ComplianceGuard
import com.example.nearbydiscount.data.location.AppLocationManager
import com.example.nearbydiscount.data.remote.ApiFactory
import com.example.nearbydiscount.data.remote.createPromotionProvider
import com.example.nearbydiscount.data.repository.NearbyRepository
import com.example.nearbydiscount.domain.NearbyInteractor
import com.example.nearbydiscount.domain.model.Merchant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NearbyUiState(
    val isLoading: Boolean = false,
    val merchants: List<Merchant> = emptyList(),
    val error: String? = null,
    val radiusKm: Int = AppConfig.DEFAULT_RADIUS_KM,
    val keyword: String = "",
    val statusText: String = "",
    val needPermission: Boolean = false,
    val blockedReason: String? = null,
    val demoMode: Boolean = AppConfig.demoMode,
    val locationSource: LocationSource = LocationSource.GPS
)

enum class LocationSource { GPS, PICKED }

class NearbyViewModel(
    private val locationManager: AppLocationManager,
    private val interactor: NearbyInteractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastLocation: Pair<Double, Double>? = null
    private var pickedLocation: Pair<Double, Double>? = null

    init {
        when (val check = ComplianceGuard.check(AppConfig.demoMode)) {
            is ComplianceCheck.Blocked -> _uiState.update { it.copy(blockedReason = check.reason) }
            ComplianceCheck.Passed -> Unit
        }
    }

    fun onRadiusSelected(km: Int) {
        if (km !in AppConfig.radiusOptionsKm) return
        _uiState.update { it.copy(radiusKm = km) }
        reload()
    }

    fun onKeywordChange(text: String) {
        _uiState.update { it.copy(keyword = text) }
    }

    fun onSearch() = reload()

    fun onRefresh() = reload()

    /** 用户在地图上选点后回调：保存坐标并立即以该点检索，无需定位权限。 */
    fun onLocationPicked(latitude: Double, longitude: Double) {
        pickedLocation = latitude to longitude
        _uiState.update { it.copy(locationSource = LocationSource.PICKED) }
        reload()
    }

    /** 切回设备 GPS 定位重新检索。 */
    fun onUseCurrentLocation() {
        pickedLocation = null
        lastLocation = null
        _uiState.update { it.copy(locationSource = LocationSource.GPS) }
        reload()
    }

    /** 当前用于检索的坐标，供地图选点页作为初始中心点。 */
    fun currentCenter(): Pair<Double, Double>? = pickedLocation ?: lastLocation

    fun onPermissionResult(granted: Boolean) {
        if (granted) reload()
        else _uiState.update {
            it.copy(needPermission = true, isLoading = false, statusText = "需要定位权限才能检索附近商家")
        }
    }

    private fun reload() {
        if (_uiState.value.blockedReason != null) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, needPermission = false) }

            val source = _uiState.value.locationSource
            val location = when {
                source == LocationSource.PICKED && pickedLocation != null -> pickedLocation!!
                lastLocation != null -> lastLocation!!
                else -> when (val result = locationManager.currentLocation()) {
                    is AppLocationManager.LocationResult.Success -> {
                        result.latitude to result.longitude
                    }
                    AppLocationManager.LocationResult.PermissionDenied -> {
                        _uiState.update {
                            it.copy(isLoading = false, needPermission = true, statusText = "需要定位权限才能检索附近商家")
                        }
                        return@launch
                    }
                    AppLocationManager.LocationResult.Unavailable -> {
                        _uiState.update { it.copy(isLoading = false, error = "设备定位不可用，请检查系统定位开关") }
                        return@launch
                    }
                    AppLocationManager.LocationResult.Timeout -> {
                        _uiState.update { it.copy(isLoading = false, error = "定位超时，请到开阔位置重试") }
                        return@launch
                    }
                }
            }
            lastLocation = location

            val state = _uiState.value
            when (val result = interactor.load(location.first, location.second, state.radiusKm, state.keyword)) {
                is AppResult.Success -> _uiState.update {
                    val sourceLabel = if (it.locationSource == LocationSource.PICKED) "已选位置" else "已定位"
                    it.copy(
                        isLoading = false,
                        merchants = result.data,
                        statusText = "$sourceLabel · 检索半径 ${it.radiusKm} km · 共 ${result.data.size} 家"
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}

class NearbyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = context.applicationContext
        val interactor = NearbyInteractor(
            repository = NearbyRepository(ApiFactory.lbsApi()),
            promotionProvider = createPromotionProvider()
        )
        return NearbyViewModel(AppLocationManager(app), interactor) as T
    }
}
