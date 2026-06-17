# מיזוג פריטים כפולים - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** זיהוי שתי שורות (או יותר) של אותו פריט ברשימה משותפת, הדגשתן, והצעת מיזוג ידני לשורה אחת עם הכמות הגדולה והערות משולבות.

**Architecture:** לוגיקת זיהוי ומיזוג טהורה ב-`domain/util` (testable), כתיבת batch אטומית ב-Firestore (כמו `finishShoppingBatch`), זיהוי קבוצות כפילות ב-`HomeViewModel.observeItems` בכל snapshot, והדגשה + כפתור "מזג" ב-HomeScreen. העדפת יחידה (משקל/ספירה) נשמרת ב-DataStore.

**Tech Stack:** Kotlin, JUnit 4, Hilt, Room, Firestore, DataStore, Jetpack Compose.

**מקור:** מסמך עיצוב ב-`docs/superpowers/specs/2026-06-17-merge-duplicates-design.md`.

**הערת build (Windows):** לפני כל פקודת gradle, ב-Bash tool (זה bash, לא PowerShell):
`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart`

---

## File Structure

| קובץ | אחריות |
|------|--------|
| `domain/util/ItemNameKey.kt` (חדש) | נרמול שם פריט למפתח השוואה |
| `domain/util/QuantityMerge.kt` (חדש) | מיזוג שתי מחרוזות כמות לפי העדפת יחידה |
| `domain/util/ItemMerge.kt` (חדש) | מיזוג קבוצת פריטים → פריט שורד + IDs למחיקה |
| `domain/repository/ListRepository.kt` (modify) | חתימות getMergeUnitPreference/setMergeUnitPreference |
| `data/repository/ListRepositoryImpl.kt` (modify) | מימוש ההגדרה ב-DataStore |
| `data/remote/FirestoreService.kt` (modify) | mergeItemsBatch (WriteBatch אטומי) |
| `domain/repository/ItemRepository.kt` (modify) | חתימת mergeDuplicates |
| `data/repository/ItemRepositoryImpl.kt` (modify) | מימוש mergeDuplicates (טהור + batch + Room) |
| `presentation/screens/home/HomeViewModel.kt` (modify) | זיהוי קבוצות, state, פעולת מיזוג, איסוף הגדרה |
| `presentation/screens/home/HomeScreen.kt` (modify) | הדגשה + כפתור מזג + הגדרה בתפריט |

תיקיית הבדיקות: `app/src/test/java/com/amir/buysmart/domain/util/`

---

## Task 1: ItemNameKey (TDD)

נרמול שם פריט: trim + lowercase + צמצום רווחים פנימיים כפולים.

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/domain/util/ItemNameKey.kt`
- Test: `app/src/test/java/com/amir/buysmart/domain/util/ItemNameKeyTest.kt`

- [ ] **Step 1: כתוב את הבדיקה הנכשלת**

צור `app/src/test/java/com/amir/buysmart/domain/util/ItemNameKeyTest.kt`:

```kotlin
package com.amir.buysmart.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemNameKeyTest {

    @Test
    fun `trims and lowercases`() {
        assertEquals("חלב", ItemNameKey.of("  חלב "))
        assertEquals("milk", ItemNameKey.of("MILK"))
    }

    @Test
    fun `collapses internal double spaces`() {
        assertEquals("חלב עיזים", ItemNameKey.of("חלב   עיזים"))
    }

    @Test
    fun `equal names produce equal keys`() {
        assertEquals(ItemNameKey.of("חלב "), ItemNameKey.of("חלב"))
    }
}
```

- [ ] **Step 2: הרץ את הבדיקה כדי לוודא שהיא נכשלת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.ItemNameKeyTest"`
Expected: FAIL - `Unresolved reference: ItemNameKey`.

- [ ] **Step 3: כתוב את המימוש**

צור `app/src/main/java/com/amir/buysmart/domain/util/ItemNameKey.kt`:

