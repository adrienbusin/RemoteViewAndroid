package fr.busin.appa.remoteView

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import fr.busin.appa.R
import fr.busin.appa.remoteView.RemoteViewService.Companion.REMOTE_VIEW
import fr.busin.appa.remoteView.RemoteViewService.Companion.REMOTE_VIEW_ORDER
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import java.lang.ref.WeakReference


class RemoteViewManager private constructor(
    private val context: Context,
    private val serviceInfo: ServiceInfo?,
    private val packageName: String,
    private val action: String,
    private val bundle: Bundle
) {

    // receives callbacks from bind and unbind invocations
    private lateinit var connection: RemoteServiceConnection

    private lateinit var serviceIntent: Intent

    private val singleSubject: SingleSubject<RemoteViewResult> = SingleSubject.create()

    fun get(): Single<RemoteViewResult> = if (serviceInfo == null) {
        singleSubject.apply {
            onSuccess(NoRemoteViewFoundError(Intent(action).apply {
                setPackage(
                    packageName
                )
            }))
        }
    } else {
        connection = RemoteServiceConnection()
        serviceIntent = Intent().apply {
            component = ComponentName(serviceInfo.applicationInfo.packageName, serviceInfo.name)
        }

        // Bind to the remote service
        context.bindService(serviceIntent, connection, BIND_AUTO_CREATE)
        singleSubject.doFinally { context.unbindService(connection) }
    }


    private fun onServiceResponse(msg: Message) {
        val remoteViews = msg.data.getParcelable<RemoteViews>(REMOTE_VIEW)
        val viewOrder = msg.data.getInt(REMOTE_VIEW_ORDER)
        remoteViews?.toView()
            ?.let { singleSubject.onSuccess(RemoteViewSuccess(serviceIntent, it, viewOrder)) }

        if (remoteViews == null) {
            singleSubject.onSuccess(NoRemoteViewFoundError(serviceIntent))
        }
    }


    /**
     * Transform RemoteViews to View
     */
    private fun RemoteViews?.toView() =
        this?.apply(context, View.inflate(context, R.layout.remoteview, null) as ViewGroup) as View


    private inner class RemoteServiceConnection : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, binder: IBinder) {
            val messenger = Messenger(binder) //used to make an RPC invocation
            try {
                //Setup the message for invocation
                val message = Message()

                //Set the ReplyTo Messenger for processing the invocation response
                message.replyTo = Messenger(IncomingHandler(this@RemoteViewManager))

                // datas to send
                message.data.putAll(bundle)

                //Make the invocation
                messenger.send(message)
            } catch (e: Exception) {
                Log.e(TAG, "Binding to the remote service error", e)
                singleSubject.onSuccess(RemoteViewServiceBindingError(serviceIntent))
            }

        }

        override fun onServiceDisconnected(component: ComponentName) {}
    }

    private class IncomingHandler(manager: RemoteViewManager) : Handler() {
        private val mManager: WeakReference<RemoteViewManager> = WeakReference(manager)

        override fun handleMessage(msg: Message) {
            mManager.get()?.onServiceResponse(msg)
        }
    }


    //region builder

    companion object {
        private const val TAG = "RemoteViewManager"
        fun with(context: Context) = Builder(context)
    }

    open class Builder(private val context: Context) {
        private var action: String = ""
        private var ignoreItself: Boolean = false
        private val bundle: Bundle = Bundle()

        fun action(action: String) = apply { this.action = action }
        fun ignoreItself() = apply { this.ignoreItself = true }

        fun putInt(key: String, value: Int) = apply { bundle.putInt(key, value) }
        fun putLong(key: String, value: Long) = apply { bundle.putLong(key, value) }
        fun putString(key: String, value: String) = apply { bundle.putString(key, value) }
        fun putBoolean(key: String, value: Boolean) = apply { bundle.putBoolean(key, value) }
        fun putAll(bundle: Bundle) = apply { this.bundle.putAll(bundle) }

        fun get(packageName: String): Single<RemoteViewResult> = RemoteViewManager(
            context,
            getPackageNamesWithServiceAction().find { it.packageName == packageName },
            packageName,
            action,
            bundle
        ).get()

        fun get(): Flowable<RemoteViewResult> {
            val singles = getPackageNamesWithServiceAction().map { serviceInfo ->
                RemoteViewManager(
                    context,
                    serviceInfo,
                    serviceInfo.packageName,
                    action,
                    bundle
                ).get()
            }

            return Single.merge(singles)
                .sorted { rvc1, rvc2 ->
                    when {
                        rvc1 is RemoteViewSuccess && rvc2 is RemoteViewSuccess -> if (rvc1.displayOrder < rvc2.displayOrder) -1 else 1
                        rvc1 is RemoteViewSuccess && rvc2 !is RemoteViewSuccess -> -1
                        rvc1 !is RemoteViewSuccess && rvc2 is RemoteViewSuccess -> 1
                        else -> 1
                    }
                }
        }

        fun getStartWith(packageNamePrefix: String): Flowable<RemoteViewResult> {
            val singles = getPackageNames(packageNamePrefix).map { serviceInfo ->
                RemoteViewManager(
                    context,
                    serviceInfo,
                    serviceInfo.packageName,
                    action,
                    bundle
                ).get()
            }

            return Single.merge(singles)
                .sorted { rvc1, rvc2 ->
                    when {
                        rvc1 is RemoteViewSuccess && rvc2 is RemoteViewSuccess -> if (rvc1.displayOrder < rvc2.displayOrder) -1 else 1
                        rvc1 is RemoteViewSuccess && rvc2 !is RemoteViewSuccess -> -1
                        rvc1 !is RemoteViewSuccess && rvc2 is RemoteViewSuccess -> 1
                        else -> 1
                    }
                }
        }

        private fun getPackageNames(packageNamePrefix: String): List<ServiceInfo> =
            getPackageNamesWithServiceAction().filter { serviceInfo ->
                serviceInfo.packageName.startsWith(
                    packageNamePrefix
                )
            }

        private fun getPackageNamesWithServiceAction() =
            context.packageManager.queryIntentServices(Intent(action), 0)
                .map { resolveInfo -> resolveInfo.serviceInfo }
                .filter { serviceInfo -> if (ignoreItself) serviceInfo.packageName != context.packageName else true }
                ?: listOf()
    }
    //endregion builder
}

