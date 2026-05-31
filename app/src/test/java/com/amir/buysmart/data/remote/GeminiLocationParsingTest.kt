package com.amir.buysmart.data.remote

import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiLocationParsingTest {

    @Test
    fun `מזהה ירקניה`() {
        assertEquals(ShoppingLocation.GREENGROCER, GeminiLocationClassifier.parseLocation("ירקניה"))
        assertEquals(ShoppingLocation.GREENGROCER, GeminiLocationClassifier.parseLocation("ירקן"))
    }

    @Test
    fun `מזהה מזון מוכן בשתי צורות הכתיב`() {
        assertEquals(ShoppingLocation.DELI, GeminiLocationClassifier.parseLocation("מזון_מוכן"))
        assertEquals(ShoppingLocation.DELI, GeminiLocationClassifier.parseLocation("מזון מוכן"))
    }

    @Test
    fun `מזהה מאפייה ובית מרקחת`() {
        assertEquals(ShoppingLocation.BAKERY, GeminiLocationClassifier.parseLocation("מאפייה"))
        assertEquals(ShoppingLocation.PHARMACY, GeminiLocationClassifier.parseLocation("בית מרקחת"))
        assertEquals(ShoppingLocation.PHARMACY, GeminiLocationClassifier.parseLocation("בית_מרקחת"))
    }

    @Test
    fun `'אחר' מתפרש כ-null`() {
        assertNull(GeminiLocationClassifier.parseLocation("אחר"))
    }

    @Test
    fun `מילה לא מוכרת נופלת לסופר`() {
        assertEquals(ShoppingLocation.SUPERMARKET, GeminiLocationClassifier.parseLocation("סופר"))
        assertEquals(ShoppingLocation.SUPERMARKET, GeminiLocationClassifier.parseLocation("משהו לא צפוי"))
    }

    @Test
    fun `טקסט ריק מחזיר null`() {
        assertNull(GeminiLocationClassifier.parseLocation(""))
        assertNull(GeminiLocationClassifier.parseLocation("   "))
    }

    @Test
    fun `רווחים ואותיות רישיות לא משבשים`() {
        assertEquals(ShoppingLocation.BAKERY, GeminiLocationClassifier.parseLocation("  מאפייה  "))
    }
}
