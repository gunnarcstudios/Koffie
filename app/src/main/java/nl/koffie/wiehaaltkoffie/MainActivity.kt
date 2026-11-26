package nl.koffie.wiehaaltkoffie

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * De Android-app is een omhulsel om de webapp (docs/index.html op GitHub Pages).
 * Android, iPhone en browser gebruiken dus exact dezelfde app en dezelfde teams.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Opbouw: [ruimte voor statusbalk] [webapp] [ruimte voor onderste Android-balk]
        // Zo komt de webapp (en haar menu) altijd BOVEN de onderste Android-knoppen.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF8F1E9.toInt())
        }
        val topSpace = View(this)
        val bottomSpace = View(this).apply { setBackgroundColor(0xFFEFE2D2.toInt()) }
        web = WebView(this).apply { setBackgroundColor(0xFFF8F1E9.toInt()) }

        root.addView(topSpace, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomSpace, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            topSpace.layoutParams.height = bars.top
            bottomSpace.layoutParams.height = maxOf(bars.bottom, ime.bottom)
            topSpace.requestLayout()
            bottomSpace.requestLayout()
            insets
        }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true // nodig om je naam, drankje en teams te onthouden

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

    /** Opent een gedeelde team-link direct in de app. */
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
