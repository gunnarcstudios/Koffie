package nl.koffie.wiehaaltkoffie

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * De Android-app is een omhulsel om de webapp (docs/index.html op GitHub Pages).
 * Android, iPhone en browser gebruiken dus exact dezelfde app en dezelfde rondes.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.setBackgroundColor(0xFFF8F1E9.toInt())
        setContentView(web)

        ViewCompat.setOnApplyWindowInsetsListener(web) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true // nodig om je naam/ID te onthouden

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == BuildConfig.WEB_HOST) return false
                // Andere links (bijv. WhatsApp) openen in de juiste app
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: Exception) {
                }
                return true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadDataWithBaseURL(
                        null,
                        "<html><body style='font-family:sans-serif;background:#F8F1E9;color:#3B2416;" +
                            "text-align:center;padding:48px 24px'><h2>Geen internet</h2>" +
                            "<p>Controleer je verbinding en probeer het opnieuw.</p>" +
                            "<p><a href='${BuildConfig.WEB_URL}' style='color:#6F4E37;font-size:18px'>" +
                            "Opnieuw proberen</a></p></body></html>",
                        "text/html", "utf-8", null
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl(targetUrl(intent))
    }

    /** Opent een gedeelde ronde-link direct in de app. */
    private fun targetUrl(intent: Intent?): String {
        val data: Uri? = intent?.data
        return if (data != null && data.host == BuildConfig.WEB_HOST) data.toString() else BuildConfig.WEB_URL
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        web.loadUrl(targetUrl(intent))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }
}
