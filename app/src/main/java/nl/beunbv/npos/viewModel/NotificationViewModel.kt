package nl.beunbv.npos.viewModel

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.model.Messages
import nl.beunbv.npos.model.NotificationModel
import nl.beunbv.npos.model.StoreModel
import java.time.LocalDateTime

class NotificationViewModel {
    companion object {
        private var isRunning = true

        //Initiate background coroutine to keep track of store times
        @OptIn(DelicateCoroutinesApi::class)
        fun initStoreCheckingService(
            context: Context
        ) {
            //Initiate notification handler
            NotificationModel.init(context = context)

            //Start Coroutine
            GlobalScope.launch(Dispatchers.IO) {
                val stores = MainActivity.dataViewModel.stores

                //Loop every minute
                while (isRunning) {
                    //Get and convert the current time to a Pair format
                    val localDateTime = LocalDateTime.now()
                    val currentTime = Pair(
                        first = localDateTime.hour,
                        second = localDateTime.minute
                    )

                    //Use the current time Pair to compare against store times
                    stores.forEach { store ->
                        //Send notification at certain times
                        checkTime(
                            store = store,
                            currentTime = currentTime,
                            context = context
                        )
                    }

//                    delay(3000L)
                    delay(60000L)
                }
            }

            Log.println(Log.DEBUG, "DEBUG", "Started service thread")
        }

        //Checks the time of a given store and sends a notification if necessary
        fun checkTime(
            store: StoreModel,
            currentTime: Pair<Int, Int>,
            context: Context?
        ): Boolean {
            //Opening time
            if (store.openTime.compareTimes(now = currentTime)) {
                context?.let {
                    NotificationModel.postMessage(
                        storeName = store.name,
                        storeID = store.id,
                        message = Messages.OPEN,
                        context = context
                    )
                }
                return true
            } else
            //Closing time
                if (store.closeTime.compareTimes(now = currentTime)) {
                    context?.let {
                        NotificationModel.postMessage(
                            storeName = store.name,
                            storeID = store.id,
                            message = Messages.CLOSE,
                            context = context
                        )
                    }
                    return true
                }

            return false
        }

        //Finds out if the given store time (this) is 10 minutes away from the "now" time
        fun Pair<Int, Int>.compareTimes(now: Pair<Int, Int>): Boolean {
            //Same hour
            if (this.first == now.first) {
                if (this.second - now.second == 10) {
                    return true
                }
            } else
            //Only 50 min is required because of the data in the database
                if (this.first - now.first == 1 && now.second == 50) {
                    return true
                }

            return false
        }
    }
}