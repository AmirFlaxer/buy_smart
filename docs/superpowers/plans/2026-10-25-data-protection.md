# הגנה על נתוני הרשימה - תוכנית מימוש

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** אי אפשר יהיה למחוק פיזית את פריטי הרשימה מהאפליקציה. יהיה גיבוי אוטומטי פעמיים בשבוע. מחיקה גורפת, או היעלמות של פריטים מהשרת, תשוחזר מעצמה. מסך "נמחקו לאחרונה" יאפשר החזרה ידנית. המסך הראשי יהיה פחות עמוס: כפתור בולט ל"יוצאים לקניות", ו"לחידוש" כתגיות.

**Architecture:**
- **מחיקה רכה:** השדות `deletedAt`, `deleteReason` ו-`autoRestoredAt` נשמרים במסמך הפריט. `ItemRepository.getItemsForList` ממשיך להחזיר רק פריטים פעילים, ולכן כל הצרכנים הקיימים לא משתנים.
- **שכבת הגנה נפרדת** (`DataProtectionRepository`) רואה את כל המסמכים יחד עם מטא-דאטה של מטמון. היא משמשת את:
  - `ListGuardian`, שמופעל מ-HomeViewModel ומבצע את שחזורים א' וב'.
  - `BackupWorker`, עבודת WorkManager יומית שמגבה בימי ראשון וחמישי ומנקה.
- **הלוגיקה הטהורה** יושבת ב-`domain/protection`, עם בדיקות-יחידה.
- **האכיפה בשרת:** `firestore.rules`.

**Tech Stack:** Kotlin, Compose, Hilt 2.54, Firestore (BOM 33.7.0), WorkManager 2.9.1 + androidx.hilt:hilt-work 1.2.0, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-25-data-protection-design.md`. לקרוא אותו לפני כל משימה.

## Global Constraints

- Firebase Spark בלבד. אין Cloud Functions ואין שירות בתשלום.
- **ספים, להעתיק בדיוק:**
  - מחיקה גורפת: 10 פריטים ומעלה, `deleteReason == "manual"`, בחלון של 5 שניות, ב-24 השעות האחרונות.
  - פריט שהוחזר אוטומטית לא נספר באשכול חדש במשך 24 שעות.
  - ניקוי פריטים מחוקים: אחרי 30 יום.
  - ניקוי גיבויים: אחרי 35 יום.
  - אישור שני לשחזור ב': אחרי 5 שניות.
- **ערכי `deleteReason`:** `"manual"` / `"finish"` / `"merge"`.
- **ימי גיבוי:** ראשון וחמישי, לפי `ZoneId.of("Asia/Jerusalem")`. מזהה הגיבוי הוא `yyyy-MM-dd`.
- **טקסטים למשתמש, להעתיק בדיוק:**
  - `"שוחזרו $n פריטים שנמחקו בבת אחת"`
  - `"פריטי הרשימה נמחקו מהשרת ושוחזרו מגיבוי מיום $date"`, כש-`$date` בפורמט `dd/MM`.
- בקוד, בהערות ובטקסטים: מקף רגיל ( - ) בלבד, בלי חצים, בלי תווי-כיווניות.
- **פקודת בנייה:** `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew.bat testDebugUnitTest assembleDebug`
- **גרסה:** `versionCode = 6`, `versionName = "1.4.0"`.

## Review Focus

1. **`serverTimestamp` ממתין.** מיד אחרי מחיקה מקומית, `getTimestamp("deletedAt")` מחזיר null, והפריט "קופץ" חזרה לרגע. צריך לקרוא עם `ServerTimestampBehavior.ESTIMATE`. משימה 1 מגדירה את זה, והאימות הידני במשימה 9 (Step 2: הפריט לא מהבהב חזרה).
2. **התראת "X הוסיף" על פריטים ששוחזרו.** במכשיר השני, שחזור א' או ב' ייראה כמו פריטים חדשים ויציף התראות. `notifyNewItems` מדלג על פריט עם `autoRestoredAtMillis` מהשעה האחרונה. הבדיקה במשימה 4.
3. **"סיום קנייה" מעבד שוב פריטים שכבר נמחקו רכות.** הם עדיין `isBought=true`, ולכן כל סיום-קנייה היה מאפס את שעון ה-30 יום שלהם. `getBoughtItemsByCategoryKey` חייב לסנן מחוקים. הבדיקה הידנית במשימה 9.
4. **undo אחרי מחיקה.** חייב לנקות את השדות במסמך המקורי, ולא ליצור מסמך חדש. אחרת נשאר מחוק-רך כפול, שגם נספר באשכול. הבדיקה הידנית במשימה 9.
5. **רשימה חדשה לגמרי** (אפס מסמכים, אין גיבוי) לא מפעילה שחזור ולא קורסת. ההחלטה בבדיקה במשימה 2.

---

### Task 1: מחיקה רכה במודל ובכתיבה

**Files:**
- Modify: `app/src/main/java/com/amir/buysmart/domain/model/ShoppingItem.kt`
- Modify: `app/src/main/java/com/amir/buysmart/data/remote/FirestoreService.kt` (המיפוי `toShoppingItem`, `getItemsForList`, `deleteItem`, `finishShoppingBatch`, `mergeItemsBatch`, `getBoughtItemsByCategoryKey`)
- Modify: `app/src/main/java/com/amir/buysmart/data/repository/ItemRepositoryImpl.kt`
- Modify: `app/src/main/java/com/amir/buysmart/domain/repository/ItemRepository.kt`
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt` (`undoDelete`)
- Test: `app/src/test/java/com/amir/buysmart/domain/model/ShoppingItemDeletedTest.kt`