```kotlin
package com.amir.buysmart.domain.util

/** מנרמל שם פריט למפתח השוואה: trim + lowercase + רווח יחיד בין מילים. */
object ItemNameKey {
    private val multiSpace = Regex("\\s+")
    fun of(name: String): String =
        name.trim().lowercase().replace(multiSpace, " ")
}
```

- [ ] **Step 4: הרץ את הבדיקה כדי לוודא שהיא עוברת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.ItemNameKeyTest"`
Expected: PASS - 3 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/domain/util/ItemNameKey.kt app/src/test/java/com/amir/buysmart/domain/util/ItemNameKeyTest.kt
git commit -m "feat: ItemNameKey - נרמול שם פריט למפתח כפילות (+tests)"
```

---

## Task 2: QuantityMerge (TDD)

מיזוג שתי מחרוזות כמות. הרכיב הרגיש ביותר.

**הגדרת התנהגות:**
- `UnitType { WEIGHT, COUNT }` - WEIGHT = כל יחידת מדידה (משקל/נפח: ק"ג/קג/קילו/גרם/גר/ג/ליטר/ל/מ"ל/מל). COUNT = כל השאר (ריק, מספר בלבד, "יחידות", "חבילה").
- כמות "ניתנת לניתוח" = מתחילה במספר (אפשר עשרוני).
- **שתיהן ניתנות לניתוח:**
  - אותו UnitType → נרמול לסקלה מספרית בסיסית (ק"ג×1000, ליטר×1000, אחרת ×1) והחזרת המחרוזת המקורית של הגדולה.
  - UnitType שונה → המחרוזת מהסוג המועדף (`preference`).
- **רק אחת ניתנת לניתוח** → היא הנבחרת (אינפורמטיבית יותר).
- **אף אחת לא ניתנת לניתוח** → הראשונה הלא-ריקה (או "" אם שתיהן ריקות).

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/domain/util/QuantityMerge.kt`
- Test: `app/src/test/java/com/amir/buysmart/domain/util/QuantityMergeTest.kt`

- [ ] **Step 1: כתוב את הבדיקה הנכשלת**

צור `app/src/test/java/com/amir/buysmart/domain/util/QuantityMergeTest.kt`:

```kotlin
package com.amir.buysmart.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityMergeTest {

    @Test
    fun `same count unit takes the larger number`() {
        assertEquals("4", QuantityMerge.merge("2", "4", UnitType.WEIGHT))
        assertEquals("3 יחידות", QuantityMerge.merge("2 יחידות", "3 יחידות", UnitType.WEIGHT))
    }

    @Test
    fun `same weight family normalizes and picks larger`() {
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("500 גרם", "1.5 ק\"ג", UnitType.WEIGHT))
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("1.5 ק\"ג", "500 גרם", UnitType.WEIGHT))
    }

    @Test
    fun `different types follow weight preference`() {
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("5", "1.5 ק\"ג", UnitType.WEIGHT))
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("1.5 ק\"ג", "5", UnitType.WEIGHT))
    }

    @Test
    fun `different types follow count preference`() {
        assertEquals("5", QuantityMerge.merge("5", "1.5 ק\"ג", UnitType.COUNT))
        assertEquals("5", QuantityMerge.merge("1.5 ק\"ג", "5", UnitType.COUNT))
    }

    @Test
    fun `only one parseable wins`() {
        assertEquals("3", QuantityMerge.merge("", "3", UnitType.WEIGHT))
        assertEquals("2", QuantityMerge.merge("חבילה", "2", UnitType.WEIGHT))
    }

    @Test
    fun `neither parseable falls back to first non-blank`() {
        assertEquals("חבילה", QuantityMerge.merge("חבילה", "קופסה", UnitType.WEIGHT))
        assertEquals("קופסה", QuantityMerge.merge("", "קופסה", UnitType.WEIGHT))
        assertEquals("", QuantityMerge.merge("", "", UnitType.WEIGHT))
    }
}
```

- [ ] **Step 2: הרץ את הבדיקה כדי לוודא שהיא נכשלת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.QuantityMergeTest"`
Expected: FAIL - `Unresolved reference: QuantityMerge`.

