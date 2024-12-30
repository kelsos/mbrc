package com.kelsos.mbrc.rx

import rx.Completable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

object RxUtils {
  /**
   * [rx.Observable.Transformer] that transforms the source observable to subscribe in the
   * io thread and observe on the Android's UI thread.
   */
  private var ioMainTransformer: Observable.Transformer<*, *>
  private var uiCompletableScheduler: Completable.Transformer

  init {
    ioMainTransformer = createIoToMain<Any>()
    uiCompletableScheduler = createUiCompletableScheduler()
  }

  private fun createUiCompletableScheduler(): Completable.Transformer {
    return Completable.Transformer { it ->
      it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }
  }

  /**
   * Get [rx.Observable.Transformer] that transforms the source observable to subscribe in
   * the io thread and observe on the Android's UI thread.

   * Because it doesn't interact with the emitted items it's safe ignore the unchecked casts.

   * @return [rx.Observable.Transformer]
   */
  @SuppressWarnings("unchecked")
  private fun <T> createIoToMain(): Observable.Transformer<T, T> {
    return Observable.Transformer { tObservable ->
      tObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> ioToMain(): Observable.Transformer<T, T> {
    return ioMainTransformer as Observable.Transformer<T, T>
  }

  fun uiTask(): Completable.Transformer {
    return uiCompletableScheduler
  }
}
