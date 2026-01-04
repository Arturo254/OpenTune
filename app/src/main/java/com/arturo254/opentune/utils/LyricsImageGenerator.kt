package com.arturo254.opentune.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import coil.ImageLoader
import coil.request.ImageRequest
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ComposeToImage {

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(context, bitmap, fileName)
        } else {
            saveToCache(context, bitmap, fileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val contentValues = buildContentValues(fileName)
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create new MediaStore record")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return uri
    }

    private fun buildContentValues(fileName: String): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OpenTune")
    }

    private fun saveToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val imageFile = File(cachePath, "$fileName.png")

        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.FileProvider",
            imageFile
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        showCoverArt: Boolean = true,
        showLogo: Boolean = true,
        backgroundStyle: String = "SOLID",
        gradientColors: IntArray? = null,
        fontStyle: String = "EXTRA_BOLD",
        logoPosition: String = "BOTTOM_LEFT",
        cornerRadius: Float = 28f,
        patternOpacity: Float = 0.03f,
        textAlignment: String = "CENTER",
        padding: Float = 32f,
        showArtistName: Boolean = true,
        showSongTitle: Boolean = true,
        textShadowEnabled: Boolean = true,
        borderEnabled: Boolean = false,
        borderColor: Int? = null,
        borderWidth: Float = 2f,
        logoSize: String = "MEDIUM",
        coverArtStyle: String = "ROUNDED",
        lyricsStyle: String = "NORMAL",
        showAccentLine: Boolean = false,
        accentColor: Int? = null,
        spacingBetweenElements: Float = 16f,
        lyricsLineSpacing: Float = 1.3f
    ): Bitmap = withContext(Dispatchers.Default) {
        val imageSize = 1080
        val bitmap = createBitmap(imageSize, imageSize)
        val canvas = Canvas(bitmap)

        val bgColor = backgroundColor ?: 0xFF0A0A0A.toInt()
        val mainTextColor = textColor ?: 0xFFFFFFFF.toInt()
        val secTextColor = secondaryTextColor ?: 0xFFB0B0B0.toInt()
        val bColor = borderColor ?: mainTextColor
        val aColor = accentColor ?: mainTextColor

        val outerPadding = imageSize * (padding / 380f)
        val thumbnailSize = imageSize * 0.21f
        val logoSizeMultiplier = when (logoSize) {
            "SMALL" -> 0.7f
            "MEDIUM" -> 1f
            "LARGE" -> 1.3f
            else -> 1f
        }
        val logoSizePx = imageSize * 0.073f * logoSizeMultiplier
        val cornerRadiusPx = (cornerRadius / 28f) * (imageSize * 0.073f)
        val elementSpacing = imageSize * (spacingBetweenElements / 380f)

        val bgRect = RectF(0f, 0f, imageSize.toFloat(), imageSize.toFloat())
        renderBackground(
            canvas,
            bgRect,
            cornerRadiusPx,
            bgColor,
            mainTextColor,
            backgroundStyle,
            gradientColors,
            patternOpacity,
            imageSize
        )

        if (borderEnabled) {
            val borderPaint = Paint().apply {
                color = bColor
                style = Paint.Style.STROKE
                strokeWidth = borderWidth * (imageSize / 380f)
                isAntiAlias = true
            }
            canvas.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, borderPaint)
        }

        if (showCoverArt) {
            val coverArt = loadCoverArtwork(context, coverArtUrl)
            renderCoverArtHighRes(
                canvas,
                coverArt,
                outerPadding,
                thumbnailSize,
                mainTextColor,
                coverArtStyle
            )

            val metadataHeight = renderSongMetadataHighRes(
                canvas,
                songTitle,
                artistName,
                outerPadding,
                thumbnailSize,
                imageSize,
                mainTextColor,
                secTextColor,
                fontStyle,
                showSongTitle,
                showArtistName,
                elementSpacing
            )

            if (showAccentLine) {
                renderAccentLine(
                    canvas,
                    outerPadding,
                    outerPadding + thumbnailSize + elementSpacing + metadataHeight + elementSpacing,
                    imageSize,
                    aColor
                )
            }
        }

        renderLyricsHighRes(
            canvas,
            lyrics,
            imageSize,
            outerPadding,
            thumbnailSize,
            mainTextColor,
            bgColor,
            fontStyle,
            showCoverArt,
            textAlignment,
            textShadowEnabled,
            lyricsStyle,
            elementSpacing,
            showAccentLine,
            lyricsLineSpacing
        )

        if (showLogo && logoPosition != "NONE") {
            renderAppLogoHighRes(
                context,
                canvas,
                imageSize,
                outerPadding,
                logoSizePx,
                mainTextColor,
                logoPosition,
                logoSizeMultiplier
            )
        }

        bitmap
    }

    private fun renderBackground(
        canvas: Canvas,
        rect: RectF,
        cornerRadius: Float,
        backgroundColor: Int,
        textColor: Int,
        backgroundStyle: String,
        gradientColors: IntArray?,
        patternOpacity: Float,
        imageSize: Int
    ) {
        when (backgroundStyle) {
            "SOLID" -> {
                val paint = Paint().apply {
                    color = backgroundColor
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
            "GRADIENT" -> {
                val colors = gradientColors ?: intArrayOf(backgroundColor, backgroundColor)
                val paint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(
                        0f, 0f,
                        rect.width(), rect.height(),
                        colors,
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
            "PATTERN" -> {
                val paint = Paint().apply {
                    color = backgroundColor
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                val clipPath = Path().apply {
                    addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)

                val patternPaint = Paint().apply {
                    color = textColor
                    alpha = (patternOpacity * 255).toInt()
                    isAntiAlias = true
                }
                val pattern = imageSize * 0.037f
                val radius = imageSize * 0.00185f
                var x = 0f
                while (x <= rect.width()) {
                    var y = 0f
                    while (y <= rect.height()) {
                        canvas.drawCircle(x, y, radius, patternPaint)
                        y += pattern
                    }
                    x += pattern
                }
                canvas.restore()
            }
        }
    }

    private suspend fun loadCoverArtwork(context: Context, coverArtUrl: String?): Bitmap? {
        if (coverArtUrl.isNullOrEmpty()) return null
        return try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(coverArtUrl)
                .size(800, 800)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            result.drawable?.toBitmap(800, 800, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderCoverArtHighRes(
        canvas: Canvas,
        coverArt: Bitmap?,
        padding: Float,
        size: Float,
        textColor: Int,
        coverArtStyle: String
    ) {
        coverArt?.let { artwork ->
            val rect = RectF(padding, padding, padding + size, padding + size)

            val cornerRadius = when (coverArtStyle) {
                "CIRCLE" -> size / 2f
                "SQUARE" -> size * 0.05f
                "ROUNDED" -> size * 0.25f
                else -> size * 0.25f
            }

            val bgPaint = Paint().apply {
                color = textColor
                alpha = 25
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(artwork, null, rect, paint)
            canvas.restore()

            val borderPaint = Paint().apply {
                color = textColor
                alpha = 51
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        }
    }

    private fun renderSongMetadataHighRes(
        canvas: Canvas,
        songTitle: String,
        artistName: String,
        padding: Float,
        thumbnailSize: Float,
        imageSize: Int,
        mainTextColor: Int,
        secondaryTextColor: Int,
        fontStyle: String,
        showSongTitle: Boolean,
        showArtistName: Boolean,
        elementSpacing: Float
    ): Float {
        val startX = padding + thumbnailSize + elementSpacing
        val maxWidth = (imageSize - startX - padding).toInt()

        val titleTypeface = when (fontStyle) {
            "REGULAR" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            "BOLD" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
            "EXTRA_BOLD" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            else -> Typeface.create("sans-serif-black", Typeface.BOLD)
        }

        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = imageSize * 0.0526f
            typeface = titleTypeface
            isAntiAlias = true
            letterSpacing = -0.025f
        }

        val artistPaint = TextPaint().apply {
            color = secondaryTextColor
            textSize = imageSize * 0.042f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.0125f
        }

        val titleLayout = if (showSongTitle) {
            StaticLayout.Builder.obtain(
                songTitle, 0, songTitle.length, titlePaint, maxWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(2)
                .setLineSpacing(0f, 1.2f)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        } else null

        val artistLayout = if (showArtistName) {
            StaticLayout.Builder.obtain(
                artistName, 0, artistName.length, artistPaint, maxWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(1)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        } else null

        val centerY = padding + thumbnailSize / 2f
        val titleHeight = titleLayout?.height ?: 0
        val artistHeight = artistLayout?.height ?: 0
        val spacing2 = if (showSongTitle && showArtistName) imageSize * 0.0105f else 0f
        val textBlockHeight = titleHeight + artistHeight + spacing2
        val startY = centerY - textBlockHeight / 2f

        canvas.withTranslation(startX, startY) {
            titleLayout?.draw(this)
            if (showArtistName && artistLayout != null) {
                translate(0f, titleHeight + spacing2)
                artistLayout.draw(this)
            }
        }

        return textBlockHeight
    }

    private fun renderAccentLine(
        canvas: Canvas,
        padding: Float,
        yPosition: Float,
        imageSize: Int,
        accentColor: Int
    ) {
        val linePaint = Paint().apply {
            color = accentColor
            strokeWidth = imageSize * 0.0028f
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val lineRect = RectF(
            padding,
            yPosition,
            imageSize - padding,
            yPosition + imageSize * 0.0028f
        )

        val cornerRadius = imageSize * 0.0019f
        canvas.drawRoundRect(lineRect, cornerRadius, cornerRadius, linePaint)
    }

    private fun renderLyricsHighRes(
        canvas: Canvas,
        lyrics: String,
        imageSize: Int,
        padding: Float,
        thumbnailSize: Float,
        textColor: Int,
        backgroundColor: Int,
        fontStyle: String,
        showCoverArt: Boolean,
        textAlignment: String,
        textShadowEnabled: Boolean,
        lyricsStyle: String,
        elementSpacing: Float,
        showAccentLine: Boolean,
        lineSpacing: Float
    ) {
        val lyricsTypeface = when (fontStyle) {
            "REGULAR" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            "BOLD" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
            "EXTRA_BOLD" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            else -> Typeface.create("sans-serif-black", Typeface.BOLD)
        }

        val alignment = when (textAlignment) {
            "LEFT" -> Layout.Alignment.ALIGN_NORMAL
            "CENTER" -> Layout.Alignment.ALIGN_CENTER
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val maxWidth = (imageSize * 0.85f).toInt()
        val headerHeight = if (showCoverArt) {
            padding + thumbnailSize + elementSpacing + (if (showAccentLine) elementSpacing * 1.5f else 0f)
        } else {
            padding
        }
        val footerHeight = imageSize * 0.148f
        val maxHeight = imageSize - headerHeight - footerHeight

        var textSize = imageSize * 0.055f
        val minTextSize = imageSize * 0.037f

        val paint = TextPaint().apply {
            color = textColor
            typeface = lyricsTypeface
            isAntiAlias = true
            letterSpacing = if (lyricsStyle == "CONDENSED") -0.025f else 0.015f

            if (lyricsStyle == "ITALIC") {
                textSkewX = -0.25f
            }
        }

        if (textShadowEnabled) {
            paint.setShadowLayer(
                imageSize * 0.0037f,
                imageSize * 0.00185f,
                imageSize * 0.00185f,
                backgroundColor
            )
        }

        var layout: StaticLayout
        do {
            paint.textSize = textSize
            layout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, paint, maxWidth)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(0f, lineSpacing)
                .build()
            if (layout.height <= maxHeight) break
            textSize -= imageSize * 0.0028f
        } while (textSize > minTextSize)

        if (textSize < minTextSize) {
            paint.textSize = minTextSize
            layout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, paint, maxWidth)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(0f, lineSpacing)
                .build()
        }

        val posX = (imageSize - layout.width) / 2f
        val availableHeight = imageSize - headerHeight - footerHeight
        val posY = headerHeight + (availableHeight - layout.height) / 2f

        canvas.withTranslation(posX, posY) {
            layout.draw(this)
        }
    }

    private fun renderAppLogoHighRes(
        context: Context,
        canvas: Canvas,
        imageSize: Int,
        padding: Float,
        logoSize: Float,
        textColor: Int,
        logoPosition: String,
        logoSizeMultiplier: Float
    ) {
        val logoBitmap = getBitmapFromVectorDrawable(
            context, R.drawable.opentune, logoSize.toInt(), logoSize.toInt()
        ) ?: return

        val appName = context.getString(R.string.app_name)
        val textPaint = TextPaint().apply {
            color = textColor
            textSize = imageSize * 0.0395f * logoSizeMultiplier
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.02f
        }

        val textBounds = Rect()
        textPaint.getTextBounds(appName, 0, appName.length, textBounds)
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        val totalWidth = logoSize + (imageSize * 0.0315f) + textWidth
        val totalHeight = maxOf(logoSize, textHeight.toFloat())

        val (logoX, logoY) = when (logoPosition) {
            "BOTTOM_LEFT" -> Pair(padding, imageSize - padding - totalHeight)
            "BOTTOM_RIGHT" -> Pair(imageSize - padding - totalWidth, imageSize - padding - totalHeight)
            "TOP_LEFT" -> Pair(padding, padding)
            "TOP_RIGHT" -> Pair(imageSize - padding - totalWidth, padding)
            else -> Pair(padding, imageSize - padding - totalHeight)
        }

        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            isAntiAlias = true
        }

        val logoCenterY = logoY + totalHeight / 2f - logoSize / 2f
        canvas.drawBitmap(logoBitmap, logoX, logoCenterY, paint)

        val textX = logoX + logoSize + (imageSize * 0.0315f)
        val textY = logoY + totalHeight / 2f + textHeight / 2f

        canvas.drawText(appName, textX, textY, textPaint)
    }

    private fun getBitmapFromVectorDrawable(
        context: Context,
        drawableId: Int,
        width: Int,
        height: Int
    ): Bitmap? {
        val drawable = context.getDrawable(drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }
}