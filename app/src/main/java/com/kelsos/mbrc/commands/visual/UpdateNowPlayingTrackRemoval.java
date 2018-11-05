package com.kelsos.mbrc.commands.visual;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.kelsos.mbrc.events.ui.TrackRemoval;
import com.kelsos.mbrc.interfaces.ICommand;
import com.kelsos.mbrc.interfaces.IEvent;
import com.kelsos.mbrc.utilities.MainThreadBusWrapper;

public class UpdateNowPlayingTrackRemoval implements ICommand {
  private MainThreadBusWrapper bus;

  @Inject public UpdateNowPlayingTrackRemoval(MainThreadBusWrapper bus) {
    this.bus = bus;
  }

  @Override public void execute(final IEvent e) {
    e.getData();
    bus.post(new TrackRemoval((ObjectNode) e.getData()));
  }
}
