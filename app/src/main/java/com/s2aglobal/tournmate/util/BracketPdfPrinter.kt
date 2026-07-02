package com.s2aglobal.tournmate.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.Tournament
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders bracket / matches / standings to a printable PDF.
 *
 * Uses Android's PrintManager — the user gets the system print dialog where
 * they can choose "Save as PDF" or send to a physical printer.
 * Mirrors iOS shareCurrentTabAsPDF() / PDFDocumentWrapper behaviour.
 */
object BracketPdfPrinter {

    fun print(
        context: Context,
        tournament: Tournament,
        matches: List<Match>,
        registrations: List<Registration>,
        tabName: String,
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val isLandscape = tabName == "Bracket" || tabName == "Draw"
        val mediaSize = if (isLandscape)
            PrintAttributes.MediaSize.ISO_A4.asLandscape()
        else
            PrintAttributes.MediaSize.NA_LETTER

        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val docName = "${tournament.title} — $tabName"
        printManager.print(
            docName,
            buildAdapter(context, tournament, matches, registrations, tabName),
            attributes,
        )
    }

    // ── PrintDocumentAdapter ─────────────────────────────────────────────────

    private fun buildAdapter(
        context: Context,
        tournament: Tournament,
        matches: List<Match>,
        registrations: List<Registration>,
        tabName: String,
    ): PrintDocumentAdapter = object : PrintDocumentAdapter() {

        private var currentAttrs: PrintAttributes? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            currentAttrs = newAttributes
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder("${tournament.title} — $tabName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build(),
                oldAttributes != newAttributes,
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onWriteCancelled(); return }

            val attrs = currentAttrs
            val widthPts  = attrs?.mediaSize?.widthMils?.let { it * 72 / 1000 } ?: 612
            val heightPts = attrs?.mediaSize?.heightMils?.let { it * 72 / 1000 } ?: 792

            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(widthPts, heightPts, 1).create()
            val page = doc.startPage(pageInfo)

            drawPage(page.canvas, tournament, matches, registrations, tabName, widthPts.toFloat(), heightPts.toFloat())

            doc.finishPage(page)
            try {
                doc.writeTo(FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                Log.e("BracketPdf", "Write failed", e)
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
            }
        }
    }

    // ── Canvas drawing ───────────────────────────────────────────────────────

    private val brandPurple = Color.rgb(0x63, 0x08, 0x93)
    private val textBlack   = Color.BLACK
    private val textGray    = Color.GRAY
    private val bgGray      = Color.rgb(0xF2, 0xF2, 0xF7)
    private val divider     = Color.rgb(0xE0, 0xE0, 0xE0)

    private fun drawPage(
        canvas: Canvas,
        tournament: Tournament,
        matches: List<Match>,
        registrations: List<Registration>,
        tabName: String,
        width: Float,
        height: Float,
    ) {
        val margin = 36f
        val regMap = registrations.associateBy { it.id.toString().uppercase() }
        fun teamName(id: String?): String {
            val reg = regMap[id?.uppercase()] ?: return "TBD"
            return listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")
        }

        // ── Header ────────────────────────────────────────────────────────────
        drawHeader(canvas, tournament, tabName, width, margin)
        val bodyTop = 90f

        // ── Body ─────────────────────────────────────────────────────────────
        when (tabName) {
            "Bracket" -> drawBracket(canvas, matches, regMap, margin, bodyTop, width, height)
            "Draw"    -> drawDraw(canvas, matches, regMap, margin, bodyTop, width, height)
            "Standings" -> drawStandings(canvas, matches, regMap, margin, bodyTop, width, height, tournament)
            else        -> drawMatchList(canvas, matches, regMap, margin, bodyTop, width, height)
        }
    }

