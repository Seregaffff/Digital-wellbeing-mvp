package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.preferences.ProtectionPreferences
import com.example.myapplication.data.repository.GamificationRepository
import com.example.myapplication.data.repository.SavedTimeRepository
import com.example.myapplication.data.repository.SavingsRepository
import com.example.myapplication.data.repository.UsageRepository

class HomeViewModelFactory(
    private val repository: UsageRepository,
    private val savedTimeRepository: SavedTimeRepository,
    private val gamificationRepository: GamificationRepository,
    private val savingsRepository: SavingsRepository,
    private val protectionPreferences: ProtectionPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                repository = repository,
                savedTimeRepository = savedTimeRepository,
                gamificationRepository = gamificationRepository,
                savingsRepository = savingsRepository,
                protectionPreferences = protectionPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
