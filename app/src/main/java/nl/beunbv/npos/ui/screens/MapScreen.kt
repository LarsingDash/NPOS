package nl.beunbv.npos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.data.Store
import nl.beunbv.npos.notification.Messages
import nl.beunbv.npos.notification.NotificationHandler
import nl.beunbv.npos.ui.components.*

@Composable
fun MapScreen(
    storeID: Int,
    navController: NavController,
) {
    val context = LocalContext.current

    //Initialize OSMMap component
    OSMMap(navController = navController, context = context)

    //Remember selected store? that could have been passed through as a navigational argument
    //Null means no route is active
    val store = remember {
        mutableStateOf(if (storeID == -1) null else MainActivity.jsonHandler.stores[storeID])
    }

    //If a store was passed through, that means a route should be started
    store.value?.let {
        //Create route
        addRouteToMap(
            user = MainActivity.userLocation,
            store = store.value!!.location,
            context = context
        )

        //Center map to destination (store)
        recenter(
            geoPoint = store.value!!.location,
            isInstant = true
        )
    }

    //Add all stores to map
    addStoreListToMap(
        StoreList = MainActivity.jsonHandler.stores,
        context = context
    )

    //Add / update user on the map
    updateUserLocation(
        geoPoint = MainActivity.userLocation,
        context = context
    )

    //Subscribe to callback from locationProvider
    MainActivity.locationUpdateCallback = { newLocation ->
        //Update user location when a new location is received
        updateUserLocation(
            geoPoint = newLocation,
            context = context
        )

        //If a route was active: add / refresh it on the map
        store.value?.let {
            //Returns true when route has been finished
            if (addRouteToMap(
                    user = newLocation,
                    store = store.value!!.location,
                    context = context
                )
            ) {

                //Push notification of the route being finished
                store.value?.let {
                    NotificationHandler.postMessage(
                        storeName = it.name,
                        storeID = it.id,
                        message = Messages.ARRIVE,
                        context = context
                    )
                }

                //Clear selected store
                store.value = null
            }
        }
    }

    //Main ui
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        //If no route is selected: "center me" button fullscreen, otherwise split evenly
        val weight = store.value?.let { 0.5f } ?: run { 1f }

        //"Center me" button
        Box(
            modifier = Modifier
                .weight(weight = weight)
                .padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 5.dp,
                    bottom = 0.dp
                )
                .clip(shape = RoundedCornerShape(size = 7.5.dp))
                .background(color = Color(color = 0xFF6200EE))
                .clickable {
                    //Center user on mapView
                    recenter(
                        geoPoint = MainActivity.userLocation,
                        isInstant = false
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
                textAlign = TextAlign.Center,
                text = "Centreer mij",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        //If a route is active: display "Center store" button
        store.value?.let {
            Box(
                modifier = Modifier
                    .weight(weight = weight)
                    .padding(
                        start = 5.dp,
                        top = 10.dp,
                        end = 10.dp,
                        bottom = 0.dp
                    )
                    .clip(shape = RoundedCornerShape(size = 7.5.dp))
                    .background(color = Color(color = 0xFF6200EE))
                    .clickable {
                        store.value?.let {
                            //Center selected store on mapView
                            val temp = store.value as Store
                            recenter(
                                geoPoint = temp.location,
                                isInstant = false
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    textAlign = TextAlign.Center,
                    text = "Centreer winkel",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}