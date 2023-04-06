package nl.beunbv.npos.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.data.Store
import java.time.LocalDateTime

class StoreCheckingService : Service() {
    private var isRunning = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        NotificationHandler.init(context = this)

        Log.println(Log.DEBUG, "DEBUG", "Started service thread")

        val context = this
        GlobalScope.launch(Dispatchers.IO) {
            val stores = MainActivity.jsonHandler.stores

            while (isRunning) {
                val localDateTime = LocalDateTime.now()
                val currentTime = Pair(
                    first = localDateTime.hour,
                    second = localDateTime.minute
                )

                stores.forEach { store ->
                    checkTime(
                        store = store,
                        currentTime = currentTime,
                        context = context
                    )
                }

//                delay(3000L)
                delay(60000L)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        Log.println(Log.DEBUG, "DEBUG", "Stopped service thread")

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        fun checkTime(
            store: Store,
            currentTime: Pair<Int, Int>,
            context: Context?
        ): Boolean {
            if (store.openTime.compareTimes(now = currentTime)) {
                context?.let {
                    NotificationHandler.postMessage(
                        storeName = store.name,
                        storeID = store.id,
                        message = Messages.OPEN,
                        context = context
                    )
                }
                return true
            } else if (store.closeTime.compareTimes(now = currentTime)) {
                context?.let {
                    NotificationHandler.postMessage(
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

        fun Pair<Int, Int>.compareTimes(now: Pair<Int, Int>): Boolean {
            if (this.first == now.first) {
                if (this.second - now.second == 10) {
                    return true
                }
            } else if (this.first - now.first == 1 && now.second == 50) {
                return true
            }

            return false
        }
    }
}