package cl.aldemar.musselapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.toArgb
import cl.aldemar.musselapp.R
import cl.aldemar.musselapp.data.model.EstadoMuestra
import cl.aldemar.musselapp.data.model.Muestra
import cl.aldemar.musselapp.ui.theme.Acento
import cl.aldemar.musselapp.ui.theme.AcentoSuave
import cl.aldemar.musselapp.ui.theme.AcentoTexto
import cl.aldemar.musselapp.ui.theme.AlertaFondo
import cl.aldemar.musselapp.ui.theme.AlertaTexto
import cl.aldemar.musselapp.ui.theme.BordeTarjeta
import cl.aldemar.musselapp.ui.theme.Linea
import cl.aldemar.musselapp.ui.theme.Principal
import cl.aldemar.musselapp.ui.theme.SuperficieTenue
import cl.aldemar.musselapp.ui.theme.Texto
import cl.aldemar.musselapp.ui.theme.TextoSecundario
import cl.aldemar.musselapp.ui.theme.TextoSobreOscuro
import cl.aldemar.musselapp.ui.theme.colores
import java.io.File
import java.io.FileOutputStream

/**
 * Ficha técnica de una muestra en PDF (A4) y las acciones del Detalle:
 * descargar, imprimir y compartir. El contenido viene de [FichaMuestra].
 *
 * [generar] lee la foto y escribe un archivo: llamarlo fuera del hilo principal.
 */
object FichaPdf {

    // Hoja A4 en puntos (1/72 de pulgada)
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 36f

    /** Authority del FileProvider declarado en el AndroidManifest. */
    private fun authority(context: Context) = "${context.packageName}.archivos"

