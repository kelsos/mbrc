package com.kelsos.mbrc.content.library.genres

import android.arch.paging.DataSource
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class GenreRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: RemoteGenreDataSource,
    private val dao: GenreDao
) : GenreRepository {

  private val mapper = GenreDtoMapper()

  override fun getAll(): Single<DataSource.Factory<Int, GenreEntity>> {
    return Single.just(dao.getAll())
  }

  override fun getAndSaveRemote(): Single<DataSource.Factory<Int, GenreEntity>> {
    return getRemote().andThen(getAll())
  }

  override fun getRemote(): Completable {
    dao.deleteAll()
    return remoteDataSource.fetch().doOnNext {
      dao.saveAll(it.map { mapper.map(it) })
    }.ignoreElements()
  }

  override fun search(term: String): Single<DataSource.Factory<Int, GenreEntity>> {
    return Single.just(dao.search(term))
  }

  override fun cacheIsEmpty(): Single<Boolean> = Single.just(dao.count() == 0L)
}