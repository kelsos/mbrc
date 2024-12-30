package com.kelsos.mbrc.ui.navigation.library

/**
 * The class is responsible for the library metadata and playlist data sync.
 */
interface LibrarySyncInteractor {
  /**
   * Starts the sync process for the library and playlist metadata. The sync can be
   * either manual or automatic. The automatic sync should happen only under certain
   * conditions.
   *
   * @param auto Marks the sync process as automatic (initiated by conditions) or
   * manual (initiated by the user)
   */
  fun sync(auto: Boolean = false)

  /**
   * Provides access to the interactor's current status.
   *
   * @return Should return true if the interactor is active and running, or false if not
   */
  fun isRunning(): Boolean

  fun setOnCompleteListener(onCompleteListener: OnCompleteListener?)

  fun setOnStartListener(onStartListener: OnStartListener?)

  suspend fun syncStats(): LibraryStats

  interface OnCompleteListener {
    fun onTermination()

    fun onFailure(throwable: Throwable)

    fun onSuccess(stats: LibraryStats)
  }

  interface OnStartListener {
    fun onStart()
  }
}
