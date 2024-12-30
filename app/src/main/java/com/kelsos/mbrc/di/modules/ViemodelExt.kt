package com.kelsos.mbrc.di.modules

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import toothpick.Toothpick

fun <T : ViewModel> Fragment.obtainViewModel(viewModelClass: Class<T>) =
  ViewModelProvider(this, Toothpick.openScope(requireActivity().application)
    .getInstance(ViewModelFactory::class.java))
    .get(viewModelClass)
