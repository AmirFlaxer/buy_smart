package com.amir.buysmart.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val Green = Color(0xFF2E7D32)
private val OnGreen = Color(0xFFFFFFFF)
private val Surface = Color(0xFFF7FBF1)
private val OnSurface = Color(0xFF191D17)
private const val MAX_ITEMS = 12
private val activeListKey = stringPreferencesKey("active_list_id")

class BuySmartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val listId = entryPoint.dataStore().data.first()[activeListKey]
        val items: List<ShoppingItem> = if (listId == null) {
            emptyList()
        } else {
            val entities = entryPoint.itemDao().getItemsForList(listId).first()
            widgetItems(entities.map { it.toDomain() }, MAX_ITEMS)
        }
        provideContent {
            WidgetContent(loggedOut = listId == null, items = items)
        }
    }

    @Composable
    private fun WidgetContent(loggedOut: Boolean, items: List<ShoppingItem>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Surface))
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BuySmart",
                    style = TextStyle(
                        color = ColorProvider(Green),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                        .clickable(actionStartActivity<MainActivity>())
                )
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .cornerRadius(18.dp)
                        .background(ColorProvider(Green))
                        .clickable(actionStartActivity<QuickAddActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", style = TextStyle(color = ColorProvider(OnGreen), fontSize = 22.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = GlanceModifier.size(8.dp))

            when {
                loggedOut -> CenterMessage("התחבר כדי לראות את הרשימה")
                items.isEmpty() -> CenterMessage("הרשימה ריקה 🎉")
                else -> LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                        ItemRow(item)
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemRow(item: ShoppingItem) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.location.emoji, style = TextStyle(fontSize = 16.sp))
            Spacer(modifier = GlanceModifier.size(8.dp))
            Text(
                text = item.name + if (item.quantity.isNotBlank()) " (${item.quantity})" else "",
                style = TextStyle(color = ColorProvider(OnSurface), fontSize = 15.sp),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }

    @Composable
    private fun CenterMessage(text: String) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(color = ColorProvider(OnSurface), fontSize = 15.sp),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
            )
        }
    }
}
