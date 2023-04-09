package nl.beunbv.npos

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.location.*
import nl.beunbv.npos.view.NPOS
import nl.beunbv.npos.view.theme.NPOSTheme
import nl.beunbv.npos.viewModel.DataViewModel
import nl.beunbv.npos.viewModel.LocationViewModel
import nl.beunbv.npos.viewModel.LocationViewModel.Companion.setupUserLocation
import nl.beunbv.npos.viewModel.NotificationViewModel.Companion.initStoreCheckingService
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Thread policies for internet access
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //Orientation lock
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        //Initialize JSON
        dataViewModel = DataViewModel(
            productsStream = resources.openRawResource(R.raw.products),
            storesStream = resources.openRawResource(R.raw.stores),
        )

        //Start location service
        setupUserLocation(context = this)

        //Start background service
        initStoreCheckingService(this)

        //Setup ui
        setContent {
            NPOSTheme {
                NPOS()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 1) {
            if (grantResults[0] == 0 && grantResults[1] == 0)
                LocationViewModel.init(
                    context = this,
                    isFromCallback = true
                )
        }
    }

    companion object {
        //Global variables
        lateinit var dataViewModel: DataViewModel
        var unfoldedStore: Int = -1
    }
}