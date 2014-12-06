package com.kelsos.mbrc.events;

import com.kelsos.mbrc.data.ConnectionSettings;
import com.kelsos.mbrc.events.actions.ButtonPressedEvent;
import com.kelsos.mbrc.events.ui.CoverAvailable;
import com.kelsos.mbrc.events.ui.DiscoveryStatus;
import com.kelsos.mbrc.events.ui.TrackInfoChange;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class Events {
    public static PublishSubject<MessageEvent> Messages = PublishSubject.create();
    public static BehaviorSubject<CoverAvailable> CoverAvailableNotification = BehaviorSubject.create();
    public static BehaviorSubject<TrackInfoChange> TrackInfoChangeNotification = BehaviorSubject.create();
    public static PublishSubject<ConnectionSettings> ConnectionSettingsNotification = PublishSubject.create();
    public static PublishSubject<DiscoveryStatus> DiscoveryStatusNotification = PublishSubject.create();
    public static PublishSubject<ButtonPressedEvent> ButtonPressedNotification = PublishSubject.create();
}