    private fun drawHeader(canvas: Canvas, tournament: Tournament, tabName: String, width: Float, margin: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Purple header bar
        paint.color = brandPurple
        canvas.drawRect(0f, 0f, width, 72f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(tournament.title, margin, 28f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        canvas.drawText(
            "${tournament.matchFormat.displayName}  •  ${tournament.location}  •  ${dateFmt.format(tournament.date)}",
            margin, 48f, paint,
        )

        // Tab label (right-aligned)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(tabName.uppercase(), width - margin, 36f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBracket(
        canvas: Canvas,
        matches: List<Match>,
        regMap: Map<String, Registration>,
        margin: Float, top: Float, width: Float, height: Float,
    ) {
        val byRound = matches.groupBy { it.round ?: 1 }
            .toSortedMap(compareBy { it })
        val rounds = byRound.keys.toList()
        if (rounds.isEmpty()) return

        val colWidth = (width - 2 * margin) / rounds.size.coerceAtLeast(1)
        val matchH = 36f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        rounds.forEachIndexed { colIdx, round ->
            val x = margin + colIdx * colWidth
            val roundMatches = byRound[round] ?: return@forEachIndexed

            // Round label
            paint.color = brandPurple; paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(
                Match.bracketRoundName(round, rounds.size).uppercase(),
                x, top + 10f, paint,
            )

            roundMatches.forEachIndexed { i, match ->
                val y = top + 20f + i * (matchH + 8f)
                drawMatchBox(canvas, match, regMap, x, y, colWidth - 10f, matchH, paint)
            }
        }
    }

    private fun drawDraw(
        canvas: Canvas,
        matches: List<Match>,
        regMap: Map<String, Registration>,
        margin: Float, top: Float, width: Float, height: Float,
    ) {
        val groups = matches.filter { it.groupLabel != null }
            .groupBy { it.groupLabel!! }.toSortedMap()

        if (groups.isEmpty()) {
            drawBracket(canvas, matches, regMap, margin, top, width, height)
            return
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colW = (width - 2 * margin) / groups.size.coerceAtLeast(1)
        var col = 0

        groups.forEach { (label, gMatches) ->
            val x = margin + col * colW
            var y = top

            paint.color = Color.rgb(0xFF, 0x95, 0x00); paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Group $label", x, y, paint)
            y += 14f

            gMatches.sortedBy { it.round }.forEach { m ->
                drawMatchBox(canvas, m, regMap, x, y, colW - 10f, 34f, paint)
                y += 42f
            }
            col++
        }
    }

    private fun drawStandings(
        canvas: Canvas,
        matches: List<Match>,
        regMap: Map<String, Registration>,
        margin: Float, top: Float, width: Float, height: Float,
        tournament: Tournament,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = textBlack; paint.textSize = 11f

        // Compute standings from wins/losses
        data class StandingEntry(val teamId: String, var wins: Int, var losses: Int, var points: Int)
        val map = mutableMapOf<String, StandingEntry>()
        matches.filter { it.status == MatchStatus.FINISHED }.forEach { m ->
        val aEntry = map.getOrPut(m.teamAId) { StandingEntry(m.teamAId, 0, 0, 0) }
            val bEntry = map.getOrPut(m.teamBId) { StandingEntry(m.teamBId, 0, 0, 0) }
            val aWon = m.winnerRegistrationId == m.teamAId
        if (aWon) { aEntry.wins++; aEntry.points += 2; bEntry.losses++ }
        else      { bEntry.wins++; bEntry.points += 2; aEntry.losses++ }
        }
        val standings = map.values.sortedByDescending { it.points }

        var y = top + 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = brandPurple; paint.textSize = 9f
        canvas.drawText("#   PLAYER/TEAM", margin, y, paint)
        canvas.drawText("W", width - margin - 80f, y, paint)
        canvas.drawText("L", width - margin - 50f, y, paint)
        canvas.drawText("PTS", width - margin - 20f, y, paint)
        y += 4f

        // Divider
        paint.color = divider; paint.strokeWidth = 0.5f
        canvas.drawLine(margin, y, width - margin, y, paint)
        y += 10f

        paint.typeface = Typeface.DEFAULT; paint.textSize = 10f
        standings.forEachIndexed { idx, entry ->
            if (y > height - 36f) return
            val name = regMap[entry.teamId.uppercase()]?.let {
                listOfNotNull(it.player.name, it.partner?.name).joinToString(" & ")
            } ?: entry.teamId.take(20)

            paint.color = if (idx % 2 == 0) Color.WHITE else bgGray
            canvas.drawRect(margin - 4f, y - 10f, width - margin + 4f, y + 4f, paint)

            paint.color = textBlack
            canvas.drawText("${idx + 1}", margin, y, paint)
            canvas.drawText(name, margin + 20f, y, paint)
            canvas.drawText("${entry.wins}", width - margin - 80f, y, paint)
            canvas.drawText("${entry.losses}", width - margin - 50f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${entry.points}", width - margin - 20f, y, paint)
            paint.typeface = Typeface.DEFAULT
            y += 18f
        }
    }

    private fun drawMatchList(
        canvas: Canvas,
        matches: List<Match>,
        regMap: Map<String, Registration>,
        margin: Float, top: Float, width: Float, height: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = top + 14f

        val grouped = matches.groupBy { it.groupLabel ?: "Matches" }.toSortedMap()

        grouped.forEach { (group, gMatches) ->
            if (y > height - 36f) return

            if (group != "Matches") {
                paint.color = Color.rgb(0xFF, 0x95, 0x00); paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Group $group", margin, y, paint)
                y += 14f
            }

            gMatches.sortedBy { it.round }.forEach { m ->
                if (y > height - 36f) return
                drawMatchBox(canvas, m, regMap, margin, y, width - 2 * margin, 30f, paint)
                y += 38f
            }
        }
    }

    private fun drawMatchBox(
        canvas: Canvas,
        match: Match,
        regMap: Map<String, Registration>,
        x: Float, y: Float, w: Float, h: Float,
        paint: Paint,
    ) {
        fun teamName(id: String?): String {
            val reg = regMap[id?.uppercase()] ?: return "TBD"
            return listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")
        }

        // Box
        paint.color = bgGray; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint)

        paint.color = divider; paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.5f
        canvas.drawRoundRect(x, y, x + w, y + h, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        val nameA = teamName(match.teamAId)
        val nameB = teamName(match.teamBId)
        val score = match.displayScoreLine.ifBlank { "vs" }
        val midY = y + h / 2

        val aWon = match.winnerRegistrationId == match.teamAId
        val bWon = match.winnerRegistrationId == match.teamBId

        paint.textSize = 7.5f; paint.typeface = Typeface.DEFAULT
        paint.color = if (aWon) brandPurple else textBlack
        paint.typeface = if (aWon) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        canvas.drawText(nameA.take(25), x + 4f, midY - 4f, paint)

        paint.color = if (bWon) brandPurple else textBlack
        paint.typeface = if (bWon) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        canvas.drawText(nameB.take(25), x + 4f, midY + 8f, paint)

        // Score right-aligned
        paint.color = textGray; paint.typeface = Typeface.DEFAULT; paint.textSize = 7f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(score, x + w - 4f, midY + 2f, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}
