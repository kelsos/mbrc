package com.kelsos.mbrc.changelog

import android.content.Context
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ChangelogDialog {
  fun show(
    context: Context,
    @RawRes resId: Int,
  ): AlertDialog {
    val parser = ChangelogParser(context)
    val dialog =
      MaterialAlertDialogBuilder(context)
        .setTitle(R.string.changelog_dialog__title)
        .setView(R.layout.changelog_dialog__layout)
        .setPositiveButton(android.R.string.ok) { materialDialog, _ -> materialDialog.dismiss() }
        .show()

    val log = parser.changelog(resId)
    val changelog =
      dialog.findViewById<RecyclerView>(R.id.changelog) ?: error("could not find the changelog")
    changelog.layoutManager = LinearLayoutManager(context)
    changelog.adapter = VersionAdapter(log)
    return dialog
  }
}
