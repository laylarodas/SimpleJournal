package com.laylarodas.simplejournal.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests para verificar que las constantes de Firestore son correctas.
 * 
 * Aunque parezca simple, estos tests son útiles porque:
 * 1. Documentan los valores esperados
 * 2. Previenen cambios accidentales que romperían la base de datos
 * 3. Sirven como ejemplo de test unitario básico
 */
class FirestoreConstantsTest {

    // ==========================================
    // TEST: Nombre de la colección
    // ==========================================
    @Test
    fun `collection name is correct`() {
        // El nombre de la colección debe ser exactamente "journalEntries"
        // Si alguien lo cambia accidentalmente, este test fallará
        assertEquals("journalEntries", FirestoreConstants.JOURNAL_COLLECTION)
    }

    // ==========================================
    // TEST: Nombres de campos
    // ==========================================
    @Test
    fun `field names are correct`() {
        // Estos nombres deben coincidir exactamente con los que usa Firestore
        assertEquals("userId", FirestoreConstants.FIELD_USER_ID)
        assertEquals("timestamp", FirestoreConstants.FIELD_TIMESTAMP)
    }

    // ==========================================
    // TEST: Los campos coinciden con JournalEntry.toMap()
    // ==========================================
    @Test
    fun `constants match JournalEntry toMap keys`() {
        // Este test verifica que las constantes coinciden con las claves del Map
        // Creamos una entrada de prueba
        val entry = com.laylarodas.simplejournal.data.model.JournalEntry(
            title = "Test",
            content = "Content",
            timestamp = 123L,
            userId = "user"
        )
        
        val map = entry.toMap()
        
        // Las constantes deben ser claves válidas en el Map
        assertTrue(map.containsKey(FirestoreConstants.FIELD_TIMESTAMP))
        assertTrue(map.containsKey(FirestoreConstants.FIELD_USER_ID))
    }
}
