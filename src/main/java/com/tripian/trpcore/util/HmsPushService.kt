//package com.tripian.trpcore.util
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.media.RingtoneManager
//import android.os.Build
//import android.os.Bundle
//import androidx.core.app.NotificationCompat
//import com.huawei.hms.push.HmsMessageService
//import com.huawei.hms.push.RemoteMessage
//import com.tripian.trpcore.R
//import com.tripian.trpcore.ui.mytrip.ACMyTrip
//
///**
// * Created by semihozkoroglu on 14.11.2021.
// */
//class HmsPushService: HmsMessageService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        val title = remoteMessage.notification?.title ?: "Tripian"
//        val body = remoteMessage.notification?.body ?: ""
//
//        val data = Bundle()
//
//        if (remoteMessage.dataOfMap.containsKey("poiId")) {
//            // new offer olursa poiId + offerId exp:845454 - cancel olmasi durumu poiId gelmiyor netlestirilecek
//            data.putInt("poiId", remoteMessage.dataOfMap["poiId"]!!.toInt())
//        }
//
//        if (remoteMessage.dataOfMap.containsKey("offerId")) {
//            // offerId tek basina geliyor ise sadece cancelled offer durumunda oluyor exp:18
//            data.putLong("offerId", remoteMessage.dataOfMap["offerId"]!!.toLong())
//        }
//
//        sendNotification(title, body, data)
//    }
//
//    private fun sendNotification(title: String, body: String, extras: Bundle) {
//        val intent = Intent(this, ACMyTrip::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        intent.putExtras(extras)
//        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "1")
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setContentTitle(title)
//            .setContentText(body)
//            .setPriority(Notification.PRIORITY_MAX)
//            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
//            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//            .setAutoCancel(true)
//            .setColor(Color.argb(255, 0, 162, 255))
//            .setContentIntent(pendingIntent)
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "1",
//                "notification",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        notificationManager.notify(body.hashCode(), notificationBuilder.build())
//    }
//}