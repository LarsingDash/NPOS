package nl.beunbv.npos.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.beunbv.npos.model.StoreModel
import nl.beunbv.npos.viewModel.recenter
import nl.beunbv.npos.viewModel.LocationViewModel

@Composable
fun MapScreenOverlay(
    store: MutableState<StoreModel?>
) {
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
                        geoPoint = LocationViewModel.getUserLocation(),
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
                            val temp = store.value as StoreModel
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