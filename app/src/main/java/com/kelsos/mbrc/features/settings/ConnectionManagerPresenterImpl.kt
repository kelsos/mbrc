package com.kelsos.mbrc.features.settings

import com.kelsos.mbrc.common.mvp.BasePresenter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ConnectionManagerPresenterImpl
  @Inject
  constructor(
    private val repository: ConnectionRepository,
  ) : BasePresenter<ConnectionManagerView>(),
    ConnectionManagerPresenter {
    override fun load() {
      checkIfAttached()
      scope.launch {
        try {
          val settings = repository.getAll()
          view?.updateModel(ConnectionModel(repository.defaultId, settings))
        } catch (e: Exception) {
          Timber.v(e, "Failure")
        }
      }
    }

    override fun setDefault(settings: ConnectionSettings) {
      checkIfAttached()
      scope.launch {
        repository.setDefault(settings)
        view?.defaultChanged()
        view?.dataUpdated()
      }
    }

    override fun save(settings: ConnectionSettings) {
      checkIfAttached()
      scope.launch {
        try {
          if (settings.id > 0) {
            repository.update(settings)
          } else {
            repository.save(settings)
          }

          if (settings.id == repository.defaultId) {
            view?.defaultChanged()
          }

          view?.dataUpdated()
        } catch (e: Exception) {
          Timber.v(e)
        }
      }
    }

    override fun delete(settings: ConnectionSettings) {
      scope.launch {
        checkIfAttached()
        repository.delete(settings)
        if (settings.id == repository.defaultId) {
          view?.defaultChanged()
        }

        view?.dataUpdated()
      }
    }
  }
