package com.kelsos.mbrc.di.modules;

import android.support.v4.app.NotificationManagerCompat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelsos.mbrc.di.providers.NotificationManagerCompatProvider;
import com.kelsos.mbrc.events.bus.RxBus;
import com.kelsos.mbrc.events.bus.RxBusImpl;
import com.kelsos.mbrc.services.LibraryService;
import com.kelsos.mbrc.services.LibraryServiceImpl;
import com.kelsos.mbrc.services.NowPlayingService;
import com.kelsos.mbrc.services.NowPlayingServiceImpl;
import toothpick.config.Module;

public class RemoteModule extends Module {
  public RemoteModule() {
    bind(RxBus.class).toInstance(new RxBusImpl());
    bind(ObjectMapper.class).toInstance(new ObjectMapper());
    bind(LibraryService.class).to(LibraryServiceImpl.class);
    bind(NowPlayingService.class).to(NowPlayingServiceImpl.class);
    bind(NotificationManagerCompat.class).toProvider(NotificationManagerCompatProvider.class);
  }
}
