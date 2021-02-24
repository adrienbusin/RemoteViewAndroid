package fr.busin.appb.remoteview

import fr.busin.appb.MainActivity
import fr.busin.appb.R


class AppRemoteViewService : RemoteViewService(
    layoutRes = R.layout.button_to_show_in_app_a,
    onClickIdRes = R.id.button_widget,
    displayOrder = 1,
    activity = MainActivity::class.java
)