- [ ] **Step 3: כתוב את המימוש**

צור `app/src/main/java/com/amir/buysmart/domain/util/QuantityMerge.kt`:

```kotlin
package com.amir.buysmart.domain.util

enum class UnitType { WEIGHT, COUNT }

/**
 * ממזג שתי מחרוזות כמות. כשהיחידות מאותו סוג - בוחר את הגדולה.
 * כשסוג שונה (משקל מול ספירה) - לפי ההעדפה. ראה QuantityMergeTest להתנהגות מלאה.
 */
object QuantityMerge {

    // יחידה טקסטואלית → מכפיל נרמול לסקלה בסיסית. מילים אלו מסומנות WEIGHT.
    private val weightUnits = mapOf(
        "ק\"ג" to 1000.0, "קג" to 1000.0, "קילו" to 1000.0,
        "גרם" to 1.0, "גר" to 1.0, "ג" to 1.0, "ג'" to 1.0,
        "ליטר" to 1000.0, "ל" to 1000.0, "ל'" to 1000.0,
        "מ\"ל" to 1.0, "מל" to 1.0
    )

    private val leadingNumber = Regex("^(\\d+(?:\\.\\d+)?)\\s*(.*)$")

    private data class Parsed(val value: Double, val type: UnitType)

    private fun parse(q: String): Parsed? {
        val m = leadingNumber.find(q.trim()) ?: return null
        val num = m.groupValues[1].toDoubleOrNull() ?: return null
        val unit = m.groupValues[2].trim()
        val multiplier = weightUnits[unit]
        return if (multiplier != null) Parsed(num * multiplier, UnitType.WEIGHT)
        else Parsed(num, UnitType.COUNT)
    }

    fun merge(a: String, b: String, preference: UnitType): String {
        val pa = parse(a)
        val pb = parse(b)
        return when {
            pa != null && pb != null -> when {
                pa.type == pb.type -> if (pa.value >= pb.value) a else b
                else -> if (pa.type == preference) a else b
            }
            pa != null -> a
            pb != null -> b
            a.isNotBlank() -> a
            else -> b
        }
    }
}
```

- [ ] **Step 4: הרץ את הבדיקה כדי לוודא שהיא עוברת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.QuantityMergeTest"`
Expected: PASS - 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/domain/util/QuantityMerge.kt app/src/test/java/com/amir/buysmart/domain/util/QuantityMergeTest.kt
git commit -m "feat: QuantityMerge - מיזוג כמויות לפי העדפת יחידה (+tests)"
```

---

## Task 3: ItemMerge (TDD)

מיזוג קבוצת פריטים → פריט שורד + IDs למחיקה.

**Files:**
- Create: `app/src/main/java/com/amir/buysmart/domain/util/ItemMerge.kt`
- Test: `app/src/test/java/com/amir/buysmart/domain/util/ItemMergeTest.kt`

- [ ] **Step 1: כתוב את הבדיקה הנכשלת**

צור `app/src/test/java/com/amir/buysmart/domain/util/ItemMergeTest.kt`:

```kotlin
package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemMergeTest {

    private fun item(
        id: String,
        quantity: String = "",
        note: String = "",
        priority: ItemPriority = ItemPriority.NORMAL,
        type: ItemType = ItemType.ONE_TIME,
        imageUrl: String = ""
    ) = ShoppingItem(
        id = id, name = "חלב", quantity = quantity, note = note,
        location = ShoppingLocation.SUPERMARKET, type = type,
        addedBy = "u", listId = "l", priority = priority, imageUrl = imageUrl
    )

    @Test
    fun `survivor is first item, others are deleted`() {
        val result = ItemMerge.merge(listOf(item("a"), item("b"), item("c")), UnitType.WEIGHT)
        assertEquals("a", result.survivor.id)
        assertEquals(listOf("b", "c"), result.deleteIds)
    }

    @Test
    fun `quantity is merged with larger value`() {
        val result = ItemMerge.merge(listOf(item("a", quantity = "2"), item("b", quantity = "4")), UnitType.WEIGHT)
        assertEquals("4", result.survivor.quantity)
    }

    @Test
    fun `notes are combined uniquely`() {
        val result = ItemMerge.merge(
            listOf(item("a", note = "1%"), item("b", note = "3%"), item("c", note = "1%")),
            UnitType.WEIGHT
        )
        assertEquals("1%, 3%", result.survivor.note)
    }

    @Test
    fun `highest priority wins`() {
        val result = ItemMerge.merge(
            listOf(item("a", priority = ItemPriority.NORMAL), item("b", priority = ItemPriority.URGENT)),
            UnitType.WEIGHT
        )
        assertEquals(ItemPriority.URGENT, result.survivor.priority)
    }

    @Test
    fun `recurring type wins over one-time`() {
        val result = ItemMerge.merge(
            listOf(item("a", type = ItemType.ONE_TIME), item("b", type = ItemType.RECURRING)),
            UnitType.WEIGHT
        )
        assertEquals(ItemType.RECURRING, result.survivor.type)
    }

    @Test
    fun `first non-blank image is kept`() {
        val result = ItemMerge.merge(
            listOf(item("a", imageUrl = ""), item("b", imageUrl = "img1"), item("c", imageUrl = "img2")),
            UnitType.WEIGHT
        )
        assertEquals("img1", result.survivor.imageUrl)
    }
}
```

- [ ] **Step 2: הרץ את הבדיקה כדי לוודא שהיא נכשלת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.ItemMergeTest"`
Expected: FAIL - `Unresolved reference: ItemMerge`.

