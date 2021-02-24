package fr.busin.appa.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import android.util.Log

class ReplyServiceConnection : ServiceConnection {

    var mService: Messenger? = null

    var mReplyMessenger = Messenger(ReplyHandler(this))

    internal class ReplyHandler(private val replyServiceConnection: ReplyServiceConnection) :
        Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                RESPONSE_FROM_B -> {
                    Log.d(TAG, "Message received with data ${msg.data.getString(MESSAGE)}")
                    replyServiceConnection.onResponse(msg.data.getString(MESSAGE))
                }
            }
        }
    }


    override fun onServiceDisconnected(className: ComponentName?) {
        mService = null
    }

    override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
        mService = Messenger(service)
    }

    fun onResponse(text: String?) {
        //DO SOMETHING
    }

    fun sendToB(s: String) {
        val message = Message.obtain(null, MSG_TELL_SOMETHING_TO_B)
        message.replyTo = mReplyMessenger
        message.data = Bundle().apply {
            putString(MESSAGE, s)
        }
        mService?.send(message)
    }

    companion object {
        const val MSG_TELL_SOMETHING_TO_B = 1
        const val RESPONSE_FROM_B = 2
        const val MESSAGE = "MESSAGE"
        const val TAG = "ReplyServiceConnection"
    }

}