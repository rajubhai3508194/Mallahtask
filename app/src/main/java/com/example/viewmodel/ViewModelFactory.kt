package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.TaskMallahRepository

class ViewModelFactory(
    private val repository: TaskMallahRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskMallahViewModel(repository) as T
    }
}
