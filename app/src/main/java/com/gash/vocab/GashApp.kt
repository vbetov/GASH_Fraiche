package com.gash.vocab

import android.app.Application
import com.gash.vocab.data.db.AppDatabase
import com.gash.vocab.data.importer.SeedLoader
import com.gash.vocab.data.repository.SettingsRepository
import com.gash.vocab.data.repository.VocabRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GashApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: VocabRepository by lazy { VocabRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            SeedLoader.seedIfNeeded(this@GashApp, database)
        }
    }
}
