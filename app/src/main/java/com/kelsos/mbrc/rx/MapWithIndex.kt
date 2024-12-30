package com.kelsos.mbrc.rx

import rx.Observable
import rx.Observable.Transformer

/*
  Picked from https://github.com/davidmoten/rxjava-extras
 */
class MapWithIndex<T> : Transformer<T, MapWithIndex.Indexed<T>> {
  override fun call(source: Observable<T>): Observable<Indexed<T>> = source.zipWith(NaturalNumbers.instance()) { t, n -> Indexed(t, n!!) }

  private object Holder {
    val INSTANCE: MapWithIndex<*> = MapWithIndex<Any>()
  }

  class Indexed<out T>(
    private val value: T,
    private val index: Long,
  ) {
    override fun toString(): String = "$index -> $value"

    fun index(): Long = index

    fun value(): T = value
  }

  private class NaturalNumbers : Iterable<Long> {
    override fun iterator(): Iterator<Long> =
      object : Iterator<Long> {
        private var n: Long = 0

        override fun hasNext(): Boolean = true

        override fun next(): Long = n++
      }

    private object Holder {
      val INSTANCE = NaturalNumbers()
    }

    companion object {
      internal fun instance(): NaturalNumbers = Holder.INSTANCE
    }
  }

  companion object {
    @Suppress("UNCHECKED_CAST")
    fun <T> instance(): MapWithIndex<T> = Holder.INSTANCE as MapWithIndex<T>
  }
}
