# עיצוב: Widget ל-BuySmart

**תאריך:** 2026-06-13
**סטטוס:** מאושר - מוכן לתכנון מימוש

## מטרה
Widget למסך הבית של מכשיר Android שמציג את פריטי הרשימה הפעילה (שטרם נקנו) ומאפשר הוספה מהירה דרך דיאלוג צף - בלי לפתוח את האפליקציה המלאה. נבחר כפיצ'ר ה-UX עם הערך היומיומי הגבוה ביותר מתוך רשימת ה-SPEC.

## החלטות מוצר (מאושרות)
- **מטרת ה-widget:** תצוגת הרשימה + הוספה מהירה (גם read וגם write).
- **כפתור +:** פותח דיאלוג הוספה צף דרך Activity שקופה - בלי לפתוח את האפליקציה המלאה.
- **לחיצה על פריט / כותרת:** פותחת את האפליקציה במסך הבית.
- **טכנולוגיה:** Glance (Jetpack Compose for App Widgets) - מתאים ל-stack הקיים (Compose).

## ארכיטקטורה ורכיבים
package חדש: `presentation/widget/`

| רכיב | תפקיד |
|------|-------|
| `BuySmartWidget : GlanceAppWidget` | מרנדר את ה-UI (כותרת, רשימת פריטים, כפתור +). קורא נתונים ב-`provideGlance` |
| `BuySmartWidgetReceiver : GlanceAppWidgetReceiver` | רשום ב-Manifest, מחבר את ה-widget למערכת |
| `QuickAddActivity` | Activity שקופה (`Theme.Translucent`) - דיאלוג Compose להוספה מהירה |
| `WidgetUpdater` | אובייקט עזר עם `suspend fun update(context)` שקורא `BuySmartWidget().updateAll(context)` |
| `WidgetEntryPoint` | `@EntryPoint` (Hilt) שחושף `ItemDao` + `DataStore<Preferences>` ל-Glance |
| `WidgetItems` | פונקציה טהורה `widgetItems(items, max): List<ShoppingItem>` - סינון/מיון/חיתוך (testable) |
| `res/xml/buysmart_widget_info.xml` | מטא-דאטה: גודל מינימלי, תדירות עדכון, preview |

## זרימת נתונים
ה-widget קורא מ-**Room** (ולא מ-Firestore ישירות) - אין צורך ב-auth או רשת, וה-cache כבר מתעדכן בכל snapshot מ-Firestore:

1. `active_list_id` מ-DataStore → ה-listId הנוכחי.
2. `ItemDao.getItemsForList(listId).first()` → רשימת הפריטים.
3. `widgetItems(...)` מסנן `!isBought && !pendingRefill`, ממיין לפי priority, וחותך לכמות מקסימלית (~12).
4. גישה ל-Hilt מתוך Glance דרך `EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)` - Glance לא תומך ב-`@Inject` ישיר.

## אינטראקציות
- **כפתור +** → `actionStartActivity<QuickAddActivity>()`.
  הדיאלוג: שדה שם + כפתור "הוסף". בעת הוספה:
  - סיווג location בדפוס הקיים אך **מקומי בלבד** (ללא await לרשת): `ItemNotePresets.suggestLocation(name)` → history (`getHistory`) → ברירת מחדל `SUPERMARKET`.
  - קריאה ל-`AddItemUseCase`, שמירת history, סגירת ה-Activity, קריאה ל-`WidgetUpdater.update`.
  - ה-location ניתן לעריכה אחר כך באפליקציה; הסיווג של Gemini יתבצע ממילא כשייפתח מסך הבית. החלטה זו מעדיפה מהירות-תגובה על דיוק סיווג מיידי.
- **לחיצה על פריט / כותרת** → `actionStartActivity<MainActivity>()` (פותח את מסך הבית).

## עדכון ה-widget
קריאה ל-`WidgetUpdater.update(context)` ב-`ItemRepositoryImpl`:
- אחרי `replaceItemsForList` (כל snapshot מ-Firestore).
- אחרי הוספה/מחיקה/עדכון מקומיים.

כך ה-widget תמיד מסונכרן בלי listener נפרד שמבזבז סוללה.

## טיפול בקצוות
- **לא מחובר / אין רשימה פעילה** (`active_list_id == null`) → מצב ריק: "התחבר כדי לראות את הרשימה", לחיצה פותחת את האפליקציה.
- **רשימה ריקה** → "הרשימה ריקה 🎉" + כפתור +.
- **QuickAdd ללא משתמש מחובר** → הדיאלוג סוגר את עצמו ופותח את `MainActivity` (להתחברות).

## dependencies חדשים
`androidx.glance:glance-appwidget:1.1.1` (יציב, תומך minSdk 26) - תוספת ל-`libs.versions.toml` ול-`build.gradle.kts`.

## בדיקות
- **Unit test** ל-`widgetItems(...)`: מסננת נקנו/refill, ממיינת לפי priority, חותכת לכמות מקסימלית.
- **בדיקה ידנית:** הוספת ה-widget למסך הבית, הוספת פריט דרך +, סימון פריט כנקנה באפליקציה ובדיקה שה-widget מתעדכן.

## מחוץ ל-scope (YAGNI)
- סימון פריט כנקנה ישירות מה-widget (הוחלט: לחיצה על פריט פותחת את האפליקציה).
- הוספה קולית מה-widget.
- בחירת רשימה מתוך ה-widget (משתמש ברשימה הפעילה בלבד).