    /** Crea el PDF en la caché de la app (`cacheDir/fichas/`) y devuelve el archivo. */
    fun generar(context: Context, muestra: Muestra): File {
        val documento = PdfDocument()
        try {
            val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_musselapp)
            dibujar(pagina.canvas, muestra, logo)
            documento.finishPage(pagina)

            val carpeta = File(context.cacheDir, "fichas").apply { mkdirs() }
            val archivo = File(carpeta, FichaMuestra.nombreArchivo(muestra))
            FileOutputStream(archivo).use { documento.writeTo(it) }
            return archivo
        } finally {
            documento.close()
        }
    }

    /** Copia el PDF al destino que eligió el usuario (selector "Guardar como" de Android). */
    fun guardarEn(context: Context, pdf: File, destino: Uri) {
        val salida = context.contentResolver.openOutputStream(destino)
            ?: error("No se pudo abrir el destino")
        salida.use { s -> pdf.inputStream().use { it.copyTo(s) } }
    }

    /** Abre el menú de compartir de Android con el PDF adjunto y un resumen en texto. */
    fun compartir(context: Context, pdf: File, muestra: Muestra) {
        val uri = FileProvider.getUriForFile(context, authority(context), pdf)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, FichaMuestra.titulo(muestra))
            putExtra(Intent.EXTRA_TEXT, FichaMuestra.textoCompartir(muestra))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(envio, "Compartir ficha ${muestra.codigo}"))
    }

    /** Envía el PDF al servicio de impresión de Android (también permite "Guardar como PDF"). */
    fun imprimir(context: Context, pdf: File, muestra: Muestra) {
        val impresion = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        impresion.print(
            FichaMuestra.titulo(muestra),
            AdaptadorPdf(pdf),
            PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
        )
    }

    // ── Dibujo de la página ──────────────────────────────────────────────
    // Colores del tema de la app (ui/theme), para que la ficha se vea como la app

    private val cTexto = Texto.toArgb()
    private val cTextoSecundario = TextoSecundario.toArgb()
    private val cSobreOscuro = TextoSobreOscuro.toArgb()
    private val cPrincipal = Principal.toArgb()
    private val cAcento = Acento.toArgb()
    private val cAcentoSuave = AcentoSuave.toArgb()
    private val cAcentoTexto = AcentoTexto.toArgb()
    private val cTenue = SuperficieTenue.toArgb()
    private val cLinea = Linea.toArgb()
    private val cBorde = BordeTarjeta.toArgb()
    private val cAlertaFondo = AlertaFondo.toArgb()
    private val cAlertaTexto = AlertaTexto.toArgb()

    private val semiNegrita: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun pintura(color: Int, tamano: Float, tipo: Typeface = Typeface.DEFAULT, espaciado: Float = 0f) =
        TextPaint().apply {
            this.color = color
            textSize = tamano
            typeface = tipo
            letterSpacing = espaciado
            isAntiAlias = true
        }

    private fun relleno(color: Int) = Paint().apply { this.color = color; isAntiAlias = true }

    private fun trazo(color: Int, ancho: Float = 0.8f) = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = ancho
        isAntiAlias = true
    }

    private fun dibujar(canvas: Canvas, muestra: Muestra, logo: Bitmap?) {
        val anchoUtil = ANCHO - 2 * MARGEN
        var y = dibujarEncabezado(canvas, muestra, logo)

        // Foto a la izquierda y mediciones destacadas a la derecha
        val altoBloque = 150f
        val anchoFoto = 200f
        dibujarFoto(canvas, muestra, RectF(MARGEN, y, MARGEN + anchoFoto, y + altoBloque))
        dibujarMetricas(canvas, muestra, MARGEN + anchoFoto + 12f, y, anchoUtil - anchoFoto - 12f, altoBloque)
        y += altoBloque + 12f

        y = dibujarAlertas(canvas, muestra, y, anchoUtil) + 12f

        // Secciones en dos columnas: identificación y trazabilidad a la izquierda, condiciones y revisión a la derecha
        val secciones = FichaMuestra.secciones(muestra)
        val anchoColumna = (anchoUtil - 12f) / 2
        var yIzq = y
        var yDer = y
        listOf(secciones[0], secciones[3]).forEach { yIzq = dibujarSeccion(canvas, it, MARGEN, yIzq, anchoColumna) + 10f }
        listOf(secciones[1], secciones[2]).forEach { yDer = dibujarSeccion(canvas, it, MARGEN + anchoColumna + 12f, yDer, anchoColumna) + 10f }
        y = maxOf(yIzq, yDer)

        // Observación del operario, a todo el ancho
        dibujarSeccion(
            canvas,
            SeccionFicha("Observación de terreno", listOf("" to muestra.observacion.ifBlank { "Sin observaciones." })),
            MARGEN, y, anchoUtil,
        )

        dibujarPie(canvas)
    }

    /** Banda azul oscura con el logo, el código de la muestra y su estado. Devuelve dónde sigue la página. */
    private fun dibujarEncabezado(canvas: Canvas, muestra: Muestra, logo: Bitmap?): Float {
        val alto = 104f
        canvas.drawRect(0f, 0f, ANCHO.toFloat(), alto, relleno(cPrincipal))
        canvas.drawRect(0f, alto, ANCHO.toFloat(), alto + 3f, relleno(cAcento))

        var x = MARGEN
        if (logo != null) {
            // Logo en blanco sobre un círculo tenue
            canvas.drawCircle(MARGEN + 28f, 52f, 28f, relleno(0x1AFFFFFF))
            val blanco = Paint().apply {
                colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
                isFilterBitmap = true
                isAntiAlias = true
            }
            canvas.drawBitmap(logo, null, RectF(MARGEN + 6f, 30f, MARGEN + 50f, 74f), blanco)
            x += 70f
        }
        canvas.drawText("FICHA TÉCNICA DE MUESTRA", x, 38f, pintura(cSobreOscuro, 8.5f, semiNegrita, 0.12f))
        canvas.drawText(muestra.codigo, x, 66f, pintura(android.graphics.Color.WHITE, 24f, Typeface.DEFAULT_BOLD))
        canvas.drawText(
            "${FichaMuestra.nombreCentro(muestra.centroId)} · Tren ${muestra.tren} · ${muestra.codigoLinea}",
            x, 86f, pintura(cSobreOscuro, 9.5f),
        )

        // Estado como chip, con los mismos colores que en la app
        val colores = muestra.estado.colores
        val textoEstado = muestra.estado.etiqueta.uppercase()
        val pEstado = pintura(colores.texto.toArgb(), 8.5f, Typeface.DEFAULT_BOLD, 0.08f)
        val anchoChip = pEstado.measureText(textoEstado) + 22f
        val chip = RectF(ANCHO - MARGEN - anchoChip, 30f, ANCHO - MARGEN, 50f)
        canvas.drawRoundRect(chip, 10f, 10f, relleno(colores.fondo.toArgb()))
        canvas.drawText(textoEstado, chip.left + 11f, chip.top + 13.5f, pEstado)
        val pFecha = pintura(cSobreOscuro, 8.5f)
        val registrada = "Registrada ${FichaMuestra.fecha(muestra.registradaEn)}"
        canvas.drawText(registrada, ANCHO - MARGEN - pFecha.measureText(registrada), 66f, pFecha)
        return alto + 3f + 18f
    }

    /** Foto recortada al recuadro (sin deformarla) o un recuadro vacío si no hay foto. */
    private fun dibujarFoto(canvas: Canvas, muestra: Muestra, caja: RectF) {
        val foto = cargarFoto(muestra.fotoRuta, caja.width().toInt() * 2)
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(caja, 8f, 8f, Path.Direction.CW) })
        if (foto != null) {
            val escala = maxOf(caja.width() / foto.width, caja.height() / foto.height)
            val anchoFuente = caja.width() / escala
            val altoFuente = caja.height() / escala
            val origen = Rect(
                ((foto.width - anchoFuente) / 2).toInt(), ((foto.height - altoFuente) / 2).toInt(),
                ((foto.width + anchoFuente) / 2).toInt(), ((foto.height + altoFuente) / 2).toInt(),
            )
            canvas.drawBitmap(foto, origen, caja, Paint().apply { isFilterBitmap = true })
        } else {
            canvas.drawRect(caja, relleno(cTenue))
            val p = pintura(cTextoSecundario, 9.5f)
            val texto = "Sin fotografía"
            canvas.drawText(texto, caja.centerX() - p.measureText(texto) / 2, caja.centerY() + 3f, p)
        }
        canvas.restore()
        canvas.drawRoundRect(caja, 8f, 8f, trazo(cBorde))
    }

    /** Cuatro recuadros con las mediciones y el desvío de densidad debajo. */
    private fun dibujarMetricas(canvas: Canvas, muestra: Muestra, x: Float, y: Float, ancho: Float, alto: Float) {
        val separacion = 8f
        val altoDesvio = 26f
        val anchoCelda = (ancho - separacion) / 2
        val altoCelda = (alto - altoDesvio - 2 * separacion) / 2
        val pEtiqueta = pintura(cTextoSecundario, 7.5f, semiNegrita, 0.1f)
        val pValor = pintura(cTexto, 22f, Typeface.DEFAULT_BOLD)
        val pUnidad = pintura(cTextoSecundario, 9f)
        FichaMuestra.metricas(muestra).forEachIndexed { i, m ->
            val cx = x + (i % 2) * (anchoCelda + separacion)
            val cy = y + (i / 2) * (altoCelda + separacion)
            val celda = RectF(cx, cy, cx + anchoCelda, cy + altoCelda)
            canvas.drawRoundRect(celda, 8f, 8f, relleno(cTenue))
            canvas.drawText(m.etiqueta.uppercase(), cx + 10f, cy + 16f, pEtiqueta)
            canvas.drawText(m.valor, cx + 10f, cy + altoCelda - 12f, pValor)
            canvas.drawText(m.unidad, cx + 14f + pValor.measureText(m.valor), cy + altoCelda - 12f, pUnidad)
        }
        // Desvío de densidad respecto al estándar: rojo si está fuera de ±20 %
        val fuera = kotlin.math.abs(muestra.desvioDensidadPct) > 20
        val franja = RectF(x, y + alto - altoDesvio, x + ancho, y + alto)
        canvas.drawRoundRect(franja, 8f, 8f, relleno(if (fuera) cAlertaFondo else cAcentoSuave))
        canvas.drawText(
            "Desvío de densidad: ${FichaMuestra.desvio(muestra)}",
            franja.left + 10f, franja.top + 16.5f,
            pintura(if (fuera) cAlertaTexto else cAcentoTexto, 8.5f, semiNegrita),
        )
    }

    /** Franja con las alertas automáticas como chips, o un aviso de que no hay. Devuelve su borde inferior. */
    private fun dibujarAlertas(canvas: Canvas, muestra: Muestra, y: Float, ancho: Float): Float {
        val alertas = FichaMuestra.listaAlertas(muestra)
        val alto = 30f
        val caja = RectF(MARGEN, y, MARGEN + ancho, y + alto)
        canvas.drawRoundRect(caja, 8f, 8f, relleno(if (alertas.isEmpty()) cAcentoSuave else 0xFFF8FAFC.toInt()))
        canvas.drawRoundRect(caja, 8f, 8f, trazo(if (alertas.isEmpty()) cAcentoSuave else cLinea))
        val pTitulo = pintura(if (alertas.isEmpty()) cAcentoTexto else cTexto, 8.5f, Typeface.DEFAULT_BOLD, 0.06f)
        val titulo = if (alertas.isEmpty()) "SIN ALERTAS AL REGISTRAR" else "ALERTAS AL REGISTRAR"
        canvas.drawText(titulo, caja.left + 12f, caja.top + 18.5f, pTitulo)
        var x = caja.left + 24f + pTitulo.measureText(titulo)
        alertas.forEach { a ->
            val fondo = if (a.critica) cAlertaFondo else EstadoMuestra.PENDIENTE.colores.fondo.toArgb()
            val tinta = if (a.critica) cAlertaTexto else EstadoMuestra.PENDIENTE.colores.texto.toArgb()
            val p = pintura(tinta, 8.5f, semiNegrita)
            val anchoChip = p.measureText(a.etiqueta) + 18f
            if (x + anchoChip > caja.right - 8f) return@forEach // sin espacio: se omite (la lista sale en el texto al compartir)
            val chip = RectF(x, caja.top + 7f, x + anchoChip, caja.bottom - 7f)
            canvas.drawRoundRect(chip, 8f, 8f, relleno(fondo))
            canvas.drawText(a.etiqueta, chip.left + 9f, chip.top + 11.5f, p)
            x += anchoChip + 6f
        }
        return caja.bottom
    }

    /**
     * Tarjeta con título de sección y filas "etiqueta sobre valor". Una fila sin etiqueta
     * muestra solo el valor (texto libre). Devuelve el borde inferior de la tarjeta.
     */
    private fun dibujarSeccion(canvas: Canvas, seccion: SeccionFicha, x: Float, y: Float, ancho: Float): Float {
        val relleno = 12f
        val anchoTexto = (ancho - 2 * relleno).toInt()
        val pEtiqueta = pintura(cTextoSecundario, 7.5f, semiNegrita, 0.06f)
        val pValor = pintura(cTexto, 9.5f, semiNegrita)
        val bloques = seccion.filas.map { (etiqueta, valor) -> etiqueta to bloqueTexto(valor, pValor, anchoTexto) }
        val altoTitulo = 26f
        val altoFilas = bloques.sumOf { (etiqueta, b) -> (b.height + (if (etiqueta.isEmpty()) 0 else 11) + 8).toDouble() }.toFloat()
        val caja = RectF(x, y, x + ancho, y + altoTitulo + altoFilas + 4f)

        canvas.drawRoundRect(caja, 8f, 8f, relleno(android.graphics.Color.WHITE))
        canvas.drawRoundRect(caja, 8f, 8f, trazo(cBorde))
        // Título: barra de acento y texto en mayúsculas
        canvas.drawRoundRect(RectF(x + relleno, y + 9f, x + relleno + 3f, y + 20f), 1.5f, 1.5f, relleno(cAcento))
        canvas.drawText(seccion.titulo.uppercase(), x + relleno + 9f, y + 18.5f, pintura(cAcentoTexto, 8.5f, Typeface.DEFAULT_BOLD, 0.08f))
        canvas.drawLine(x + relleno, y + altoTitulo, x + ancho - relleno, y + altoTitulo, trazo(cLinea))

        var cy = y + altoTitulo + 7f
        bloques.forEachIndexed { i, (etiqueta, bloque) ->
            if (etiqueta.isNotEmpty()) {
                canvas.drawText(etiqueta.uppercase(), x + relleno, cy + 7f, pEtiqueta)
                cy += 11f
            }
            canvas.save()
            canvas.translate(x + relleno, cy)
            bloque.draw(canvas)
            canvas.restore()
            cy += bloque.height + 8f
            if (i < bloques.lastIndex) canvas.drawLine(x + relleno, cy - 4f, x + ancho - relleno, cy - 4f, trazo(cLinea, 0.5f))
        }
        return caja.bottom
    }

    private fun dibujarPie(canvas: Canvas) {
        val y = ALTO - 30f
        canvas.drawLine(MARGEN, y - 12f, ANCHO - MARGEN, y - 12f, trazo(cLinea))
        val p = pintura(cTextoSecundario, 7.5f)
        canvas.drawText(FichaMuestra.PIE, MARGEN, y, p)
        val generada = "Generada el ${FichaMuestra.fecha(System.currentTimeMillis())}"
        canvas.drawText(generada, ANCHO - MARGEN - p.measureText(generada), y, p)
    }

    private fun bloqueTexto(texto: String, pintura: TextPaint, ancho: Int): StaticLayout =
        StaticLayout.Builder.obtain(texto, 0, texto.length, pintura, ancho)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    /** Lee la foto reducida al ancho de la página y la gira según su EXIF. */
    private fun cargarFoto(ruta: String?, anchoMax: Int): Bitmap? {
        if (ruta == null || !File(ruta).exists()) return null
        return try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(ruta, limites)
            var reduccion = 1
            while (limites.outWidth / (reduccion * 2) >= anchoMax * 2) reduccion *= 2
            val bitmap = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = reduccion })
                ?: return null
            val grados = when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (grados == 0f) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(grados) }, true)
        } catch (e: Exception) {
            null // Sin foto la ficha se genera igual
        }
    }

    /** Entrega un PDF ya generado al servicio de impresión. */
    private class AdaptadorPdf(private val pdf: File) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: PrintDocumentAdapter.LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(pdf.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: PrintDocumentAdapter.WriteResultCallback,
        ) {
            try {
                pdf.inputStream().use { entrada ->
                    FileOutputStream(destination.fileDescriptor).use { entrada.copyTo(it) }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}