**Interfaces:**
- Produces:
  - שדות חדשים ב-`ShoppingItem`: `deletedAtMillis: Long? = null`, `deleteReason: String = ""`, `deletedByName: String = ""`, `autoRestoredAtMillis: Long? = null`, `purchaseCount: Int = 0`, ו-`val isDeleted: Boolean get() = deletedAtMillis != null`.
  - שדות במסמך Firestore: `deletedAt`, `deleteReason`, `deletedBy` (uid), `deletedByName`, `autoRestoredAt`, `purchaseCount`. `purchaseCount` נקרא עם `getLong(...)?.toInt() ?: 0`, ונכלל ב-`toFirestoreMap()` (משימה 3), ולכן גם בגיבוי.
  - `object DeleteReason { const val MANUAL = "manual"; const val FINISH = "finish"; const val MERGE = "merge" }` באותו קובץ.
  - `data class ItemsSnapshot(val items: List<ShoppingItem>, val fromCache: Boolean)` ב-`domain/model/ItemsSnapshot.kt`.
  - `FirestoreService.observeAllItems(listId: String): Flow<ItemsSnapshot>` מחזיר את **כל** המסמכים, כולל מחוקים. הוא מחליף את `getItemsForList` הקיים, שנמחק.
  - `data class Deleter(val uid: String, val name: String)` ב-`domain/model/ItemsSnapshot.kt`.
  - `FirestoreService.softDeleteItem(itemId: String, listId: String, reason: String, by: Deleter)`.
  - `finishShoppingBatch(listId, deleteIds, refillIds, by: Deleter)` ו-`mergeItemsBatch(listId, survivor, deleteIds, by: Deleter)` מקבלים פרמטר `by` חדש. **ב-`finishShoppingBatch`, לכל `refillIds` נוסף גם `"purchaseCount" to FieldValue.increment(1)`.**
  - `ItemRepositoryImpl` מקבל `FirebaseAuth` בהזרקה (כבר מסופק ב-`AppModule`), ובונה ממנו `Deleter`. השם: `displayName ?: email?.substringBefore("@") ?: ""`, כמו `displayName` ב-HomeViewModel.
  - `FirestoreService.undeleteItems(listId: String, itemIds: List<String>, markAutoRestored: Boolean)`. מנקה את `deletedAt`, `deleteReason`, `deletedBy` ו-`deletedByName` עם `FieldValue.delete()`. כשהדגל דולק, מציב גם `autoRestoredAt = serverTimestamp()`. הכתיבה ב-batch.
  - `ItemRepository.undeleteItem(item: ShoppingItem)`.
  - `ItemRepository.getItemsForList` ממשיך להחזיר `Flow<List<ShoppingItem>>` של פריטים **פעילים בלבד**, ו-Room מקבל רק אותם.
  - המיפוי מ-`toShoppingItem` הופך ל-`internal`, כדי שמשימה 3 תשתמש בו.

- [ ] **Step 1: לכתוב בדיקה נכשלת.** `ShoppingItemDeletedTest`:
  - `isDeleted false when deletedAtMillis null`
  - `isDeleted true when deletedAtMillis set`
  - `DeleteReason values match spec`: `assertEquals("manual", DeleteReason.MANUAL)`, וכך גם לשני האחרים.
