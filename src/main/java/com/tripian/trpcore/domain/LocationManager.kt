package com.tripian.trpcore.domain

import android.app.Activity
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.LocationProvider
import com.tripian.trpcore.util.RequestCodes
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.dialog.DGContent
import javax.inject.Inject

class LocationManager @Inject constructor() : BaseUseCase<Location, Unit>() {

    private var locationProvider: LocationProvider? = null
    private var locationBuilder: LocationProvider.Builder? = null

    fun setLifecycleOwner(fragment: Activity) {
        locationBuilder = LocationProvider.Builder(fragment)
            .intervalMs(1000 * 5) // 5sn
            .accuracy(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .locationListener(object : LocationProvider.LocationObserver {
                override fun onLocationDisabled() {
                    showLocationPermissionDialog()
                }

                override fun onPermissionOk() {
                }

                override fun onLocation(location: Location?) {
                    if (location != null) {
                        onSendSuccess(location)

                        clear()
                    }
                }

                override fun onPermissionDenied(type: LocationProvider.PermissionDenied) {
                    when (type) {
                        LocationProvider.PermissionDenied.FIRST,
                        LocationProvider.PermissionDenied.AGAIN -> {
                            showLocationPermissionDialog()
                        }
                        LocationProvider.PermissionDenied.FOREVER -> {
                            onSendError(ErrorModel())
                        }
                    }
                }
            })
    }

    private fun showLocationPermissionDialog() {
        val dgContent = DGContent()
        dgContent.title = miscRepository.getLanguageValueForKey(LanguageConst.LOCATION_PERMISSION)
        dgContent.content = miscRepository.getLanguageValueForKey(LanguageConst.ENABLE_LOCATION_PERMISSION)
        dgContent.negativeBtn = miscRepository.getLanguageValueForKey(LanguageConst.CANCEL)
        dgContent.positiveBtn = miscRepository.getLanguageValueForKey(LanguageConst.CONFIRM)
        dgContent.negativeListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                clear()

                onSendError(ErrorModel())
            }
        }

        dgContent.positiveListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                on(Unit)
            }
        }

        showDialog(dgContent)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.REQUEST_CHECK_SETTINGS) {
            locationProvider?.onActivityResult(requestCode, resultCode, data)
        } else {
            clear()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        locationProvider?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun on(params: Unit?) {
        locationProvider = locationBuilder?.build()

        locationProvider?.startTrackingLocation()
    }

    override fun clear() {
        super.clear()

        locationProvider?.stopTrackingLocation()
    }
}