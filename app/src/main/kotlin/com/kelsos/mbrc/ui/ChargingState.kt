package com.kelsos.mbrc.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device is on power, as a flow.
 *
 * Driven by `ACTION_BATTERY_CHANGED` and its `EXTRA_PLUGGED` rather than by the
 * connected/disconnected pair. Those two fire only on a real transition, so a state that changed
 * while nothing was registered is missed, and `EXTRA_STATUS` is not a substitute: it still reads
 * as charging on a device whose supply has gone away. What is being asked here is whether the
 * device is on a cable, which is exactly what `EXTRA_PLUGGED` reports.
 *
 * The first value comes from the sticky broadcast, so a device already plugged in when this starts
 * collecting counts immediately rather than waiting for the next change.
 */
fun Context.chargingState(): Flow<Boolean> = callbackFlow {
  val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      trySend(intent.isPluggedIn())
    }
  }

  val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
  val sticky = registerReceiver(receiver, filter)

  trySend(sticky.isPluggedIn())

  awaitClose { unregisterReceiver(receiver) }
}.distinctUntilChanged()

internal fun Intent?.isPluggedIn(): Boolean {
  val plugged = this?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
  return plugged != 0
}
