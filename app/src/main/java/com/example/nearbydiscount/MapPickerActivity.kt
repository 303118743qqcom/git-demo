package com.example.nearbydiscount

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult

/**
 * 地图选点页：拖动地图，中心标记所在位置即所选坐标。
 * 仅使用高德官方 SDK；不做任何抓包、逆向或非官方数据访问。
 */
class MapPickerActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var geocodeSearch: GeocodeSearch? = null
    private var center: LatLng = LatLng(DEFAULT_LAT, DEFAULT_LNG)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.AMAP_KEY.isBlank()) {
            Toast.makeText(this, R.string.map_key_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 高德 SDK 要求：构造 MapView 之前完成隐私合规声明
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        val startLat = intent.getDoubleExtra(EXTRA_LAT, DEFAULT_LAT)
        val startLng = intent.getDoubleExtra(EXTRA_LNG, DEFAULT_LNG)
        center = LatLng(startLat, startLng)

        setContentView(R.layout.activity_map_picker)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        aMap = mapView.map.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
            moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15f))
            setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) = Unit
                override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                    position?.target?.let { onCenterChanged(it) }
                }
            })
        }

        runCatching {
            geocodeSearch = GeocodeSearch(this).apply {
                setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                    override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
                        if (rCode == 1000 && result != null) {
                            val address = result.regeocodeAddress?.formatAddress
                            val addressText = findViewById<android.widget.TextView?>(R.id.addressText)
                            if (!address.isNullOrBlank() && addressText != null) addressText.text = address
                        }
                    }

                    override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) = Unit
                })
            }
        }
        reverseGeocode(center)

        findViewById<android.widget.TextView>(R.id.addressText).text =
            getString(R.string.map_resolving_address)
        findViewById<android.view.View>(R.id.confirmButton).setOnClickListener {
            val data = Intent().apply {
                putExtra(EXTRA_LAT, center.latitude)
                putExtra(EXTRA_LNG, center.longitude)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun onCenterChanged(latLng: LatLng) {
        center = latLng
        aMap?.clear()
        aMap?.addMarker(
            MarkerOptions().position(latLng).title(getString(R.string.map_marker_title))
        )
        reverseGeocode(latLng)
    }

    private fun reverseGeocode(latLng: LatLng) {
        val search = geocodeSearch ?: return
        val addressText = findViewById<android.widget.TextView?>(R.id.addressText) ?: return
        addressText.text = getString(R.string.map_resolving_address)
        runCatching {
            search.getFromLocationAsyn(
                RegeocodeQuery(
                    LatLonPoint(latLng.latitude, latLng.longitude),
                    100f,
                    GeocodeSearch.AMAP
                )
            )
        }.onFailure {
            addressText.text = getString(R.string.map_coord_only, latLng.latitude, latLng.longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        private const val DEFAULT_LAT = 39.90872
        private const val DEFAULT_LNG = 116.39752
    }
}
