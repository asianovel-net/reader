package net.asianovel.reader.service

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.asianovel.reader.R


class ReaderMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        getFirebaseMessage(
            remoteMessage.notification!!.title, remoteMessage.notification!!
                .body
        )
    }

    fun getFirebaseMessage(title: String?, msg: String?) {
        val builder = NotificationCompat.Builder(this, "myFirebaseChannel")
            .setSmallIcon(R.drawable.ic_launcher1)
            .setContentTitle(title)
            .setContentText(msg)
            .setAutoCancel(true)
        val manager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(101, builder.build())
    }
}