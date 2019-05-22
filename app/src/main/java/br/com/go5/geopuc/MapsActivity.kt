package br.com.go5.geopuc

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.android.gms.location.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var locationRequest = LocationRequest()
    private lateinit var locationCallback: LocationCallback
    private val REQUEST_LOCATION = 2
    private val IDENTITY_POOL_ID = "us-east-1:82b774e8-babf-4395-879f-089b2a4d105f"
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        setupFragment()
        getRuntimePermissions()
    }

    private fun setupFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    private fun getRuntimePermissions() {
        if (isFineLocationPermissionEnabled()) {
            setupLocationUpdateCallback()
            startLocationUpdates()
        } else {
            requestFineLocationPermission()
        }
    }

    private fun isFineLocationPermissionEnabled(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestFineLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
    }

    private fun setupLocationUpdateCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                moveCameraByLocation(locationResult.lastLocation)
                invokeLambda(locationResult.lastLocation)
            }
        }
    }

    private fun moveCameraByLocation(location: Location) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            null)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (isFineLocationPermissionEnabled()) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun invokeLambda(location: Location?) {

        val factory = createLambdaFactory()

        doAsync {
            try {
                if (location !== null) {
                    val response = callLambdaFunction(factory, location)
                    uiThread {
                        toast(response)
                    }
                } else {
                    throw Exception("Can't get user's location")
                }
            } catch (ex: Exception) {
                Log.e("LAMBDA", "Lambda execution failed: " + ex.message)
            }
        }
    }

    private fun createLambdaFactory(): LambdaInvokerFactory {
        val credentialsProvider = CognitoCachingCredentialsProvider(
            this@MapsActivity.applicationContext,
            IDENTITY_POOL_ID,
            Regions.US_EAST_1)

        return LambdaInvokerFactory(
            this@MapsActivity.applicationContext,
            Regions.US_EAST_1,
            credentialsProvider)
    }

    private fun callLambdaFunction(factory: LambdaInvokerFactory, location: Location): String {
        val  myInterface = factory.build(GetDistanceToPucLambdaInterface::class.java)
        val request =  RequestClass(location.latitude, location.longitude)
        return myInterface.getDistanceToPuc(request)
    }


}
