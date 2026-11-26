package nl.koffie.wiehaaltkoffie

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.util.Base64
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
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
            val wantTop = bars.top
            val wantBottom = maxOf(bars.bottom, ime.bottom)
            // Houd alleen ruimte vrij als het venster ook echt ONDER de Android-balken doorloopt.
            // Sommige telefoons laten het venster boven de balk stoppen; dan zou het dubbel tellen.
            root.post {
                val loc = IntArray(2)
                root.getLocationOnScreen(loc)
                val runsBehindTop = loc[1] <= 0
                val runsBehindBottom = loc[1] + root.height >= realScreenHeight() - 2
                topSpace.layoutParams.height = if (runsBehindTop) wantTop else 0
                bottomSpace.layoutParams.height = if (runsBehindBottom) wantBottom else 0
                topSpace.requestLayout()
                bottomSpace.requestLayout()
            }
            insets
        }

        // De webapp meldt haar themakleur, zodat de balk onder het menu bij het thema past (Pro)
        web.addJavascriptInterface(KoffieBridge(
            onBars = { bg, bar ->
                runOnUiThread {
                    try {
                        root.setBackgroundColor(Color.parseColor(bg))
                        web.setBackgroundColor(Color.parseColor(bg))
                        bottomSpace.setBackgroundColor(Color.parseColor(bar))
                    } catch (_: Exception) {
                    }
                }
            },
            onFile = { name, b64, mime -> shareFile(name, b64, mime) }
        ), "KoffieApp")

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true // nodig om je naam, drankje en teams te onthouden
        // Haal bij elke start de nieuwste versie van de webapp op (geen oude opgeslagen pagina)
        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.clearCache(true)

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

    /** Bewaart het Excel-bestand tijdelijk en opent het deelmenu (Excel, Drive, mail, WhatsApp...). */
    private fun shareFile(name: String, b64: String, mime: String) {
        try {
            val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60)
            val dir = File(cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, safe)
            file.writeBytes(Base64.decode(b64, Base64.DEFAULT))
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runOnUiThread { startActivity(Intent.createChooser(send, safe)) }
        } catch (_: Exception) {
        }
    }

    private fun realScreenHeight(): Int =
        if (Build.VERSION.SDK_INT >= 30) {
            windowManager.maximumWindowMetrics.bounds.height()
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(m)
            m.heightPixels
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

/** Kleine brug tussen de webapp en de app: kleuren van de balken en het delen van een export. */
class KoffieBridge(
    private val onBars: (String, String) -> Unit,
    private val onFile: (String, String, String) -> Unit
) {
    @JavascriptInterface
    fun setBars(bg: String, bar: String) = onBars(bg, bar)

    @JavascriptInterface
    fun saveFile(name: String, base64: String, mime: String) {
        // Alleen Excel- en tekstbestanden toestaan
        if (mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || mime == "text/csv") {
            onFile(name, base64, mime)
        }
    }
}
