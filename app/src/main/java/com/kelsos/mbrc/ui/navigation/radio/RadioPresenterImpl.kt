package com.kelsos.mbrc.ui.navigation.radio

import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.RadioRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@RadioActivity.Presenter
class RadioPresenterImpl
@Inject
constructor(
  private val radioRepository: RadioRepository,
  private val queue: QueueHandler,
  dispatcher: AppDispatchers
) : BasePresenter<RadioView>(dispatcher.main), RadioPresenter {

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
