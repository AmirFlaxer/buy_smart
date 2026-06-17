package com.amir.buysmart.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** מרענן את כל מופעי ה-widget. נקרא אחרי שינוי נתונים. */
object WidgetUpdater {
    suspend fun update(context: Context) {
        BuySmartWidget().updateAll(context)
    }
}
