package pl.atmos.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AtmosWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { updateAll(context, manager, ids) }
    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { id ->
                val prefs = context.getSharedPreferences("atmos_widget", Context.MODE_PRIVATE)
                val city = prefs.getString("city", "Gostynin") ?: "Gostynin"
                val lat = prefs.getFloat("lat", 52.4297f).toDouble()
                val lon = prefs.getFloat("lon", 19.4619f).toDouble()
                executor.execute {
                    var temp = "--°"; var wind = "-- km/h"; var description = "Otwórz ATMOS, aby odświeżyć"
                    try {
                        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$"+"lat&longitude=$"+"lon&current=temperature_2m,wind_speed_10m,weather_code&timezone=auto")
                        val connection = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 9000; readTimeout = 9000; requestMethod = "GET" }
                        val body = connection.inputStream.bufferedReader().use { it.readText() }; connection.disconnect()
                        val current = JSONObject(body).getJSONObject("current")
                        temp = "${"+"current.getDouble(\"temperature_2m\").toInt()}°"
                        wind = "${"+"current.getDouble(\"wind_speed_10m\").toInt()} km/h"
                        description = weatherText(current.optInt("weather_code", -1))
                    } catch (_: Exception) { description = "Brak połączenia • dane niedostępne" }
                    val views = RemoteViews(context.packageName, R.layout.atmos_widget)
                    views.setTextViewText(R.id.widget_city, city); views.setTextViewText(R.id.widget_temp, temp)
                    views.setTextViewText(R.id.widget_condition, description); views.setTextViewText(R.id.widget_wind, "Wiatr $"+ "wind")
                    val intent = Intent(context, MainActivity::class.java)
                    val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_root, pending); manager.updateAppWidget(id, views)
                }
            }
        }
        private fun weatherText(code: Int): String = when (code) {
            0 -> "Bezchmurnie"; 1, 2 -> "Małe zachmurzenie"; 3 -> "Pochmurno"; 45, 48 -> "Mgła"
            51, 53, 55, 56, 57 -> "Mżawka"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Opady deszczu"
            71, 73, 75, 77, 85, 86 -> "Opady śniegu"; 95, 96, 99 -> "Burza"; else -> "Warunki pogodowe"
        }
    }
}
