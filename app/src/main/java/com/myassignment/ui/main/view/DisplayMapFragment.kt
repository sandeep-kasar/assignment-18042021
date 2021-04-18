package com.myassignment.ui.main.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.myassignment.R
import com.myassignment.data.service.LocationService
import com.myassignment.utils.PreferenceHelper
import com.myassignment.utils.PreferenceHelper.get
import kotlinx.android.synthetic.main.fragment_display_map.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

class DisplayMapFragment : Fragment(), OnMapReadyCallback {

    private var locationPermissionGranted = false

    private var isMessageDisplayed = true

    private lateinit var mMap: GoogleMap

    private lateinit var  listOfLatLng : MutableList<LatLng>

     private val defaultLocation = LatLng(19.1250106, 72.9164449) // provided location
    // private val defaultLocation = LatLng(19.900735836994706, 73.83794340971126)  //2km
    // private val defaultLocation = LatLng(20.002933594273465, 73.79264123679603)   // >2km

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_map, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ){
        super.onViewCreated(view, savedInstanceState)

        listOfLatLng =  mutableListOf()

        val mapFragment =
            childFragmentManager?.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        getLocationPermission()
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ){
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && resultCode == Activity.RESULT_OK ){
            startLocationService()
        }
    }

    override fun onMapReady(
        googleMap: GoogleMap?
    ) {

        mMap = googleMap!!

        // Add a marker in defaultLocation and move the camera
        mMap.addMarker(MarkerOptions()
            .position(defaultLocation)
            .title("Marker in defaultLocation"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))

        // if location permission is granted then start service
        if (locationPermissionGranted){
            startLocationService()
        }

        // display location after 5 sec
        lifecycleScope.launch {
            while (isActive) {
                delay(5_000)
                getDeviceLocation()

            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(activity, LocationService::class.java)
        activity?.stopService(intent)
    }

    private fun getDeviceLocation() {

        val prefs = PreferenceHelper.defaultPrefs(requireContext())
        var latitude = prefs["LATITUDE","-1"]
        var longitude = prefs["LONGITUDE","-1"]

        // Log.e(TAG, "Current Location $latitude,  $longitude ")

        try {

            if (latitude == "-1"){
                Log.e(TAG, "Location Not Found")
            }else{

                // calculate distance
                val myLocation = Location("currentLocation")
                myLocation.latitude = latitude!!.toDouble()
                myLocation.longitude = longitude!!.toDouble()

                val destLocation = Location("Destination")
                destLocation.latitude = defaultLocation.latitude
                destLocation.longitude = defaultLocation.longitude

                val distance: Int = myLocation.distanceTo(destLocation).toInt()
                val distInKm = distance/1000

                // display distance
                tvDistance.text =  "Distance : $distance Mtr"

                // Log.e(TAG, "Distance $distInKm KM  ")

                // show message if user is within the given radius of 2 KM
                if (isMessageDisplayed){
                    if( distInKm > RADIUS_OF_2_KM ){
                        showMessage(false)
                    } else {
                        showMessage(true)
                    }
                }

                // add marker for users current location
                val currentLocation = LatLng(latitude!!.toDouble(), longitude!!.toDouble())
                var  marker = mMap.addMarker(MarkerOptions()
                    .position(currentLocation)
                    .title("Distance $distInKm KM")
                )

                // zoom to the location
                if (isMessageDisplayed){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,DEFAULT_ZOOM.toFloat()))
                }

                // draw line on map
                listOfLatLng.add(currentLocation)
                val options = PolylineOptions().width(10f).color(Color.BLUE).geodesic(true)
                for (location in 0 until listOfLatLng.size) {
                    val point: LatLng = listOfLatLng[location]
                    options.add(point)
                }
                mMap.addPolyline(options)

                // remove old marker
                lifecycleScope.launch {
                    while (isActive) {
                        delay(5_000)
                        marker.remove()
                    }
                }.start()

                if (locationPermissionGranted){
                    isMessageDisplayed = false
                }
            }

        }catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }

    }

    private fun startLocationService(){
        // capture location in background
        lifecycleScope.launch(Dispatchers.Default) {
            val intent = Intent(activity, LocationService::class.java)
            activity?.startService(intent)
        }
    }

    private fun showMessage(
        withinRadius : Boolean
    ) {

        val dialog = Dialog(requireActivity())
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.layout_diaplay_message)
        dialog.show()

        var imgClose = dialog.findViewById(R.id.imgClose) as AppCompatImageView
        var tvMsg = dialog.findViewById(R.id.tvMsg) as AppCompatTextView

        // if user is outside of the radius
        if (!withinRadius){
            tvMsg.text = getString(R.string.msg_not_in_range)
            tvMsg.setTextColor(Color.RED)
        }

        // dismiss dialog after 3 seconds if user is within radius
        if (withinRadius){
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    dialog.dismiss()
                    Timer().cancel()
                }
            }, 3000)
        }

        imgClose.setOnClickListener {
            dialog.cancel()
        }

    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            checkPermission()
        }
    }

    private fun checkPermission() {
        Dexter.withActivity(activity)
                .withPermissions(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            locationPermissionGranted = true
                            startLocationService()
                        }
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                            showSettingsDialog()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                        permissionToken.continuePermissionRequest()
                    }
                }).withErrorListener { Toast.makeText(context, "Error occurred! ", Toast.LENGTH_SHORT).show() }.onSameThread().check()
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GOTO SETTINGS") { dialog, which ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity?.packageName, null)
            intent.data = uri
            startActivityForResult(intent, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    companion object {
        private val TAG = DisplayMapFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101
        private const val RADIUS_OF_2_KM = 2
    }
}


