package com.kelsos.mbrc.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kelsos.mbrc.annotations.Repeat;
import com.kelsos.mbrc.annotations.Repeat.Mode;
import com.kelsos.mbrc.constants.Const;
import com.kelsos.mbrc.constants.Protocol;
import com.kelsos.mbrc.constants.ProtocolEventType;
import com.kelsos.mbrc.constants.UserInputEventType;
import com.kelsos.mbrc.data.MusicTrack;
import com.kelsos.mbrc.data.Playlist;
import com.kelsos.mbrc.enums.ConnectionStatus;
import com.kelsos.mbrc.enums.LfmStatus;
import com.kelsos.mbrc.enums.PlayState;
import com.kelsos.mbrc.events.MessageEvent;
import com.kelsos.mbrc.events.ui.ConnectionStatusChange;
import com.kelsos.mbrc.events.ui.CoverAvailable;
import com.kelsos.mbrc.events.ui.LfmRatingChanged;
import com.kelsos.mbrc.events.ui.LyricsUpdated;
import com.kelsos.mbrc.events.ui.NotificationDataAvailable;
import com.kelsos.mbrc.events.ui.NowPlayingListAvailable;
import com.kelsos.mbrc.events.ui.OnMainFragmentOptionsInflated;
import com.kelsos.mbrc.events.ui.PlayStateChange;
import com.kelsos.mbrc.events.ui.PlaylistAvailable;
import com.kelsos.mbrc.events.ui.RatingChanged;
import com.kelsos.mbrc.events.ui.RemoteClientMetaData;
import com.kelsos.mbrc.events.ui.RepeatChange;
import com.kelsos.mbrc.events.ui.ScrobbleChange;
import com.kelsos.mbrc.events.ui.ShuffleChange;
import com.kelsos.mbrc.events.ui.TrackInfoChange;
import com.kelsos.mbrc.events.ui.VolumeChange;
import com.kelsos.mbrc.utilities.MainThreadBusWrapper;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import static com.kelsos.mbrc.events.ui.ShuffleChange.OFF;
import static com.kelsos.mbrc.events.ui.ShuffleChange.ShuffleState;

@Singleton
public class MainDataModel {

  private MainThreadBusWrapper bus;
  private float rating;
  private String title;
  private String artist;
  private String album;
  private String year;
  private String lyrics;
  private int volume;
  private Bitmap cover;
  private boolean connectionActive;
  private boolean isHandShakeDone;
  private String mShuffleState;
  private boolean isScrobblingActive;
  private boolean isMuteActive;
  private PlayState playState;
  private ArrayList<MusicTrack> nowPlayingList;
  private LfmStatus lfmRating;
  private String pluginVersion;
  private double pluginProtocol;
  private List<Playlist> playlists;

  @Mode
  private String repeatMode;

  @Inject
  public MainDataModel(MainThreadBusWrapper bus) {
    this.bus = bus;
    bus.register(this);
    repeatMode = Repeat.NONE;

    title = artist = album = year = Const.EMPTY;
    volume = 100;

    connectionActive = false;
    isHandShakeDone = false;
    mShuffleState = OFF;
    isScrobblingActive = false;
    isMuteActive = false;
    playState = PlayState.Stopped;
    cover = null;
    rating = 0;
    lyrics = Const.EMPTY;

    nowPlayingList = new ArrayList<>();
    lfmRating = LfmStatus.NORMAL;
    pluginVersion = Const.EMPTY;
  }

  public void setLfmRating(String rating) {
    switch (rating) {
      case "Love":
        lfmRating = LfmStatus.LOVED;
        break;
      case "Ban":
        lfmRating = LfmStatus.BANNED;
        break;
      default:
        lfmRating = LfmStatus.NORMAL;
        break;
    }

    bus.post(new LfmRatingChanged(lfmRating));
  }

