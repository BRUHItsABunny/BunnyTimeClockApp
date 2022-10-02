package com.bunnytechsolutions.bunnytimeclockapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.creative.ipfyandroid.Ipfy
import com.creative.ipfyandroid.IpfyClass
import timber.log.Timber
import java.net.URL
import java.net.URLEncoder


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var spSettings: SharedPreferences
    private lateinit var ipAddress: String

    // Prevent logging in and clocking in or out from an unknown IP
    private fun validateIPAddress(){
        if (!this::spSettings.isInitialized) {
            spSettings = PreferenceManager.getDefaultSharedPreferences(this)
        }
        if (!this::ipAddress.isInitialized) {
            ipAddress = spSettings.getString("ipAddress", "")!!
        }
        Ipfy.init(this, IpfyClass.IPv4)
        Ipfy.getInstance().getPublicIpObserver().observe(this) { ipData ->

            if (ipData.currentIpAddress == null) {
                // No net
                Toast.makeText(this, "No network available", Toast.LENGTH_LONG).show()
                initWebView(false)
            }
            if (ipAddress != "" && ipAddress != ipData.currentIpAddress) {
                // Bad net
                Toast.makeText(this, "You are not on the right network", Toast.LENGTH_LONG).show()
                initWebView(false)
            }

            if (ipAddress == "" || ipAddress == ipData.currentIpAddress) {
                initWebView(true)
            }
        }
    }

    private fun initWebView(validIP: Boolean){
        if (!this::webView.isInitialized) {
            webView = findViewById(R.id.wvID)
            // TODO: https://github.com/acsbendi/Android-Request-Inspector-WebView/issues/2
            webView.webViewClient = object : RequestInspectorWebViewClient(webView) {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript("document.documentElement.outerHTML") {
                        val html = it.replace("\\u003C", "<")
                        Timber.d(html)
                    }
                }
            }

            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.builtInZoomControls = true
            webView.settings.userAgentString = spSettings.getString("userAgent", "bunnyAgent")!!
        }

        val fullURL = spSettings.getString("url", "https://httpbin.org/get")!!
        val url = URL(fullURL)
        val cookieName = spSettings.getString("cookieName", "TCMAC")!!
        val cookieValue = URLEncoder.encode(spSettings.getString("cookieValue", "bunny")!!, "utf-8")
        val cookiePath = spSettings.getString("cookiePath", "/")!!
        CookieManager.getInstance().setCookie(url.protocol.toString() + "://" + url.host, "$cookieName=$cookieValue; path=$cookiePath")
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        if(validIP){
            webView.loadUrl(fullURL)
        } else {
            webView.loadUrl("https://httpbin.org/get")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())
        validateIPAddress()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itemSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.itemReload -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if(this::webView.isInitialized && webView.canGoBack()) { // Go back in the WebView instead of in the app
            return webView.goBack()
        }
        super.onBackPressed()
    }
}