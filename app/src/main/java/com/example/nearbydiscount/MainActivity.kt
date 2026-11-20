package com.example.nearbydiscount

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nearbydiscount.data.location.AppLocationManager
import com.example.nearbydiscount.ui.NearbyScreen
import com.example.nearbydiscount.ui.NearbyViewModel
import com.example.nearbydiscount.ui.NearbyViewModelFactory

class MainActivity : ComponentActivity() {

    private val locationManager by lazy { AppLocationManager(applicationContext) }
    private var viewModel: NearbyViewModel? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel?.onPermissionResult(granted) }

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, Double.NaN)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, Double.NaN)
            if (lat != null && lng != null && !lat.isNaN() && !lng.isNaN()) {
                viewModel?.onLocationPicked(lat, lng)
            }
        }
    }

    private fun openMapPicker() {
        val center = viewModel?.currentCenter()
        val intent = android.content.Intent(this, MapPickerActivity::class.java).apply {
            if (center != null) {
                putExtra(MapPickerActivity.EXTRA_LAT, center.first)
                putExtra(MapPickerActivity.EXTRA_LNG, center.second)
            }
        }
        mapPickerLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val factory = remember { NearbyViewModelFactory(applicationContext) }
            val vm: NearbyViewModel = viewModel(factory = factory)
            viewModel = vm
            val state by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                if (com.example.nearbydiscount.core.AppConfig.demoMode) {
                    // 演示模式无需定位权限，直接加载内置样本数据。
                    vm.onSearch()
                } else if (locationManager.hasPermission()) {
                    vm.onPermissionResult(true)
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            NearbyScreen(
                state = state,
                onRadiusSelected = vm::onRadiusSelected,
                onKeywordChange = vm::onKeywordChange,
                onSearch = vm::onSearch,
                onRefresh = vm::onRefresh,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onChooseLocation = { openMapPicker() },
                onUseCurrentLocation = vm::onUseCurrentLocation
            )
        }
    }
}
