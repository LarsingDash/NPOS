package nl.beunbv.npos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import nl.beunbv.npos.data.Store
import nl.beunbv.npos.ui.Pages
import nl.beunbv.npos.ui.currentPage

@Composable
fun StoreItem(
    store: Store,
    onFoldClick: () -> Unit,
    isFoldedOut: Boolean,
    navController: NavController,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 10.dp)
            .clip(shape = RoundedCornerShape(
                topStart = 25.dp,
                topEnd = 25.dp,
                bottomStart = 25.01.dp,
                bottomEnd = 25.dp))
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                .background(color = Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ListItemHeader(
                text = store.name,
                isFoldedOut = isFoldedOut,
                onFoldClick = onFoldClick,
            )
            if (isFoldedOut) {
                ListItemBody(
                    store = store,
                    navController = navController)
            }
        }
    }
}

@Composable
fun ListItemHeader(
    text: String,
    isFoldedOut: Boolean,
    onFoldClick: () -> Unit,
) {
    val color = if (isFoldedOut) Color(color = 0xFF6200EE)
    else Color.White
    val textCol = if (isFoldedOut) Color.White
    else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .height(height = 75.dp)
            .background(color = color)
            .clickable { onFoldClick.invoke() },
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 0.9f),
                text = text,
                style = MaterialTheme.typography.h1,
                color = textCol,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )

            if (isFoldedOut) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = "Close",
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Open",
                )
            }
        }
    }
}

@Composable
fun ListItemBody(
    store: Store,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.navigate(route = Pages.Map.title + "/${store.id}")
                currentPage.value = Pages.Map.title
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (product in store.products) {
            Text(
                text = product.name,
                modifier = Modifier.padding(all = 7.5.dp)
            )
        }

        Box(
            modifier = Modifier.padding(all = 7.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 10.dp))
                    .background(color = Color(color = 0xFF6200EE))
            ) {
                Text(
                    text = "Klik om te navigeren!",
                    color = Color.White,
                    modifier = Modifier.padding(all = 7.5.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}