- [ ] **Step 2: להריץ ולוודא כישלון.** `./gradlew.bat testDebugUnitTest --tests "*ShoppingItemDeletedTest"`. צפוי: שגיאת קומפילציה, כי `isDeleted`/`DeleteReason` לא קיימים.
- [ ] **Step 3: לממש.**
  - **המיפוי:** `getTimestamp("deletedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time`, וכנ"ל ל-`autoRestoredAt`. `deleteReason` נקרא עם ברירת מחדל `""`.
  - **`observeAllItems`:** משתמש ב-`addSnapshotListener(MetadataChanges.EXCLUDE)` ופולט `ItemsSnapshot(items, snapshot.metadata.isFromCache)`.
  - **`deleteItem`** ב-Repository קורא ל-`softDeleteItem(..., DeleteReason.MANUAL)`.
  - **`finishShoppingBatch` ו-`mergeItemsBatch`:** במקום `batch.delete`, `batch.update(ref, mapOf("deletedAt" to FieldValue.serverTimestamp(), "deleteReason" to <reason>))`.
  - **`getBoughtItemsByCategoryKey`** מסנן `!it.isDeleted`.
  - **`ItemRepositoryImpl.getItemsForList`** הופך ל-`firestoreService.observeAllItems(listId).map { s -> s.items.filter { !it.isDeleted } }.onEach { /* Room כמו היום */ }`.
  - **`undoDelete`** ב-HomeViewModel קורא ל-`itemRepository.undeleteItem(item)` במקום `addItemUseCase(item.copy(id = ""))`. עדיף להוסיף ל-HomeViewModel את `itemRepository`, שכבר מוזרק, ולא use-case חדש.
- [ ] **Step 4: להריץ את כל הבדיקות ואת הבנייה.** פקודת-הבנייה מ-Global Constraints. צפוי: BUILD SUCCESSFUL, וכל הבדיקות עוברות.
- [ ] **Step 5: Commit.** `git commit -m "feat: מחיקה רכה של פריטים (deletedAt) במקום מחיקה פיזית"`

### Task 2: לוגיקת-הגנה טהורה

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/domain/protection/ProtectionPolicy.kt`
- Test: `app/src/test/java/com/amir/buysmart/domain/protection/ProtectionPolicyTest.kt`

**Interfaces:**
- Consumes: `ShoppingItem.isDeleted`, `deletedAtMillis`, `deleteReason`, `autoRestoredAtMillis` ו-`DeleteReason` ממשימה 1.
- Produces: `object ProtectionPolicy` עם קבועים ציבוריים: `MASS_DELETE_MIN_ITEMS = 10`, `MASS_DELETE_WINDOW_MS = 5_000L`, `LOOKBACK_MS = 24h`, `ITEM_PURGE_AGE_MS = 30d`, `BACKUP_PURGE_AGE_MS = 35d`, `SERVER_RECHECK_DELAY_MS = 5_000L`, `RESTORE_NOTIFY_SUPPRESS_MS = 1h`. ופונקציות:
  - `fun massDeletionCluster(items: List<ShoppingItem>, nowMillis: Long): List<String>`. מחזיר מזהים לשחזור, או רשימה ריקה.
  - `fun isBackupDay(date: LocalDate): Boolean`
  - `fun backupId(date: LocalDate): String`, בפורמט `yyyy-MM-dd`.
  - `fun itemsToPurge(items: List<ShoppingItem>, nowMillis: Long): List<String>`
  - `fun backupsToPurge(backups: Map<String, Long>, nowMillis: Long): List<String>`. המפתח הוא backupId, והערך הוא createdAtMillis.
  - `fun shouldRestoreFromBackup(serverDocCount: Int, fromCache: Boolean, latestBackupItemCount: Int?): Boolean`

**האלגוריתם של `massDeletionCluster`**, כי החתימה לא קובעת אותו:
1. מועמדים: `isDeleted && deleteReason == MANUAL && now - deletedAtMillis <= LOOKBACK_MS`, ולא כאלה עם `autoRestoredAtMillis` שבתוך `LOOKBACK_MS` מ-now.
2. ממיינים לפי `deletedAtMillis`.
3. חלון מחליק: מחפשים את הקבוצה הגדולה ביותר שבה `max - min <= MASS_DELETE_WINDOW_MS`.
4. אם היא בגודל `MASS_DELETE_MIN_ITEMS` ומעלה, מחזירים את המזהים שלה. אחרת רשימה ריקה.

- [ ] **Step 1: לכתוב בדיקות נכשלות.** `ProtectionPolicyTest`, עם helper `deleted(id, at, reason = "manual", restoredAt = null)`:
  - `cluster of 10 manual deletes within 5s is restored`: 10 פריטים ב-`now-60_000 + i*400`, והתוצאה בגודל 10.
  - `9 deletes is not a cluster`: התוצאה ריקה.
  - `10 deletes spread over 6s is not a cluster`: מרווח 650ms, התוצאה ריקה.
  - `finish and merge reasons are ignored`: 10 עם reason `"finish"`, התוצאה ריקה.
  - `deletes older than 24h are ignored`
  - `recently auto-restored items are not counted`: 10 עם `restoredAt = now - 1h`, התוצאה ריקה.
  - `isBackupDay true only on Sunday and Thursday`: 25/10/2026 (ראשון) true, 29/10/2026 (חמישי) true, 26/10/2026 false.
  - `backupId formats yyyy-MM-dd`: `"2026-10-25"`.
  - `purges deletes older than 30 days when active items exist`
  - `keeps newest deleted item when list has no active items`: 3 מחוקים ישנים ואפס פעילים, ונשאר החדש מביניהם.
  - `does not purge recent deletes`: מחוק בן 29 יום לא מוחזר.
  - `backupsToPurge returns only older than 35 days`
  - `shouldRestoreFromBackup`:
    - `(0,false,5)` true
    - `(0,true,5)` false
    - `(1,false,5)` false
    - `(0,false,null)` false (רשימה חדשה בלי גיבוי)
    - `(0,false,0)` false
- [ ] **Step 2: להריץ ולוודא כישלון.** `./gradlew.bat testDebugUnitTest --tests "*ProtectionPolicyTest"`. צפוי: שגיאת קומפילציה.
- [ ] **Step 3: לממש את `ProtectionPolicy`** לפי החתימות והאלגוריתם. java.time זמין, כי minSdk הוא 26.
- [ ] **Step 4: להריץ.** אותה פקודה. צפוי: כל הבדיקות עוברות.
- [ ] **Step 5: Commit.** `git commit -m "feat: לוגיקת הגנה - זיהוי מחיקה גורפת, ימי גיבוי, ניקוי"`

### Task 3: DataProtectionRepository: גיבויים ושחזורים מול Firestore

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/data/remote/BackupService.kt`
- Create: `app/src/main/java/com/amir/buysmart/data/repository/DataProtectionRepository.kt`. מחלקה קונקרטית `@Singleton`, בלי ממשק. זה מספיק לפרויקט, וזו הכרעה.

