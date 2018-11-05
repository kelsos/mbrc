package com.kelsos.mbrc.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.inject.Inject;
import com.kelsos.mbrc.R;
import com.kelsos.mbrc.adapters.DrawerAdapter;
import com.kelsos.mbrc.constants.UserInputEventType;
import com.kelsos.mbrc.controller.RemoteService;
import com.kelsos.mbrc.data.NavigationEntry;
import com.kelsos.mbrc.enums.DisplayFragment;
import com.kelsos.mbrc.events.MessageEvent;
import com.kelsos.mbrc.events.ui.ConnectionStatusChange;
import com.kelsos.mbrc.events.ui.DrawerEvent;
import com.kelsos.mbrc.ui.activities.FeedbackActivity;
import com.kelsos.mbrc.ui.activities.SettingsActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import roboguice.fragment.RoboListFragment;

public class DrawerFragment extends RoboListFragment
    implements FragmentManager.OnBackStackChangedListener {

  private final ArrayList<NavigationEntry> mNavigation;
  @Inject Bus bus;
  @Bind(R.id.menu_connect) TextView connectText;
  @Bind(R.id.menu_exit) TextView exitText;
  @Bind(R.id.menu_help) TextView helpText;
  @Bind(R.id.menu_settings) TextView settingsText;
  @Bind(R.id.menu_feedback) TextView feedbackText;

  private Typeface robotoMedium;
  private DrawerLayout mDrawerLayout;
  private int mSelection;
  private boolean mBackstackChanging;

  public DrawerFragment() {
    mNavigation = new ArrayList<>();
    mNavigation.add(new NavigationEntry(R.string.menu_home, R.drawable.ic_home));
    mNavigation.add(new NavigationEntry(R.string.menu_search, R.drawable.ic_search));
    mNavigation.add(new NavigationEntry(R.string.menu_now_playing, R.drawable.ic_view_list));
    mNavigation.add(new NavigationEntry(R.string.menu_lyrics, R.drawable.ic_view_headline));
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    robotoMedium = Typeface.createFromAsset(getActivity().getAssets(), "fonts/roboto_medium.ttf");
    mSelection = 0;
    mBackstackChanging = false;

    if (savedInstanceState != null) {
      mSelection = savedInstanceState.getInt("mSelection");
    }
    getActivity().getSupportFragmentManager().addOnBackStackChangedListener(this);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.ui_fragment_drawer, container, false);
    final ListView list = (ListView) view.findViewById(android.R.id.list);
    final View footer = inflater.inflate(R.layout.ui_drawer_footer, list, false);
    list.addFooterView(footer, null, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override public void onStart() {
    super.onStart();
    bus.register(this);

    connectText.setTypeface(robotoMedium);
    helpText.setTypeface(robotoMedium);
    exitText.setTypeface(robotoMedium);
    settingsText.setTypeface(robotoMedium);
    feedbackText.setTypeface(robotoMedium);

    setListAdapter(new DrawerAdapter(getActivity(), R.layout.ui_drawer_item, mNavigation));
    getListView().setOnItemClickListener(new DrawerOnClickListener());
    getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    getListView().setItemChecked(mSelection, true);
    getActivity().setTitle(mNavigation.get(mSelection).getTitleId());

    if (mDrawerLayout == null) {
      mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
    }
  }

  @OnLongClick(R.id.connect_layout) public boolean onConnectLongClick(View view) {
    bus.post(new MessageEvent(UserInputEventType.ResetConnection));
    return false;
  }

  @OnClick(R.id.connect_layout) public void onClick(View v) {
    bus.post(new MessageEvent(UserInputEventType.StartConnection));
  }

  @OnClick(R.id.settings_layout) public void onSettingsClicked(View v) {
    bus.post(new DrawerEvent());
    startActivity(new Intent(getActivity(), SettingsActivity.class));
  }

  @OnClick(R.id.help_layout) public void onHelpClicked(View v) {
    bus.post(new DrawerEvent());
    Intent openHelp = new Intent(Intent.ACTION_VIEW);
    openHelp.setData(Uri.parse("http://kelsos.net/musicbeeremote/help/"));
    startActivity(openHelp);
  }

  @OnClick(R.id.exit_layout) public void onExitClicked(View v) {
    final Activity activity = getActivity();
    activity.stopService(new Intent(activity, RemoteService.class));
    activity.finish();
  }

  @OnClick(R.id.feedback_layout) public void onFeedbackClicked(View v) {
    bus.post(new DrawerEvent());
    final Activity activity = getActivity();
    activity.startActivity(new Intent(activity, FeedbackActivity.class));
  }

  @Override public void onStop() {
    super.onStop();
    bus.unregister(this);
  }

  @Subscribe public void handleConnectionStatusChange(final ConnectionStatusChange change) {
    if (connectText == null) {
      return;
    }
    switch (change.getStatus()) {
      case CONNECTION_OFF:
        connectText.setText(R.string.drawer_connection_status_off);
        break;
      case CONNECTION_ON:
        connectText.setText(R.string.drawer_connection_status_on);
        break;
      case CONNECTION_ACTIVE:
        connectText.setText(R.string.drawer_connection_status_active);
        break;
      default:
        connectText.setText(R.string.drawer_connection_status_off);
        break;
    }
  }

  @Override public void onBackStackChanged() {
    if (!mBackstackChanging
        && getActivity().getSupportFragmentManager().getBackStackEntryCount() == 0) {
      mSelection = 0;
      getActivity().setTitle(mNavigation.get(mSelection).getTitleId());
      getListView().setItemChecked(mSelection, true);
    }
    mBackstackChanging = false;
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putInt("mSelection", mSelection);
    super.onSaveInstanceState(outState);
  }

  private class DrawerOnClickListener implements ListView.OnItemClickListener {

    @Override public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
      mBackstackChanging = true;
      DrawerEvent dEvent;
      if (mSelection != i) {

        getListView().setItemChecked(i, true);
        getActivity().setTitle(mNavigation.get(i).getTitleId());
        mSelection = i;

        DisplayFragment dfrag = DisplayFragment.values()[i];
        dEvent = new DrawerEvent(dfrag);
      } else {
        dEvent = new DrawerEvent();
      }

      bus.post(dEvent);
    }
  }
}
