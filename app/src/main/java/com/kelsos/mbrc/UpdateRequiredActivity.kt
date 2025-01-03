package com.kelsos.mbrc

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

class UpdateRequiredActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
    findViewById<View>(android.R.id.content).transitionName = "shared_element_container"
    setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    window.sharedElementEnterTransition =
      MaterialContainerTransform().apply {
        addTarget(android.R.id.content)
        duration = ENTER_DURATION
      }
    window.sharedElementReturnTransition =
      MaterialContainerTransform().apply {
        addTarget(android.R.id.content)
        duration = RETURN_DURATION
      }
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_update_required)
    val version = intent.getStringExtra(VERSION)
    val text = getString(R.string.plugin_update__description, version)
    findViewById<TextView>(R.id.main_update_text).text = text
    findViewById<Button>(R.id.main_update_ok).setOnClickListener {
      finish()
    }
  }

  companion object {
    const val VERSION: String = "version"
    const val ENTER_DURATION = 300L
    const val RETURN_DURATION = 250L
  }
}
