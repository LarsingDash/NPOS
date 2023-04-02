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
import nl.beunbv.npos.ui.components.*

@Composable
fun MapScreen(
    storeID: Int,
    navController: NavController,
) {
    val context = LocalContext.current
    OSMMap(navController = navController, context)

    val store = remember {
        mutableStateOf(if (storeID == -1) null else MainActivity.jsonHandler.stores[storeID])
    }

    if (store.value != null) {
        addRouteToMap(MainActivity.userLocation, store.value!!.location, context)
        recenter(store.value!!.location, true)
    }
    addStoreListToMap(MainActivity.jsonHandler.stores, context)

    updateUserLocation(MainActivity.userLocation, context)
    MainActivity.locationUpdateCallback = { newLocation ->
        updateUserLocation(newLocation, context)

        if (store.value != null) {
            if (addRouteToMap(newLocation, store.value!!.location, context)) {
                store.value = null
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        val weight = if (store.value != null) 0.5f else 1f

        //User
        Box(
            modifier = Modifier
                .weight(weight)
                .padding(10.dp, 10.dp, 5.dp, 0.dp)
                .clip(RoundedCornerShape(7.5.dp))
                .background(Color(0xFF6200EE))
                .clickable { recenter(MainActivity.userLocation, false) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(10.dp, 5.dp),
                textAlign = TextAlign.Center,
                text = "Centreer mij",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        if (store.value != null) {
            //Store
            Box(
                modifier = Modifier
                    .weight(weight)
                    .padding(5.dp, 10.dp, 10.dp, 0.dp)
                    .clip(RoundedCornerShape(7.5.dp))
                    .background(Color(0xFF6200EE))
                    .clickable {
                        if (store.value != null) {
                            val temp = store.value as Store
                            recenter(temp.location, false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(10.dp, 5.dp),
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