package nl.beunbv.npos.viewModel

import android.content.Context
import android.graphics.Paint
import kotlinx.coroutines.runBlocking
import nl.beunbv.npos.R
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

class RoadManagerViewModel {
    companion object {
        private lateinit var roadManager: RoadManager

        fun init(
            context: Context
        ) {
            //Setup roadManager
            roadManager = OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
            (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        }

        fun getRoad(
            points: ArrayList<GeoPoint>
        ): Road = roadManager.getRoad(points)

        fun createRoadOverlay(
            user: GeoPoint,
            store: GeoPoint,
            context: Context
        ): Polyline? {
            var returnValue: Polyline?

            //Run internet / API call on a blocking coroutine
            runBlocking {
                //Create path / road
                val road = roadManager.getRoad(arrayListOf<GeoPoint>(user, store))

                //Check if the user has reached the given store, if not -> design and add the path
                if (road.mLength < 0.02) {
                    returnValue = null
                } else {
                    val routeOverlay = RoadManager.buildRoadOverlay(road)

                    //Give road overlay design
                    routeOverlay.outlinePaint.strokeCap = Paint.Cap.ROUND
                    routeOverlay.outlinePaint.strokeWidth = 15f
                    routeOverlay.outlinePaint.strokeJoin = Paint.Join.ROUND
                    routeOverlay.outlinePaint.color = context.getColor(R.color.purple_500)

                    returnValue = routeOverlay
                }
            }

            return returnValue
        }
    }
}