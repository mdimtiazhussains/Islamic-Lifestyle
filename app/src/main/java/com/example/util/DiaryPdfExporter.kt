package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.DiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Configuration options for generating the PDF export.
 */
data class PdfExportOptions(
    val documentTitle: String = "My Daily Reflections",
    val authorName: String = "Imtiaz",
    val subtitle: String = "Exported Journal & Mindset Diary",
    val includeGratitude: Boolean = true,
    val includeMoodAndEnergy: Boolean = true,
    val includeTags: Boolean = true,
    val includeSummaryStats: Boolean = true
)

/**
 * Result returned after successfully creating the PDF.
 */
data class PdfExportResult(
    val file: File,
    val uri: Uri,
    val fileName: String,
    val fileSizeFormatted: String,
    val pageCount: Int,
    val entryCount: Int,
    val destinationPath: String
)

/**
 * DiaryPdfExporter
 *
 * Renders saved diary entries into a clean, multi-page PDF document
 * using Android's native PdfDocument engine.
 * Supports download to device storage, viewing, and sharing.
 */
class DiaryPdfExporter(private val context: Context) {

    // A4 dimensions at 72 dpi (points)
    private val pageWidth = 595
    private val pageHeight = 842

    private val marginLeft = 40f
    private val marginRight = 555f
    private val marginTop = 45f
    private val marginBottom = 795f
    private val contentWidth = marginRight - marginLeft // 515 points

