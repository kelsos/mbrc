package com.kelsos.mbrc.events.ui;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

public class CoverAvailable {
  private boolean available;
  private Bitmap cover;

  public CoverAvailable(Bitmap cover) {
    this.available = true;
    this.cover = cover;
  }

  public CoverAvailable() {
    this.available = false;
    this.cover = null;
  }

  public boolean isAvailable() {
    return this.available;
  }

  @Nullable
  public Bitmap getCover() {
    return this.cover;
  }
}
