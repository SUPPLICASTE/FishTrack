package org.fishtrack.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class PdfViewerActivity : Activity() {
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var pageIndex = 0
    private lateinit var pageLabel: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var pageImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("license_file").orEmpty()
        if (!id.matches(Regex("[A-Za-z0-9-]{1,50}"))) {
            finish()
            return
        }
        try {
            val file = File(File(filesDir, "licenses"), "$id.pdf")
            if (!file.exists()) throw IllegalStateException()
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor!!)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.rgb(243, 247, 246))
            }
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            previousButton = Button(this).apply { text = "Anterior"; setOnClickListener { showPage(pageIndex - 1) } }
            pageLabel = TextView(this).apply {
                textSize = 14f
                setTextColor(Color.rgb(22, 52, 58))
                gravity = Gravity.CENTER
            }
            nextButton = Button(this).apply { text = "Siguiente"; setOnClickListener { showPage(pageIndex + 1) } }
            controls.addView(previousButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            controls.addView(pageLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            controls.addView(nextButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(controls)
            val scroll = ScrollView(this)
            pageImage = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.WHITE)
            }
            scroll.addView(pageImage, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            setContentView(root)
            showPage(0)
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showPage(index: Int) {
        val pdf = renderer ?: return
        if (index !in 0 until pdf.pageCount) return
        pageIndex = index
        val page = pdf.openPage(index)
        try {
            val width = (resources.displayMetrics.widthPixels - 24).coerceAtLeast(600)
            val height = (width * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pageImage.setImageBitmap(bitmap)
        } finally {
            page.close()
        }
        pageLabel.text = "Página ${index + 1} de ${pdf.pageCount}"
        previousButton.isEnabled = index > 0
        nextButton.isEnabled = index < pdf.pageCount - 1
    }

    override fun onDestroy() {
        renderer?.close()
        descriptor?.close()
        super.onDestroy()
    }
}
