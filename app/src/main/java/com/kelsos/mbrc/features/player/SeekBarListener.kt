package com.kelsos.mbrc.features.player

import android.widget.SeekBar
import timber.log.Timber

fun SeekBar.listen(onSeek: (Int) -> Unit) {
  setOnSeekBarChangeListener(
    object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(
        seekBar: SeekBar?,
        progress: Int,
        fromUser: Boolean,
      ) {
        if (fromUser) onSeek(progress)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
        Timber.d("onStartTrackingTouch")
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
        Timber.d("onStopTrackingTouch")
      }
    },
  )
}

fun SeekBar.removeListener() {
  setOnSeekBarChangeListener(null)
}
