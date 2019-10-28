package com.example.wakemeup

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.directions.route.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_map.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, RoutingListener{

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private var click_status: Int = 0
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    lateinit var destination: String
    lateinit var destinationLatLng: LatLng
    lateinit var currentLatLng: LatLng

    lateinit var  fileds: List<Place.Field>

    lateinit var  polylines:List<Polyline>
    private val COLORS = intArrayOf(R.color.polyLine)
        //intArrayOf(R.color.colorPrimary)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        polylines = ArrayList()
        
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fileds = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_api_key), Locale.US)
        }

        val mPlacesClient = Places.createClient(this)

        pick.setOnClickListener {

            val intenet = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fileds)
                .setCountry("LK")
                .setTypeFilter(TypeFilter.ADDRESS)
                .build(this)
            startActivityForResult(intenet, PLACE_PICKER_REQUEST)
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
            }
        }

        createLocationRequest()

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)

        setUpMap()

    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        mMap.isMyLocationEnabled = true

        mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this){
                location ->
            if(location != null)
            {
                lastLocation = location
                currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
        mMap.addMarker(markerOptions)
    }

    private fun createLocationRequest() {

        locationRequest = LocationRequest()

        locationRequest.interval = 10000

        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->

            if (e is ResolvableApiException)
            {
                try {
                    e.startResolutionForResult(this@MapActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (e: Exception)
                {

                }
            }
        }
    }

    private fun startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()

            }
        }

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                destination = place.name!!
                destinationLatLng = place.latLng!!
                mMap.addMarker(MarkerOptions().position(destinationLatLng).title(destination))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationLatLng))
                val mCameraUpdate = CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15F)
                mMap.animateCamera(mCameraUpdate)
                getPolyLines(destinationLatLng)
            }
        }
    }

    private fun getPolyLines(destinationLatLng: LatLng)
    {
        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.TRANSIT)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(LatLng(lastLocation.latitude, lastLocation.longitude), destinationLatLng)
            .key("AIzaSyAG36QakYK2Q7Ma6bQlal4we7Vv6fKuks8")
            .build()
        routing.execute()
    }


    override fun onPause() {
        super.onPause()
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onRoutingCancelled() {
        TODO("not implemented")
    }

    override fun onRoutingStart()
    {

    }

    override fun onRoutingFailure(p0: RouteException?)
    {
        if(p0 != null) {

            Toast.makeText(this, "Error: " + p0.message, Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }

    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, p1: Int)
    {
        if (polylines.isNotEmpty())
        {
            for (poly in polylines)
            {
                poly.remove()
            }
        }
        polylines = ArrayList()
        for (i in 0 until route!!.size)
        {
            val colorIndex = i % COLORS.size
            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex]))
            polyOptions.width((15 + i * 3).toFloat())
            polyOptions.addAll(route.get(i).points)
            val polyline = mMap.addPolyline(polyOptions)
            (polylines as ArrayList<Polyline>).add(polyline)
            Toast.makeText(applicationContext, "Route " + (i + 1) + ": distance - " + route.get(i).distanceValue + ": duration - " + route.get(i).durationValue, Toast.LENGTH_SHORT).show()
        }
    }

    private fun erasePolyLines()
    {
        for (poly in polylines)
        {
            poly.remove()
        }

    }
}

