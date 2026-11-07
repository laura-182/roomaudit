package com.roomcheck.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.roomcheck.app.RoomCheckApp
import com.roomcheck.app.data.Repository

/** Builds a ViewModel factory that hands the app's [Repository] and nav arguments to the ViewModel. */
inline fun <reified VM : ViewModel> repoFactory(
    crossinline create: (Repository, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoomCheckApp
        create(app.repository, createSavedStateHandle())
    }
}

