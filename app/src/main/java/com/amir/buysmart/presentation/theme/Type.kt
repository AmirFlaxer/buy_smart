package com.amir.buysmart.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.amir.buysmart.R

// ───────────────────────── BuySmart - טיפוגרפיה ─────────────────────────
// גופן Rubik (variable) - עברית איכותית ואופי מודרני, במקום Roboto ברירת המחדל.
// משקלים נגזרים מציר ה-wght של הגופן הווריאבילי.

@OptIn(ExperimentalTextApi::class)
private fun rubik(weight: Int) = Font(
    R.font.rubik_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val RubikFamily = FontFamily(
    rubik(300),
    rubik(400),
    rubik(500),
    rubik(600),
    rubik(700),
)

private val base = Typography()

/** Typography מלא של Material 3, עם Rubik כגופן לכל הסגנונות. */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = RubikFamily),
    displayMedium = base.displayMedium.copy(fontFamily = RubikFamily),
    displaySmall = base.displaySmall.copy(fontFamily = RubikFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = RubikFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = RubikFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = RubikFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = RubikFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = RubikFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = RubikFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = RubikFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = RubikFamily),
    bodySmall = base.bodySmall.copy(fontFamily = RubikFamily),
    labelLarge = base.labelLarge.copy(fontFamily = RubikFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = RubikFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = RubikFamily, fontWeight = FontWeight.Medium),
)
