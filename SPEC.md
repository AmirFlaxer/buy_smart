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

### ⏳ נשאר לסיום Firebase setup
1. צור פרויקט Firebase ב-console.firebase.google.com
2. הפעל Authentication (Google Sign-In) + Firestore + Cloud Messaging
3. הורד `google-services.json` → `app/` (לא ב-git)
4. Android Studio → Gradle sync → Run

---

## TODO — פיצ'רים הבאים

### 🔴 HIGH — למידת היסטוריה לפי פריט
**תיאור**: כשמוסיפים פריט "חלב עם קטגוריה סופר והערה קרטון", בפעם הבאה שמקלידים "חלב" —
הקטגוריה והערה מתמלאות אוטומטית כמו שהיו בפעם האחרונה.

**מה צריך לבנות:**
- `ItemHistory` entity ב-Room: `(name, location, note, quantity)` — PrimaryKey = name (lowercase)
- `ItemHistoryDao`: `upsert(history)` + `getByName(name): ItemHistoryEntity?`
- Migration Room version 2→3 (טבלה חדשה)
- אחרי כל הוספה מוצלחת (QuickAdd + AddItemScreen): שמור ב-ItemHistory
- כשמקלידים שם ≥2 תווים: חפש ב-ItemHistory → אם נמצא, מלא אוטומטית קטגוריה + הערה
- עדיפות: History > ItemNotePresets (history מנצח presets אם קיים)
- ItemNotePresets ממשיך לספק חלופות כ-chips (לא כ-default)

**קבצים לשנות:**
- `ItemHistoryEntity.kt` (חדש)
- `ItemHistoryDao.kt` (חדש)
- `AppDatabase.kt` — version 3 + migration + DAO
- `ItemRepository.kt` + `ItemRepositoryImpl.kt` — `saveHistory` + `getHistory`
- `HomeViewModel.kt` — save history אחרי `quickAdd()`
- `AddItemViewModel.kt` — save history אחרי `doSave()`
- `AddItemViewModel.onNameChange` + `HomeViewModel.onQuickAddNameChange` — pre-fill from history

### 🟡 MEDIUM — שיפורים UX
- [ ] Widget לאפליקציה — הוספה מהירה ממסך הבית
- [ ] Wear OS support (OurGroceries-style)
- [ ] מיון ידני של פריטים בתוך קטגוריה (drag & drop)

### 🟢 LOW — תשתית
- [ ] Firebase Cloud Functions לשליחת Push Notifications
- [ ] Multiple lists — מעבר בין רשימות פעילות
