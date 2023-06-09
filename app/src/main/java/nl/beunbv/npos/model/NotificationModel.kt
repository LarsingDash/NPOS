package nl.beunbv.npos.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import nl.beunbv.npos.MainActivity
import nl.beunbv.npos.R

enum class Messages(val wentOpen: Boolean) {
    OPEN(true),
    CLOSE(false),
    ARRIVE(true)
}

class NotificationModel {
    companion object {
        private lateinit var manager: NotificationManager
        private const val channelID = "NPOS"

        //Sets up the notification channel in Android
        fun init(context: Context) {
            manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelID,
                "NPOS",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        //Push a notification
        fun postMessage(
            storeName: String,
            storeID: Int,
            message: Messages,
            context: Context
        ) {
            //Decide what text the notification will have according to the given enum Messages
            val text: String =
                //Arrival
                if (message == Messages.ARRIVE) "Je hebt $storeName berijkt!"
                //Open / Close
                else "$storeName zal over 10 minuten ${
                    if (message.wentOpen)
                        "openen" else "sluiten"
                }!"

            //Build the notification
            val builder = NotificationCompat.Builder(context, channelID)
                .setSmallIcon(
                    when {
                        storeName.contains(other = "Jumbo") -> R.drawable.jumbo
                        storeName.contains(other = "Coop") -> R.drawable.coop
                        else -> R.drawable.albert_heijn
                    }
                )
                .setContentTitle(text)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )

            //Push the notification
            manager.notify(storeID, builder.build())
        }
    }
}