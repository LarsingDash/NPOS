package nl.beunbv.npos.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.data.Store
import java.time.LocalDateTime

class StoreCheckingService : Service() {
    private var isRunning = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        NotificationHandler.init(this)

        Log.println(Log.DEBUG, "DEBUG", "Started service thread")

        Thread {
            val stores = MainActivity.jsonHandler.stores

            while (isRunning) {
                val localDateTime = LocalDateTime.now()
                val currentTime = Pair(localDateTime.hour, localDateTime.minute)

                for (store in stores) {
                    checkTime(store, currentTime, this)
                }

//                Thread.sleep(3000)
                Thread.sleep(60000)
            }
        }.start()

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
            if (compareTimes(store.openTime, currentTime)) {
                if (context != null)
                    NotificationHandler.postMessage(store.name, store.id, true, context)
                return true
            } else if (compareTimes(store.closeTime, currentTime)) {
                if (context != null)
                    NotificationHandler.postMessage(store.name, store.id, false, context)
                return true
            }

            return false
        }

        fun compareTimes(store: Pair<Int, Int>, now: Pair<Int, Int>): Boolean {
            if (store.first == now.first) {
                if (store.second - now.second == 10) {
                    return true
                }
            } else if (store.first - now.first == 1 && now.second == 50) {
                return true
            }

            return false
        }
    }
}