**Interfaces:**
- Consumes: `FirestoreService.observeAllItems`, `undeleteItems`, המיפוי `toShoppingItem` ומפת-השדות של `addItem` (לחלץ אותה ל-`internal fun ShoppingItem.toFirestoreMap(): Map<String, Any>` ב-FirestoreService ולהשתמש בה גם ב-`addItem`). ו-`ProtectionPolicy` ממשימה 2.
- Produces (ב-`DataProtectionRepository`):
  - `fun observeAllItems(listId: String): Flow<ItemsSnapshot>`
  - `suspend fun autoRestore(listId: String, itemIds: List<String>)`. קורא ל-`undeleteItems(..., markAutoRestored = true)`.
  - `suspend fun serverItemDocCount(listId: String): Int`. מבצע `get(Source.SERVER)` על האוסף.
  - `suspend fun listExistsAndIsMember(listId: String, uid: String): Boolean`. מבצע `get(Source.SERVER)` על מסמך הרשימה.
  - `suspend fun latestBackup(listId: String): BackupInfo?`, עם `data class BackupInfo(val id: String, val createdAtMillis: Long, val itemCount: Int, val customLocations: List<String>)`. זה הגיבוי בעל ה-id הגבוה ביותר שיש לו כותרת.
  - `suspend fun restoreFromBackup(listId: String, backup: BackupInfo)`:
    - קורא את `backups/{id}/items`.
    - כותב כל פריט ב-`set` עם `toFirestoreMap()` ועוד `autoRestoredAt = serverTimestamp()`, בלי `createdAt`.
    - מבצע `arrayUnion` של `customLocations` על מסמך הרשימה.
    - batches של עד 450 פעולות.
  - `suspend fun createBackupIfMissing(listId: String, backupId: String, uid: String): Boolean`:
    - אם הכותרת קיימת, מחזיר false.
    - קורא את הפריטים הפעילים (`get(Source.SERVER)` ומסנן `!isDeleted`). אם אין, מחזיר false.
    - קורא אילו מסמכים כבר קיימים ב-`backups/{id}/items`, וכותב רק את החסרים: `toFirestoreMap()` ועוד `createdAt = serverTimestamp()`, ב-batches של עד 450.
    - בסוף כותב את הכותרת `{createdAt, createdBy=uid, itemCount, customLocations}` ומחזיר true.
    - `FirebaseFirestoreException` עם `PERMISSION_DENIED` על הכותרת, כלומר מכשיר אחר כתב אותה בינתיים, מחזיר false ולא זורק.
  - `suspend fun purge(listId: String, nowMillis: Long)`:
    - מוחק את `ProtectionPolicy.itemsToPurge(...)` מהפריטים.
    - מוחק את `backupsToPurge(...)`: קודם items, אחר כך הכותרת.
  - `suspend fun userListIds(uid: String): List<String>`. מבצע `whereArrayContains("members", uid).get()`.

