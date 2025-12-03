package com.example.detector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfGenerator {

    fun generateHistoryPdf(
        context: Context,
        historico: List<HistoricalData>,
        alertas: List<AlertData>
    ): File {
        // Carpeta destino (app-specific, no requiere permiso)
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reportes")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "historial_gases.pdf")

        val pdf = PdfDocument()

        // Tamaño página en px (A4 aprox). Puedes ajustar si quieres más resolución.
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36 // px

        // Pinceles
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        val linePaint = Paint().apply { strokeWidth = 1f }

        // Columnas (ancho total = pageWidth - 2*margin)
        val contentWidth = pageWidth - margin * 2
        val colFecha = (contentWidth * 0.35f)
        val colMq2   = (contentWidth * 0.18f)
        val colMq7   = (contentWidth * 0.18f)
        val colUser  = (contentWidth * 0.29f)

        val rowHeight = 18 // altura por fila de texto
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Función para nueva página
        var pageNumber = 1
        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page = pdf.startPage(pageInfo)
            return page to page.canvas
        }

        var (page, canvas) = newPage()
        var y = margin + 10

        // Título
        canvas.drawText("Reporte Histórico de Lecturas de Gases", margin.toFloat(), y.toFloat(), titlePaint)
        y += 24

        // Encabezado tabla
        canvas.drawText("Fecha", margin.toFloat(), y.toFloat(), headerPaint)
        canvas.drawText("MQ-2 (ppm)", (margin + colFecha).toFloat(), y.toFloat(), headerPaint)
        canvas.drawText("MQ-7 (ppm)", (margin + colFecha + colMq2).toFloat(), y.toFloat(), headerPaint)
        canvas.drawText("Usuario", (margin + colFecha + colMq2 + colMq7).toFloat(), y.toFloat(), headerPaint)
        y += 6
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 12

        // Filas
        fun maybeNewPage() {
            if (y > pageHeight - margin - rowHeight * 4) {
                pdf.finishPage(page)
                val np = newPage()
                page = np.first
                canvas = np.second
                y = margin + 10
            }
        }

        historico.forEach {
            maybeNewPage()
            val fecha = it.fecha.take(30) // por si es muy largo
            canvas.drawText(fecha, margin.toFloat(), y.toFloat(), textPaint)
            canvas.drawText(it.mq2.toString(), (margin + colFecha).toFloat(), y.toFloat(), textPaint)
            canvas.drawText(it.mq7.toString(), (margin + colFecha + colMq2).toFloat(), y.toFloat(), textPaint)
            canvas.drawText(it.usuario, (margin + colFecha + colMq2 + colMq7).toFloat(), y.toFloat(), textPaint)
            y += rowHeight
        }

        // Separador y sección de alertas
        y += 8
        maybeNewPage()
        canvas.drawText("Alertas Detectadas", margin.toFloat(), y.toFloat(), headerPaint)
        y += 6
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 12

        if (alertas.isEmpty()) {
            maybeNewPage()
            canvas.drawText("No se detectaron alertas en este periodo.", margin.toFloat(), y.toFloat(), textPaint)
            y += rowHeight
        } else {
            alertas.forEach { a ->
                maybeNewPage()
                val linea = "⚠ ${a.fecha}: ${a.tipo} superó el umbral (valor ${a.valor})"
                canvas.drawText(linea, margin.toFloat(), y.toFloat(), textPaint)
                y += rowHeight
            }
        }

        // Cerrar página
        pdf.finishPage(page)

        // Escribir archivo
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }
}

// Modelos de datos (igual que antes)
data class HistoricalData(
    val fecha: String,
    val mq2: Double,
    val mq7: Double,
    val usuario: String
)

data class AlertData(
    val fecha: String,
    val tipo: String,
    val valor: Double
)

