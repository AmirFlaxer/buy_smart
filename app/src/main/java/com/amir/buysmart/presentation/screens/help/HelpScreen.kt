package com.amir.buysmart.presentation.screens.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** סעיף במדריך המהיר: כותרת (עם אימוג'י) + הסבר קצר. */
private data class HelpSection(val title: String, val body: String)

/** התוכן המקוצר — מקביל ל-QUICK_GUIDE.md שבריפו. לעדכן בשניהם יחד. */
private val helpSections = listOf(
    HelpSection(
        "➕ הוספת פריט",
        "כתבו שם בשורת \"הוסף פריט...\" (או לחצו על המיקרופון ואמרו בקול) ולחצו \"הוסף\". " +
        "האפליקציה מסווגת את הפריט לקטגוריה המתאימה אוטומטית. " +
        "אפשר להוסיף גם כמות, דחיפות, הערה ותמונה דרך כפתור ה-+ העגול."
    ),
    HelpSection(
        "🛒 יוצאים לקניות",
        "לחצו על אייקון העגלה למעלה, בחרו את החנות שבה אתם נמצאים וסמנו כל פריט שנלקח. " +
        "בסיום לחצו \"סיימתי\" — פריטים קבועים עוברים לאזור \"לחידוש\" ופריטים חד-פעמיים נמחקים."
    ),
    HelpSection(
        "🔄 לחידוש",
        "פריטים שנקנו ממתינים באזור \"לחידוש\" בתחתית המסך, מקובצים לפי קטגוריה. " +
        "לחצו \"הוסף שוב\" כדי להחזיר פריט לרשימה לקנייה הבאה, או X כדי להסיר אותו."
    ),
    HelpSection(
        "👨‍👩‍👧 שיתוף הרשימה",
        "בתפריט שלוש הנקודות מוצג קוד הרשימה (לחיצה מעתיקה). " +
        "אפשר גם ללחוץ על אייקון השיתוף ולשלוח לינק הזמנה בוואטסאפ. " +
        "מי שמבקש להצטרף מופיע בראש המסך — כל חבר ברשימה יכול לאשר או לדחות."
    ),
    HelpSection(
        "🔑 הצטרפות לרשימה",
        "קיבלתם לינק? לחצו עליו והאפליקציה תשלח בקשת הצטרפות. " +
        "קיבלתם קוד? תפריט שלוש הנקודות, \"הצטרף לרשימה\", והקלידו את הקוד. " +
        "ההצטרפות מושלמת ברגע שחבר ברשימה מאשר."
    ),
    HelpSection(
        "🏬 חנויות וקטגוריות משלכם",
        "מעבר לקטגוריות המובנות אפשר להוסיף חנות מותאמת (למשל \"בית מרקחת\") " +
        "דרך צ'יפ \"הוסף\" בשורת הקטגוריות. " +
        "אחרי סיום קנייה בחנות מותאמת האפליקציה תציע למחוק אותה אם הייתה חד-פעמית."
    ),
    HelpSection(
        "✏️ עריכה ומחיקה",
        "לחצו \"שינוי\" ליד פריט כדי לערוך שם, כמות, קטגוריה, דחיפות או תמונה. " +
        "למחיקה החליקו את הפריט הצידה — ואם טעיתם, לחצו \"בטל\" שמופיע למטה."
    ),
    HelpSection(
        "🔀 כפילויות",
        "שני פריטים עם אותו שם באותה קטגוריה מודגשים באדום עם כפתור \"מזג\" — " +
        "לחיצה מאחדת אותם לפריט אחד עם הכמויות המחוברות."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📖 מדריך מהיר") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(helpSections) { section ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                section.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
