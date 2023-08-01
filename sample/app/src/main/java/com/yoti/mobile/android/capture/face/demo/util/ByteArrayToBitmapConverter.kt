package com.yoti.mobile.android.capture.face.demo.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ByteArrayToBitmapConverter(
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun convert(image: ByteArray): Bitmap = withContext(ioDispatcher) {
        BitmapFactory.decodeByteArray(image, 0, image.size)
    }
}