- [ ] **Step 1: לממש את `BackupService`** (גישה ל-Firestore בלבד: מסלולי `lists/{id}/backups/{bid}` ו-`/items`) **ואת `DataProtectionRepository`** לפי החתימות. בלי בדיקות-יחידה: אין בפרויקט תשתית mock ל-Firestore. ההחלטות נבדקו במשימה 2, והאימות הוא ידני במשימה 9.
- [ ] **Step 2: בנייה.** פקודת-הבנייה. צפוי: BUILD SUCCESSFUL, והבדיקות הקיימות עוברות.
- [ ] **Step 3: Commit.** `git commit -m "feat: DataProtectionRepository - גיבוי, שחזור מגיבוי, ניקוי"`

### Task 4: ListGuardian: שחזור אוטומטי א' ו-ב' במסך הראשי

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/presentation/screens/home/ListGuardian.kt`
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt` (`observeItems`, `notifyNewItems`)
- Test: `app/src/test/java/com/amir/buysmart/domain/protection/RestoreNotifyTest.kt`

**Interfaces:**
- Consumes: `DataProtectionRepository` ממשימה 3, ו-`ProtectionPolicy` ממשימה 2.
- Produces:
  - `class ListGuardian @Inject constructor(repo: DataProtectionRepository)` עם `suspend fun onSnapshot(listId: String, uid: String, snapshot: ItemsSnapshot, nowMillis: Long): String?`. מחזיר הודעה להצגה, או null.
  - `fun ProtectionPolicy.isRecentlyRestored(item: ShoppingItem, nowMillis: Long): Boolean`. כלומר `autoRestoredAtMillis` בתוך `RESTORE_NOTIFY_SUPPRESS_MS`.

**התנהגות `onSnapshot`:**
- **א':** `massDeletionCluster(snapshot.items, now)`. אם לא ריק, מבצע `repo.autoRestore` ומחזיר `"שוחזרו ${ids.size} פריטים שנמחקו בבת אחת"`.
- **ב':** אם `shouldRestoreFromBackup(snapshot.items.size, snapshot.fromCache, latest?.itemCount)` (כש-`latest` מגיע מ-`repo.latestBackup`, ונקרא רק כשהתמונה ריקה ומהשרת):
  - `delay(SERVER_RECHECK_DELAY_MS)`.
  - אם `repo.serverItemDocCount == 0 && repo.listExistsAndIsMember(listId, uid)`, מבצע `restoreFromBackup` ומחזיר `"פריטי הרשימה נמחקו מהשרת ושוחזרו מגיבוי מיום ${dd/MM של latest.id}"`.
- **מניעת ריצות מקבילות:** שדה `Mutex` או `inProgress` בתוך ה-Guardian. Snapshot שמגיע בזמן שחזור מדולג.
- **חריגות:** נתפסות ומתועדות ב-`Log.w`. מחזיר null ולא קורס.

- [ ] **Step 1: לכתוב בדיקה נכשלת.** `RestoreNotifyTest`:
  - `item restored 10 minutes ago is suppressed`: true.
  - `item restored 2 hours ago is not suppressed`: false.
  - `item never restored is not suppressed`: false.
- [ ] **Step 2: להריץ ולוודא כישלון.** `--tests "*RestoreNotifyTest"`. צפוי: שגיאת קומפילציה.
- [ ] **Step 3: לממש.**
  - `isRecentlyRestored` ב-`ProtectionPolicy.kt`.
  - `ListGuardian`.
  - ב-HomeViewModel:
    - להזריק את `ListGuardian` ואת `DataProtectionRepository`.
    - ב-`observeItems(listId)` לפתוח job נוסף, `guardJob`, שאוסף את `dataProtectionRepository.observeAllItems(listId)`, קורא ל-`guardian.onSnapshot(listId, userId, it, System.currentTimeMillis())`, ואם יש הודעה מציב אותה ב-`errorMessage`, שהוא ערוץ ה-snackbar הקיים.
    - לבטל את `guardJob` כמו את `itemsJob`.
    - ב-`notifyNewItems` להוסיף לסינון `&& !ProtectionPolicy.isRecentlyRestored(item, now)`.
- [ ] **Step 4: בדיקות ובנייה.** פקודת-הבנייה. צפוי: הכל עובר.
- [ ] **Step 5: Commit.** `git commit -m "feat: שחזור אוטומטי - מחיקה גורפת ושחזור מגיבוי"`

### Task 5: מסך "נמחקו לאחרונה"

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/presentation/screens/home/RecentlyDeletedScreen.kt`
- Modify: `app/src/main/java/com/amir/buysmart/domain/protection/ProtectionPolicy.kt`
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeScreen.kt` (פריט בתפריט ⋮ והצגת המסך)
- Test: `app/src/test/java/com/amir/buysmart/domain/protection/RecentlyDeletedTest.kt`

