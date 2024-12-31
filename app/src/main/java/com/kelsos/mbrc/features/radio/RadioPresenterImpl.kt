package com.kelsos.mbrc.features.radio

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@RadioActivity.Presenter
class RadioPresenterImpl
  @Inject
  constructor(
    private val radioRepository: RadioRepository,
    private val queue: QueueHandler,
    dispatcher: AppCoroutineDispatchers,
  ) : BasePresenter<RadioView>(dispatcher.main),
    RadioPresenter {
    override fun load() {
      view?.showLoading()
      scope.launch {
        try {
          view?.update(radioRepository.getAndSaveRemote())
        } catch (e: Exception) {
          view?.error(e)
        }
        view?.hideLoading()
      }
    }

    override fun refresh() {
      view?.showLoading()
      scope.launch {
        try {
          view?.update(radioRepository.getAndSaveRemote())
        } catch (e: Exception) {
          view?.error(e)
        }
        view?.hideLoading()
      }
    }

    override fun play(path: String) {
      scope.launch {
        val (success, _) = queue.queuePath(path)
        if (success) {
          view?.radioPlaySuccessful()
        } else {
          view?.radioPlayFailed()
        }
      }
    }
  }
