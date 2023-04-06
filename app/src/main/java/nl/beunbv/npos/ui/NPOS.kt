package nl.beunbv.npos.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.ui.screens.MapScreen
import nl.beunbv.npos.ui.screens.SearchScreen
import nl.beunbv.npos.ui.theme.iconSelected
import nl.beunbv.npos.ui.theme.iconUnselected

enum class Pages(val title: String) {
    Search("ZOEKEN"),
    Map("KAART")
}

lateinit var currentPage: MutableState<String>

@Composable
fun NPOS() {
    val navController: NavHostController = rememberNavController()

    currentPage = remember {
        mutableStateOf(Pages.Search.title)
    }

    val currentActivity = LocalContext.current as Activity

    Scaffold(
        bottomBar = {
            BottomBar(
                searchButtonUnit = {
                    if (currentPage.value != Pages.Search.title) {
                        MainActivity.unfoldedStore = -1
                        navController.navigate(Pages.Search.title)
                        currentPage.value = Pages.Search.title
                    }
                },
                mapButtonUnit = {
                    if (currentPage.value != Pages.Map.title) {
                        navController.navigate(Pages.Map.title + "/-1")
                        currentPage.value = Pages.Map.title
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Pages.Search.title,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                route = Pages.Search.title,
            ) {
                SearchScreen(
                    storeID = MainActivity.unfoldedStore,
                    navController = navController)
                BackHandler(enabled = true) {
                    if (MainActivity.unfoldedStore == -1) currentActivity.finish()
                    else {
                        navController.popBackStack()
                        currentPage.value = Pages.Map.title
                    }
                }
            }

            //Map
            composable(
                route = Pages.Map.title + "/{storeID}",
                arguments = listOf(navArgument("storeID") { type = NavType.IntType })
            ) {
                val storeID = it.arguments?.getInt("storeID")

                MapScreen(
                    storeID = storeID!!,
                    navController = navController)
                BackHandler(enabled = true) {
                    if (storeID == 0) currentActivity.finish()
                    else {
                        navController.popBackStack()
                        currentPage.value = Pages.Search.title
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    searchButtonUnit: () -> Unit,
    mapButtonUnit: () -> Unit,
) {
    BottomAppBar(
        elevation = 0.dp
    ) {
        Row {
            //Search
            BottomNavigationItem(
                label = {
                    Text(
                        text = Pages.Search.title,
                        style = MaterialTheme.typography.body1,
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = Pages.Search.title,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 3.dp)
                    )
                },
                selected = currentPage.value == Pages.Search.title,
                onClick = searchButtonUnit,
                selectedContentColor = iconSelected,
                unselectedContentColor = iconUnselected
            )

            //Map
            BottomNavigationItem(
                label = {
                    Text(
                        text = Pages.Map.title,
                        style = MaterialTheme.typography.body1,
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = Pages.Map.title,
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 3.dp)
                    )
                },
                selected = currentPage.value == Pages.Map.title,
                onClick = mapButtonUnit,
                selectedContentColor = iconSelected,
                unselectedContentColor = iconUnselected
            )
        }
    }
}