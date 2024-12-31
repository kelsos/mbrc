package com.kelsos.mbrc.events.bus

import com.jakewharton.rxrelay.PublishRelay
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.LinkedList
import javax.inject.Inject

class RxBusImpl
  @Inject
  constructor() : RxBus {
    init {
      Timber.v("Injecting RxBus instance %s", this)
    }

    private val serializedRelay = PublishRelay.create<Any>().toSerialized()
    private val activeSubscriptions = HashMap<Any, MutableList<Subscription>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> register(
      receiver: Any,
      eventClass: Class<T>,
      onNext: (T) -> Unit,
    ) {
      //noinspection unchecked
      val subscription =
        serializedRelay
          .filter {
            it.javaClass == eventClass
          }.map { obj -> obj as T }
          .subscribe(onNext)

      updateSubscriptions(receiver, subscription)
    }

    override fun <T> register(
      receiver: Any,
      eventClass: Class<T>,
      onNext: (T) -> Unit,
      main: Boolean,
    ) {
      val subscription = register(eventClass, onNext, true)
      updateSubscriptions(receiver, subscription)
    }

    private fun updateSubscriptions(
      receiver: Any,
      subscription: Subscription,
    ) {
      val subscriptions: MutableList<Subscription> =
        activeSubscriptions[receiver] ?: LinkedList<Subscription>()
      subscriptions.add(subscription)
      activeSubscriptions[receiver] = subscriptions
    }

    override fun unregister(receiver: Any) {
      val subscriptions = activeSubscriptions.remove(receiver)
      if (subscriptions != null) {
        Observable.from(subscriptions).filter { !it.isUnsubscribed }.subscribe { it.unsubscribe() }
      }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> register(
      eventClass: Class<T>,
      onNext: (T) -> Unit,
      main: Boolean,
    ): Subscription {
      //noinspection unchecked
      val observable = serializedRelay.filter { it.javaClass == eventClass }.map { obj -> obj as T }
      val scheduler = if (main) AndroidSchedulers.mainThread() else Schedulers.immediate()
      return observable.onBackpressureBuffer().observeOn(scheduler).subscribe(onNext)
    }

    override fun post(event: Any) {
      serializedRelay.call(event)
    }
  }
