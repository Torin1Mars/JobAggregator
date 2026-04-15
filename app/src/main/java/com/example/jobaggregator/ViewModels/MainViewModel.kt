package com.example.jobaggregator.ViewModels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext context: Context): ViewModel() {


    val ok = Toast.makeText(context, "Ok", Toast.LENGTH_SHORT).show()
}
