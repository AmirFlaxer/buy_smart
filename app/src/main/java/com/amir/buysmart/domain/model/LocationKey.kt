package com.amir.buysmart.domain.model

/**
 * מפתח קטגוריה — מובנה (enum) או מותאם אישית (מחרוזת).
 * משמש להצגה ולקיבוץ אחידים.
 */
sealed interface LocationKey {
    val displayName: String
    val emoji: String
    val isCustom: Boolean
    val key: String

    data class BuiltIn(val location: ShoppingLocation) : LocationKey {
        override val displayName: String get() = location.displayName
        override val emoji: String get() = location.emoji
        override val isCustom: Boolean = false
        override val key: String get() = location.name
    }

    data class Custom(val name: String) : LocationKey {
        override val displayName: String get() = name
        override val emoji: String = "🏬"
        override val isCustom: Boolean = true
        override val key: String get() = "CUSTOM:$name"
    }

    companion object {
        fun fromKey(key: String): LocationKey {
            return if (key.startsWith("CUSTOM:")) {
                Custom(key.removePrefix("CUSTOM:"))
            } else {
                try {
                    BuiltIn(ShoppingLocation.valueOf(key))
                } catch (e: Exception) {
                    BuiltIn(ShoppingLocation.OTHER)
                }
            }
        }

        fun fromItem(item: ShoppingItem): LocationKey {
            return if (item.customLocation.isNotBlank()) Custom(item.customLocation)
                   else BuiltIn(item.location)
        }
    }
}
