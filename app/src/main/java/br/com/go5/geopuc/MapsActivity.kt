package br.com.go5.geopuc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import android.support.v4.app.ActivityCompat
import android.util.Log
import br.com.go5.geopuc.lambda.GetDistanceToPucLambdaInterface
import br.com.go5.geopuc.model.RequestClass
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory
import com.amazonaws.regions.Regions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private val REQUEST_LOCATION = 2
    private val IDENTITY_POOL_ID = "us-east-1:82b774e8-babf-4395-879f-089b2a4d105f"
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getRuntimePermissions()
        setContentView(R.layout.activity_maps)
        setupFragment()
    }

    private fun setupFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getRuntimePermissions() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            getMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        setupMap(googleMap)
        var currentLocation: LatLng
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 1000f, this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun setupMap(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun getMyLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                callLambda(location)
            }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
           callLambda(location)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun callLambda(location: Location?) {

        val credentialsProvider = CognitoCachingCredentialsProvider(
            this@MapsActivity.applicationContext,
            IDENTITY_POOL_ID,
            Regions.US_EAST_1)

        val factory = LambdaInvokerFactory(
            this@MapsActivity.applicationContext,
            Regions.US_EAST_1,
            credentialsProvider)

        doAsync {
            try {
                val  myInterface = factory.build(GetDistanceToPucLambdaInterface::class.java)
                val request = RequestClass(location!!.latitude, location.longitude)
                val response: String = myInterface.getDistanceToPuc(request)
                uiThread {
                    toast(response)
                }
            } catch (ex: LambdaFunctionException) {
                Log.e("LAMBDA", "Lambda execution failed " + ex.details)
            }
        }
    }


    // LOCATION LISTENERS

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(provider: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLocationChanged(location: Location?) {
        val currentLocation = LatLng(location!!.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
        callLambda(location)
    }


}
