package com.amir.buysmart.domain.model

object ItemNotePresets {

    private val presets: List<Pair<String, List<String>>> = listOf(
        "חלב" to listOf("בקבוק", "קרטון", "דל לקטוז", "3% שומן", "1% שומן", "1.5 ליטר", "2 ליטר"),
        "לחם" to listOf("שחור", "לבן", "מלא", "חלה", "ללא גלוטן", "פרוס"),
        "גבינה" to listOf("צהובה", "לבנה", "קוטג'", "5%", "9%", "ממרח", "בולגרית"),
        "יוגורט" to listOf("תות", "ביו", "לייט", "גדול", "קטן", "טבעי"),
        "ביצים" to listOf("M", "L", "XL", "6 יחידות", "12 יחידות", "חופשיות"),
        "עוף" to listOf("שלם", "חזה", "ירכיים", "כנפיים", "טחון", "ללא עור", "0.5 ק\"ג", "1 ק\"ג"),
        "בשר" to listOf("בקר", "עגל", "טחון", "אסאדו", "אנטריקוט", "צוואר", "0.5 ק\"ג", "1 ק\"ג"),
        "דג" to listOf("סלמון", "בקלה", "טונה", "מוסר ים", "טרי", "קפוא", "פילה"),
        "שמן" to listOf("זית", "קנולה", "חמניות", "קטן", "גדול", "תרסיס"),
        "אורז" to listOf("לבן", "מלא", "בסמטי", "יסמין", "1 ק\"ג", "2 ק\"ג"),
        "פסטה" to listOf("ספגטי", "פנה", "פרפרים", "ריגטוני", "500 גרם", "1 ק\"ג"),
        "תפוח אדמה" to listOf("1 ק\"ג", "2 ק\"ג", "קטן", "גדול", "חדש"),
        "בצל" to listOf("לבן", "סגול", "1 ק\"ג", "2 ק\"ג", "ירוק"),
        "עגבנייה" to listOf("שרי", "רגיל", "1 ק\"ג", "רומא", "לסלט"),
        "מלפפון" to listOf("קטן", "גדול", "חממה", "1 ק\"ג"),
        "גזר" to listOf("1 ק\"ג", "2 ק\"ג", "קטן", "גדול", "מקולף"),
        "בננה" to listOf("אשכול", "1 ק\"ג", "בשל", "ירוק"),
        "תפוח" to listOf("גרנד", "גאלה", "פינק ליידי", "1 ק\"ג", "2 ק\"ג", "ירוק", "אדום"),
        "לימון" to listOf("1 ק\"ג", "יורו"),
        "מים" to listOf("מינרלים", "מסוננים", "קטן", "גדול", "500 מ\"ל", "1.5 ליטר", "6 בקבוקים"),
        "קפה" to listOf("בוץ", "נס", "קפסולות", "אספרסו", "250 גרם", "500 גרם", "טחון"),
        "תה" to listOf("שחור", "ירוק", "נענע", "קמומיל", "אדום", "פירות יער"),
        "חמאה" to listOf("רגיל", "ללא מלח", "קלה", "מלוחה"),
        "שמנת" to listOf("מתוקה", "חמוצה", "לבישול", "לקפה", "38%", "15%"),
        "מיץ" to listOf("תפוחים", "תפוזים", "ענבים", "אשכולית", "טבעי", "1 ליטר", "קטן"),
        "שוקולד" to listOf("מריר", "חלב", "לבן", "אגוזים", "100 גרם", "200 גרם"),
        "עוגיות" to listOf("שוקו", "חמאה", "שיבולת שועל", "קרמבו"),
        "חטיף" to listOf("מלוח", "מתוק", "שקדי מלך", "במבה", "ביסלי"),
        "תרופה" to listOf("לכאב ראש", "לחום", "לשיעול", "לאלרגיה", "ויטמין C"),
        "ויטמין" to listOf("C", "D", "B12", "אומגה 3", "מולטי ויטמין"),
        "סבון" to listOf("כביסה", "כלים", "ידיים", "גוף", "נוזלי", "אבקה"),
        "נייר" to listOf("טואלט", "מגבות", "אלומיניום", "יד"),
    )

    private val locationHints: List<Pair<String, ShoppingLocation>> = listOf(
        "לחם" to ShoppingLocation.BAKERY,
        "חלה" to ShoppingLocation.BAKERY,
        "פיתה" to ShoppingLocation.BAKERY,
        "בגט" to ShoppingLocation.BAKERY,
        "קרואסון" to ShoppingLocation.BAKERY,
        "עוגה" to ShoppingLocation.BAKERY,
        "מאפה" to ShoppingLocation.BAKERY,
        "תרופה" to ShoppingLocation.PHARMACY,
        "ויטמין" to ShoppingLocation.PHARMACY,
        "אספירין" to ShoppingLocation.PHARMACY,
        "קרם" to ShoppingLocation.PHARMACY,
        "שמפו" to ShoppingLocation.PHARMACY,
        "עגבנייה" to ShoppingLocation.GREENGROCER,
        "מלפפון" to ShoppingLocation.GREENGROCER,
        "בצל" to ShoppingLocation.GREENGROCER,
        "תפוח" to ShoppingLocation.GREENGROCER,
        "בננה" to ShoppingLocation.GREENGROCER,
        "לימון" to ShoppingLocation.GREENGROCER,
        "גזר" to ShoppingLocation.GREENGROCER,
        "תרד" to ShoppingLocation.GREENGROCER,
        "חסה" to ShoppingLocation.GREENGROCER,
        "פטרוזיליה" to ShoppingLocation.GREENGROCER,
        "תפוח אדמה" to ShoppingLocation.GREENGROCER,
        "אבטיח" to ShoppingLocation.GREENGROCER,
        "מלון" to ShoppingLocation.GREENGROCER,
        "ענבים" to ShoppingLocation.GREENGROCER,
        "פלפל" to ShoppingLocation.GREENGROCER,
        "חציל" to ShoppingLocation.GREENGROCER,
        "כרובית" to ShoppingLocation.GREENGROCER,
        "ברוקולי" to ShoppingLocation.GREENGROCER,
        "בשר" to ShoppingLocation.DELI,
        "עוף" to ShoppingLocation.DELI,
        "דג" to ShoppingLocation.DELI,
        "קציצה" to ShoppingLocation.DELI,
        "שניצל" to ShoppingLocation.DELI,
        "נקניק" to ShoppingLocation.DELI,
        "סלמי" to ShoppingLocation.DELI,
        "פסטרמה" to ShoppingLocation.DELI,
        "נקניקייה" to ShoppingLocation.DELI,
    )

    fun getPresetNotes(name: String): List<String> {
        if (name.length < 2) return emptyList()
        val lower = name.trim().lowercase()
        return presets
            .firstOrNull { (key, _) -> lower.contains(key) || key.contains(lower) }
            ?.second ?: emptyList()
    }

    fun suggestLocation(name: String): ShoppingLocation? {
        if (name.length < 2) return null
        val lower = name.trim().lowercase()
        return locationHints
            .firstOrNull { (key, _) -> lower.contains(key) || key.contains(lower) }
            ?.second
    }
}
