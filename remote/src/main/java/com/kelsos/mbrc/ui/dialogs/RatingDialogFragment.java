package com.kelsos.mbrc.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.RatingBar;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.inject.Inject;
import com.kelsos.mbrc.R;
import com.kelsos.mbrc.constants.Protocol;
import com.kelsos.mbrc.constants.ProtocolEventType;
import com.kelsos.mbrc.data.UserAction;
import com.kelsos.mbrc.events.MessageEvent;
import com.kelsos.mbrc.events.ui.RatingChanged;
import com.kelsos.mbrc.utilities.MainThreadBusWrapper;
import com.squareup.otto.Subscribe;
import roboguice.RoboGuice;

public class RatingDialogFragment extends DialogFragment {

  @Inject private MainThreadBusWrapper bus;
  private RatingBar mRatingBar;
  private float mRating;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RoboGuice.getInjector(getContext()).injectMembers(this);
    bus.register(this);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  @Subscribe public void handleRatingChange(RatingChanged event) {
    mRating = event.getRating();
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
    builder.customView(R.layout.ui_dialog_rating, false);
    final MaterialDialog dialog = builder.build();
    mRatingBar = (RatingBar) dialog.getCustomView().findViewById(R.id.ratingBar);
    mRatingBar.setOnRatingBarChangeListener((ratingBar, ratingValue, isUserInitiated) -> {
      if (isUserInitiated) {
        bus.post(new MessageEvent(ProtocolEventType.UserAction,
            new UserAction(Protocol.NowPlayingRating, ratingValue)));
      }
    });
    mRatingBar.setRating(mRating);

    return dialog;
  }
}