- [ ] **Step 3: כתוב את המימוש**

צור `app/src/main/java/com/amir/buysmart/domain/util/ItemMerge.kt`:

```kotlin
package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem

/** תוצאת מיזוג: הפריט השורד (עם הערכים הממוזגים) והמזהים למחיקה. */
data class MergeResult(val survivor: ShoppingItem, val deleteIds: List<String>)

object ItemMerge {

    /** ממזג קבוצת פריטים (אותו שם מנורמל) לפריט אחד. הראשון שורד. */
    fun merge(group: List<ShoppingItem>, preference: UnitType): MergeResult {
        val first = group.first()
        val quantity = group.map { it.quantity }
            .reduce { acc, q -> QuantityMerge.merge(acc, q, preference) }
        val note = group.flatMap { it.note.split(", ") }
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        val priority = group.minByOrNull { it.priority.ordinal }!!.priority
        val type = if (group.any { it.type == ItemType.RECURRING }) ItemType.RECURRING else ItemType.ONE_TIME
        val imageUrl = group.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl ?: ""
        val survivor = first.copy(
            quantity = quantity, note = note, priority = priority, type = type, imageUrl = imageUrl
        )
        return MergeResult(survivor, group.drop(1).map { it.id })
    }
}
```

- [ ] **Step 4: הרץ את הבדיקה כדי לוודא שהיא עוברת**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest --tests "com.amir.buysmart.domain.util.ItemMergeTest"`
Expected: PASS - 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/domain/util/ItemMerge.kt app/src/test/java/com/amir/buysmart/domain/util/ItemMergeTest.kt
git commit -m "feat: ItemMerge - מיזוג קבוצת פריטים לפריט שורד (+tests)"
```

---

## Task 4: העדפת יחידה ב-DataStore

**Files:**
- Modify: `app/src/main/java/com/amir/buysmart/domain/repository/ListRepository.kt`
- Modify: `app/src/main/java/com/amir/buysmart/data/repository/ListRepositoryImpl.kt`

- [ ] **Step 1: הוסף חתימות ל-ListRepository**

ב-`ListRepository.kt`, אחרי השורה `suspend fun clearPendingJoin()` (לפני סוגר ה-interface), הוסף:

