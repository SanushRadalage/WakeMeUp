package com.example.wakemeup

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.*
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
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
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.card_view.*
import org.jetbrains.anko.vibrator
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    RoutingListener {

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private var click_status: Int = 0
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    lateinit var destination: String
    lateinit var destinationLatLng: LatLng
    lateinit var currentLatLng: LatLng
    var distance: Float = 2000.0F
    lateinit var mStarterMarker: Marker
    lateinit var mEndMarker: Marker

    lateinit var fileds: List<Place.Field>

    var isOpen: Boolean = false

    lateinit var polylines: List<Polyline>
    private val COLORS = intArrayOf(R.color.polyLine)
    //intArrayOf(R.color.colorPrimary)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val context = this

        val myLocation = findViewById<ImageView>(R.id.iconMyLocation)

        myLocation.setOnClickListener {
            setUpMap()
        }

        val item_view = AnimationUtils.loadAnimation(applicationContext, R.anim.open)
        val item_hide = AnimationUtils.loadAnimation(applicationContext, R.anim.close)
        val clock = AnimationUtils.loadAnimation(applicationContext, R.anim.clock_wise)
        val re_clock = AnimationUtils.loadAnimation(applicationContext, R.anim.re_clock_wise)

        options.setOnClickListener {
            if (isOpen)
            {
                options.startAnimation(re_clock)
                btnTheme.startAnimation(item_hide)
                btnLogout.startAnimation(item_hide)
                txtTheme.startAnimation(item_hide)
                txtSignOut.startAnimation(item_hide)
                btnTheme.isClickable = false
                btnLogout.isClickable = false
                txtSignOut.isClickable = false
                txtTheme.isClickable = false
                isOpen = false
            }
            else
            {
                options.startAnimation(clock)
                btnTheme.startAnimation(item_view)
                btnLogout.startAnimation(item_view)
                txtTheme.startAnimation(item_view)
                txtSignOut.startAnimation(item_view)
                btnTheme.isClickable = true
                btnLogout.isClickable = true
                txtSignOut.isClickable = true
                txtTheme.isClickable = true
                isOpen = true
            }
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


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
                .build(this)
            startActivityForResult(intenet, PLACE_PICKER_REQUEST)
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations)
                {
                    lastLocation = location
                    Toast.makeText(context, distance.toString(), Toast.LENGTH_SHORT).show()
                    test()
                }

            }
        }

        createLocationRequest()

        //Toast.makeText(this, distance.toString(), Toast.LENGTH_SHORT).show()

    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator.cancel()
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }

    fun test()
    {
        if (distance <= 500) {
            cardlayout.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= 26) {
                val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(

                    VibrationEffect.createOneShot(
                        10000,
                        VibrationEffect.EFFECT_DOUBLE_CLICK
                    )
                )
                okayBtn.setOnClickListener() {
                    vibrator.cancel()
                    cardlayout.visibility = View.GONE
                }

            } else {
                vibrator.vibrate(10000)
                okayBtn.setOnClickListener() {
                    vibrator.cancel()
                    cardlayout.visibility = View.GONE
                }

            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.setOnMarkerClickListener(this)

        var success = googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this, R.raw.mapstyle
            )
        )

        setUpMap()

    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true

        mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                currentLatLng = LatLng(location.latitude, location.longitude)
                mStarterMarker = mMap.addMarker(
                    MarkerOptions().position(currentLatLng).title("Going to Sleep!").icon(
                        BitmapDescriptorFactory.fromResource(R.mipmap.sleeping)
                    )
                )
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
    }

//    private fun placeMarkerOnMap(location: LatLng)
//    {
//        val markerOptions = MarkerOptions().position(location)
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
//        mMap.addMarker(markerOptions)
//    }

    private fun createLocationRequest() {

        locationRequest = LocationRequest()

        locationRequest.interval = 500

        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        test()
        Toast.makeText(this, distance.toString(), Toast.LENGTH_SHORT).show()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->

            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(
                        this@MapActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper())
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
                mEndMarker = mMap.addMarker(
                    MarkerOptions().position(destinationLatLng).title(destination).icon(
                        BitmapDescriptorFactory.fromResource(R.mipmap.busstop)
                    )
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationLatLng))
                val mCameraUpdate = CameraUpdateFactory.newLatLngZoom(destinationLatLng, 13F)
                mMap.animateCamera(mCameraUpdate)
                getPolyLines(destinationLatLng)

                var lastLoc = Location("")
                lastLoc.longitude = lastLocation!!.longitude
                lastLoc.latitude = lastLocation!!.latitude


                var destinationLoc = Location("")
                destinationLoc.longitude = destinationLatLng.longitude
                destinationLoc.latitude = destinationLatLng.latitude

                distance = lastLoc.distanceTo(destinationLoc)
                test()
                Toast.makeText(this, distance.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPolyLines(destinationLatLng: LatLng) {
        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.TRANSIT)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(LatLng(lastLocation!!.latitude, lastLocation!!.longitude), destinationLatLng)
            .key("AIzaSyAG36QakYK2Q7Ma6bQlal4we7Vv6fKuks8")
            .build()
        routing.execute()
    }


    override fun onPause() {
        super.onPause()
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
        //setUpMap()
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onRoutingCancelled() {
        mEndMarker.remove()
    }

    override fun onRoutingStart() {

    }

    override fun onRoutingFailure(p0: RouteException?) {
        if (p0 != null) {

            Toast.makeText(this, "Error: " + p0.message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show()
        }

    }


    @SuppressLint("MissingPermission")
    override fun onRoutingSuccess(route: ArrayList<Route>?, p1: Int) {
        if (polylines.isNotEmpty()) {
            for (poly in polylines) {
                poly.remove()
            }
        }
        polylines = ArrayList()
        for (i in 0 until route!!.size) {
            val colorIndex = i % COLORS.size
            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex]))
            polyOptions.width((5 + i * 3).toFloat())
            polyOptions.addAll(route.get(i).points)
            val polyline = mMap.addPolyline(polyOptions)
            (polylines as ArrayList<Polyline>).add(polyline)
        }
    }

    private fun erasePolyLines() {
        for (poly in polylines) {
            poly.remove()
        }
    }

}

