package com.sipun.netspeedindicator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sipun.netspeedindicator.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.8.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 21.6.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.2.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.2.sp, letterSpacing = 0.2.sp),
    titleMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 25.2.sp, letterSpacing = 0.2.sp),
    titleSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 25.2.sp, letterSpacing = 0.2.sp),
    bodyLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.4.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.6.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.6.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp)
)