package fr.busin.appb.service

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log

class ReceiveServiceMessenger : Service() {

    companion object {
        const val TAG = "OtherServiceMessenger"
        const val MSG_FROM_A = 1
        const val MSG_TOP_A = 2
        const val MESSAGE = "MESSAGE"
    }

    internal class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_FROM_A -> {
                    Log.d(
                        TAG, "request received to search customer card : ${msg.data.getString(
                            MESSAGE
                        )}")
                    val reply = Message.obtain(null, MSG_TOP_A)
                    reply.data = Bundle().apply {
                        putString(MESSAGE, "hello from B")
                    }
                }
            }
        }
    }

    private lateinit var mMessenger: Messenger

    override fun onBind(intent: Intent?): IBinder? {
        mMessenger = Messenger(IncomingHandler())
        return mMessenger.binder
    }

}