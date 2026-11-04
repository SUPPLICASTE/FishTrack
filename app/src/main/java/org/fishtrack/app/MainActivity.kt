package org.fishtrack.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.Toast
import java.io.OutputStreamWriter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null
    private val pickImage = 101
    private val requestLocation = 102
    private val saveBackupRequest = 103
    private val pickLicenseRequest = 104
    private var pendingBackupText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.setGeolocationEnabled(true)
        web.addJavascriptInterface(NativeBridge(), "FishTrackNative")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (!request.isForMainFrame || url.startsWith("file:///android_asset/")) return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } catch (_: Exception) {
                    true
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                uploadCallback?.onReceiveValue(null)
                uploadCallback = callback
                val accepts = params?.acceptTypes?.filter { it.isNotBlank() } ?: emptyList()
                val wantsJson = accepts.any { it.contains("json", ignoreCase = true) || it.contains(".json", ignoreCase = true) }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = if (wantsJson) "application/json" else "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(Intent.createChooser(intent, if (wantsJson) "Seleccionar copia FishTrack" else "Seleccionar fotografía"), pickImage)
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fine || coarse) callback?.invoke(origin, true, false)
                else {
                    geoOrigin = origin
                    geoCallback = callback
                    this@MainActivity.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), requestLocation)
                }
            }
        }
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun shareText(title: String, message: String) {
            runOnUiThread {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                startActivity(Intent.createChooser(send, title))
            }
        }
        @JavascriptInterface
        fun saveBackup(content: String, filename: String) {
            runOnUiThread {
                pendingBackupText = content
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, filename)
                }
                startActivityForResult(intent, saveBackupRequest)
            }
        }

        @JavascriptInterface
        fun chooseLicense() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, pickLicenseRequest)
            }
        }

        @JavascriptInterface
        fun openLicense(id: String) {
            runOnUiThread {
                if (id.matches(Regex("[A-Za-z0-9-]{1,50}"))) {
                    val file = File(File(filesDir, "licenses"), "$id.pdf")
                    if (file.exists()) {
                        startActivity(Intent(this@MainActivity, PdfViewerActivity::class.java).putExtra("license_file", id))
                    } else {
                        Toast.makeText(this@MainActivity, "No se encuentra el PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        @JavascriptInterface
        fun deleteLicense(id: String) {
            runOnUiThread {
                if (id.matches(Regex("[A-Za-z0-9-]{1,50}"))) {
                    File(File(filesDir, "licenses"), "$id.pdf").delete()
                }
            }
        }

        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
    }

    @Deprecated("Deprecated Android callback retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickLicenseRequest) {
            val uri = if (resultCode == RESULT_OK) data?.data else null
            if (uri != null) {
                try {
                    val directory = File(filesDir, "licenses")
                    if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("No se pudo crear carpeta")
                    var displayName = "licencia.pdf"
                    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) displayName = cursor.getString(0) ?: displayName
                    }
                    if (!displayName.lowercase().endsWith(".pdf")) displayName += ".pdf"
                    val id = UUID.randomUUID().toString()
                    val destination = File(directory, "$id.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destination).use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("No se pudo leer el PDF")
                    web.evaluateJavascript(
                        "window.onLicensePicked && window.onLicensePicked(${JSONObject.quote(id)}, ${JSONObject.quote(displayName)})",
                        null
                    )
                    Toast.makeText(this, "Licencia guardada", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == pickImage) {
            val result = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
            uploadCallback?.onReceiveValue(result)
            uploadCallback = null
        } else if (requestCode == saveBackupRequest) {
            val uri = if (resultCode == RESULT_OK) data?.data else null
            val content = pendingBackupText
            pendingBackupText = null
            if (uri != null && content != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
                    }
                    Toast.makeText(this, "Copia FishTrack guardada", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo guardar la copia", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestLocation) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            geoCallback?.invoke(geoOrigin, granted, false)
            geoCallback = null
            geoOrigin = null
        }
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
