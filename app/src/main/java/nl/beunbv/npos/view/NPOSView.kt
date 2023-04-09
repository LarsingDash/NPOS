package nl.beunbv.npos.view

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
import nl.beunbv.npos.view.screens.MapScreen
import nl.beunbv.npos.view.screens.SearchScreen
import nl.beunbv.npos.view.theme.iconSelected
import nl.beunbv.npos.view.theme.iconUnselected

enum class Pages(val title: String) {
    Search("ZOEKEN"),
    Map("KAART")
}

lateinit var currentPage: MutableState<String>

@Composable
fun NPOS() {
    val navController: NavHostController = rememberNavController()

    //Remember current page
    currentPage = remember {
        mutableStateOf(value = Pages.Search.title)
    }

    //Save activity
    val currentActivity = LocalContext.current as Activity

    //Main ui
    Scaffold(
        //Bottom bar: Search - Map
        //Unit switches page if that page wasn't already selected
        bottomBar = {
            BottomBar(
                searchButtonUnit = {
                    if (currentPage.value != Pages.Search.title) {
                        //-1 == none
                        MainActivity.unfoldedStore = -1

                        currentPage.value = Pages.Search.title
                        navController.navigate(route = Pages.Search.title)
                    }
                },
                mapButtonUnit = {
                    if (currentPage.value != Pages.Map.title) {
                        currentPage.value = Pages.Map.title
                        navController.navigate(route = Pages.Map.title + "/-1")
                    }
                }
            )
        }
    ) { paddingValues ->
        //Main body
        NavHost(
            navController = navController,
            startDestination = Pages.Search.title,
            modifier = Modifier.padding(paddingValues = paddingValues)
        ) {
            //Search page
            composable(
                route = Pages.Search.title,
            ) {
                //Page
                SearchScreen(
                    storeID = MainActivity.unfoldedStore,
                    navController = navController
                )

                //Back button - return back to map if unfoldedStore != -1
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
                arguments = listOf(navArgument(name = "storeID") { type = NavType.IntType })
            ) {
                it.arguments?.let { bundle ->
                    val storeID = bundle.getInt("storeID")

                    //Page
                    MapScreen(
                        storeID = storeID,
                        navController = navController
                    )

                    //Back button - return back to search if storeID != 0
                    BackHandler(enabled = true) {
                        if (storeID == -1) currentActivity.finish()
                        else {
                            navController.popBackStack()
                            currentPage.value = Pages.Search.title
                        }
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
                        modifier = Modifier.padding(
                            start = 0.dp,
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 3.dp
                        )
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
                        modifier = Modifier.padding(
                            start = 0.dp,
                            top = 0.dp,
                            end = 0.dp,
                            bottom = 3.dp
                        )
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