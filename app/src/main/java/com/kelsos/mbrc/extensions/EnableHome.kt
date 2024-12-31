package com.kelsos.mbrc.extensions

import androidx.appcompat.app.ActionBar

fun ActionBar.enableHome(title: String?) {
  this.setDisplayHomeAsUpEnabled(true)
  this.setDisplayShowHomeEnabled(true)
  this.title = title ?: ""
}
