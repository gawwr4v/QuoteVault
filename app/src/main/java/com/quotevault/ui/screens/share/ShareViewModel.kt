package com.quotevault.ui.screens.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotevault.domain.model.Quote
import com.quotevault.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun loadQuote(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = quoteRepository.getQuote(id)
            if (result.isSuccess) {
                 _uiState.update { it.copy(quote = result.getOrThrow(), isLoading = false) }
            } else {
                 _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun copyToClipboard(quote: Quote) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = "\"${quote.content}\"\n- ${quote.author}"
        val clip = android.content.ClipData.newPlainText("Quote", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(toastMessage = "Quote copied to clipboard!") }
    }
    
    fun saveToGallery(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val filename = "QuoteVault_${System.currentTimeMillis()}.png"
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Quotes")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                
                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(imageUri, values, null, null)
                    }
                    
                    _uiState.update { it.copy(toastMessage = "Image saved to gallery!") }
                } ?: run {
                    _uiState.update { it.copy(error = "Failed to save image") }
                }
            } catch (e: Exception) {
                android.util.Log.e("ShareVM", "Save failed", e)
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun shareImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "share_image.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                }
                
                _uiState.update { it.copy(shareIntent = shareIntent) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save image") }
            }
        }
    }
    
    fun clearShareIntent() {
        _uiState.update { it.copy(shareIntent = null) }
    }
    
    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}

data class ShareUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val shareIntent: Intent? = null,
    val toastMessage: String? = null
)
