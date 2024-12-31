package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.utilities.RemoteUtils

fun CoverInfo.key(): String = RemoteUtils.sha1("${artist}_$album")