```kotlin

    // ──── העדפת יחידה למיזוג כפילויות (DataStore) ────
    /** "WEIGHT" (ברירת מחדל) או "COUNT". */
    fun getMergeUnitPreference(): kotlinx.coroutines.flow.Flow<String>
    suspend fun setMergeUnitPreference(value: String)
```

- [ ] **Step 2: ממש ב-ListRepositoryImpl**

ב-`ListRepositoryImpl.kt`, הוסף import בראש הקובץ (אחרי `import kotlinx.coroutines.flow.first`):

```kotlin
import kotlinx.coroutines.flow.map
```

הוסף מפתח חדש אחרי `pendingJoinNameKey` (שורה 23):

```kotlin
    private val mergeUnitKey = stringPreferencesKey("merge_unit_preference")
```

הוסף את המימוש לפני סוגר המחלקה:

```kotlin
    override fun getMergeUnitPreference(): Flow<String> =
        dataStore.data.map { it[mergeUnitKey] ?: "WEIGHT" }

    override suspend fun setMergeUnitPreference(value: String) {
        dataStore.edit { it[mergeUnitKey] = value }
    }
```

- [ ] **Step 3: ודא קומפילציה**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/domain/repository/ListRepository.kt app/src/main/java/com/amir/buysmart/data/repository/ListRepositoryImpl.kt
git commit -m "feat: העדפת יחידה למיזוג (merge_unit_preference ב-DataStore)"
```

---

## Task 5: mergeItemsBatch + ItemRepository.mergeDuplicates

**Files:**
- Modify: `app/src/main/java/com/amir/buysmart/data/remote/FirestoreService.kt`
- Modify: `app/src/main/java/com/amir/buysmart/domain/repository/ItemRepository.kt`
- Modify: `app/src/main/java/com/amir/buysmart/data/repository/ItemRepositoryImpl.kt`

- [ ] **Step 1: הוסף mergeItemsBatch ל-FirestoreService**

ב-`FirestoreService.kt`, אחרי הפונקציה `finishShoppingBatch` (מסתיימת בשורה 169), הוסף:

```kotlin

    /**
     * ממזג כפילויות ב-batch אטומי: מעדכן את הפריט השורד ומוחק את המיותרים.
     * כשל רשת באמצע לא משאיר מצב חצי-ממוזג.
     */
    suspend fun mergeItemsBatch(listId: String, survivor: ShoppingItem, deleteIds: List<String>) {
        if (deleteIds.isEmpty()) return
        val batch = firestore.batch()
        batch.update(
            itemsCollection(listId).document(survivor.id),
            mapOf(
                "quantity" to survivor.quantity,
                "note" to survivor.note,
                "priority" to survivor.priority.name,
                "type" to survivor.type.name,
                "imageUrl" to survivor.imageUrl
            )
        )
        deleteIds.forEach { batch.delete(itemsCollection(listId).document(it)) }
        batch.commit().await()
    }
```

- [ ] **Step 2: הוסף חתימה ל-ItemRepository**

ב-`ItemRepository.kt`, לפני סוגר ה-interface, הוסף:

```kotlin
    suspend fun mergeDuplicates(group: List<ShoppingItem>, unitPreference: String)
```

- [ ] **Step 3: ממש ב-ItemRepositoryImpl**

ב-`ItemRepositoryImpl.kt`, הוסף imports בראש הקובץ:

```kotlin
import com.amir.buysmart.domain.util.ItemMerge
import com.amir.buysmart.domain.util.UnitType
```

הוסף את המימוש לפני סוגר המחלקה (אחרי `approvePendingRefill`):

```kotlin
    override suspend fun mergeDuplicates(group: List<ShoppingItem>, unitPreference: String) {
        if (group.size < 2) return
        val pref = if (unitPreference == "COUNT") UnitType.COUNT else UnitType.WEIGHT
        val result = ItemMerge.merge(group, pref)
        firestoreService.mergeItemsBatch(group.first().listId, result.survivor, result.deleteIds)
        itemDao.deleteItemsByIds(result.deleteIds)
        itemDao.updateItem(ShoppingItemEntity.fromDomain(result.survivor))
    }
