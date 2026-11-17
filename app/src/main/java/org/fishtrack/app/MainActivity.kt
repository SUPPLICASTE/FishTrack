package org.fishtrack.app

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
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
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null
    private val pickImage = 101
    private val requestLocation = 102
    private val requestNativeLocation = 106
    private val saveBackupRequest = 103
    private val pickLicenseRequest = 104
    private val capturePhotoRequest = 105
    private val pickGalleryPhotoRequest = 107
    private var pendingCameraTarget: String? = null
    private var pendingGalleryTarget: String? = null
    private var pendingBackupText: String? = null
    private var pendingNativeGps = false
    private var gpsListener: LocationListener? = null
    private val gpsHandler = Handler(Looper.getMainLooper())

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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == "file:///android_asset/index.html") {
                    // Restore missing WebView keys from the private snapshot before the app is used.
                    web.evaluateJavascript("""
                        (function() {
                          try {
                            var raw = FishTrackNative.getWebStorageBackup();
                            if (raw) {
                              var saved = JSON.parse(raw);
                              Object.keys(saved).forEach(function(key) {
                                if (localStorage.getItem(key) === null) localStorage.setItem(key, saved[key]);
                              });
                            }
                            var current = {};
                            for (var i = 0; i < localStorage.length; i++) {
                              var key = localStorage.key(i);
                              current[key] = localStorage.getItem(key);
                            }
                            FishTrackNative.saveWebStorage(JSON.stringify(current));
                          } catch (e) {}
                        })();
                    """.trimIndent(), null)
                }
            }

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
        fun getWebStorageBackup(): String {
            return try {
                val backup = File(filesDir, "fishtrack-webstorage-backup.json")
                if (backup.exists()) backup.readText(Charsets.UTF_8) else ""
            } catch (_: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun saveWebStorage(content: String) {
            // Keep a private recovery copy. Never replace a non-empty backup with empty data.
            try {
                if (content.isBlank() || content == "{}") return
                val backup = File(filesDir, "fishtrack-webstorage-backup.json")
                val temporary = File(filesDir, "fishtrack-webstorage-backup.tmp")
                FileOutputStream(temporary).use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                    stream.fd.sync()
                }
                if (backup.exists()) backup.delete()
                if (!temporary.renameTo(backup)) {
                    temporary.copyTo(backup, overwrite = true)
                    temporary.delete()
                }
            } catch (_: Exception) {
                // The normal WebView storage remains the primary store.
            }
        }

        @JavascriptInterface
        fun getLicenseBackup(): String {
            return try {
                val directory = File(filesDir, "licenses")
                val result = JSONObject()
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles()?.filter { it.isFile && it.name.matches(Regex("[A-Za-z0-9-]{1,50}\\.pdf")) }?.forEach { file ->
                        if (file.length() <= 20L * 1024L * 1024L) {
                            val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                            result.put(file.nameWithoutExtension, encoded)
                        }
                    }
                }
                result.toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        @JavascriptInterface
        fun restoreLicenseBackup(content: String) {
            try {
                val source = JSONObject(content)
                val directory = File(filesDir, "licenses")
                if (!directory.exists() && !directory.mkdirs()) return
                val totalLimit = 40L * 1024L * 1024L
                var totalBytes = 0L
                val keys = source.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    if (!id.matches(Regex("[A-Za-z0-9-]{1,50}"))) continue
                    val encoded = source.optString(id, "")
                    if (encoded.isBlank()) continue
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    if (bytes.size > 20 * 1024 * 1024 || totalBytes + bytes.size > totalLimit) continue
                    val destination = File(directory, "$id.pdf")
                    val temporary = File(directory, "$id.pdf.tmp")
                    FileOutputStream(temporary).use { stream ->
                        stream.write(bytes)
                        stream.fd.sync()
                    }
                    if (destination.exists()) destination.delete()
                    if (!temporary.renameTo(destination)) {
                        temporary.copyTo(destination, overwrite = true)
                        temporary.delete()
                    }
                    totalBytes += bytes.size
                }
            } catch (_: Exception) {
                // A malformed or oversized optional PDF backup must not block record recovery.
            }
        }

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
        fun capturePhoto(target: String) {
            runOnUiThread {
                if (target != "gear" && target != "catch" && !target.startsWith("gearEdit:") && !target.startsWith("catchEdit:") && !target.startsWith("spotEdit:") && !target.startsWith("sessionEdit:")) return@runOnUiThread
                try {
                    pendingCameraTarget = target
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, capturePhotoRequest)
                    } else {
                        pendingCameraTarget = null
                        Toast.makeText(this@MainActivity, "No se encuentra una aplicación de cámara", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    pendingCameraTarget = null
                    Toast.makeText(this@MainActivity, "No se pudo abrir la cámara", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun chooseGalleryPhoto(target: String) {
            runOnUiThread {
                pendingGalleryTarget = target
                try {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivityForResult(Intent.createChooser(intent, "Seleccionar fotografía"), pickGalleryPhotoRequest)
                } catch (_: Exception) {
                    pendingGalleryTarget = null
                    Toast.makeText(this@MainActivity, "No se pudo abrir la galería", Toast.LENGTH_LONG).show()
                }
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
        fun requestGpsLocation() {
            runOnUiThread {
                val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!fine && !coarse) {
                    pendingNativeGps = true
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), requestNativeLocation)
                } else {
                    obtainNativeGpsLocation()
                }
            }
        }

        @JavascriptInterface
        fun openUpdate(url: String) {
            runOnUiThread {
                try {
                    val parsed = Uri.parse(url)
                    if (parsed.scheme != "https" || parsed.host != "github.com") {
                        Toast.makeText(this@MainActivity, "Enlace de actualización no válido", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, parsed))
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "No se pudo abrir la descarga", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun obtainNativeGpsLocation() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            val hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) {
                web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('Concede permiso de ubicación a FishTrack en los ajustes del móvil.')", null)
                return
            }

            val providers = mutableListOf<String>()
            if (hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
            if (manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) providers.add(LocationManager.PASSIVE_PROVIDER)

            fun deliver(location: Location) {
                gpsHandler.removeCallbacksAndMessages(null)
                gpsListener?.let { try { manager.removeUpdates(it) } catch (_: Exception) {} }
                gpsListener = null
                web.post {
                    web.evaluateJavascript(
                        "window.onFishTrackGpsLocation && window.onFishTrackGpsLocation(${location.latitude},${location.longitude})",
                        null
                    )
                }
            }

            if (providers.isEmpty()) {
                web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('Activa la ubicación del móvil y vuelve a intentarlo.')", null)
                return
            }

            val lastKnown = providers.mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null } catch (_: Exception) { null }
            }.maxByOrNull { it.time }
            if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 120_000L) {
                deliver(lastKnown)
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    deliver(location)
                }
                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            gpsListener = listener

            var requested = false
            for (provider in providers) {
                try {
                    manager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
                    requested = true
                } catch (_: SecurityException) {
                } catch (_: IllegalArgumentException) {
                } catch (_: Exception) {
                }
            }
            if (!requested) {
                gpsListener = null
                web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('No hay un proveedor de ubicación disponible. Activa la ubicación del móvil.')", null)
                return
            }

            gpsHandler.postDelayed({
                val fallback = providers.mapNotNull { provider ->
                    try { manager.getLastKnownLocation(provider) } catch (_: Exception) { null }
                }.maxByOrNull { it.time }
                if (fallback != null) {
                    deliver(fallback)
                } else if (gpsListener === listener) {
                    try { manager.removeUpdates(listener) } catch (_: Exception) {}
                    gpsListener = null
                    web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('No se pudo fijar la ubicación. Activa la ubicación, concede permiso preciso si es posible y vuelve a intentarlo.')", null)
                }
            }, 30_000L)
        } catch (_: SecurityException) {
            web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('Concede permiso de ubicación a FishTrack en los ajustes del móvil.')", null)
        } catch (_: Exception) {
            web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('Error al consultar el GPS del móvil. Inténtalo de nuevo.')", null)
        }
    }

    @Deprecated("Deprecated Android callback retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == capturePhotoRequest) {
            val target = pendingCameraTarget
            pendingCameraTarget = null
            val bitmap = if (resultCode == RESULT_OK) data?.extras?.get("data") as? Bitmap else null
            if (target != null && bitmap != null) {
                try {
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$encoded"
                    web.evaluateJavascript("window.onCameraPhoto && window.onCameraPhoto(${JSONObject.quote(target)}, ${JSONObject.quote(dataUrl)})", null)
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo procesar la foto", Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == pickGalleryPhotoRequest) {
            val target = pendingGalleryTarget
            pendingGalleryTarget = null
            val uri = if (resultCode == RESULT_OK) data?.data else null
            if (target != null && uri != null) {
                try {
                    val bitmap = contentResolver.openInputStream(uri)?.use { input ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(uri)?.use { it2 -> BitmapFactory.decodeStream(it2, null, bounds) }
                        var sample = 1
                        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                        val options = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeStream(input, null, options)
                    } ?: throw IllegalStateException("No se pudo leer la fotografía")
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)
                    bitmap.recycle()
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$encoded"
                    web.evaluateJavascript("window.onCameraPhoto && window.onCameraPhoto(${JSONObject.quote(target)}, ${JSONObject.quote(dataUrl)})", null)
                } catch (_: Exception) {
                    Toast.makeText(this, "No se pudo cargar la fotografía", Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == pickLicenseRequest) {
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
        } else if (requestCode == requestNativeLocation) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            pendingNativeGps = false
            if (granted) obtainNativeGpsLocation()
            else web.evaluateJavascript("window.onFishTrackGpsError && window.onFishTrackGpsError('Permiso de ubicación denegado. Actívalo en los ajustes de FishTrack.')", null)
        }
    }

    override fun onPause() {
        if (::web.isInitialized) {
            // Snapshot local records before Android backgrounds the activity for an app update.
            web.evaluateJavascript("""
                (function() {
                  try {
                    var current = {};
                    for (var i = 0; i < localStorage.length; i++) {
                      var key = localStorage.key(i);
                      current[key] = localStorage.getItem(key);
                    }
                    if (Object.keys(current).length) FishTrackNative.saveWebStorage(JSON.stringify(current));
                  } catch (e) {}
                })();
            """.trimIndent(), null)
        }
        super.onPause()
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
