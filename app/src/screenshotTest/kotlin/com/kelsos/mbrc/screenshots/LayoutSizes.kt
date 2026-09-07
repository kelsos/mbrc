package com.kelsos.mbrc.screenshots

import androidx.compose.ui.tooling.preview.Preview

/**
 * The size matrix for screens whose layout changes with available width.
 *
 * The phone entry keeps the 360x720 spec the suite already used, so applying this annotation to an
 * existing preview leaves its reference image unchanged and only adds the two wider ones.
 *
 * Components that look the same at every width should not use this - an extra 1280dp render of a
 * rating bar costs a reference image and proves nothing.
 */
@Preview(name = "phone", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(name = "foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Preview(name = "tablet", showBackground = true, widthDp = 1280, heightDp = 800)
annotation class LayoutSizes

/**
 * [LayoutSizes] plus a tablet in portrait, for screens where the portrait tablet case is a distinct
 * layout rather than a wider version of the landscape one.
 */
@Preview(name = "phone", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(name = "foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Preview(name = "tablet", showBackground = true, widthDp = 1280, heightDp = 800)
@Preview(name = "tablet portrait", showBackground = true, widthDp = 800, heightDp = 1280)
annotation class LayoutSizesWithPortraitTablet
