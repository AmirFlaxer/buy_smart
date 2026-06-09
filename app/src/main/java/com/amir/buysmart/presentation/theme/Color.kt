package com.amir.buysmart.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.amir.buysmart.domain.model.ItemPriority

// ───────────────────────── BuySmart - פלטת צבעים ─────────────────────────
// ערכה מלאה הנגזרת מהמותג הירוק (#2E7D32), כדי שאף תפקיד צבע לא ייפול
// לברירת המחדל הסגלגלה של Material 3. כולל גם ערכה כהה.

// ---- Light ----
val md_primary = Color(0xFF2E7D32)
val md_onPrimary = Color(0xFFFFFFFF)
val md_primaryContainer = Color(0xFFAEF2A7)
val md_onPrimaryContainer = Color(0xFF002106)
val md_secondary = Color(0xFF52634F)
val md_onSecondary = Color(0xFFFFFFFF)
val md_secondaryContainer = Color(0xFFD5E8CE)
val md_onSecondaryContainer = Color(0xFF111F0E)
val md_tertiary = Color(0xFF386568)
val md_onTertiary = Color(0xFFFFFFFF)
val md_tertiaryContainer = Color(0xFFBCEBEE)
val md_onTertiaryContainer = Color(0xFF002022)
val md_error = Color(0xFFBA1A1A)
val md_onError = Color(0xFFFFFFFF)
val md_errorContainer = Color(0xFFFFDAD6)
val md_onErrorContainer = Color(0xFF410002)
val md_background = Color(0xFFF7FBF1)
val md_onBackground = Color(0xFF191D17)
val md_surface = Color(0xFFF7FBF1)
val md_onSurface = Color(0xFF191D17)
val md_surfaceVariant = Color(0xFFDEE5D8)
val md_onSurfaceVariant = Color(0xFF424940)
val md_outline = Color(0xFF72796F)
val md_outlineVariant = Color(0xFFC2C9BD)
val md_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_surfaceContainerLow = Color(0xFFF1F5EC)
val md_surfaceContainer = Color(0xFFEBEFE6)
val md_surfaceContainerHigh = Color(0xFFE6E9E0)
val md_surfaceContainerHighest = Color(0xFFE0E4DB)

// ---- Dark ----
val md_primary_dark = Color(0xFF93D88E)
val md_onPrimary_dark = Color(0xFF00390C)
val md_primaryContainer_dark = Color(0xFF135218)
val md_onPrimaryContainer_dark = Color(0xFFAEF2A7)
val md_secondary_dark = Color(0xFFB9CCB3)
val md_onSecondary_dark = Color(0xFF243423)
val md_secondaryContainer_dark = Color(0xFF3A4B38)
val md_onSecondaryContainer_dark = Color(0xFFD5E8CE)
val md_tertiary_dark = Color(0xFFA0CFD2)
val md_onTertiary_dark = Color(0xFF003739)
val md_tertiaryContainer_dark = Color(0xFF1E4E51)
val md_onTertiaryContainer_dark = Color(0xFFBCEBEE)
val md_error_dark = Color(0xFFFFB4AB)
val md_onError_dark = Color(0xFF690005)
val md_errorContainer_dark = Color(0xFF93000A)
val md_onErrorContainer_dark = Color(0xFFFFDAD6)
val md_background_dark = Color(0xFF101411)
val md_onBackground_dark = Color(0xFFE0E4DB)
val md_surface_dark = Color(0xFF101411)
val md_onSurface_dark = Color(0xFFE0E4DB)
val md_surfaceVariant_dark = Color(0xFF424940)
val md_onSurfaceVariant_dark = Color(0xFFC2C9BD)
val md_outline_dark = Color(0xFF8C9388)
val md_outlineVariant_dark = Color(0xFF424940)
val md_surfaceContainerLowest_dark = Color(0xFF0B0F0C)
val md_surfaceContainerLow_dark = Color(0xFF191D17)
val md_surfaceContainer_dark = Color(0xFF1D211B)
val md_surfaceContainerHigh_dark = Color(0xFF272B25)
val md_surfaceContainerHighest_dark = Color(0xFF323630)

// ---- צבעי דחיפות (רקע כרטיס פריט) ----
private val PriorityUrgentLight = Color(0xFFFFDAD6)
private val PriorityUrgentDark = Color(0xFF5C2B2B)
private val PriorityNotUrgentLight = Color(0xFFFFF1C2)
private val PriorityNotUrgentDark = Color(0xFF4A4420)

/**
 * גוון רקע לפי דחיפות הפריט, מותאם למצב בהיר/כהה.
 * מחזיר null עבור דחיפות רגילה - הקורא בוחר צבע ברירת מחדל משלו (surface/surfaceVariant).
 */
@Composable
fun priorityTint(priority: ItemPriority): Color? {
    val dark = isSystemInDarkTheme()
    return when (priority) {
        ItemPriority.URGENT -> if (dark) PriorityUrgentDark else PriorityUrgentLight
        ItemPriority.NOT_URGENT -> if (dark) PriorityNotUrgentDark else PriorityNotUrgentLight
        ItemPriority.NORMAL -> null
    }
}

/** גוון מותג לוגו - שני גווני ירוק (במקום הטורקיז שהתנגש עם ה-primary). */
val BrandLogoStart = Color(0xFF43A047)
val BrandLogoEnd = Color(0xFF1B5E20)
val BrandSpark = Color(0xFFFFD740)