  public String getPluginVersion() {
    return pluginVersion;
  }

  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion.substring(0, pluginVersion.lastIndexOf('.'));
    bus.post(new MessageEvent(ProtocolEventType.PluginVersionCheck));
  }

  @Produce
  public LfmRatingChanged produceLfmRating() {
    return new LfmRatingChanged(lfmRating);
  }

  public void setNowPlayingList(ArrayList<MusicTrack> nowPlayingList) {
    this.nowPlayingList = nowPlayingList;
    bus.post(new NowPlayingListAvailable(nowPlayingList,
        nowPlayingList.indexOf(new MusicTrack(artist, title))));
  }

  @Produce
  public NowPlayingListAvailable produceNowPlayingListAvailable() {
    int index = nowPlayingList.indexOf(new MusicTrack(artist, title));
    return new NowPlayingListAvailable(nowPlayingList, index);
  }

  public void setRating(double rating) {
    this.rating = (float) rating;
    bus.post(new RatingChanged(this.rating));
  }

  @Produce
  public RatingChanged produceRatingChanged() {
    return new RatingChanged(this.rating);
  }

  private void updateNotification() {
    if (!connectionActive) {
      bus.post(new MessageEvent(UserInputEventType.CancelNotification));
    } else {
      bus.post(new NotificationDataAvailable(artist, title, album, cover, playState));
    }
  }

  public void setTrackInfo(String artist, String album, String title, String year) {
    this.artist = artist;
    this.album = album;
    this.year = year;
    this.title = title;
    bus.post(new TrackInfoChange(artist, title, album, year));
    updateNotification();
    updateRemoteClient();
  }

  private void updateRemoteClient() {
    bus.post(new RemoteClientMetaData(artist, title, album, cover));
  }

  @Produce
  public TrackInfoChange produceTrackInfo() {
    return new TrackInfoChange(artist, title, album, year);
  }

  public String getArtist() {
    return this.artist;
  }

  public String getTitle() {
    return this.title;
  }

  public int getVolume() {
    return this.volume;
  }

  public void setVolume(int volume) {
    if (volume != this.volume) {
      this.volume = volume;
      bus.post(new VolumeChange(this.volume));
    }
  }

  public void setCover(final String base64format) {
    if (base64format == null || Const.EMPTY.equals(base64format)) {
      cover = null;
      bus.post(new CoverAvailable());
      updateNotification();
      updateRemoteClient();
    } else {
      Observable.create((Subscriber<? super Bitmap> subscriber) -> {
        byte[] decodedImage = Base64.decode(base64format, Base64.DEFAULT);
        subscriber.onNext(BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length));
        subscriber.onCompleted();
      }).subscribeOn(Schedulers.io())
          .subscribe(this::setAlbumCover, throwable -> {
            cover = null;
            bus.post(new CoverAvailable());
          });
    }
  }

  private void setAlbumCover(Bitmap cover) {
    this.cover = cover;
    bus.post(new CoverAvailable(cover));
    updateNotification();
    updateRemoteClient();
  }

  @Produce
  public CoverAvailable produceAvailableCover() {
    return cover == null ? new CoverAvailable() : new CoverAvailable(cover);
  }

  public void setConnectionState(String connectionActive) {
    this.connectionActive = Boolean.parseBoolean(connectionActive);
    if (!this.connectionActive) {
      setPlayState(Const.STOPPED);
    }
    bus.post(new ConnectionStatusChange(
        this.connectionActive ? (isHandShakeDone ? ConnectionStatus.CONNECTION_ACTIVE
            : ConnectionStatus.CONNECTION_ON) : ConnectionStatus.CONNECTION_OFF));
  }

  public void setHandShakeDone(boolean handShakeDone) {
    this.isHandShakeDone = handShakeDone;
    bus.post(new ConnectionStatusChange(
        connectionActive ? (isHandShakeDone ? ConnectionStatus.CONNECTION_ACTIVE
            : ConnectionStatus.CONNECTION_ON) : ConnectionStatus.CONNECTION_OFF));
  }

  @Produce
  public ConnectionStatusChange produceConnectionStatus() {
    return new ConnectionStatusChange(
        connectionActive ? (isHandShakeDone ? ConnectionStatus.CONNECTION_ACTIVE
            : ConnectionStatus.CONNECTION_ON) : ConnectionStatus.CONNECTION_OFF);
  }

  public boolean isConnectionActive() {
    return connectionActive;
  }

  public void setRepeatState(String repeat) {
    if (Protocol.ALL.equalsIgnoreCase(repeat)) {
      repeatMode = Repeat.ALL;
    } else if (Protocol.ONE.equalsIgnoreCase(repeat)) {
      repeatMode = Repeat.ONE;
    } else {
      repeatMode = Repeat.NONE;
    }

    bus.post(new RepeatChange(repeatMode));
  }

  @Produce
  public RepeatChange produceRepeatChange() {
    return new RepeatChange(this.repeatMode);
  }

  public void setShuffleState(@ShuffleState String shuffleState) {
    mShuffleState = shuffleState;
    bus.post(new ShuffleChange(mShuffleState));
  }

  @Produce
  public ShuffleChange produceShuffleChange() {
    return new ShuffleChange(this.mShuffleState);
  }

  public void setScrobbleState(boolean scrobbleButtonActive) {
    isScrobblingActive = scrobbleButtonActive;
    bus.post(new ScrobbleChange(isScrobblingActive));
  }

  @Produce
  public ScrobbleChange produceScrobbleChange() {
    return new ScrobbleChange(this.isScrobblingActive);
  }

  public void setMuteState(boolean isMuteActive) {
    this.isMuteActive = isMuteActive;
    bus.post(isMuteActive ? new VolumeChange() : new VolumeChange(volume));
  }

  @Produce
  public VolumeChange produceVolumeChange() {
    return isMuteActive ? new VolumeChange() : new VolumeChange(volume);
  }

  public void setPlayState(String playState) {
    PlayState newState;
    if (Const.PLAYING.equalsIgnoreCase(playState)) {
      newState = PlayState.Playing;
    } else if (Const.STOPPED.equalsIgnoreCase(playState)) {
      newState = PlayState.Stopped;
    } else if (Const.PAUSED.equalsIgnoreCase(playState)) {
      newState = PlayState.Paused;
    } else {
      newState = PlayState.Undefined;
    }

    this.playState = newState;

    bus.post(new PlayStateChange(this.playState));
    updateNotification();
  }

  @Produce
  public PlayStateChange producePlayState() {
    if (this.playState == null) {
      playState = PlayState.Undefined;
    }
    return new PlayStateChange(this.playState);
  }

  @Produce
  public LyricsUpdated produceLyricsUpdate() {
    return new LyricsUpdated(lyrics);
  }

  public void setLyrics(String lyrics) {
    if (lyrics == null || this.lyrics.equals(lyrics)) {
      return;
    }
    this.lyrics = lyrics.replace("<p>", "\r\n")
        .replace("<br>", "\n")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
        .replace("<p>", "\r\n")
        .replace("<br>", "\n")
        .trim();
    bus.post(new LyricsUpdated(this.lyrics));
  }

  public boolean isMute() {
    return isMuteActive;
  }

  public void setPluginProtocol(double pluginProtocol) {
    this.pluginProtocol = pluginProtocol;
  }

  public double getPluginProtocol() {
    return this.pluginProtocol;
  }

  @Subscribe
  public void resendOnInflate(OnMainFragmentOptionsInflated inflated) {
    bus.post(new ScrobbleChange(isScrobblingActive));
    bus.post(new LfmRatingChanged(lfmRating));
  }

  public void setPlaylists(List<Playlist> playlists) {
    this.playlists = playlists;
    bus.post(PlaylistAvailable.create(playlists));
  }

  @Produce
  public PlaylistAvailable producePlaylistAvailable() {
    return PlaylistAvailable.create(playlists);
  }
}

