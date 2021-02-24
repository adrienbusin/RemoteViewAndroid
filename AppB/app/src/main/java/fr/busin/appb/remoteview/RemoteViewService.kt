package fr.busin.appb.remoteview

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import java.lang.ref.WeakReference


abstract class RemoteViewService(
    // Layout id used to send as RemoteViews.
    @LayoutRes val layoutRes: Int,

    // View id used by OnClickPendingIntent
    @IdRes val onClickIdRes: Int,

    // Allows you to sort the remote view according to others remote view
    val displayOrder: Int = -1,

    // Activity to start when OnClickPendingIntent event
    val activity: Class<*>
) : Service() {

    companion object {
        const val REMOTE_VIEW = "REMOTE_VIEW"
        const val REMOTE_VIEW_ORDER = "REMOTE_VIEW_ORDER"
        const val TAG = "RVS"
    }

    private lateinit var messenger: Messenger //receives remote invocations

    override fun onBind(intent: Intent): IBinder? {
        if (!this::messenger.isInitialized) {
            synchronized(RemoteViewService::class.java) {
                if (!this::messenger.isInitialized) {
                    this.messenger = Messenger(IncomingHandler(this))
                }
            }
        }
        //Return the proper IBinder instance
        return this.messenger.binder
    }


    private class IncomingHandler(service: RemoteViewService) : Handler() {
        private val mService: WeakReference<RemoteViewService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            mService.get()?.onServiceRequest(msg)
        }
    }

    /**
     * Setup the reply message
     */
    private fun onServiceRequest(msg: Message) {
        //make the RPC invocation
        msg.replyTo.send(Message().apply {
            var remoteViews: RemoteViews? = null
            try {
                remoteViews = getRemoteViews(msg.data.apply { classLoader = activity.classLoader })
            } catch (e: Throwable) {
                // capture exceptions located in RemoteViewService in order to return an empty remote view
                Log.e(TAG, "Error on getRemoteViews", e)
            }
            data.apply {
                putInt(REMOTE_VIEW_ORDER, displayOrder)
                putParcelable(REMOTE_VIEW, remoteViews)
            }
        })
    }

    /**
     * Create remote view
     */
    open fun getRemoteViews(bundle: Bundle): RemoteViews? =
        RemoteViews(packageName, layoutRes).apply {
            setOnClickPendingIntent(
                onClickIdRes,
                PendingIntent.getActivity(
                    applicationContext, 0,
                    Intent(applicationContext, activity).apply {
                        putExtras(bundle)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }, PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
}