**Interfaces:**
- Consumes: ה-`guardJob` ממשימה 4, שכבר אוסף את כל המסמכים. `DataProtectionRepository` ממשימה 3. `FirestoreService.undeleteItems` ממשימה 1.
- Produces:
  - `fun ProtectionPolicy.recentlyDeleted(items: List<ShoppingItem>, nowMillis: Long, zone: ZoneId): List<Pair<LocalDate, List<ShoppingItem>>>`:
    - כולל רק `isDeleted && deleteReason in (MANUAL, MERGE) && now - deletedAtMillis <= ITEM_PURGE_AGE_MS`.
    - מקובץ לפי תאריך מקומי, הימים מהחדש לישן, ובתוך יום מהחדש לישן.
  - `DataProtectionRepository.undelete(listId: String, itemIds: List<String>)`. קורא ל-`undeleteItems(..., markAutoRestored = false)`.
  - ב-`HomeUiState`: `deletedGroups: List<Pair<LocalDate, List<ShoppingItem>>> = emptyList()` ו-`showRecentlyDeleted: Boolean = false`.
  - ב-HomeViewModel: `openRecentlyDeleted()`, `closeRecentlyDeleted()`, `restoreDeleted(ids: List<String>)`.

**התנהגות המסך:**
- **צורה:** `Dialog` במסך מלא (`DialogProperties(usePlatformDefaultWidth = false)`), עם סרגל עליון "נמחקו לאחרונה" וחץ חזרה. כך ההחזרה עוברת דרך אותו ViewModel, בלי נתיב-ניווט חדש. *(הכרעה.)*
- **כותרת יום:** `"היום"` / `"אתמול"` / `dd/MM`, ולידה כפתור `"החזר הכל"`.
- **שורה:** שם הפריט, קטגוריה (`LocationKey.fromItem(it).displayName`), `"נמחק ע\"י ${deletedByName} · HH:mm"` (בלי "ע"י" כשהשם ריק), וכפתור `"החזר"`.
- **מצב ריק:** `"אין פריטים שנמחקו ב-30 הימים האחרונים"`.
- **פריט בתפריט ⋮:** `"נמחקו לאחרונה"`, עם אייקון `Icons.Default.Restore` או דומה.

- [ ] **Step 1: לכתוב בדיקות נכשלות.** `RecentlyDeletedTest`:
  - `finish deletions are excluded`
  - `manual and merge deletions are included`
  - `deletions older than 30 days are excluded`
  - `groups by local day newest first`: שני פריטים מ-25/10 ואחד מ-23/10. התוצאה: `[25/10 -> 2, 23/10 -> 1]`, ובתוך 25/10 החדש ראשון.
- [ ] **Step 2: להריץ ולוודא כישלון.** `--tests "*RecentlyDeletedTest"`.
- [ ] **Step 3: לממש** את הפונקציה, ה-state, הפעולות במודל והמסך. `deletedGroups` מתעדכן מתוך ה-`guardJob`, כלומר מכל snapshot.
- [ ] **Step 4: בדיקות ובנייה.** פקודת-הבנייה. צפוי: הכל עובר.
- [ ] **Step 5: Commit.** `git commit -m "feat: מסך נמחקו לאחרונה עם החזרה"`

### Task 6: המסך הראשי: כפתור "יוצאים לקניות" ו"לחידוש" כתגיות

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/presentation/screens/home/RefillTags.kt` (תגיות + מסך "לחידוש" המלא)
- Modify: `app/src/main/java/com/amir/buysmart/domain/protection/ProtectionPolicy.kt`. **או** קובץ חדש `domain/util/RefillOrder.kt`, וזה עדיף, כי זה לא קשור להגנה. *(הכרעה: `RefillOrder.kt`.)*
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeScreen.kt` (סרגל עליון, כפתור, החלפת `PendingRefillSection` בתגיות)
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt` (`showFullRefill`)
- Test: `app/src/test/java/com/amir/buysmart/domain/util/RefillOrderTest.kt`

**Interfaces:**
- Consumes: `ShoppingItem.purchaseCount` ממשימה 1. `PendingRefillSection` הקיים ב-HomeScreen.kt:475. `viewModel::approvePendingRefill`.
- Produces:
  - `object RefillOrder { const val TAG_LIMIT = 8; fun topTags(items: List<ShoppingItem>): List<ShoppingItem> }`. הסדר: `purchaseCount` יורד, ובשוויון `name` עולה. מחזיר עד 8.
  - ב-`HomeUiState`: `showFullRefill: Boolean = false`. ב-HomeViewModel: `openFullRefill()` ו-`closeFullRefill()`.

**התנהגות:**
- **כפתור:** `Button` ירוק (צבע ה-primary של ה-theme) ברוחב מלא, מתחת לשורת ההוספה ומעל צ'יפי הקטגוריות. הטקסט `"🛒 יוצאים לקניות · ${state.totalItems} פריטים"`. מנווט לאותו יעד שאייקון העגלה מנווט אליו היום. **את `BadgedBox` עם `ShoppingCart` בסרגל העליון (HomeScreen.kt:129-134) מסירים.** כש-`totalItems == 0`, `enabled = false`.
- **תגיות:**
  - במקום `PendingRefillSection` בתוך ה-LazyColumn: כותרת `"🔄 לחידוש · ${n}"`, ואז `FlowRow` עם `AssistChip` לכל פריט ב-`RefillOrder.topTags(...)`.
  - לחיצה על תגית קוראת ל-`viewModel.approvePendingRefill(item)`, כך שהטיפול ב-`RefillConflict` הקיים חל.
  - כשיש יותר מ-8: `TextButton("הצג הכל (${n}) ›")`, שקורא ל-`openFullRefill()`.
- **"לחידוש" המלא:**
  - `Dialog` במסך מלא, כמו במשימה 5, עם סרגל עליון `"🔄 לחידוש (${n})"` וחץ חזרה.
  - בתוכו `PendingRefillSection` הקיים, עם אותם callbacks.
  - **להעביר אותו ל-`internal`.** בלי לשנות את התוכן שלו.
- **התנאי למסך ריק** (HomeScreen.kt:273) נשאר כמו שהוא.

- [ ] **Step 1: לכתוב בדיקות נכשלות.** `RefillOrderTest`:
  - `orders by purchaseCount descending`
  - `ties broken by name ascending`: "בננות" לפני "גלידה" כששניהם 0.
  - `returns at most 8`: 12 פריטים, התוצאה בגודל 8.
  - `empty input gives empty output`
- [ ] **Step 2: להריץ ולוודא כישלון.** `--tests "*RefillOrderTest"`.
- [ ] **Step 3: לממש** את `RefillOrder`, `RefillTags.kt` והשינויים ב-HomeScreen וב-HomeViewModel.
- [ ] **Step 4: בדיקות ובנייה.** פקודת-הבנייה. צפוי: הכל עובר.
- [ ] **Step 5: Commit.** `git commit -m "feat: כפתור יוצאים לקניות בולט ו-לחידוש כתגיות לפי תדירות"`

### Task 7: BackupWorker: WorkManager עם Hilt

**Files:**
- Modify: `gradle/libs.versions.toml`. להוסיף `work = "2.9.1"` ו-`hiltWork = "1.2.0"`, וספריות `androidx-work-runtime-ktx`, `hilt-work` (`androidx.hilt:hilt-work`) ו-`hilt-work-compiler` (`androidx.hilt:hilt-compiler`).
- Modify: `app/build.gradle.kts`. `implementation(libs.androidx.work.runtime.ktx)`, `implementation(libs.hilt.work)` ו-`ksp(libs.hilt.work.compiler)`.
- Modify: `app/src/main/java/com/amir/buysmart/BuySmartApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`. להסיר את `androidx.work.WorkManagerInitializer` מ-`androidx.startup.InitializationProvider` (`tools:node="remove"`), ולהוסיף `xmlns:tools`.
- Create: `app/src/main/java/com/amir/buysmart/data/backup/BackupWorker.kt`

**Interfaces:**
- Consumes: `DataProtectionRepository.userListIds`, `createBackupIfMissing`, `purge` ו-`ProtectionPolicy.isBackupDay` / `backupId`.
- Produces:
  - `@HiltWorker class BackupWorker @AssistedInject constructor(@Assisted ctx: Context, @Assisted params: WorkerParameters, repo: DataProtectionRepository, auth: FirebaseAuth) : CoroutineWorker`.
  - `companion object { const val PERIODIC_NAME = "data_backup_daily"; const val STARTUP_NAME = "data_backup_startup"; fun schedule(context: Context) }`.

**התנהגות:**
- **`doWork`:**
  - אין משתמש מחובר: `Result.success()`.
  - לכל רשימה: אם `isBackupDay(today in Asia/Jerusalem)`, מבצע `createBackupIfMissing(listId, backupId(today), uid)`. אחר כך, בכל מקרה, `purge(listId, now)`.
  - חריגה: `Result.retry()`.
- **`schedule`:**
  - `enqueueUniquePeriodicWork(PERIODIC_NAME, KEEP, PeriodicWorkRequest(24h))` עם `NetworkType.CONNECTED`.
  - בנוסף `enqueueUniqueWork(STARTUP_NAME, REPLACE, OneTimeWorkRequest)` עם אותו constraint. ריצה בכל פתיחה מגבירה אמינות, והעבודה אידמפוטנטית. *(הכרעה במסגרת "שילוב WorkManager נשאר לבונה".)*
- **`BuySmartApp`:** מממש `Configuration.Provider` עם `@Inject lateinit var workerFactory: HiltWorkerFactory`, וב-`onCreate` קורא ל-`BackupWorker.schedule(this)`.

- [ ] **Step 1: לממש** את כל השינויים בקבצים למעלה.
- [ ] **Step 2: בנייה.** פקודת-הבנייה. צפוי: BUILD SUCCESSFUL. אם hilt-work לא מסתדר עם Hilt 2.54 או KSP 2.0.21, לעצור ולדווח, לא לשדרג גרסאות על דעת עצמך.
- [ ] **Step 3: Commit.** `git commit -m "feat: גיבוי אוטומטי בראשון ובחמישי (WorkManager)"`

### Task 8: חוקי Firestore, גרסה ותיעוד

**Files:**
- Modify: `firestore.rules`
- Modify: `app/build.gradle.kts` (`versionCode = 6`, `versionName = "1.4.0"`)
- Modify: `SPEC.md`, ו-`QUICK_GUIDE.md` רק אם יש בו תיאור של מחיקה.

**החוקים, בתוך `match /lists/{listId}`:**
- `allow delete: if false;` במקום `isMember`.
- **items:** לפצל את `read, write` ל-`read, create, update` לחבר, ו-`delete`:
  ```
  allow delete: if isMember(get(/databases/$(database)/documents/lists/$(listId)).data)
                && resource.data.deletedAt is timestamp
                && resource.data.deletedAt < request.time - duration.value(30, 'd');
  ```
- **בלוק חדש:** `match /backups/{backupId}` עם `read, create` לחבר, ו-`delete` לחבר רק כש-`resource.data.createdAt < request.time - duration.value(35, 'd')`. בלי update. בתוכו `match /items/{itemId}` עם אותם חוקים בדיוק.

- [ ] **Step 1: לערוך את הקבצים.** **לא לפרוס חוקים.** הפריסה במשימה 9, ורק אחרי ששני הטלפונים על 1.4.0.
- [ ] **Step 2: בדיקת תחביר בלי פריסה.** `firebase deploy --only firestore:rules --dry-run`. צפוי: "rules file firestore.rules compiled successfully". אם `--dry-run` לא נתמך, לדלג ולציין זאת.
- [ ] **Step 3: בנייה.** פקודת-הבנייה. צפוי: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit.** `git commit -m "feat: חוקי שרת נגד מחיקה + גרסה 1.4.0"`

### Task 9: אימות על הטלפון ופריסה

אימות ידני על הטלפון 3694e208. ההתקנה ב-`adb install -r`, שומר נתונים. ב-Git Bash: `MSYS_NO_PATHCONV=1`, ו-adb נמצא ב-`$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`.

- [ ] **Step 1: התקנה והפעלה.** ב-logcat אין קריסה.
- [ ] **Step 2: מחיקה ו-undo.** למחוק פריט, והוא נעלם. ללחוץ "בטל", והוא חוזר. במטמון-Firestore של הטלפון (העתקה ב-`run-as`) אין מסמך כפול. (Review Focus 4.)
- [ ] **Step 3: סיום קנייה.** לסמן פריט חד-פעמי כנקנה ולבצע "סיום קנייה". הוא נעלם. סיום-קנייה שני באותה קטגוריה לא מעדכן אותו שוב. (Review Focus 3.)
- [ ] **Step 3א: המסך הראשי.** הכפתור הירוק מופיע, ואייקון העגלה בסרגל לא מופיע. התגיות מוצגות: עד 8, ו"הצג הכל" כשיש יותר. לחיצה על תגית מחזירה פריט לרשימה. "הצג הכל" פותח את "לחידוש" המלא. צילום-מסך לאמיר.
- [ ] **Step 3ב: נמחקו לאחרונה.** הפריט שנמחק ב-Step 2 (ולא שוחזר) מופיע עם "נמחק ע"י" ושעה. "החזר" מחזיר אותו למקומו.
- [ ] **Step 4: גיבוי.** אם היום ראשון או חמישי, ב-logcat רואים שהעבודה רצה, וקיים `lists/HRsKQKa68WoZrgQ5tlLa/backups/<היום>`. אפשר לאמת דרך המטמון המקומי או דרך קונסולת Firebase (אמיר).
- [ ] **Step 5: להפיץ את 1.4.0 ב-App Distribution** לפי `memory/process_firebase_distribution.md`. **פעולה חיצונית: לקבל אישור של אמיר לפני ההעלאה.**
- [ ] **Step 6: פריסת חוקים.** **רק אחרי שאמיר מאשר ששני הטלפונים על 1.4.0:** `firebase deploy --only firestore:rules`. אחר כך: מחיקה רגילה עדיין עובדת (עדכון), ו"מחק" בגרסה ישנה, אם נשארה, נכשל.
- [ ] **Step 7: עדכון `current_focus.md`** במצב הפריסה.
