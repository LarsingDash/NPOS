package nl.beunbv.npos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import nl.beunbv.npos.data.JSONHandler
import nl.beunbv.npos.notification.StoreCheckingService
import nl.beunbv.npos.ui.NPOS
import nl.beunbv.npos.ui.screens.fullList
import nl.beunbv.npos.ui.theme.NPOSTheme
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //Orientation lock
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        jsonHandler = JSONHandler(
            productsStream = resources.openRawResource(R.raw.products),
            storesStream = resources.openRawResource(R.raw.stores),
        )

        setupUserLocation(context = this)

        val intent = Intent(this, StoreCheckingService::class.java)
        startService(intent)

        setContent {
            NPOSTheme {
                NPOS()
            }
        }
    }

    override fun onDestroy() {
        val intent = Intent(this, StoreCheckingService::class.java)
        stopService(intent)

        super.onDestroy()
    }

    companion object {
        lateinit var jsonHandler: JSONHandler
        var unfoldedStore: Int = -1

        private var hasPermissions: Boolean = false
        var userLocation = GeoPoint(51.5892100, 4.7805200)
        var lastUserLocation = GeoPoint(0.0, 0.0)
        var locationUpdateCallback: (GeoPoint) -> Unit = {}

        fun setupUserLocation(context: Context) {
            //Setup provider and requests
            val provider = LocationServices.getFusedLocationProviderClient(context)
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                500L
            ).build()

            //Setup callback
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)

                    //Update locations
                    val location = result.lastLocation
                    if (location != null) {
                        val geoLocation = GeoPoint(location.latitude, location.longitude)
                        userLocation = geoLocation

                        if (userLocation.distanceToAsDouble(lastUserLocation) > 0.5) {
                            lastUserLocation = userLocation
                            fullList.value = jsonHandler.stores

                            locationUpdateCallback.invoke(userLocation)
                        }
                    }
                }
            }

            //Check permissions before assigning request
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCourseLocation = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation && hasCourseLocation) {
                hasPermissions = true

                //Assign request
                provider.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                )

            } else {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    99
                )
            }
        }
    }
}