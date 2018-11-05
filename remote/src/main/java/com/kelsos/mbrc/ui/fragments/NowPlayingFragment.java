package com.kelsos.mbrc.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kelsos.mbrc.R;
import com.kelsos.mbrc.adapters.NowPlayingAdapter;
import com.kelsos.mbrc.constants.Protocol;
import com.kelsos.mbrc.constants.ProtocolEventType;
import com.kelsos.mbrc.data.MusicTrack;
import com.kelsos.mbrc.data.UserAction;
import com.kelsos.mbrc.events.MessageEvent;
import com.kelsos.mbrc.events.ui.NowPlayingListAvailable;
import com.kelsos.mbrc.events.ui.TrackInfoChange;
import com.kelsos.mbrc.events.ui.TrackMoved;
import com.kelsos.mbrc.events.ui.TrackRemoval;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.HashMap;
import java.util.Map;
import roboguice.RoboGuice;

public class NowPlayingFragment extends ListFragment implements SearchView.OnQueryTextListener {
  public int dragStartMode = DragSortController.ON_DOWN;
  public boolean removeEnabled = true;
  public int removeMode = DragSortController.FLING_REMOVE;
  public boolean sortEnabled = true;
  public boolean dragEnabled = true;
  @Inject Injector injector;
  @Inject private Bus bus;
  private NowPlayingAdapter adapter;
  private SearchView mSearchView;
  private MenuItem mSearchItem;
  private MusicTrack mTrack;
  private DragSortListView mDslv;
  private DragSortController mController;
  private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
    @Override public void drop(int from, int to) {
      if (from != to) {
        mTrack = adapter.getItem(from);
        adapter.remove(mTrack);
        adapter.insert(mTrack, to);
        adapter.notifyDataSetChanged();

        adapter.setPlayingTrackIndex(calculateNewIndex(from, to, adapter.getPlayingTrackIndex()));

        Map<String, Integer> move = new HashMap<>();
        move.put("from", from);
        move.put("to", to);
        bus.post(new MessageEvent(ProtocolEventType.UserAction,
            new UserAction(Protocol.NowPlayingListMove, move)));
      }
    }
  };
  private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
    @Override public void remove(int which) {
      mTrack = adapter.getItem(which);
      adapter.remove(mTrack);
      adapter.notifyDataSetChanged();
      bus.post(new MessageEvent(ProtocolEventType.UserAction,
          new UserAction(Protocol.NowPlayingListRemove, which)));
    }
  };

  public DragSortController buildController(DragSortListView dslv) {
    // defaults are
    // dragStartMode = onDown
    // removeMode = flingRight
    DragSortController controller = new DragSortController(dslv);
    controller.setDragHandleId(R.id.drag_handle);
    controller.setRemoveEnabled(removeEnabled);
    controller.setSortEnabled(sortEnabled);
    controller.setDragInitMode(dragStartMode);
    controller.setRemoveMode(removeMode);
    return controller;
  }

  @Subscribe public void handleNowPlayingListAvailable(NowPlayingListAvailable event) {
    adapter = new NowPlayingAdapter(getActivity(), R.layout.ui_list_track_item, event.getList());
    setListAdapter(adapter);
    adapter.setPlayingTrackIndex(event.getIndex());
    this.getListView().setSelection(event.getIndex());
  }

  @Subscribe public void handlePlayingTrackChange(TrackInfoChange event) {
    if (adapter == null || !adapter.getClass().equals(NowPlayingAdapter.class)) {
      return;
    }
    adapter.setPlayingTrackIndex(
        adapter.getPosition(new MusicTrack(event.getArtist(), event.getTitle())));
    adapter.notifyDataSetChanged();
  }

  public boolean onQueryTextSubmit(String query) {
    bus.post(new MessageEvent(ProtocolEventType.UserAction,
        new UserAction(Protocol.NowPlayingListSearch, query.trim())));
    mSearchView.setIconified(true);
    MenuItemCompat.collapseActionView(mSearchItem);
    return false;
  }

  public boolean onQueryTextChange(String newText) {
    return true;
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_now_playing, menu);
    mSearchItem = menu.findItem(R.id.now_playing_search_item);
    mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
    mSearchView.setQueryHint(getString(R.string.now_playing_search_hint));
    mSearchView.setIconifiedByDefault(true);
    mSearchView.setOnQueryTextListener(this);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RoboGuice.getInjector(getContext()).injectMembers(this);
    setHasOptionsMenu(true);
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mDslv = (DragSortListView) getListView();
    mDslv.setDropListener(onDrop);
    mDslv.setRemoveListener(onRemove);
    injector.injectMembers(getListView());
  }

  @Override public void onStart() {
    super.onStart();
    bus.post(new MessageEvent(ProtocolEventType.UserAction,
        new UserAction(Protocol.NowPlayingList, true)));
  }

  @Override public void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override public void onPause() {
    super.onPause();
    bus.unregister(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View mView = inflater.inflate(R.layout.ui_fragment_nowplaying, container, false);
    mDslv = (DragSortListView) mView.findViewById(android.R.id.list);
    mController = buildController(mDslv);
    mDslv.setFloatViewManager(mController);
    mDslv.setOnTouchListener(mController);
    mDslv.setDragEnabled(dragEnabled);
    return mView;
  }

  @Override public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    adapter.setPlayingTrackIndex(position);
    adapter.notifyDataSetChanged();
    bus.post(new MessageEvent(ProtocolEventType.UserAction,
        new UserAction(Protocol.NowPlayingListPlay, position + 1)));
  }

  private int calculateNewIndex(int from, int to, int index) {
    int dist = Math.abs(from - to);
    if (dist == 1 && index == from
        || dist > 1 && from > to && index == from
        || dist > 1 && from < to && index == from) {
      index = to;
    } else if (dist == 1 && index == to) {
      index = from;
    } else if (dist > 1 && from > to && index == to || from > index && to < index) {
      index += 1;
    } else if (dist > 1 && from < to && index == to || from < index && to > index) {
      index -= 1;
    }
    return index;
  }

  @Subscribe public void handleTrackMoved(TrackMoved event) {
    // In case the action failed revert the change
    if (!event.isSuccess()) {
      mTrack = adapter.getItem(event.getTo());
      adapter.remove(mTrack);
      adapter.insert(mTrack, event.getFrom());
    }
  }

  @Subscribe public void handleTrackRemoval(TrackRemoval event) {
    // In case the action failed revert the change
    if (!event.isSuccess()) {
      adapter.insert(mTrack, event.getIndex());
    }
  }
}
