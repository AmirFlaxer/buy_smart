# BuySmart — SPEC & TODO

## מצב נוכחי (2026-05-12)

### ✅ מה מוכן
- Clean Architecture מלא (Domain / Data / Presentation)
- Firebase Auth (Google Sign-In) + Firestore real-time sync
- Room local cache (version 2) + DataStore
- מסך Auth, Home, Shopping, AddItem
- QuickAdd bar עם autocomplete מהיסטוריה (Room)
- הוספת פריט מלאה: שם, כמות, הערה, קטגוריה, סוג (חד-פעמי/חוזר)
- הערות מובנות לפי שם פריט (ItemNotePresets — 30+ פריטים)
- אוטו-קטגוריה לפי שם (חלב→סופר, לחם→מאפייה וכו')
- הצגת "מי הוסיף" (addedByName)
- עריכת פריט קיים (BottomSheet עם כל השדות)
- מניעת כפילויות: דיאלוג "הגדל כמות / הוסף בכל זאת"
- Progress bar בזמן קניות
- Deep link: `buysmart://join/XXXXXX`
- FCM תשתית (token registration)
- Bug fix: רשימה נשמרת אחרי כניסה מחדש לאפליקציה
- Bug fix: מסך לא קפוא בזמן יצירת רשימה

### ⏳ נשאר לסיום Firebase + Gemini setup
1. צור פרויקט Firebase ב-console.firebase.google.com
2. הפעל Authentication (Google Sign-In) + Firestore + Cloud Messaging
3. הורד `google-services.json` → `app/` (לא ב-git)
4. קבל Gemini API key מ-aistudio.google.com/apikey
5. הוסף ל-`local.properties`: `GEMINI_API_KEY=your_key`
6. Android Studio → Gradle sync → Run

---

## ✅ מה נוסף (2026-05-14)

### Single-list UX
- רשימה נוצרת אוטומטית בכניסה ראשונה ("הרשימה שלנו") — ללא דיאלוג
- הצטרפות דרך לינק בלבד (auto-join, ללא קוד הקלדה)

### ItemHistory + Gemini Categorization
- `ItemHistory` entity ב-Room v3: `(name PK, location, note, quantity)`
- `GeminiLocationClassifier`: Gemini 2.0 Flash, ~20 טוקן לפריט, cached לתמיד
- Priority: History > locationHints > Gemini (debounce 600ms)
- שמירה אחרי כל add ב-HomeViewModel + AddItemViewModel

### 🟡 MEDIUM — שיפורים UX
- [ ] Widget לאפליקציה — הוספה מהירה ממסך הבית
- [ ] Wear OS support (OurGroceries-style)
- [ ] מיון ידני של פריטים בתוך קטגוריה (drag & drop)

### 🟢 LOW — תשתית
- [ ] Multiple lists — ארכיטקטורה מוכנה (listId קיים), ממתין ל-UX

---

## ✅ Hardening לקראת סיום (2026-05-31)

### חוסם הפצה / אבטחה
- ✅ `app/proguard-rules.pro` — keep rules ל-Firestore/Gemini/models (release עם minify לא ייכשל)
- ✅ `firestore.rules` + `storage.rules` + `firebase.json` בריפו (membership-based access)
- ✅ versionCode 1→2, versionName 1.0→1.1

### ארכיטקטורה חינמית — ללא Blaze (Spark בלבד)
המשתמש בחר לא לשלם. Cloud Functions ו-Storage חדש דורשים Blaze — לכן:
- ✅ **תמונות → base64 ב-Firestore**: `ImageUploader.encodeItemImage` (720px/JPEG75 → base64), composable `ItemImage` (base64/URL), הוסרה כל תלות ב-Firebase Storage
- ✅ **התראות → מקומיות**: `ItemNotificationHelper` + זיהוי פריטים חדשים ב-`HomeViewModel.observeItems`. עובד כשהאפליקציה רצה (אין שרת). הוסרה תשתית FCM + Cloud Function
- ✅ **firestore.rules נפרס בהצלחה** (Console: buy-smart-62b61, benqueman@gmail.com). **אין יותר מה לפרוס — הכל בצד הלקוח**

### איכות קוד
- ✅ חילוץ `DocumentSnapshot.toShoppingItem()` (היה משוכפל פעמיים) + logging בכל catch
- ✅ `errorMessage` ב-HomeUiState + Snackbar שגיאות (יצירת רשימה / הוספה / עיבוד תמונה)
- ✅ Firestore offline persistence מפורש (PersistentCacheSettings)
- ✅ unit tests: QuantityUtils, LocationKey, Gemini parsing (חולצו ללוגיקה testable)

---

## ✅ אמינות וביצועים (2026-06-10)

- ✅ `finishShopping` אטומי - WriteBatch אחד במקום לולאת מחיקות/עדכונים (כשל רשת לא משאיר קטגוריה חצי-מטופלת)
- ✅ בדיקת כפילויות ב-QuickAdd כוללת פריטי "לחידוש" - דיאלוג מציע "החזר לרשימה" במקום ליצור כפול
- ✅ מטמון Room מוחלף במלואו בכל snapshot (`replaceItemsForList`) - פריטים שנמחקו במכשיר אחר לא נשארים בהשלמה האוטומטית
- ✅ מסך הקנייה: listener יחיד על כל הפריטים + סינון בזיכרון (הוסרו `getItemsByCategoryKey` + `GetItemsByLocationUseCase`) - פחות reads, מעבר קטגוריות מיידי
- ✅ מדריך משתמש: פרק "0. התקנת האפליקציה" (Firebase App Distribution, מקור לא מוכר, סדר לינק-אחרי-התקנה) + הערת אישור הצטרפות. עודכן גם ב-docs (HTML+PDF)

### ✅ פרודקשן (בוצע 2026-06-10)
- ✅ `firebase deploy --only firestore:rules` - תיקוני האבטחה (Vuln 1+2) פעילים בשרת
- ✅ APK 1.2 (versionCode 3) הופץ לבודקים דרך App Distribution (iris.ynet@gmail.com)

---

## ✅ מיזוג פריטים כפולים בין משתמשים (2026-06-17)

כששני משתמשים מוסיפים אותו פריט (race condition) נוצרות שתי שורות. הפיצ'ר מזהה כפילויות (שם מנורמל זהה), מדגיש אותן, ומציע מיזוג ידני לשורה אחת.

- ✅ רכיבי לוגיקה טהורים ב-`domain/util`: `ItemNameKey` (נרמול), `QuantityMerge` (כמות הגדולה; משקל/ספירה לפי העדפה), `ItemMerge` (שילוב הערות, עדיפות גבוהה, RECURRING גובר). 20 unit tests.
- ✅ `FirestoreService.mergeItemsBatch` - WriteBatch אטומי (עדכון שורד + מחיקת מיותרים)
- ✅ העדפת יחידה (משקל/ספירה) ב-DataStore (`merge_unit_preference`), הגדרה בתפריט overflow
- ✅ HomeViewModel מזהה קבוצות כפילות בכל snapshot (פריטים לא-נקנו), HomeScreen מדגיש (errorContainer) + כפתור "מזג"
- ✅ אומת ידנית על 2 מכשירים (2026-06-24, דרך App Distribution release 1.2(3)) - עובד
- ✅ מוזג ל-master (merge commit `0b60fe5`), branch נמחק (מקומי+origin)
- מסמך עיצוב: `docs/superpowers/specs/2026-06-17-merge-duplicates-design.md`
- ⚠️ טרם הופץ כגרסה רשמית - versionName עדיין 1.2. לעדכון רחב צריך להעלות versionCode/versionName

---

## ✅ תיקוני יציבות + מדריך מהיר (2026-07-04)

### הבאג שדווח: אי אפשר להוסיף פריט לחנות שכל פריטיה נקנו
- ✅ מסך "הוסף פריט": דיאלוג הכפילות מזהה פריט שממתין "לחידוש" ומציע "החזר לרשימה" (כמו QuickAdd), במקום "הגדל כמות" שעדכן פריט חבוי בלי תוצאה נראית
- ✅ שני הדיאלוגים מציגים שם חנות מותאמת נכון (`LocationKey.fromItem` במקום `location.displayName`)

### תיקונים מסריקה רחבה (3 סוכני סקירה, 11 ממצאים)
- ✅ race בעריכת תמונה - תמונה לא "נדבקת" לפריט אחר (נעילה על item.id)
- ✅ try/catch אחיד + Snackbar בכל פעולות Firestore (Home/Shopping/AddItem) - אין יותר קריסות על כשל רשת/הרשאות
- ✅ מיזוג כפילויות רק בתוך אותה קטגוריה (מפתח `categoryKey|nameKey`) - לא מאבד צורך בחנות אחרת
- ✅ קטגוריה יתומה: החזרה מ"לחידוש" מוסיפה אוטומטית את החנות המותאמת אם נמחקה
- ✅ QuickAdd לא מאפס קטגוריה מסווגת ל-SUPERMARKET בכל הקשה
- ✅ הצטרפות מקישור עומק נשלחת פעם אחת (handledInviteCode ב-ViewModel)
- ✅ סנכרון "הערה חופשית" ב-AddItem מהיסטוריה
- ✅ "סיים קנייה" מוצא גם פריטים ישנים בלי שדה customLocation (סינון בזיכרון)
- ✅ getItemByName מעדיף פריט פעיל על "לחידוש" (ORDER BY pendingRefill)
- ✅ נרמול שמות אחיד (ItemNameKey.collapseSpaces) בשמירה ובבדיקות כפילות + unit test
- ✅ key(item.id) ל-swipe state ב-LocationSection

### פיצ'רים
- ✅ אזור "לחידוש" מקובץ לפי קטגוריות (כותרות משנה עם אימוג'י ומונה)
- ✅ מסך "מדריך מהיר" (`HelpScreen`) - 8 סעיפי תפעול, נגיש מתפריט ⋮ במסך הראשי; מקור התוכן גם ב-`QUICK_GUIDE.md`

### סטטוס
- ✅ הותקן על הטלפון ואושר ידנית (2026-07-04)

### צבעי קטגוריה (המשך אותו סשן)
- ✅ `categoryTint` ב-Color.kt: רקע + מסגרת ייחודיים לכל קטגוריה (6 מובנות קבועות, 4 גוונים למותאמות לפי hash השם), זוגות בהיר/כהה
- ✅ LocationSection: כרטיס צבוע + מסגרת 1.5dp; שורות פריט יורשות את הרקע (דחיפות/כפילות דורסות)
- ✅ "לחידוש": כל קבוצת קטגוריה במשטח צבוע עם מסגרת - מעבר ברור בין רשימות
- ✅ הותקן ואושר ידנית


## ✅ תמונות + "לחידוש" (2026-10-03)

### תמונות
- ✅ לחיצה על תמונת פריט פותחת אותה במסך מלא (`ItemImage(expandable = true)`); לחיצה נוספת או "חזור" סוגרת. פעיל ברשימה, ב"לחידוש", במסך הקניות, בחלון "שינוי" ובהוספת פריט
- ✅ תמונות מוצגות גם באזור "לחידוש" (לפני כן לא הוצגו שם, ולכן נראה כאילו פריטים עם תמונה נמחקו)
- ✅ במסך המלא (ברשימה וב"לחידוש"): "החלף תמונה" / "מחק תמונה", נשמר מיד, מחיקה עם אישור. במסך הקניות לצפייה בלבד
- ✅ בחלון "שינוי" ובהוספת פריט: כפתורי "החלף תמונה" / "מחק תמונה" במקום ה-X הקטן (`ImageEditActions`, `rememberImagePicker`)
- ✅ כפתורי המסך המלא ("החלף" / "מחק") בראש המסך, מעל התמונה. בתחתית הם נפלו על כפתורי הניווט: בסמסונג A54 חלון הדיאלוג נמתח מתחת לפס הניווט בלי לדווח את גובהו, ולכן גם `navigationBarsPadding` וגם `systemBarsPadding` לא עזרו. אומת בצילום-מסך מהמכשיר

### לחידוש
- ✅ X על פריט ברשימה שואל: "העבר ללחידוש" / "מחק" / "ביטול" (`moveToPendingRefill`). לכל פריט, גם חד-פעמי. החלקה הצידה עדיין מוחקת ישירות עם "בטל"
- ✅ "הוסף שוב" על פריט שכבר ברשימה לקנייה (אותו שם + קטגוריה) לא יוצר כפילות: "הגדל כמות" / "הסר מלחידוש" (`RefillConflict`)
- ✅ המדריך המהיר (HelpScreen + QUICK_GUIDE.md) ו-USER_GUIDE.md עודכנו

### סטטוס
- ✅ הותקן על הטלפון. גרסה 1.3 (versionCode 4) הועלתה ל-App Distribution עם הערות-גרסה בעברית (2026-10-03). 1.3.1 (versionCode 5) - תיקון מיקום הכפתורים - הופצה לבודקים (אומת: Added testers 200)
