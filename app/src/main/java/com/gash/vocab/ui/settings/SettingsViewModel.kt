package com.gash.vocab.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gash.vocab.GashApp
import com.gash.vocab.data.db.AppDatabase
import com.gash.vocab.data.importer.VocabImporter
import com.gash.vocab.data.repository.DifficultyLevel
import com.gash.vocab.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val newPerDay: Int = 10,
    val reviewsPerDay: Int = 50,
    val isUncappedToday: Boolean = false,
    val learningSteps: String = "1,10",
    val graduatingInterval: Int = 1,
    val easyInterval: Int = 4,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.B1_B2,
    val totalWords: Int = 0,
    val posCategoryMap: Map<String, String> = emptyMap(),
    val importMessage: String? = null,
    val progressMessage: String? = null,
    val dbImportMessage: String? = null,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val isDbImporting: Boolean = false,
    val dbImportSuccess: Boolean = false,
    val isSavingPrompt: Boolean = false,
    val promptTemplateMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as GashApp).repository
    private val db = (application as GashApp).database
    private val settings = (application as GashApp).settings

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsUiState(
                newPerDay = settings.newPerDay,
                reviewsPerDay = settings.reviewsPerDay,
                isUncappedToday = settings.isUncappedToday,
                learningSteps = settings.learningSteps,
                graduatingInterval = settings.graduatingInterval,
                easyInterval = settings.easyInterval,
                difficultyLevel = settings.difficultyLevel,
                totalWords = repo.getWordCount(),
                posCategoryMap = settings.posCategoryMap
            )
        }
    }

    fun setDifficultyLevel(value: DifficultyLevel) {
        settings.difficultyLevel = value
        _state.value = _state.value.copy(difficultyLevel = value)
    }

    // ── Study limits ──────────────────────────────────────────────

    fun setNewPerDay(value: Int) {
        settings.newPerDay = value
        _state.value = _state.value.copy(newPerDay = value)
    }

    fun setReviewsPerDay(value: Int) {
        settings.reviewsPerDay = value
        _state.value = _state.value.copy(reviewsPerDay = value)
    }

    fun toggleUncapToday() {
        if (settings.isUncappedToday) {
            settings.removeUncap()
        } else {
            settings.uncapForToday()
        }
        _state.value = _state.value.copy(isUncappedToday = settings.isUncappedToday)
    }

    // ── Review settings ───────────────────────────────────────────

    fun setLearningSteps(value: String) {
        settings.learningSteps = value
        _state.value = _state.value.copy(learningSteps = value)
    }

    fun setGraduatingInterval(value: Int) {
        settings.graduatingInterval = value
        _state.value = _state.value.copy(graduatingInterval = value)
    }

    fun setEasyInterval(value: Int) {
        settings.easyInterval = value
        _state.value = _state.value.copy(easyInterval = value)
    }

    // ── POS mapping ───────────────────────────────────────────────

    fun updatePosCategory(pos: String, category: String) {
        val updated = _state.value.posCategoryMap.toMutableMap()
        updated[pos] = category
        settings.posCategoryMap = updated
        _state.value = _state.value.copy(posCategoryMap = updated)
    }

    fun resetPosMapping() {
        val defaults = SettingsRepository.defaultPosCategoryMap()
        settings.posCategoryMap = defaults
        _state.value = _state.value.copy(posCategoryMap = defaults)
    }

    // ── Vocab import ──────────────────────────────────────────────

    fun importVocab(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, importMessage = null)

            val importer = VocabImporter(db.wordDao())
            val result = importer.importFromUri(context, uri)

            val message = buildString {
                append("Import complete: ${result.inserted} new, ${result.updated} updated")
                if (result.errors.isNotEmpty()) {
                    append("\n${result.errors.size} errors")
                }
            }

            val newCount = repo.getWordCount()
            _state.value = _state.value.copy(
                isImporting = false,
                importMessage = message,
                totalWords = newCount
            )
        }
    }

    // ── Progress export/import ────────────────────────────────────

    fun exportProgress(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, progressMessage = null)
            val result = repo.exportProgress(context, uri)
            val message = result.fold(
                onSuccess = { "Exported $it progress entries" },
                onFailure = { "Export failed: ${it.message}" }
            )
            _state.value = _state.value.copy(isExporting = false, progressMessage = message)
        }
    }

    fun importProgress(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, progressMessage = null)
            val result = repo.importProgress(context, uri)
            val message = result.fold(
                onSuccess = { summary ->
                    buildString {
                        append("Imported ${summary.imported} progress entries")
                        if (summary.remapped > 0) {
                            append(" (${summary.remapped} matched by content to a different word id)")
                        }
                        if (summary.skipped.isNotEmpty()) {
                            append(" — ${summary.skipped.size} skipped (no matching word in current vocabulary)")
                        }
                    }
                },
                onFailure = { "Import failed: ${it.message}" }
            )
            _state.value = _state.value.copy(isImporting = false, progressMessage = message)
        }
    }

    // ── AI prompt template export ────────────────────────────────

    fun savePromptTemplate(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingPrompt = true, promptTemplateMessage = null)
            val result = repo.savePromptTemplate(context, uri)
            val message = result.fold(
                onSuccess = { count ->
                    "Saved prompt — $count existing word${if (count == 1) "" else "s"} included for deduplication"
                },
                onFailure = { "Save failed: ${it.message}" }
            )
            _state.value = _state.value.copy(isSavingPrompt = false, promptTemplateMessage = message)
        }
    }

    // ── Database file import ────────────────────────────────────

    fun importDatabase(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDbImporting = true, dbImportMessage = null, dbImportSuccess = false)
            val result = AppDatabase.restoreFromUri(context, uri)
            val message = result.fold(
                onSuccess = { "Database restored. Please restart the app for changes to take effect." },
                onFailure = { "Database import failed: ${it.message}" }
            )
            _state.value = _state.value.copy(
                isDbImporting = false,
                dbImportMessage = message,
                dbImportSuccess = result.isSuccess
            )
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            importMessage = null,
            progressMessage = null,
            dbImportMessage = null,
            promptTemplateMessage = null
        )
    }
}
