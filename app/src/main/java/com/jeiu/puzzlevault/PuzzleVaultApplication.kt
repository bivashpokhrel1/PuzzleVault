package com.jeiu.puzzlevault

import android.app.Application
import com.jeiu.puzzlevault.data.local.LocalLevelStorage
import com.jeiu.puzzlevault.data.remote.HermesApiClient
import com.jeiu.puzzlevault.data.remote.HermesRepository
import com.jeiu.puzzlevault.data.remote.HermesRepositoryImpl

class PuzzleVaultApplication : Application() {

    lateinit var mockHermesService: MockHermesService
        private set

    lateinit var hermesRepository: HermesRepository
        private set

    lateinit var levelStorage: LocalLevelStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        mockHermesService = MockHermesService()
        hermesRepository = HermesRepositoryImpl(HermesApiClient.api, mockHermesService)
        levelStorage = LocalLevelStorage(this)
        HermesApiClient.init(this)
    }

    companion object {
        lateinit var instance: PuzzleVaultApplication
            private set
    }
}