    /**
     * Exports the provided list of diary entries to a PDF file.
     */
    suspend fun exportToPdf(
        entries: List<DiaryEntry>,
        options: PdfExportOptions = PdfExportOptions()
    ): Result<PdfExportResult> = withContext(Dispatchers.IO) {
        try {
            if (entries.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No diary entries selected for export."))
            }

            // Sort entries chronologically (or reverse chronologically)
            val sortedEntries = entries.sortedWith(compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.time })

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val displayDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
            val fileName = "Diary_Export_${timestamp}.pdf"

            // Target destination in app downloads
            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads").apply { mkdirs() }
            if (!exportDir.exists()) exportDir.mkdirs()
            val outputFile = File(exportDir, fileName)

            val document = PdfDocument()

            // Pre-calculate pages and render
            val pageCount = renderDocumentPages(document, sortedEntries, options, displayDate)

            // Write to file
            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()

            // Also copy to public MediaStore Downloads if Android 10+ (API 29+)
            var savedPath = outputFile.absolutePath
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ImtiazDiary")
                    }
                    val resolver = context.contentResolver
                    val publicUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (publicUri != null) {
                        resolver.openOutputStream(publicUri)?.use { outStream ->
                            FileInputStream(outputFile).use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        savedPath = "Downloads/ImtiazDiary/$fileName"
                    }
                } catch (_: Exception) {
                    // Fall back gracefully to internal externalFilesDir
                }
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val sizeKb = (outputFile.length() / 1024.0)
            val sizeFormatted = if (sizeKb >= 1024) {
                String.format(Locale.US, "%.1f MB", sizeKb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f KB", sizeKb)
            }

            Result.success(
                PdfExportResult(
                    file = outputFile,
                    uri = fileUri,
                    fileName = fileName,
                    fileSizeFormatted = sizeFormatted,
                    pageCount = pageCount,
                    entryCount = sortedEntries.size,
                    destinationPath = savedPath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Renders pages for the document and returns the total number of pages created.
     */
    private fun renderDocumentPages(
        document: PdfDocument,
        entries: List<DiaryEntry>,
        options: PdfExportOptions,
        displayDate: String
    ): Int {
        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(26, 115, 232) // Google Blue
            isAntiAlias = true
        }

        val primaryDarkPaint = Paint().apply {
            color = Color.rgb(24, 90, 188)
            isAntiAlias = true
        }

        val textDarkPaint = TextPaint().apply {
            color = Color.rgb(32, 33, 36) // Dark grey / charcoal
            textSize = 10f
            isAntiAlias = true
        }

        val titlePaint = TextPaint().apply {
            color = Color.rgb(32, 33, 36)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val secondaryTextPaint = TextPaint().apply {
            color = Color.rgb(95, 99, 104)
            textSize = 8.5f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(232, 234, 237)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val boxBgPaint = Paint().apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val gratitudeBgPaint = Paint().apply {
            color = Color.rgb(230, 244, 234) // Soft green tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val gratitudeTextPaint = TextPaint().apply {
            color = Color.rgb(19, 115, 51) // Green dark
            textSize = 9f
            isAntiAlias = true
        }

        val badgeBgPaint = Paint().apply {
            color = Color.rgb(232, 240, 254) // Light blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeTextPaint = TextPaint().apply {
            color = Color.rgb(26, 115, 232)
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var pageIndex = 1
        var currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
        var currentPage = document.startPage(currentPageInfo)
        var canvas = currentPage.canvas
        var currentY = marginTop

        // Helper to draw footer on a canvas
        fun drawFooter(c: Canvas, pageNum: Int) {
            c.drawLine(marginLeft, marginBottom, marginRight, marginBottom, dividerPaint)
            c.drawText(
                "Imtiaz Lifestyle • Daily Agenda & Diary",
                marginLeft,
                marginBottom + 14f,
                secondaryTextPaint
            )
            val pageText = "Page $pageNum"
            val textWidth = secondaryTextPaint.measureText(pageText)
            c.drawText(pageText, marginRight - textWidth, marginBottom + 14f, secondaryTextPaint)
        }

        // Draw Cover / Top Header on Page 1
        // Colored header banner bar
        val bannerHeight = 8f
        canvas.drawRoundRect(
            RectF(marginLeft, currentY, marginRight, currentY + bannerHeight),
            4f, 4f, primaryPaint
        )
        currentY += bannerHeight + 14f

        // Document Main Title
        val docHeaderPaint = TextPaint().apply {
            color = Color.rgb(24, 90, 188)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(options.documentTitle, marginLeft, currentY, docHeaderPaint)
        currentY += 16f

        // Subtitle & Export date
        canvas.drawText("${options.subtitle} • Generated $displayDate", marginLeft, currentY, secondaryTextPaint)
        currentY += 18f

        // Summary Stats Card (if enabled)
        if (options.includeSummaryStats) {
            val totalWords = entries.sumOf { it.content.trim().split("\\s+".toRegex()).filter { w -> w.isNotBlank() }.size }
            val uniqueDates = entries.map { it.date }.distinct().size
            val cardHeight = 44f

            canvas.drawRoundRect(
                RectF(marginLeft, currentY, marginRight, currentY + cardHeight),
                8f, 8f, boxBgPaint
            )
            canvas.drawRoundRect(
                RectF(marginLeft, currentY, marginRight, currentY + cardHeight),
                8f, 8f, dividerPaint
            )

            val statTitlePaint = TextPaint().apply {
                color = Color.rgb(95, 99, 104)
                textSize = 7.5f
                isAntiAlias = true
            }
            val statValPaint = TextPaint().apply {
                color = Color.rgb(32, 33, 36)
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val colWidth = contentWidth / 3f
            // Col 1: Total Entries
            canvas.drawText("TOTAL ENTRIES", marginLeft + 14f, currentY + 16f, statTitlePaint)
            canvas.drawText("${entries.size} Reflections", marginLeft + 14f, currentY + 32f, statValPaint)

            // Col 2: Days Logged
            canvas.drawText("DAYS LOGGED", marginLeft + colWidth + 10f, currentY + 16f, statTitlePaint)
            canvas.drawText("$uniqueDates Days", marginLeft + colWidth + 10f, currentY + 32f, statValPaint)

            // Col 3: Total Words
            canvas.drawText("WORD COUNT", marginLeft + (colWidth * 2) + 10f, currentY + 16f, statTitlePaint)
            canvas.drawText("~$totalWords Words", marginLeft + (colWidth * 2) + 10f, currentY + 32f, statValPaint)

            currentY += cardHeight + 20f
        }

        // Render each diary entry
        for ((index, entry) in entries.withIndex()) {
            // Measure layouts first to estimate height needed
            val contentLayout = createStaticLayout(entry.content, textDarkPaint, contentWidth.toInt())
            val gratitudeLayout = if (options.includeGratitude && entry.gratitudeNotes.isNotBlank()) {
                createStaticLayout("🙏 Gratitude: " + entry.gratitudeNotes, gratitudeTextPaint, (contentWidth - 20f).toInt())
            } else null

            val titleLayout = if (entry.title.isNotBlank()) {
                createStaticLayout(entry.title, titlePaint, contentWidth.toInt())
            } else null

            var requiredHeight = 28f // Badge & meta row
            if (titleLayout != null) requiredHeight += titleLayout.height + 6f
            requiredHeight += contentLayout.height + 8f
            if (gratitudeLayout != null) requiredHeight += gratitudeLayout.height + 16f
            if (options.includeTags && entry.tags.isNotBlank()) requiredHeight += 16f
            requiredHeight += 18f // bottom divider & padding

            // Check if entry fits on current page
            if (currentY + requiredHeight > marginBottom) {
                // Draw footer and close page
                drawFooter(canvas, pageIndex)
                document.finishPage(currentPage)

                // Start new page
                pageIndex++
                currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                currentPage = document.startPage(currentPageInfo)
                canvas = currentPage.canvas
                currentY = marginTop

                // Mini header on subsequent pages
                canvas.drawText("${options.documentTitle} (Continued)", marginLeft, currentY, secondaryTextPaint)
                currentY += 14f
                canvas.drawLine(marginLeft, currentY, marginRight, currentY, dividerPaint)
                currentY += 16f
            }

            // Draw Entry Header Row (Date, Time, Mood badge, Energy)
            val dateBadgeText = "${entry.date}  •  ${entry.time}"
            val dateWidth = secondaryTextPaint.measureText(dateBadgeText)

            // Mood pill
            if (options.includeMoodAndEnergy && entry.mood.isNotBlank()) {
                val moodPillText = entry.mood
                val moodWidth = badgeTextPaint.measureText(moodPillText)
                val pillRect = RectF(marginLeft, currentY - 10f, marginLeft + moodWidth + 14f, currentY + 6f)
                canvas.drawRoundRect(pillRect, 6f, 6f, badgeBgPaint)
                canvas.drawText(moodPillText, marginLeft + 7f, currentY + 2f, badgeTextPaint)

                // Date & Time right after mood
                canvas.drawText(dateBadgeText, marginLeft + moodWidth + 22f, currentY + 1f, secondaryTextPaint)
            } else {
                canvas.drawText(dateBadgeText, marginLeft, currentY + 1f, secondaryTextPaint)
            }

            // Energy Rating on right
            if (options.includeMoodAndEnergy) {
                val energyText = "Energy: " + "⚡".repeat(entry.energy.coerceIn(1, 5))
                val energyWidth = secondaryTextPaint.measureText(energyText)
                canvas.drawText(energyText, marginRight - energyWidth, currentY + 1f, secondaryTextPaint)
            }

            currentY += 16f

            // Entry Title
            if (titleLayout != null) {
                canvas.save()
                canvas.translate(marginLeft, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 6f
            }

            // Entry Content
            canvas.save()
            canvas.translate(marginLeft, currentY)
            contentLayout.draw(canvas)
            canvas.restore()
            currentY += contentLayout.height + 6f

            // Gratitude Notes Box
            if (gratitudeLayout != null) {
                val boxH = gratitudeLayout.height + 12f
                val gratitudeRect = RectF(marginLeft, currentY, marginRight, currentY + boxH)
                canvas.drawRoundRect(gratitudeRect, 6f, 6f, gratitudeBgPaint)
                canvas.save()
                canvas.translate(marginLeft + 10f, currentY + 6f)
                gratitudeLayout.draw(canvas)
                canvas.restore()
                currentY += boxH + 8f
            }

            // Tags
            if (options.includeTags && entry.tags.isNotBlank()) {
                val tagsText = "Tags: ${entry.tags}"
                canvas.drawText(tagsText, marginLeft, currentY + 8f, secondaryTextPaint)
                currentY += 14f
            }

            // Entry bottom divider
            if (index < entries.size - 1) {
                currentY += 6f
                canvas.drawLine(marginLeft, currentY, marginRight, currentY, dividerPaint)
                currentY += 14f
            } else {
                currentY += 10f
            }
        }

        // Draw footer on last page
        drawFooter(canvas, pageIndex)
        document.finishPage(currentPage)

        return pageIndex
    }

    /**
     * Builds a StaticLayout with proper Android version compatibility.
     */
    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        val safeText = if (text.isBlank()) " " else text
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width.coerceAtLeast(100))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2.5f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                safeText,
                paint,
                width.coerceAtLeast(100),
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                2.5f,
                false
            )
        }
    }

    /**
     * Helper to open the PDF file with an external PDF viewer application.
     */
    fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // In case no PDF viewer is installed, open chooser
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Open PDF With"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    /**
     * Helper to share the PDF via Android Share Sheet.
     */
    fun sharePdf(uri: Uri, subject: String = "Diary Export") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Diary PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
