package com.kelsos.mbrc.repository.data

import com.raizlabs.android.dbflow.list.FlowCursorList
import com.raizlabs.android.dbflow.structure.Model
import io.reactivex.Observable
import io.reactivex.Single

interface LocalDataSource<T : Model> {
  fun deleteAll()
  fun saveAll(list: List<T>)
  fun loadAllCursor(): Observable<FlowCursorList<T>>
  fun search(term: String): Single<FlowCursorList<T>>
  fun isEmpty(): Single<Boolean>
}
