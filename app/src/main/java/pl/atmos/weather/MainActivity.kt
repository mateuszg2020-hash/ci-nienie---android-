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
import android.content.pm.PackageManager
import android.os.Build

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(Color.rgb(7,17,31))
        web.settings.javaScriptEnabled = true
        createNotificationChannel()
        web.addJavascriptInterface(AtmosBridge(), "AtmosBridge")
        web.settings.domStorageEnabled = true
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        setContentView(web)
        web.loadDataWithBaseURL("https://atmos.app/", PAGE, "text/html", "UTF-8", null)
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
                    .setContentTitle(title).setContentText(body).setStyle(android.app.Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(true).build()
                manager.notify(title.hashCode(), notification)
            }
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    companion object {
        private val PAGE = """
<!doctype html>
<html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<title>ATMOS</title>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<style>
:root{--bg:#07111f;--panel:#101f31;--panel2:#14263a;--line:#243950;--text:#f1f6fc;--muted:#91a7bd;--blue:#62c5ff;--cyan:#51e0d0;--orange:#ffb45f}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,Segoe UI,sans-serif;padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom)}
header{padding:20px 20px 12px;display:flex;align-items:center;justify-content:space-between} .logo{font-size:25px;font-weight:850;letter-spacing:3px}.logo span{color:var(--blue)}.place{font-size:13px;color:var(--muted);margin-top:3px}.pill{border:1px solid var(--line);border-radius:20px;padding:8px 12px;color:var(--blue);font-size:12px} .locbtn{border:1px solid #315675;background:#142b40;color:#dff3ff;border-radius:12px;padding:9px 12px;font-size:12px;margin-top:9px}

main{padding:0 16px 34px;max-width:760px;margin:0 auto}header{max-width:760px;margin:0 auto;padding:24px 20px 18px}.section{font-size:18px;letter-spacing:-.2px;margin:26px 2px 12px}.tile{box-shadow:0 8px 24px #00000012}.hero{padding:24px;min-height:210px}.grid{gap:11px}.mapwrap{height:420px;min-height:320px}.chartbox{padding:12px 12px 8px}.chartbox canvas{display:block;width:100%;height:210px}.chartlegend{display:flex;gap:16px;flex-wrap:wrap;color:var(--muted);font-size:12px;margin:4px 4px 8px}.chartlegend i{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:5px}.hero{background:linear-gradient(135deg,#15334b,#10243a 65%,#12283b);border:1px solid #244763;border-radius:24px;padding:20px;position:relative;overflow:hidden}.hero:after{content:"";position:absolute;width:180px;height:180px;border-radius:50%;right:-55px;top:-65px;background:#4ebdff12}.eyebrow{color:#9ab4ca;font-size:12px;letter-spacing:1.4px;text-transform:uppercase}.temp{font-size:68px;font-weight:300;letter-spacing:-4px;margin:8px 0 0}.desc{font-size:16px;color:#d3e4f2}.feels{font-size:13px;color:#9eb5c9;margin-top:7px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:12px}.tile{background:var(--panel);border:1px solid var(--line);border-radius:17px;padding:14px;min-height:88px}.tile .k{font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.8px}.tile .v{font-size:22px;font-weight:650;margin-top:8px}.tile .s{font-size:11px;color:var(--muted);margin-top:3px}.section{font-size:16px;font-weight:750;margin:22px 2px 10px}.hours{display:flex;gap:9px;overflow:auto;padding-bottom:5px}.hour{min-width:70px;background:var(--panel);border:1px solid var(--line);border-radius:15px;padding:12px 8px;text-align:center}.hour b{display:block;font-size:17px;margin:7px 0}.hour small{color:var(--muted)}.screen{display:block;margin:0 0 26px}.mapwrap{height:55vh;min-height:350px;border-radius:20px;overflow:hidden;border:1px solid var(--line);position:relative}#map,#windmap{height:100%;width:100%;background:#0b1827}.mapnote{font-size:12px;color:var(--muted);line-height:1.5;margin:10px 3px}.controls{background:var(--panel);border:1px solid var(--line);padding:14px;border-radius:17px;margin-top:11px}.controlrow{display:flex;align-items:center;justify-content:space-between;gap:10px}.controlrow strong{font-size:14px}.controlrow span{font-size:12px;color:var(--blue)}input[type=range]{width:100%;accent-color:#5cc5ff;margin:14px 0 5px}.btn{border:1px solid #2b5b7b;background:#15364e;color:#eaf7ff;border-radius:12px;padding:10px 14px;font-weight:650}.windstats{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:10px}.windstats .tile{padding:12px;min-height:70px}.windstats .v{font-size:18px}.legend{display:flex;gap:12px;flex-wrap:wrap;font-size:11px;color:var(--muted);margin:10px 4px}.dot{display:inline-block;width:8px;height:8px;border-radius:50%;margin-right:5px;background:#56d8ca}.error{color:#ffc078;font-size:12px;margin-top:8px}.longlist{display:grid;gap:9px}.dayrow{display:grid;grid-template-columns:34px 1.15fr .8fr 1fr;gap:8px;align-items:center;padding:13px;background:var(--tile);border:1px solid var(--line);border-radius:14px}.dayicon{font-size:25px;text-align:center}.dayrow .date{font-size:13px;color:var(--muted)}.dayrow .temps{font-size:17px;font-weight:700}.dayrow .detail{font-size:12px;color:var(--muted);text-align:right}.weekcard{padding:16px;background:linear-gradient(135deg,#142e46,#102033);border:1px solid var(--line);border-radius:16px}.weekhead{font-weight:700;margin-bottom:10px}.weekmetrics{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.weekmetrics b{display:block;font-size:18px;margin-top:4px}.weekmetrics small{color:var(--muted);font-size:11px}.trendnote{color:var(--muted);font-size:12px;margin-top:10px}.modalback{display:none;position:fixed;inset:0;background:#020814dd;z-index:2000;align-items:center;justify-content:center;padding:22px}.modalback.open{display:flex}.citymodal{width:100%;max-width:440px;background:#102237;border:1px solid #31516d;border-radius:22px;padding:20px;box-shadow:0 24px 80px #0009}.citymodal h2{margin:0 0 7px;font-size:21px}.citymodal p{margin:0 0 16px;color:var(--muted);font-size:13px}.cityinput{width:100%;padding:14px 15px;border-radius:13px;border:1px solid #355773;background:#091727;color:#fff;font-size:16px;outline:none}.cityinput:focus{border-color:var(--blue)}.cityresults{max-height:260px;overflow:auto;margin-top:10px}.cityresult{width:100%;text-align:left;padding:13px;margin-top:7px;border:1px solid #29445e;border-radius:12px;background:#142b40;color:#f1f6fc;font-size:14px}.cityactions{display:flex;justify-content:flex-end;margin-top:14px}.cityactions button{border:0;background:transparent;color:var(--blue);padding:10px 14px;font-weight:700}

/* ATMOS premium interface */
:root{--bg:#071321;--panel:#101f30;--panel2:#162b40;--line:#263f57;--text:#f4f8fc;--muted:#9bb0c3;--blue:#65c8ff;--cyan:#53dfcf;--orange:#ffbd72}
body{background:radial-gradient(ellipse at 80% -12%,#183b56 0,transparent 36%),var(--bg);font-family:Inter,system-ui,-apple-system,"Segoe UI",sans-serif;padding-bottom:calc(88px + env(safe-area-inset-bottom))}
header{padding:24px 20px 18px;align-items:flex-start}
.logo{font-size:30px;letter-spacing:4px;line-height:1.05}.place{font-size:12px;letter-spacing:1.3px;margin-top:9px}
.pill{border:1px solid #31516c;background:#0d1c2b;border-radius:999px;padding:9px 13px;font-size:11px;white-space:nowrap}
.locbtn{border:1px solid #315a78;background:linear-gradient(135deg,#183b55,#10283d);border-radius:999px;padding:10px 15px;font-size:12px}
main{padding:0 17px 30px;max-width:760px}
.screen{display:none;margin:0 0 18px}.screen.active{display:block;animation:screenIn .22s ease-out}
@keyframes screenIn{from{opacity:.5;transform:translateY(5px)}to{opacity:1;transform:none}}
.section{font-size:18px;font-weight:760;letter-spacing:-.45px;margin:24px 2px 13px}
.hero{min-height:230px;border-radius:28px;padding:24px;background:linear-gradient(145deg,#1a405b 0%,#122d45 54%,#102237 100%);border:1px solid #315672;box-shadow:0 18px 45px #0003}
.hero:after{width:220px;height:220px;right:-70px;top:-90px;background:radial-gradient(circle,#66c9ff24,transparent 68%)}
.eyebrow{font-size:11px;letter-spacing:1.8px}.temp{font-size:76px;line-height:1.12;font-weight:300;margin:13px 0 8px;letter-spacing:-5px}
.desc{font-size:17px;font-weight:550}.feels{font-size:13px;margin-top:10px}
.grid{gap:10px;margin-top:12px}.tile{border-radius:20px;padding:16px;background:linear-gradient(145deg,#14283c,#101f30);border:1px solid #29435b;box-shadow:0 7px 20px #00000012;min-height:100px}
.tile .k{font-size:10px;letter-spacing:1.15px}.tile .v{font-size:24px;font-weight:680;letter-spacing:-.5px;margin-top:10px}.tile .s{font-size:11px;line-height:1.45;margin-top:5px}
.hours{gap:9px;padding:2px 1px 9px}.hour{min-width:78px;border-radius:17px;background:#112337;border:1px solid #29435b;padding:13px 9px}.hour b{font-size:18px}.hour small{font-size:11px}
.chartbox{padding:15px 12px 10px}.chartbox canvas{height:220px}.chartlegend{font-size:11px}
.mapwrap{height:52vh;min-height:340px;border-radius:23px;border:1px solid #34516a;box-shadow:0 12px 28px #0003}
.controls{border-radius:20px;padding:16px;background:linear-gradient(145deg,#142b40,#0f2032);border:1px solid #2b465e}
.btn{border-radius:14px;padding:11px 16px;background:linear-gradient(135deg,#1b4966,#15354d);border:1px solid #39708f}
.dayrow{border-radius:17px;padding:14px 12px;background:linear-gradient(120deg,#14283b,#101f30);grid-template-columns:34px 1.15fr .8fr 1fr}
.weekcard{border-radius:19px;padding:17px;background:linear-gradient(140deg,#18334b,#102033);border:1px solid #2b4963}
.mapnote{font-size:12px;line-height:1.6}
.bottom-nav{position:fixed;z-index:1500;left:12px;right:12px;bottom:calc(10px + env(safe-area-inset-bottom));max-width:700px;margin:0 auto;display:grid;grid-template-columns:repeat(5,1fr);gap:3px;padding:8px 7px;background:#0d1c2beF;border:1px solid #29445c;border-radius:24px;box-shadow:0 14px 40px #0008;backdrop-filter:blur(18px)}
.nav-item{border:0;background:transparent;color:#8fa7bc;border-radius:17px;min-width:0;padding:7px 2px 6px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;font:600 10px system-ui;letter-spacing:.1px}
.nav-icon{font-size:21px;line-height:22px;font-weight:500}
.nav-item.active{color:#eaf8ff;background:linear-gradient(145deg,#1d4b68,#16354d);box-shadow:inset 0 0 0 1px #39708e}
.nav-item.active .nav-icon{color:#74d2ff}
.citymodal{border-radius:25px;box-shadow:0 28px 90px #000b}
@media(min-width:600px){.bottom-nav{bottom:18px}.hero{padding:30px}.grid{gap:13px}.tile{padding:19px}}

/* ATMOS — new editorial weather identity */
:root{--bg:#eef3f7;--panel:#ffffff;--panel2:#f7fafc;--line:#d9e3ec;--text:#102235;--muted:#62778a;--blue:#1769e8;--cyan:#00a99a;--orange:#f28b38;--tile:#fff}
html{background:#eef3f7}
body{background:linear-gradient(180deg,#e8f0f7 0%,#f4f7fa 42%,#eaf0f5 100%);color:#102235;padding-bottom:calc(94px + env(safe-area-inset-bottom))}
header{background:transparent}
.logo{color:#102235;letter-spacing:3.5px}.logo span{color:#1677ed}
.place,.eyebrow,.tile .k,.tile .s,.hour small,.mapnote,.trendnote{color:#64798d}
.pill{background:#fff;border-color:#d5e0e9;color:#1769e8;box-shadow:0 4px 14px #18334b0d}
.locbtn{background:#102b45;color:#fff;border:1px solid #244866;box-shadow:0 7px 18px #16334b20}
.section{color:#12283d}
.hero{background:radial-gradient(circle at 85% 12%,#9edbff 0,transparent 31%),linear-gradient(135deg,#123a5a 0%,#1769a7 48%,#24a6b5 100%);border:0;box-shadow:0 18px 40px #14527b28;color:#fff}
.hero .eyebrow,.hero .feels{color:#d6edff}.hero .desc{color:#fff}
.tile{background:#fff;border:1px solid #dce5ed;box-shadow:0 7px 22px #19344b0b;color:#102235}
.hour{background:#fff;border:1px solid #dce5ed;box-shadow:0 4px 14px #19344b08;color:#102235}
.controls{background:#fff;border:1px solid #d8e3ec;box-shadow:0 8px 24px #19344b0b;color:#102235}
.btn{background:linear-gradient(135deg,#1769e8,#0d4fb7);border:0;color:#fff;box-shadow:0 7px 18px #1769e833}
.dayrow,.weekcard{background:#fff;border:1px solid #dce5ed;box-shadow:0 5px 16px #19344b08;color:#102235}
.weekmetrics small{color:#64798d}
.bottom-nav{background:#ffffffed;border:1px solid #d9e3ec;box-shadow:0 12px 36px #18334b20;backdrop-filter:blur(22px)}
.nav-item{color:#74879a}.nav-item.active{background:#e5f1ff;color:#125ecb;box-shadow:inset 0 0 0 1px #c3ddfb}.nav-item.active .nav-icon{color:#1769e8}
.citymodal{background:#fff;color:#102235;border:1px solid #d9e3ec}.cityinput{background:#f5f8fb;color:#102235;border:1px solid #d5e0e9}.cityresult{background:#f5f8fb;color:#102235;border-color:#d9e3ec}
.mapwrap{border:1px solid #d2dee8;box-shadow:0 12px 30px #19344b18}
@media(prefers-reduced-motion:no-preference){.tile,.hour,.controls,.weekcard{transition:transform .18s ease,box-shadow .18s ease}.tile:active,.hour:active{transform:scale(.985)}}
</style></head><body>
<header><div><div class="logo">ATM<span>O</span>S</div><div class="place" id="placeName">GOSTYNIN • POGODA • RADAR • WIATR</div><button class="locbtn" id="changeCity">⌖ ZMIEŃ LOKALIZACJĘ</button></div><div class="pill" id="updated">ŁADOWANIE</div></header>

<main>
<section id="today" class="screen active">
<div class="hero"><div class="eyebrow" id="city">GOSTYNIN • TERAZ</div><div class="temp" id="temp">--°</div><div class="desc" id="condition">Pobieram pogodę…</div><div class="feels" id="feels">Odczuwalna --°</div></div>
<div class="grid">
<div class="tile"><div class="k">Wiatr</div><div class="v" id="windval">-- km/h</div><div class="s" id="winddir">Kierunek --</div></div>
<div class="tile"><div class="k">Porywy</div><div class="v" id="gust">-- km/h</div><div class="s">Maksymalny podmuch</div></div>
<div class="tile"><div class="k">Opady</div><div class="v" id="rain">-- mm</div><div class="s">Godzinowo</div></div>
<div class="tile"><div class="k">Wilgotność</div><div class="v" id="humidity">--%</div><div class="s">Względna</div></div>
<div class="tile"><div class="k">Ciśnienie</div><div class="v" id="pressure">-- hPa</div><div class="s">Na poziomie morza</div></div>
<div class="tile"><div class="k">Indeks UV</div><div class="v" id="uv">--</div><div class="s">Bieżąca wartość</div></div>
</div>
<div class="section">Najbliższe godziny</div><div class="hours" id="hours"></div><div class="section">Przebieg pogody — 24 godziny</div><div class="tile chartbox"><div class="chartlegend"><span><i style="background:#62c5ff"></i>Temperatura °C</span><span><i style="background:#51e0d0"></i>Opady mm</span></div><canvas id="weatherChart" height="210"></canvas><div class="s" id="weatherChartNote">Prognoza godzinowa Open-Meteo</div></div>
</section>
<section id="longrange" class="screen">
<div class="section">Prognoza na 45 dni</div>
<div class="mapnote">Pierwsze 16 dni: prognoza dzienna. Dalszy okres: orientacyjny trend ECMWF w ujęciu tygodniowym. Im dalej, tym większa niepewność.</div>
<div id="longStatus" class="mapnote">Pobieram prognozę długoterminową…</div>
<div class="section">Najbliższe 16 dni</div><div id="daily45" class="longlist"></div>
<div class="section">Dalszy trend — tygodnie 3–7</div><div id="weekly45" class="longlist"></div>
<div class="mapnote">Dane długoterminowe są aktualizowane wraz z nowymi przebiegami modeli. Nie należy traktować ich jako dokładnej prognozy dla konkretnej godziny.</div>
</section>
<section id="radar" class="screen">
<div class="section">Mapa prognozy opadów — najbliższe 12 godzin</div>
<div class="mapwrap"><div id="map"></div></div>
<div class="controls">
<div class="controlrow"><strong id="radarTime">Pobieram radar…</strong><button class="btn" id="playRadar">▶ Odtwarzaj</button></div>
<input id="radarRange" type="range" min="0" max="0" value="0">
<div class="controlrow"><span>Teraz</span><span>+12 godzin</span></div>
</div>
<div class="mapnote" id="radarNote">Mapa pokazuje prognozowane opady w kolejnych godzinach na podstawie modelu Open-Meteo. To prognoza modelowa, nie pomiar radarowy.</div>
</section>
<section id="wind" class="screen">
<div class="section">Mapa przepływu wiatru</div><div class="mapwrap"><div id="windmap"></div></div>
<div class="windstats"><div class="tile"><div class="k">Wiatr</div><div class="v" id="wSpeed">--</div></div><div class="tile"><div class="k">Porywy</div><div class="v" id="wGust">--</div></div><div class="tile"><div class="k">Kierunek</div><div class="v" id="wDir">--</div></div></div>
<div class="controls"><div class="controlrow"><strong id="windTime">Prognoza godzinowa</strong><span>Open-Meteo</span></div><input id="windRange" type="range" min="0" max="47" value="0"><div class="controlrow"><span>Teraz</span><span>+48 h</span></div></div>
<div class="mapnote" id="windNote">Strzałki pokazują kierunek przepływu wiatru na całym widocznym obszarze mapy. Kafelki i suwak pokazują wiatr dla wybranej lokalizacji.</div>
</section>
<section id="more" class="screen">
<div class="section">ATMOS — centrum warunków</div>
<div class="tile" style="margin-bottom:12px"><div class="k">Alarmy pogodowe</div><div class="v" style="font-size:17px">Ostrzegaj mnie o zmianach</div><div class="s">Alarmy działają podczas pracy aplikacji. Włącz powiadomienia systemowe.</div>
<label style="display:block;margin-top:12px"><input type="checkbox" id="alertRain" checked> Deszcz w najbliższych 2 godzinach</label>
<label style="display:block;margin-top:8px"><input type="checkbox" id="alertWind" checked> Porywy powyżej 50 km/h</label>
<label style="display:block;margin-top:8px"><input type="checkbox" id="alertFrost" checked> Temperatura spada do 2°C lub mniej</label>
<button class="btn" id="enableAlerts" style="margin-top:12px">Włącz powiadomienia</button><div class="mapnote" id="alertStatus">Powiadomienia nie są jeszcze aktywne.</div></div>
<div class="section">Ostrzeżenia IMGW</div><div class="tile" id="warningsBox"><div class="s">Pobieram oficjalne ostrzeżenia…</div></div>
<div class="section">Jakość powietrza</div><div class="grid" id="airGrid"><div class="tile"><div class="k">PM2.5</div><div class="v">—</div></div><div class="tile"><div class="k">PM10</div><div class="v">—</div></div><div class="tile"><div class="k">Indeks AQI</div><div class="v">—</div></div><div class="tile"><div class="k">Pyłki</div><div class="v">—</div></div></div><div class="mapnote" id="airNote">Dane z modelu jakości powietrza Open-Meteo.</div>
<div class="section">Ciśnienie — ostatnie 48 godzin</div><div class="tile chartbox"><div class="chartlegend"><span><i style="background:#62c5ff"></i>Ciśnienie na poziomie morza · hPa</span></div><canvas id="pressureChart" height="210"></canvas><div class="s" id="pressureNote">Dane godzinowe z Open-Meteo</div></div>
<div class="section">Słońce i Księżyc</div><div class="grid" id="sunGrid"><div class="tile"><div class="k">Wschód</div><div class="v" id="sunrise">—</div></div><div class="tile"><div class="k">Zachód</div><div class="v" id="sunset">—</div></div><div class="tile"><div class="k">Długość dnia</div><div class="v" id="dayLength">—</div></div><div class="tile"><div class="k">Faza Księżyca</div><div class="v" id="moonPhase">—</div></div></div>
<div class="section">Tryb wodniaka</div><div class="tile"><div class="k">Warunki na wodzie — najbliższe 6 godzin</div><div class="v" id="boatStatus" style="font-size:19px">Analizuję…</div><div class="s" id="boatDetails">Ocena orientacyjna na podstawie wiatru, porywów, opadów i burz. Nie zastępuje komunikatów nawigacyjnych.</div></div>

</section>
</main>
<nav class="bottom-nav" aria-label="Główna nawigacja">
<button class="nav-item active" data-screen="today"><span class="nav-icon">◉</span><span>Pogoda</span></button>
<button class="nav-item" data-screen="longrange"><span class="nav-icon">▦</span><span>45 dni</span></button>
<button class="nav-item" data-screen="radar"><span class="nav-icon">◌</span><span>Radar</span></button>
<button class="nav-item" data-screen="wind"><span class="nav-icon">↝</span><span>Wiatr</span></button>
<button class="nav-item" data-screen="more"><span class="nav-icon">☷</span><span>Więcej</span></button>
</nav>
<div class="modalback" id="cityModal"><div class="citymodal"><h2>Wybierz lokalizację</h2><p>Wpisz miasto i wybierz właściwy wynik.</p><input class="cityinput" id="cityQuery" type="search" placeholder="Np. Gostynin, Płock, Warszawa" autocomplete="off"><div class="cityresults" id="cityResults"></div><div class="cityactions"><button id="closeCity">ANULUJ</button></div></div></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
let LAT=52.4297,LON=19.4619,currentCity="Gostynin";let weather=null,precipGrid=[],precipLayers=[],radarMap=null,radarMarker=null,windMap=null,windMarker=null,playing=false,playTimer=null,particles=[],windCanvas=null,windCtx=null,windAnim=null;
const $=id=>document.getElementById(id);
function switchScreen(id){
 document.querySelectorAll('.screen').forEach(el=>el.classList.toggle('active',el.id===id));
 document.querySelectorAll('.nav-item').forEach(el=>el.classList.toggle('active',el.dataset.screen===id));
 window.scrollTo({top:0,behavior:'smooth'});
 if(id==='longrange')loadLongRange();
 if(id==='radar')setTimeout(()=>{initRadar();if(radarMap)radarMap.invalidateSize()},100);
 if(id==='wind')setTimeout(()=>{initWind();if(windMap){windMap.invalidateSize();resizeWind()}},100);
 if(id==='more')setTimeout(()=>{loadMoreFeatures();drawPressureFromForecast()},100);
 if(id==='today')setTimeout(()=>{drawWeatherChart()},100);
}
document.querySelectorAll('.nav-item').forEach(el=>el.addEventListener('click',()=>switchScreen(el.dataset.screen)));

window.onerror=function(message,source,line,col,error){const detail='BŁĄD APLIKACJI: '+String(message)+' (linia '+line+')';const status=$('updated');if(status)status.textContent='BŁĄD JS';const condition=$('condition');if(condition)condition.textContent=detail;return false;};
window.onunhandledrejection=function(event){const detail='BŁĄD DANYCH: '+String(event.reason&&event.reason.message||event.reason);if($('condition'))$('condition').textContent=detail;if($('updated'))$('updated').textContent='BŁĄD DANYCH';};
let citySearchTimer=null;
$('changeCity').onclick=()=>{$('cityModal').classList.add('open');$('cityQuery').value='';$('cityResults').innerHTML='';setTimeout(()=>$('cityQuery').focus(),120)};
$('closeCity').onclick=()=> $('cityModal').classList.remove('open');
$('cityModal').addEventListener('click',e=>{if(e.target===$('cityModal'))$('cityModal').classList.remove('open')});
$('cityQuery').addEventListener('input',()=>{clearTimeout(citySearchTimer);const q=$('cityQuery').value.trim();if(q.length<2){$('cityResults').innerHTML='';return;}$('cityResults').innerHTML='<div class="mapnote">Szukam miejscowości…</div>';citySearchTimer=setTimeout(async()=>{try{const r=await fetch('https://geocoding-api.open-meteo.com/v1/search?name='+encodeURIComponent(q)+'&count=8&language=pl&format=json');const j=await r.json();const results=j.results||[];$('cityResults').innerHTML='';if(!results.length){$('cityResults').innerHTML='<div class="mapnote">Nie znaleziono miejscowości.</div>';return;}results.forEach(p=>{const button=document.createElement('button');button.className='cityresult';button.textContent=p.name+(p.admin1?', '+p.admin1:'')+(p.country?', '+p.country:'');button.onclick=()=>selectCity(p);$('cityResults').appendChild(button)});}catch(e){$('cityResults').innerHTML='<div class="error">Nie udało się pobrać listy miast. Sprawdź internet.</div>'}},350);});
function selectCity(p){LAT=p.latitude;LON=p.longitude;currentCity=p.name+(p.country_code?', '+p.country_code:'');$('placeName').textContent=currentCity.toUpperCase()+' • POGODA • RADAR • WIATR';$('city').textContent=currentCity.toUpperCase()+' • TERAZ';$('cityModal').classList.remove('open');if(radarMap){radarMap.setView([LAT,LON],7);if(radarMarker)radarMarker.setLatLng([LAT,LON]);precipGrid=[];precipLayers.forEach(x=>radarMap.removeLayer(x));precipLayers=[];}if(windMap){windMap.setView([LAT,LON],7);if(windMarker)windMarker.setLatLng([LAT,LON]);}weather=null;const ls=$('longStatus');if(ls)ls.dataset.loaded='0';loadWeather();}
function direction(d){const a=['N','NE','E','SE','S','SW','W','NW'];return a[Math.round(d/45)%8]}
function nextHourIndex(times,now){const target=new Date(now).getTime();for(let i=0;i<times.length;i++){const t=new Date(times[i]).getTime();if(Number.isFinite(t)&&t>=target-60*1000)return i;}return Math.max(0,times.length-1)}
async function loadWeather(force=false){try{$('city').textContent=currentCity.toUpperCase()+' • TERAZ';$('updated').textContent='AKTUALIZACJA…';const u='https://api.open-meteo.com/v1/forecast?latitude='+LAT+'&longitude='+LON+'&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,weather_code&hourly=temperature_2m,precipitation,precipitation_probability,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl,weather_code&past_days=2&forecast_days=3&timezone=Europe%2FWarsaw&_='+Date.now();const controller=new AbortController();const requestTimeout=setTimeout(()=>controller.abort(),15000);let r;try{r=await fetch(u,{cache:'no-store',signal:controller.signal})}finally{clearTimeout(requestTimeout)}if(!r.ok){const body=await r.text();throw new Error('Open-Meteo HTTP '+r.status+(body?' — '+body.slice(0,140):''))}const payload=await r.json();if(payload.error||!payload.current||!payload.hourly)throw new Error(payload.reason||'Niepełna odpowiedź serwera');weather=payload;updateRainForecast();renderChartsAndBoat();drawPressureFromForecast();checkWeatherAlerts();$('windNote').textContent='Strzałki pokazują kierunek przepływu wiatru na całym widocznym obszarze mapy. Kafelki i suwak pokazują wiatr dla wybranej lokalizacji: '+currentCity+'.';const c=weather.current;$('temp').textContent=Math.round(c.temperature_2m)+'°';$('condition').textContent=c.is_day?'Warunki w ciągu dnia':'Warunki nocne';$('feels').textContent='Odczuwalna '+Math.round(c.apparent_temperature)+'°';$('windval').textContent=Math.round(c.wind_speed_10m)+' km/h';$('winddir').textContent=direction(c.wind_direction_10m)+' • '+c.wind_direction_10m+'°';$('gust').textContent=Math.round(c.wind_gusts_10m)+' km/h';$('rain').textContent=Number(c.precipitation).toFixed(1)+' mm';$('humidity').textContent=c.relative_humidity_2m+'%';$('pressure').textContent=Math.round(c.pressure_msl)+' hPa';$('uv').textContent=c.is_day?Number(c.uv_index).toFixed(1):'0';$('updated').textContent='AKTUALNE • '+new Date().toLocaleTimeString('pl-PL',{hour:'2-digit',minute:'2-digit'});$('hours').innerHTML='';let now=nextHourIndex(weather.hourly.time,c.time);for(let i=now;i<Math.min(now+18,weather.hourly.time.length);i++){const d=new Date(weather.hourly.time[i]);const el=document.createElement('div');el.className='hour';el.innerHTML='<small>'+d.toLocaleTimeString('pl-PL',{hour:'2-digit',minute:'2-digit'})+'</small><b>'+Math.round(weather.hourly.temperature_2m[i])+'°</b><small>Wiatr '+Math.round(weather.hourly.wind_speed_10m[i])+'</small>'; $('hours').appendChild(el)}$('windRange').max=Math.min(47,weather.hourly.time.length-now-1);$('windRange').oninput=()=>updateWind(Number($('windRange').value),now);updateWind(0,now)}catch(e){const reason=e&&e.name==='AbortError'?'Przekroczono 15 s oczekiwania na serwer pogody. Sprawdź internet.':(e&&e.message?e.message:String(e));$('condition').textContent='BŁĄD POGODY: '+reason;$('updated').textContent='BŁĄD AKTUALIZACJI';console.error('ATMOS weather refresh',e)}}
function updateRainForecast(){if(!weather||!weather.hourly)return;const advice=$('rainAdvice'),summary=$('rainSummary'),hours=$('rainHours');if(!advice||!summary||!hours)return;const h=weather.hourly;let start=nextHourIndex(h.time,weather.current.time);const end=Math.min(start+11,h.time.length-1);let maxProb=0,total=0,wetHours=0;for(let i=start;i<=end;i++){const p=Number(h.precipitation_probability?.[i]||0),mm=Number(h.precipitation?.[i]||0);maxProb=Math.max(maxProb,p);total+=mm;if(p>=40||mm>=0.1)wetHours++;}if(maxProb>=50&&total>=0.2){advice.textContent='☔ Tak — warto wziąć parasol';}else if(maxProb>=35||total>=0.1){advice.textContent='🌦 Możliwy deszcz — parasol się przyda';}else{advice.textContent='☀️ Raczej bez parasola';}summary.textContent='W kolejnych '+(end-start+1)+' godzinach: maks. prawdopodobieństwo '+Math.round(maxProb)+'%, suma opadów ok. '+total.toFixed(1)+' mm. Godziny z możliwym opadem: '+wetHours+'.';$('rainHours').innerHTML='';for(let i=start;i<=end;i++){const el=document.createElement('div');el.className='hour';const d=new Date(h.time[i]);el.innerHTML='<small>'+d.toLocaleTimeString('pl-PL',{hour:'2-digit',minute:'2-digit'})+'</small><b>'+(Math.round(h.precipitation_probability?.[i]||0))+'%</b><small>'+(Number(h.precipitation?.[i]||0).toFixed(1))+' mm</small>';$('rainHours').appendChild(el);}}
let featuresLoaded=false,alertLast={},alertsEnabled=false;
function localTime(v){return new Date(v).toLocaleTimeString('pl-PL',{hour:'2-digit',minute:'2-digit'})}
function chartCanvas(id,height=210){const c=$(id);if(!c)return null;const w=Math.max(280,c.clientWidth||c.parentElement?.clientWidth||320),h=height,dpr=Math.min(window.devicePixelRatio||1,2);c.width=Math.round(w*dpr);c.height=Math.round(h*dpr);const x=c.getContext('2d');x.setTransform(dpr,0,0,dpr,0,0);x.clearRect(0,0,w,h);return {x,w,h}}
function drawPressureFromForecast(){if(!weather?.hourly)return;const h=weather.hourly,now=nextHourIndex(h.time,weather.current.time),from=Math.max(0,now-48),to=Math.min(h.time.length,now+1),vals=h.pressure_msl.slice(from,to),times=h.time.slice(from,to);drawPressureChart(vals,times);$('pressureNote').textContent=vals.filter(Number.isFinite).length>5?'Godzinowe dane Open-Meteo. Ostatni punkt odpowiada bieżącej analizie modelu.':'Za mało danych do wykresu.'}
function drawPressureChart(values,times){const q=chartCanvas('pressureChart');if(!q)return;const {x,w,h}=q,nums=values.map(Number),valid=nums.filter(Number.isFinite);if(valid.length<3){x.fillStyle='#91a7bd';x.fillText('Brak wystarczających danych ciśnienia',12,35);return}const lo=Math.floor((Math.min(...valid)-2)/2)*2,hi=Math.ceil((Math.max(...valid)+2)/2)*2,range=Math.max(4,hi-lo),L=42,R=10,T=18,B=30,pw=w-L-R,ph=h-T-B;x.font='10px system-ui';x.fillStyle='#91a7bd';x.strokeStyle='#294158';for(let k=0;k<=4;k++){const y=T+k*ph/4,v=hi-k*range/4;x.beginPath();x.moveTo(L,y);x.lineTo(w-R,y);x.stroke();x.fillText(String(Math.round(v)),4,y+3)}const pts=[];nums.forEach((v,i)=>{if(Number.isFinite(v))pts.push([L+i*pw/Math.max(1,nums.length-1),T+(hi-v)/range*ph])});if(pts.length>1){x.beginPath();x.moveTo(pts[0][0],pts[0][1]);pts.slice(1).forEach(p=>x.lineTo(p[0],p[1]));x.strokeStyle='#62c5ff';x.lineWidth=2.6;x.stroke();const p=pts[pts.length-1];x.fillStyle='#62c5ff';x.beginPath();x.arc(p[0],p[1],4,0,Math.PI*2);x.fill()}x.fillStyle='#91a7bd';x.fillText(times[0]?new Date(times[0]).toLocaleDateString('pl-PL',{day:'2-digit',month:'2-digit'}):'',L,h-8);x.fillText('TERAZ',w-42,h-8)}
function drawWeatherChart(){if(!weather?.hourly)return;const hd=weather.hourly,now=nextHourIndex(hd.time,weather.current.time),end=Math.min(now+24,hd.time.length),temps=hd.temperature_2m.slice(now,end),rain=hd.precipitation.slice(now,end),q=chartCanvas('weatherChart');if(!q)return;const {x,w,h}=q,valid=temps.filter(Number.isFinite);if(!valid.length)return;const min=Math.floor(Math.min(...valid)-2),max=Math.ceil(Math.max(...valid)+2),span=Math.max(4,max-min),L=34,R=28,T=18,B=32,pw=w-L-R,ph=h-T-B;x.font='10px system-ui';x.fillStyle='#91a7bd';x.strokeStyle='#294158';for(let k=0;k<=4;k++){const y=T+k*ph/4;x.beginPath();x.moveTo(L,y);x.lineTo(w-R,y);x.stroke();x.fillText(Math.round(max-k*span/4)+'°',2,y+3)}const maxRain=Math.max(1,...rain.map(v=>Number(v)||0));rain.forEach((v,i)=>{const n=Math.max(0,Number(v)||0),bw=Math.max(3,pw/temps.length*.48),xx=L+(i+.5)*pw/temps.length,hh=n/maxRain*ph*.7;x.fillStyle='#51e0d0aa';x.fillRect(xx-bw/2,T+ph-hh,bw,hh)});x.beginPath();temps.forEach((v,i)=>{const xx=L+i*pw/Math.max(1,temps.length-1),yy=T+(max-v)/span*ph;if(i)x.lineTo(xx,yy);else x.moveTo(xx,yy)});x.strokeStyle='#62c5ff';x.lineWidth=2.8;x.stroke();temps.forEach((v,i)=>{if(i%4!==0&&i!==temps.length-1)return;const xx=L+i*pw/Math.max(1,temps.length-1),yy=T+(max-v)/span*ph;x.fillStyle='#dff5ff';x.beginPath();x.arc(xx,yy,2.7,0,Math.PI*2);x.fill();x.fillStyle='#91a7bd';x.fillText(new Date(hd.time[now+i]).toLocaleTimeString('pl-PL',{hour:'2-digit'}),xx-8,h-9)});x.fillStyle='#51e0d0';x.fillText('mm',w-24,T+10);$('weatherChartNote').textContent='Prognoza godzinowa Open-Meteo: linia oznacza temperaturę, słupki — opad.'}
window.addEventListener('resize',()=>{if(weather){drawWeatherChart();drawPressureFromForecast()}})
function moonName(date){const syn=29.530588853,ref=Date.UTC(2000,0,6,18,14),age=((date.getTime()-ref)/86400000%syn+syn)%syn;if(age<1.85)return '🌑 Nów';if(age<5.54)return '🌒 Sierp';if(age<9.22)return '🌓 I kwadra';if(age<12.91)return '🌔 Garb';if(age<16.61)return '🌕 Pełnia';if(age<20.30)return '🌖 Garb';if(age<23.99)return '🌗 III kwadra';if(age<27.68)return '🌘 Sierp';return '🌑 Nów'}
async function loadMoreFeatures(){if(featuresLoaded){if(weather)renderChartsAndBoat();return}featuresLoaded=true;loadAir();loadWarnings();loadSunData();renderChartsAndBoat()}
async function loadAir(){try{const u='https://air-quality-api.open-meteo.com/v1/air-quality?latitude='+LAT+'&longitude='+LON+'&current=pm10,pm2_5,us_aqi,european_aqi&hourly=alder_pollen,birch_pollen,grass_pollen&timezone=Europe%2FWarsaw';const r=await fetch(u,{cache:'no-store'});if(!r.ok)throw Error('HTTP '+r.status);const j=await r.json(),a=j.current||{};$('airGrid').innerHTML=[['PM2.5',a.pm2_5,'µg/m³'],['PM10',a.pm10,'µg/m³'],['AQI UE',a.european_aqi,'pkt'],['AQI USA',a.us_aqi,'pkt']].map(x=>'<div class="tile"><div class="k">'+x[0]+'</div><div class="v">'+(x[1]==null?'—':Math.round(x[1]))+' <small style="font-size:11px">'+x[2]+'</small></div></div>').join('');const h=j.hourly||{},i=nextHourIndex(h.time||[],j.current?.time||new Date().toISOString());const pollen=['alder_pollen','birch_pollen','grass_pollen'].map(k=>Number(h[k]?.[i]||0));$('airNote').textContent='Pyłki (modelowo): olcha '+pollen[0]+' • brzoza '+pollen[1]+' • trawy '+pollen[2]+'. AQI jest indeksem modelowym, nie pomiarem ze stacji.'}catch(e){$('airNote').textContent='Jakość powietrza chwilowo niedostępna: '+e.message}}
async function loadWarnings(){try{const r=await fetch('https://danepubliczne.imgw.pl/api/data/warningsmeteo',{cache:'no-store'});if(!r.ok)throw Error('HTTP '+r.status);const data=await r.json();const rows=Array.isArray(data)?data:[];const city=currentCity.toLocaleLowerCase('pl');const teryt=city.includes('gostynin')?'1404':city.includes('płock')||city.includes('plock')?'1462':city.includes('warszawa')?'1465':city.includes('włocławek')||city.includes('wloclawek')?'0464':null;const now=Date.now();const active=rows.filter(x=>{const expiry=Date.parse(String(x.obowiazuje_do||'').replace(' ','T')+'+01:00');return !Number.isFinite(expiry)||expiry>=now});const matches=teryt?active.filter(x=>Array.isArray(x.teryt)&&x.teryt.includes(teryt)):[];$('warningsBox').innerHTML=matches.length?matches.map(x=>'<div style="padding:10px 0;border-bottom:1px solid var(--line)"><b>⚠️ '+safeText(x.nazwa_zdarzenia||'Ostrzeżenie IMGW')+' • stopień '+safeText(x.stopien||'?')+'</b><div class="s">Prawdopodobieństwo: '+safeText(x.prawdopodobienstwo||'—')+'% • do '+safeText(x.obowiazuje_do||'—')+'</div><div class="s">'+safeText(x.tresc||'')+'</div></div>').join(''):'<div class="v" style="font-size:16px">Brak aktywnego ostrzeżenia IMGW dla '+safeText(currentCity)+'</div><div class="s">Sprawdzono oficjalny wykaz IMGW. Dopasowanie jest dostępne dla Gostynina i wybranych miast; dla pozostałych lokalizacji nie pokazujemy ostrzeżeń bez potwierdzenia powiatu.</div>'}catch(e){$('warningsBox').innerHTML='<div class="error">Nie udało się pobrać ostrzeżeń IMGW: '+safeText(e.message)+'</div>'}}
function safeText(s){return String(s??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]))}
async function loadSunData(){try{const d=new Date().toISOString().slice(0,10),u='https://api.open-meteo.com/v1/forecast?latitude='+LAT+'&longitude='+LON+'&daily=sunrise,sunset&timezone=Europe%2FWarsaw&forecast_days=1';const r=await fetch(u);if(!r.ok)throw Error('HTTP '+r.status);const j=await r.json(),rise=new Date(j.daily.sunrise[0]),set=new Date(j.daily.sunset[0]);$('sunrise').textContent=localTime(rise);$('sunset').textContent=localTime(set);$('dayLength').textContent=Math.floor((set-rise)/3600000)+' h '+Math.round(((set-rise)%3600000)/60000)+' min';$('moonPhase').textContent=moonName(new Date())}catch(e){$('moonPhase').textContent='Dane niedostępne'}}
function renderChartsAndBoat(){if(!weather)return;const h=weather.hourly,now=nextHourIndex(h.time,weather.current.time);drawWeatherChart();drawPressureFromForecast();let maxWind=0,maxGust=0,maxRain=0,storm=false;for(let i=now;i<Math.min(now+6,h.time.length);i++){maxWind=Math.max(maxWind,Number(h.wind_speed_10m[i]||0));maxGust=Math.max(maxGust,Number(h.wind_gusts_10m[i]||0));maxRain+=Number(h.precipitation[i]||0);storm=storm||[95,96,99].includes(Number(h.weather_code?.[i]))}let status='Warunki umiarkowane';if(storm||maxGust>=60||maxWind>=40)status='⚠️ Niekorzystne — rozważ pozostanie w porcie';else if(maxGust>=40||maxWind>=25||maxRain>=3)status='🌬️ Zachowaj ostrożność';else status='✅ Prognoza bez wyraźnych sygnałów pogorszenia';$('boatStatus').textContent=status;$('boatDetails').textContent='Najbliższe 6 h: wiatr do '+Math.round(maxWind)+' km/h, porywy do '+Math.round(maxGust)+' km/h, opad '+maxRain.toFixed(1)+' mm.'}
function checkWeatherAlerts(){if(!alertsEnabled||!weather||!window.AtmosBridge)return;const h=weather.hourly,now=nextHourIndex(h.time,weather.current.time);const rain=$('alertRain').checked&&h.precipitation_probability.slice(now,now+2).some(v=>Number(v)>=60);const wind=$('alertWind').checked&&h.wind_gusts_10m.slice(now,now+3).some(v=>Number(v)>=50);const frost=$('alertFrost').checked&&h.temperature_2m.slice(now,now+6).some(v=>Number(v)<=2);[['rain',rain,'Deszcz możliwy w ciągu 2 godzin','Prognoza wskazuje co najmniej 60% prawdopodobieństwa opadu.'],['wind',wind,'Silne porywy wiatru','Prognozowane porywy mogą przekroczyć 50 km/h.'],['frost',frost,'Możliwy przymrozek','Prognoza wskazuje temperaturę 2°C lub niższą.']].forEach(([key,on,title,body])=>{if(on&&!alertLast[key])window.AtmosBridge.notify(title,body);alertLast[key]=on})}
$('enableAlerts').onclick=()=>{if(!window.AtmosBridge){$('alertStatus').textContent='Powiadomienia systemowe niedostępne na tym urządzeniu.';return}window.AtmosBridge.requestPermission();alertsEnabled=true;$('alertStatus').textContent='Alarmy aktywne, gdy aplikacja jest uruchomiona i ma dostęp do danych.';checkWeatherAlerts()};

function weatherIcon(code){if(code===0)return '☀️';if([1,2].includes(code))return '🌤️';if(code===3)return '☁️';if([45,48].includes(code))return '🌫️';if([51,53,55,56,57].includes(code))return '🌦️';if([61,63,65,66,67,80,81,82].includes(code))return '🌧️';if([71,73,75,77,85,86].includes(code))return '❄️';if([95,96,99].includes(code))return '⛈️';return '🌡️'}
function weatherLabel(code){if(code===0)return 'Bezchmurnie';if([1,2,3].includes(code))return 'Zachmurzenie';if([45,48].includes(code))return 'Mgła';if([51,53,55,56,57].includes(code))return 'Mżawka';if([61,63,65,66,67,80,81,82].includes(code))return 'Opady';if([71,73,75,77,85,86].includes(code))return 'Śnieg';if([95,96,99].includes(code))return 'Burze';return 'Zmienna pogoda'}
async function loadLongRange(){const status=$('longStatus'),dailyBox=$('daily45'),weeklyBox=$('weekly45');if(!status||status.dataset.loaded==='1')return;status.dataset.loaded='1';status.textContent='Pobieram prognozę dla '+currentCity+'…';dailyBox.innerHTML='';weeklyBox.innerHTML='';try{const base='latitude='+LAT+'&longitude='+LON+'&timezone=Europe%2FWarsaw';const dailyUrl='https://api.open-meteo.com/v1/forecast?'+base+'&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,weather_code,wind_speed_10m_max&forecast_days=16';const seasonalUrl='https://seasonal-api.open-meteo.com/v1/seasonal?'+base+'&daily=temperature_2m_min,temperature_2m_mean,temperature_2m_max,precipitation_sum,weather_code,wind_speed_10m_mean&forecast_days=46&models=ecmwf_ec46';const responses=await Promise.all([fetch(dailyUrl),fetch(seasonalUrl)]);if(!responses[0].ok)throw new Error('Prognoza 16-dniowa: HTTP '+responses[0].status);if(!responses[1].ok)throw new Error('Model EC46: HTTP '+responses[1].status);const d=await responses[0].json(),z=await responses[1].json();if(d.error)throw new Error(d.reason||'Błąd prognozy');if(z.error)throw new Error(z.reason||'Błąd modelu EC46');const q=d.daily;if(!q||!q.time||!q.time.length)throw new Error('Brak prognozy dziennej');for(let i=0;i<q.time.length;i++){const date=new Date(q.time[i]+'T12:00:00');const row=document.createElement('div');row.className='dayrow';const rain=q.precipitation_probability_max?.[i];row.innerHTML='<div class="dayicon">'+weatherIcon(q.weather_code?.[i])+'</div><div><div class="date">'+date.toLocaleDateString('pl-PL',{weekday:'short',day:'numeric',month:'short'})+'</div><div style="font-size:12px;color:var(--muted)">'+weatherLabel(q.weather_code?.[i])+'</div></div><div class="temps">'+Math.round(q.temperature_2m_min[i])+'° / '+Math.round(q.temperature_2m_max[i])+'°</div><div class="detail">Opady '+(rain==null?'—':Math.round(rain)+'%')+'<br>Wiatr '+(q.wind_speed_10m_max?.[i]==null?'—':Math.round(q.wind_speed_10m_max[i])+' km/h')+'</div>';dailyBox.appendChild(row)}const e=z.daily;if(!e||!e.time||e.time.length<20)throw new Error('Model EC46 nie zwrócił wystarczających danych dziennych');const firstForecastDate=q.time[q.time.length-1];let startIndex=e.time.findIndex(t=>t>firstForecastDate);if(startIndex<0)startIndex=Math.min(15,e.time.length);const later=e.time.map((date,i)=>({date,min:e.temperature_2m_min?.[i],mean:e.temperature_2m_mean?.[i],max:e.temperature_2m_max?.[i],rain:e.precipitation_sum?.[i],wind:e.wind_speed_10m_mean?.[i]})).slice(startIndex,45);for(let k=0;k<later.length;k+=7){const pack=later.slice(k,k+7);if(!pack.length)continue;const avg=key=>{const a=pack.map(x=>x[key]).filter(Number.isFinite);return a.length?a.reduce((x,y)=>x+y,0)/a.length:null};const sum=key=>{const a=pack.map(x=>x[key]).filter(Number.isFinite);return a.length?a.reduce((x,y)=>x+y,0):null};const from=new Date(pack[0].date+'T12:00:00'),to=new Date(pack[pack.length-1].date+'T12:00:00');const card=document.createElement('div');card.className='weekcard';const min=avg('min'),max=avg('max'),mean=avg('mean'),rain=sum('rain'),wind=avg('wind');card.innerHTML='<div class="weekhead">'+from.toLocaleDateString('pl-PL',{day:'numeric',month:'short'})+' – '+to.toLocaleDateString('pl-PL',{day:'numeric',month:'short'})+'</div><div class="weekmetrics"><div><small>Śr. min / max</small><b>'+(min==null||max==null?'—':Math.round(min)+'° / '+Math.round(max)+'°')+'</b></div><div><small>Suma opadów*</small><b>'+(rain==null?'—':rain.toFixed(1)+' mm')+'</b></div><div><small>Śr. wiatr</small><b>'+(wind==null?'—':Math.round(wind)+' km/h')+'</b></div></div><div class="trendnote">ECMWF EC46 • trend tygodniowy'+(mean==null?'':' • średnia '+Math.round(mean)+'°')+' • orientacyjnie</div>';weeklyBox.appendChild(card)}status.textContent='Dane pobrane: '+q.time.length+' dni szczegółowo oraz '+later.length+' dni trendu EC46. Model EC46 ma siatkę ok. 36 km i nie jest prognozą punktową.';}catch(e){status.textContent='Nie udało się pobrać prognozy: '+(e.message||'błąd połączenia')+'. Dotknij zakładki 45 dni ponownie, aby spróbować jeszcze raz.';status.dataset.loaded='0';}}
async function initRadar(){if(!radarMap){radarMap=L.map('map',{zoomControl:true}).setView([LAT,LON],7);L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',{maxZoom:18,attribution:'© OpenStreetMap © CARTO'}).addTo(radarMap);radarMarker=L.circleMarker([LAT,LON],{radius:6,color:'#fff',weight:2,fillColor:'#45c5ff',fillOpacity:1}).addTo(radarMap).bindPopup(currentCity);}radarMap.invalidateSize();if(precipGrid.length){setPrecipHour(Number($('radarRange').value)||0);return;}$('radarTime').textContent='Pobieram prognozę opadów…';$('radarNote').textContent='Pobieram dane modelowe dla siatki punktów wokół '+currentCity+'.';try{const lats=[],lons=[];for(let y=-3;y<=3;y++)for(let x=-3;x<=3;x++){lats.push((LAT+y*0.32).toFixed(4));lons.push((LON+x*0.48).toFixed(4));}const url='https://api.open-meteo.com/v1/forecast?latitude='+lats.join(',')+'&longitude='+lons.join(',')+'&hourly=precipitation,precipitation_probability&forecast_days=2&timezone=Europe%2FWarsaw';const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),20000);let response;try{response=await fetch(url,{cache:'no-store',signal:controller.signal})}finally{clearTimeout(timeout)}if(!response.ok)throw new Error('Open-Meteo HTTP '+response.status);const data=await response.json();precipGrid=Array.isArray(data)?data:[data];if(precipGrid.length!==49||!precipGrid[0]?.hourly?.time?.length)throw new Error('Niepełne dane siatki opadów');$('radarRange').min=0;$('radarRange').max=12;$('radarRange').value=0;$('radarRange').oninput=()=>setPrecipHour(Number($('radarRange').value));$('playRadar').onclick=()=>{if(playing){clearInterval(playTimer);playing=false;$('playRadar').textContent='▶ Odtwarzaj';return;}playing=true;$('playRadar').textContent='Ⅱ Pauza';playTimer=setInterval(()=>{const next=(Number($('radarRange').value)+1)%13;setPrecipHour(next);},1000);};setPrecipHour(0);}catch(e){$('radarTime').textContent='Prognoza opadów niedostępna';$('radarNote').textContent='Nie udało się pobrać danych prognozy: '+(e.message||e)+'. Sprawdź połączenie i spróbuj ponownie.';precipGrid=[];}}
function setPrecipHour(offset){if(!precipGrid.length)return;precipLayers.forEach(layer=>radarMap.removeLayer(layer));precipLayers=[];const first=precipGrid[0].hourly.time;const base=nextHourIndex(first,weather?.current?.time||new Date().toISOString());const index=Math.min(base+offset,first.length-1);let wet=0;precipGrid.forEach((point,i)=>{const y=Math.floor(i/7)-3,x=i%7-3;const mm=Number(point.hourly.precipitation?.[index]||0),prob=Number(point.hourly.precipitation_probability?.[index]||0);if(mm<0.05&&prob<20)return;wet++;const intensity=Math.max(mm,prob/100*.6);const color=intensity>=2?'#8b5cf6':intensity>=.8?'#2878ff':intensity>=.25?'#20c9e8':'#48e0c0';const opacity=Math.min(.48,.18+Math.min(intensity,4)*.075);const center=[LAT+y*.32,LON+x*.48];const radius=19000+Math.min(intensity,3)*5500;precipLayers.push(L.circle(center,{radius,color,weight:0,opacity:0,fillColor:color,fillOpacity:opacity,interactive:false,smoothFactor:1}).addTo(radarMap));});const date=new Date(first[index]);$('radarTime').textContent=date.toLocaleString('pl-PL',{weekday:'short',hour:'2-digit',minute:'2-digit'})+' • '+(wet?'opady w '+wet+' obszarach':'brak prognozowanych opadów');$('radarNote').textContent=wet?'Prognoza modelowa Open-Meteo. Niebiesko-turkusowe i fioletowe pola pokazują orientacyjne obszary opadów; natężenie jest wygładzone wizualnie między punktami modelu. To nie jest pomiar radarowy.':'Model Open-Meteo nie przewiduje obecnie istotnych opadów na pokazanym obszarze. Brak kolorowych pól oznacza brak prognozowanego deszczu.';$('radarRange').value=offset;}

function initWind(){if(!windMap){windMap=L.map('windmap',{zoomControl:true}).setView([LAT,LON],7);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:'© OpenStreetMap'}).addTo(windMap);windMarker=L.circleMarker([LAT,LON],{radius:6,color:'#fff',weight:2,fillColor:'#45c5ff',fillOpacity:1}).addTo(windMap).bindPopup(currentCity);const pane=windMap.createPane('windFlowPane');pane.style.zIndex=450;pane.style.pointerEvents='none';windCanvas=document.createElement('canvas');windCanvas.style.cssText='position:absolute;inset:0;width:100%;height:100%;display:block;pointer-events:none';pane.appendChild(windCanvas);windCtx=windCanvas.getContext('2d');for(let i=0;i<90;i++)particles.push({x:Math.random(),y:Math.random()});window.addEventListener('resize',resizeWind);windMap.on('zoom move',resizeWind);resizeWind();animateWind()}windMap.invalidateSize();resizeWind()}
function resizeWind(){if(!windCanvas||!windMap)return;const size=windMap.getSize();const dpr=Math.min(window.devicePixelRatio||1,2);windCanvas.style.width=size.x+'px';windCanvas.style.height=size.y+'px';windCanvas.width=Math.max(1,Math.round(size.x*dpr));windCanvas.height=Math.max(1,Math.round(size.y*dpr));windCtx.setTransform(dpr,0,0,dpr,0,0)}
let activeDirection=240,activeSpeed=12;
function animateWind(){if(!windCtx||!windCanvas)return;const w=windMap.getSize().x,h=windMap.getSize().y;windCtx.clearRect(0,0,w,h);const flow=(activeDirection+180)%360;const rad=flow*Math.PI/180;const dx=Math.sin(rad),dy=-Math.cos(rad);const len=28+Math.min(activeSpeed,90)*.35;windCtx.lineCap='round';particles.forEach(p=>{const x=p.x*w,y=p.y*h;const ex=x+dx*len,ey=y+dy*len;windCtx.strokeStyle='rgba(12,45,62,.85)';windCtx.lineWidth=4;windCtx.beginPath();windCtx.moveTo(x-dx*8,y-dy*8);windCtx.lineTo(ex,ey);windCtx.stroke();windCtx.strokeStyle='rgba(35,220,197,.98)';windCtx.lineWidth=2.5;windCtx.beginPath();windCtx.moveTo(x-dx*8,y-dy*8);windCtx.lineTo(ex,ey);windCtx.stroke();const head=9;windCtx.fillStyle='#72ffe4';windCtx.strokeStyle='#073b43';windCtx.lineWidth=1;windCtx.beginPath();windCtx.moveTo(ex,ey);windCtx.lineTo(ex-dx*head-dy*head*.62,ey-dy*head+dx*head*.62);windCtx.lineTo(ex-dx*head+dy*head*.62,ey-dy*head-dx*head*.62);windCtx.closePath();windCtx.fill();windCtx.stroke();p.x+=dx*(.00035+activeSpeed*.000035);p.y+=dy*(.00035+activeSpeed*.000035);if(p.x<-.08||p.x>1.08||p.y<-.08||p.y>1.08){p.x=Math.random();p.y=Math.random()}});windAnim=requestAnimationFrame(animateWind)}
function updateWind(offset,base){if(!weather)return;const i=base+offset,h=weather.hourly;activeSpeed=h.wind_speed_10m[i]||0;activeDirection=h.wind_direction_10m[i]||0;$('wSpeed').textContent=Math.round(activeSpeed)+' km/h';$('wGust').textContent=Math.round(h.wind_gusts_10m[i]||0)+' km/h';$('wDir').textContent=direction(activeDirection);$('windTime').textContent=new Date(h.time[i]).toLocaleString('pl-PL',{weekday:'short',hour:'2-digit',minute:'2-digit'});$('windRange').value=offset}
$('windRange').addEventListener('input',e=>{if(!weather)return;let base=nextHourIndex(weather.hourly.time,weather.current.time);updateWind(Number(e.target.value),base)});
loadWeather(true);setTimeout(()=>{loadMoreFeatures();initRadar();initWind();loadLongRange()},700);setInterval(()=>{if(document.visibilityState==='visible')loadWeather(true)},5*60*1000);document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible'){loadWeather(true);setTimeout(()=>{if(radarMap)radarMap.invalidateSize();if(windMap){windMap.invalidateSize();resizeWind()}},400)}});
</script></body></html>
        """.trimIndent()
    }
}