```

- [ ] **Step 4: ודא קומפילציה**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/data/remote/FirestoreService.kt app/src/main/java/com/amir/buysmart/domain/repository/ItemRepository.kt app/src/main/java/com/amir/buysmart/data/repository/ItemRepositoryImpl.kt
git commit -m "feat: mergeDuplicates - batch אטומי למיזוג כפילויות"
```

---

## Task 6: HomeViewModel - זיהוי, state, פעולה

**Files:**
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt`

- [ ] **Step 1: הוסף שדות ל-HomeUiState**

ב-`HomeViewModel.kt`, ב-data class `HomeUiState`, הוסף שדות (למשל אחרי `pendingRefillItems`):

```kotlin
    // קבוצות כפילות (שם מנורמל → פריטי הקבוצה, size >= 2)
    val duplicateGroups: Map<String, List<ShoppingItem>> = emptyMap(),
    // העדפת יחידה למיזוג ("WEIGHT"/"COUNT")
    val mergeUnitPreference: String = "WEIGHT",
```

- [ ] **Step 2: הוסף import**

בראש הקובץ:

```kotlin
import com.amir.buysmart.domain.util.ItemNameKey
```

- [ ] **Step 3: חשב קבוצות כפילות ב-observeItems**

ב-`observeItems`, בתוך ה-`collect { items -> ... }`, אחרי חישוב `val pending = items.filter { it.pendingRefill }`, הוסף חישוב קבוצות הכפילות והוסף אותו ל-`_uiState.update`:

```kotlin
                val duplicates = activeItems
                    .groupBy { ItemNameKey.of(it.name) }
                    .filter { it.value.size >= 2 }
                _uiState.update { it.copy(
                    itemsByCategory = grouped,
                    pendingRefillItems = pending,
                    totalItems = activeItems.size,
                    duplicateGroups = duplicates
                )}
```

(החלף את ה-`_uiState.update` הקיים בבלוק זה - הוא מוסיף רק את שורת `duplicateGroups`.)

- [ ] **Step 4: אסוף את ההגדרה ב-init**

הוסף בלוק ב-`init {}` (אחרי `restorePendingJoin()`):

```kotlin
        observeMergePreference()
```

והוסף פונקציה פרטית חדשה (למשל ליד `loadActiveList`):

```kotlin
    private fun observeMergePreference() {
        viewModelScope.launch {
            listRepository.getMergeUnitPreference().collect { pref ->
                _uiState.update { it.copy(mergeUnitPreference = pref) }
            }
        }
    }
```

- [ ] **Step 5: הוסף פעולות מיזוג + החלפת העדפה**

הוסף פונקציות חדשות (למשל ליד פעולות ה-QuickAdd):

```kotlin
    fun mergeDuplicates(nameKey: String) {
        val group = _uiState.value.duplicateGroups[nameKey] ?: return
        val pref = _uiState.value.mergeUnitPreference
        viewModelScope.launch {
            try {
                itemRepository.mergeDuplicates(group, pref)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "המיזוג נכשל, נסה שוב") }
            }
        }
    }

    fun setMergeUnitPreference(value: String) {
        viewModelScope.launch { listRepository.setMergeUnitPreference(value) }
    }
```

- [ ] **Step 6: ודא קומפילציה**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeViewModel.kt
git commit -m "feat: HomeViewModel - זיהוי קבוצות כפילות + פעולת מיזוג"
```

---

## Task 7: HomeScreen + LocationSection - הדגשה, כפתור מזג, הגדרה בתפריט

**Files:**
- Modify: `app/src/main/java/com/amir/buysmart/presentation/components/LocationSection.kt`
- Modify: `app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeScreen.kt`

**הקשר (מבנה LocationSection הקיים):** `LocationSection(key, items, onDeleteItem, onEditItem, modifier)` הוא Card עם כותרת קטגוריה, ובתוכו `items.forEach { SwipeableItemRow(...) }`. `SwipeableItemRow` עוטף את `ItemRow`, שמרנדר את שורת הפריט עם רקע `priorityTint(item.priority) ?: surfaceVariant`. נעביר `isDuplicate` + `onMerge` דרך השרשרת LocationSection → SwipeableItemRow → ItemRow.

