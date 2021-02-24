package fr.busin.appa

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import fr.busin.appa.remoteView.NoRemoteViewFoundError
import fr.busin.appa.remoteView.RemoteViewManager
import fr.busin.appa.remoteView.RemoteViewServiceBindingError
import fr.busin.appa.remoteView.RemoteViewSuccess
import fr.busin.appa.service.ReplyServiceConnection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class MainActivity : Activity() {

    private val compositeDisposable = CompositeDisposable()
    private val replyServiceConnection = ReplyServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.textView).setOnClickListener {
            replyServiceConnection.sendToB("SALUT FROM A")
        }

        RemoteViewManager.with(this)
            .action("fr.busin.appb.action.REMOTE_VIEW")
            .get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { e ->
                    Log.e("MainActivity", "an error has occurred", e)
                },
                onNext = { result ->
                    when (result) {
                        is RemoteViewSuccess -> {
                            findViewById<FrameLayout>(R.id.button_wishlist).addView(result.view)
                        }
                        is RemoteViewServiceBindingError -> {
                            Log.e("MainActivity", result.error.message, result.error)
                        }
                        is NoRemoteViewFoundError -> {
                            Log.e("MainActivity", result.error.message, result.error)
                        }
                        else -> {
                            Log.e("MainActivity", "result.error.message")
                        }
                    }
                }
            ).addTo(compositeDisposable)
    }


    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        val serviceIntent = Intent("fr.busin.appb.messaging")
        serviceIntent.setPackage("fr.busin.appb")
        this.bindService(serviceIntent, replyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(replyServiceConnection)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

}