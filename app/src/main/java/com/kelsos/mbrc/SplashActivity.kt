package com.kelsos.mbrc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.kelsos.mbrc.features.player.PlayerActivity
import toothpick.Toothpick

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)
    Toothpick.openScope(application)
    val handler = Handler(Looper.getMainLooper())

    handler.postDelayed({
      val intent = Intent(this, PlayerActivity::class.java)
      startActivity(intent)
    }, 1500)
  }
}