- [ ] **Step 1: עדכן את ItemRow - רקע אזהרה + כפתור מזג**

ב-`LocationSection.kt`, החלף את חתימת `ItemRow` (שורות 108-114) ואת חישוב `bgColor` (שורה 115):

```kotlin
@Composable
fun ItemRow(
    item: ShoppingItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    isDuplicate: Boolean = false,
    onMerge: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDuplicate) MaterialTheme.colorScheme.errorContainer
                  else priorityTint(item.priority) ?: MaterialTheme.colorScheme.surfaceVariant
```

ואז, בתוך ה-`Row` של `ItemRow`, מיד לפני ה-`TextButton` של "שינוי" (שורה 164), הוסף כפתור מזג שמופיע רק כשהפריט כפול:

```kotlin
        if (isDuplicate) {
            TextButton(
                onClick = onMerge,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("מזג", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
```

- [ ] **Step 2: עדכן SwipeableItemRow - pass-through**

ב-`LocationSection.kt`, החלף את חתימת `SwipeableItemRow` (שורות 64-70) להוספת שני פרמטרים:

```kotlin
@Composable
private fun SwipeableItemRow(
    item: ShoppingItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isDuplicate: Boolean = false,
    onMerge: () -> Unit = {},
    modifier: Modifier = Modifier
) {
```

ובקריאה ל-`ItemRow` בתוכו (שורה 104), העבר את הפרמטרים:

```kotlin
        ItemRow(item = item, onDelete = onDelete, onEdit = onEdit, isDuplicate = isDuplicate, onMerge = onMerge)
```

- [ ] **Step 3: עדכן LocationSection - חתימה + חישוב isDup**

ב-`LocationSection.kt`, החלף את חתימת `LocationSection` (שורות 25-31) להוספת שני פרמטרים:

```kotlin
fun LocationSection(
    key: LocationKey,
    items: List<ShoppingItem>,
    onDeleteItem: (ShoppingItem) -> Unit,
    onEditItem: (ShoppingItem) -> Unit = {},
    duplicateNameKeys: Set<String> = emptySet(),
    onMergeDuplicates: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
```

והחלף את ה-`items.forEach` (שורות 51-58) כך שיחשב `isDup` ויעביר אותו:

```kotlin
            items.forEach { item ->
                val nameKey = ItemNameKey.of(item.name)
                SwipeableItemRow(
                    item = item,
                    onDelete = { onDeleteItem(item) },
                    onEdit = { onEditItem(item) },
                    isDuplicate = duplicateNameKeys.contains(nameKey),
                    onMerge = { onMergeDuplicates(nameKey) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
```

הוסף import בראש `LocationSection.kt`:

```kotlin
import com.amir.buysmart.domain.util.ItemNameKey
```

- [ ] **Step 4: חבר את הפרמטרים בקריאה מ-HomeScreen**

ב-`HomeScreen.kt`, מצא את הקריאה ל-`LocationSection(` (חפש `LocationSection(`) והוסף שני ארגומנטים (לפני `modifier` אם קיים, אחרת בכל מקום בקריאה):

```kotlin
                duplicateNameKeys = uiState.duplicateGroups.keys,
                onMergeDuplicates = viewModel::mergeDuplicates,
```

- [ ] **Step 5: הוסף הגדרה בתפריט ה-overflow**

ב-`HomeScreen.kt`, בתוך ה-`DropdownMenu`, אחרי ה-`DropdownMenuItem` של "עזוב רשימה" (מסתיים בשורה ~180), הוסף:

