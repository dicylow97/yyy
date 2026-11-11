package com.dicylow.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Convierte la opción elegida en una familia de letra lista para usar. */
@Composable
fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.MONO -> FontFamily.Monospace
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.CURSIVE -> FontFamily.Cursive
    FontChoice.INTER -> FontFamily(Font(R.font.inter_regular), Font(R.font.inter_bold, FontWeight.Bold))
    FontChoice.DEJAVU_SANS -> FontFamily(Font(R.font.dejavu_sans))
    FontChoice.DEJAVU_SERIF -> FontFamily(Font(R.font.dejavu_serif))
    FontChoice.DEJAVU_MONO -> FontFamily(Font(R.font.dejavu_mono))
    FontChoice.FREEMONO -> FontFamily(Font(R.font.freemono))
    FontChoice.CARLITO -> FontFamily(Font(R.font.carlito))
    FontChoice.CALADEA -> FontFamily(Font(R.font.caladea))
    FontChoice.FREESANS -> FontFamily(Font(R.font.freesans))
    FontChoice.GFS_BASKERVILLE -> FontFamily(Font(R.font.gfs_baskerville))
    FontChoice.GFS_PORSON -> FontFamily(Font(R.font.gfs_porson))
}
