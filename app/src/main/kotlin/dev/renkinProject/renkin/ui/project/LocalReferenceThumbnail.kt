package dev.renkinProject.renkin.ui.project

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared local PNG preview, independent of any retired analysis service. */
@Composable
internal fun AnalysisThumbnail(hash: String, read: suspend () -> ByteArray, index: Int) {
    val latest by rememberUpdatedState(read)
    val image by produceState<ImageBitmap?>(null, hash) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                val bytes = latest(); val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            value = bitmap?.asImageBitmap()
            kotlinx.coroutines.awaitCancellation()
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null }
        finally { bitmap?.recycle() }
    }
    if (image != null) Image(checkNotNull(image), stringResource(R.string.styleAnalysisPreview, index + 1), Modifier.size(64.dp))
    else Text(stringResource(R.string.styleAnalysisPreview, index + 1), Modifier.testTag("style-analysis-preview-$index"))
}
