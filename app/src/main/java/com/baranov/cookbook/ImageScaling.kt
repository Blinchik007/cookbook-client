package com.baranov.cookbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Загружает изображение из Uri, уменьшает до квадрата [maxSize] x [maxSize] с сохранением пропорций,
 * пережимает в JPEG quality 80, возвращает Base64-строку.
 *
 * Используется при сохранении фото в БД, чтобы не плодить мегабайтные строки.
 * Для аватарок передавать maxSize=256, для фото рецептов — 1024.
 *
 * @return Base64-строка или null если что-то пошло не так (нет файла, повреждённое изображение).
 */
fun scaleAndEncodeImage(context: Context, uri: Uri, maxSize: Int): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val scale = minOf(
            maxSize.toFloat() / original.width,
            maxSize.toFloat() / original.height,
            1f
        )
        val scaled = if (scale < 1f) {
            val newWidth = (original.width * scale).toInt()
            val newHeight = (original.height * scale).toInt()
            Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
        } else {
            original
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val bytes = output.toByteArray()
        output.close()
        if (scaled != original) scaled.recycle()
        original.recycle()

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}