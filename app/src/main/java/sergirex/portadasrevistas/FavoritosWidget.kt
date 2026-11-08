package sergirex.portadasrevistas

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import coil3.request.allowHardware
import coil3.size.Precision
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.WidgetStateRepository
import sergirex.portadasrevistas.domain.usecase.GetFavoriteRevistasUseCase
import sergirex.portadasrevistas.domain.usecase.GetRevistaCoversUseCase
import sergirex.portadasrevistas.notifications.launchAsync
import javax.inject.Inject

/**
 * Home-screen widget with the latest cover of the user's favorite magazines, one at a time; the arrows move
 * between them and tapping the cover opens the app. Without favorites it says how to add them.
 *
 * Work runs with goAsync() + a coroutine (see [launchAsync]): a BroadcastReceiver's process can be killed the
 * moment onReceive() returns unless goAsync()'s PendingResult is still open.
 */
@AndroidEntryPoint
class FavoritosWidget : AppWidgetProvider() {

    @Inject
    lateinit var getFavoriteRevistas: GetFavoriteRevistasUseCase

    @Inject
    lateinit var getRevistaCovers: GetRevistaCoversUseCase

    @Inject
    lateinit var widgetState: WidgetStateRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            launchAsync { renderWidget(context, appWidgetManager, appWidgetId) }
        }
    }

    /** The user resized the widget: reload the cover at the new size. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        launchAsync { renderWidget(context, appWidgetManager, appWidgetId) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val delta = when (intent.action) {
            ACTION_LEFT -> -1
            ACTION_RIGHT -> 1
            else -> return
        }
        launchAsync {
            val favorites = getFavoriteRevistas()
            if (favorites.isEmpty()) return@launchAsync
            val newIndex = (widgetState.index() + delta).coerceIn(0, favorites.lastIndex)
            widgetState.setIndex(newIndex)
            renderWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    private suspend fun renderWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.favoritos_widget)
        val favorites = getFavoriteRevistas()

        if (favorites.isEmpty()) {
            views.setViewVisibility(R.id.emptyStateText, View.VISIBLE)
            views.setViewVisibility(R.id.imageViewWidget, View.GONE)
            views.setViewVisibility(R.id.leftBtn, View.GONE)
            views.setViewVisibility(R.id.rightBtn, View.GONE)
        } else {
            val index = widgetState.index().coerceIn(0, favorites.lastIndex)

            views.setViewVisibility(R.id.emptyStateText, View.GONE)
            views.setViewVisibility(R.id.imageViewWidget, View.VISIBLE)
            views.setViewVisibility(R.id.leftBtn, if (index > 0) View.VISIBLE else View.INVISIBLE)
            views.setViewVisibility(R.id.rightBtn, if (index < favorites.lastIndex) View.VISIBLE else View.INVISIBLE)
            views.setOnClickPendingIntent(R.id.leftBtn, navigatePendingIntent(context, appWidgetId, ACTION_LEFT))
            views.setOnClickPendingIntent(R.id.rightBtn, navigatePendingIntent(context, appWidgetId, ACTION_RIGHT))
            views.setContentDescription(R.id.imageViewWidget, favorites[index].name)

            loadCoverBitmap(context, favorites[index], appWidgetManager, appWidgetId)?.let { views.setImageViewBitmap(R.id.imageViewWidget, it) }
        }

        views.setOnClickPendingIntent(R.id.imageViewWidget, openAppPendingIntent(context))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /** Reuses RevistaCoverRepository (does the magazine have a cover?) + Coil (fetch/decode/cache), like the list screens. */
    private suspend fun loadCoverBitmap(
        context: Context,
        revista: RevistaRef,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): Bitmap? {
        val cover = getRevistaCovers(listOf(revista), forceRefresh = false).firstOrNull() ?: return null
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        // The largest the launcher may show it (portrait uses the max height, landscape the max width).
        val width = context.dpToPx(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH))
        val height = context.dpToPx(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT))
        return context.loadBitmap(cover.imageUrl) {
            // The bitmap is parcelled to the launcher, so no hardware bitmap, and no bigger than the widget
            // (INEXACT never upscales a cover smaller than that). Launchers that don't report a size get the original.
            allowHardware(false)
            if (width > 0 && height > 0) {
                size(width, height)
                precision(Precision.INEXACT)
            }
        }
    }

    private fun Context.dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()

    private fun navigatePendingIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, FavoritosWidget::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        // requestCode must be unique per (widget, action) pair or the two PendingIntents collide.
        val requestCode = appWidgetId * 10 + action.hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, Portadas::class.java), PendingIntent.FLAG_IMMUTABLE)

    private companion object {
        const val ACTION_LEFT = "android.appwidget.action.LEFT"
        const val ACTION_RIGHT = "android.appwidget.action.RIGHT"
    }
}
