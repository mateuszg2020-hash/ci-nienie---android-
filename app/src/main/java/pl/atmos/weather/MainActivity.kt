package pl.atmos.weather

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ComponentName
import android.appwidget.AppWidgetManager
import android.content.pm.PackageManager
import android.os.Build

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(true)
        window.statusBarColor = Color.rgb(6, 16, 29)
        window.navigationBarColor = Color.rgb(6, 16, 29)
        web = WebView(this)
        web.setBackgroundColor(Color.rgb(6, 16, 29))
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true
        web.addJavascriptInterface(AtmosBridge(), "AtmosBridge")
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        createNotificationChannel()
        setContentView(web)
        try {
            val html = assets.open("index.html").bufferedReader().use { it.readText() }
            val css = assets.open("styles.css").bufferedReader().use { it.readText() }
            val js = assets.open("app.js").bufferedReader().use { it.readText() }
            val page = html.replace("<!-- ATMOS_STYLES -->", "<style>\n$css\n</style>")
                .replace("<!-- ATMOS_SCRIPT -->", "<script>\n$js\n</script>")
            web.loadDataWithBaseURL("https://atmos.app/", page, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            web.loadDataWithBaseURL("https://atmos.app/", "<html><body style='background:#06101d;color:white;padding:24px;font:16px sans-serif'>Nie udało się uruchomić ATMOS: ${e.message}</body></html>", "text/html", "UTF-8", null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) {
            web.onResume()
            web.resumeTimers()
            web.postDelayed({
                if (::web.isInitialized) web.evaluateJavascript(
                    "if(typeof loadWeather==='function'){loadWeather(true);}" +
                    "if(typeof radarMap!=='undefined'&&radarMap)radarMap.invalidateSize();" +
                    "if(typeof windMap!=='undefined'&&windMap){windMap.invalidateSize();resizeWind();}",
                    null
                )
            }, 500)
        }
    }

    override fun onPause() {
        if (::web.isInitialized) web.onPause()
        super.onPause()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("atmos_weather", "ATMOS — alarmy pogodowe", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    inner class AtmosBridge {
        @JavascriptInterface fun saveLocation(name: String, lat: Double, lon: Double) {
            getSharedPreferences("atmos_widget", Context.MODE_PRIVATE).edit()
                .putString("city", name).putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
            val manager = AppWidgetManager.getInstance(this@MainActivity)
            val ids = manager.getAppWidgetIds(ComponentName(this@MainActivity, AtmosWeatherWidgetProvider::class.java))
            if (ids.isNotEmpty()) AtmosWeatherWidgetProvider.updateAll(this@MainActivity, manager, ids)
        }
        @JavascriptInterface fun requestPermission() {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 501)
                }
            }
        }
        @JavascriptInterface fun notify(title: String, body: String) {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runOnUiThread
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    android.app.Notification.Builder(this@MainActivity, "atmos_weather")
                } else {
                    @Suppress("DEPRECATION")
                    android.app.Notification.Builder(this@MainActivity)
                }.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title).setContentText(body)
                    .setStyle(android.app.Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(true).build()
                manager.notify(title.hashCode(), notification)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