```kotlin
                            HorizontalDivider()
                            val nextPref = if (uiState.mergeUnitPreference == "WEIGHT") "COUNT" else "WEIGHT"
                            val prefLabel = if (uiState.mergeUnitPreference == "WEIGHT") "משקל" else "יחידות"
                            DropdownMenuItem(
                                text = { Text("העדפת מיזוג: $prefLabel") },
                                leadingIcon = { Icon(Icons.Default.Tune, null) },
                                onClick = {
                                    viewModel.setMergeUnitPreference(nextPref)
                                    overflowExpanded = false
                                }
                            )
```

הוסף import בראש `HomeScreen.kt` אם חסר: `import androidx.compose.material.icons.filled.Tune`.

- [ ] **Step 6: ודא קומפילציה**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/amir/buysmart/presentation/components/LocationSection.kt app/src/main/java/com/amir/buysmart/presentation/screens/home/HomeScreen.kt
git commit -m "feat: HomeScreen - הדגשת כפילויות + כפתור מזג + הגדרת העדפה"
```

---

## Task 8: Build מלא + כל הבדיקות + בדיקה ידנית

**Files:** אין שינוי קוד - אימות.

- [ ] **Step 1: build מלא**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: כל בדיקות היחידה**

Run: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && cd c:/Projects/buy_smart && ./gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL - כל הבדיקות עוברות (ItemNameKey, QuantityMerge, ItemMerge + הקיימות).

- [ ] **Step 3: התקן ובדוק ידנית**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

בדיקות ידניות (סמן כל אחת):
- [ ] שני מכשירים (או הוספה ידנית של שתי שורות "חלב" באותה רשימה) → שתי שורות החלב מודגשות עם אייקון אזהרה וכפתור "מזג".
- [ ] לחיצה על "מזג" → השורות מתאחדות לשורה אחת, הכמות הגדולה נשמרת, ההדגשה נעלמת.
- [ ] הוספת "חלב 1%" ו-"חלב 3%" → מיזוג נותן הערה "1%, 3%".
- [ ] תפריט overflow → "העדפת מיזוג" מתחלף בין "משקל" ל-"יחידות".
- [ ] מיזוג "בצל 5" ו-"בצל 1.5 ק\"ג": בהעדפת "משקל" → התוצאה "1.5 ק\"ג"; בהעדפת "יחידות" → "5".

- [ ] **Step 4: עדכן SPEC + memory**

- ב-`SPEC.md`: הוסף סעיף "מיזוג כפילויות בין משתמשים ✅".
- צור memory `feature_merge_duplicates.md` + עדכן `MEMORY.md`.

- [ ] **Step 5: Commit סופי**

```bash
git add SPEC.md
git commit -m "docs: מיזוג כפילויות בין משתמשים הושלם - עדכון SPEC"
```

---

## Self-Review notes (למבצע)

- **כיסוי spec:** ItemNameKey (Task 1), QuantityMerge (Task 2), mergeItems→ItemMerge (Task 3), ההגדרה (Task 4), שכבת הנתונים mergeItemsBatch+mergeDuplicates (Task 5), ViewModel זיהוי+פעולה (Task 6), UI הדגשה+כפתור+הגדרה (Task 7), בדיקות (Tasks 1-3 + Task 8).
- **שמות עקביים:** `ItemNameKey.of`, `QuantityMerge.merge(a, b, preference: UnitType)`, `UnitType { WEIGHT, COUNT }`, `ItemMerge.merge(group, preference): MergeResult(survivor, deleteIds)`, `FirestoreService.mergeItemsBatch(listId, survivor, deleteIds)`, `ItemRepository.mergeDuplicates(group, unitPreference: String)`, `HomeUiState.duplicateGroups`/`mergeUnitPreference`, מפתח DataStore `"merge_unit_preference"`.
- **Task 7 (UI):** מבנה `LocationSection` נקרא במלואו (LocationSection → SwipeableItemRow → ItemRow) והקוד מדויק מול מספרי השורות הקיימים. השינוי הוא pass-through של `isDuplicate`/`onMerge` דרך השרשרת.
- **mergeDuplicates רץ רק על קבוצה size>=2** (נבדק בשתי שכבות: ItemRepositoryImpl + ItemMerge דרך duplicateGroups שכבר מסונן).
