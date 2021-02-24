package fr.busin.appa.remoteView

import android.content.Intent
import android.view.View

sealed class RemoteViewResult(val intentService: Intent)

data class RemoteViewSuccess(val intent: Intent, val view: View, val displayOrder: Int = -1) : RemoteViewResult(intent)

data class NoRemoteViewFoundError(private val intent: Intent) : RemoteViewResult(intent) {
    val error: Exception = Exception("No remote view found from service : $intentService")
}

data class RemoteViewServiceBindingError(private val intent: Intent) : RemoteViewResult(intent) {
    val error: Exception = Exception("Service binding error for : $intentService")
}