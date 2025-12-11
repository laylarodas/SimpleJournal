package com.laylarodas.simplejournal.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la clase JournalEntry.
 * 
 * Estos tests verifican que:
 * - La creación de objetos funciona correctamente
 * - La conversión a Map (toMap) preserva los datos
 * - La conversión desde Map (fromMap) reconstruye el objeto
 * - El formateo de fecha funciona
 */
class JournalEntryTest {

    // ==========================================
    // TEST 1: Crear una entrada con valores
    // ==========================================
    @Test
    fun `create entry with all values`() {
        // GIVEN: Valores para crear una entrada
        val id = "test-id-123"
        val title = "My First Entry"
        val content = "This is the content of my journal"
        val timestamp = 1702300800000L // Dec 11, 2023
        val userId = "user-abc"

        // WHEN: Creamos la entrada
        val entry = JournalEntry(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            userId = userId
        )

        // THEN: Todos los valores deben ser correctos
        assertEquals(id, entry.id)
        assertEquals(title, entry.title)
        assertEquals(content, entry.content)
        assertEquals(timestamp, entry.timestamp)
        assertEquals(userId, entry.userId)
    }

    // ==========================================
    // TEST 2: Valores por defecto
    // ==========================================
    @Test
    fun `create entry with default values`() {
        // WHEN: Creamos una entrada sin parámetros
        val entry = JournalEntry()

        // THEN: Los valores por defecto deben aplicarse
        assertEquals("", entry.id)
        assertEquals("", entry.title)
        assertEquals("", entry.content)
        assertEquals("", entry.userId)
        // El timestamp debe ser cercano al momento actual
        assertTrue(entry.timestamp > 0)
    }

    // ==========================================
    // TEST 3: Conversión a Map (toMap)
    // ==========================================
    @Test
    fun `toMap converts entry to map correctly`() {
        // GIVEN: Una entrada con datos
        val entry = JournalEntry(
            id = "doc-123",
            title = "Test Title",
            content = "Test Content",
            timestamp = 1702300800000L,
            userId = "user-xyz"
        )

        // WHEN: Convertimos a Map
        val map = entry.toMap()

        // THEN: El Map debe contener los campos correctos
        // Nota: El ID NO se incluye en el Map (Firestore lo maneja aparte)
        assertEquals("Test Title", map["title"])
        assertEquals("Test Content", map["content"])
        assertEquals(1702300800000L, map["timestamp"])
        assertEquals("user-xyz", map["userId"])
        assertFalse(map.containsKey("id")) // ID no debe estar en el Map
    }

    // ==========================================
    // TEST 4: Conversión desde Map (fromMap)
    // ==========================================
    @Test
    fun `fromMap creates entry from map correctly`() {
        // GIVEN: Un Map simulando datos de Firestore
        val documentId = "firestore-doc-id"
        val data = mapOf(
            "title" to "Loaded Title",
            "content" to "Loaded Content",
            "timestamp" to 1702300800000L,
            "userId" to "loaded-user"
        )

        // WHEN: Creamos la entrada desde el Map
        val entry = JournalEntry.fromMap(documentId, data)

        // THEN: La entrada debe tener todos los valores correctos
        assertEquals(documentId, entry.id)
        assertEquals("Loaded Title", entry.title)
        assertEquals("Loaded Content", entry.content)
        assertEquals(1702300800000L, entry.timestamp)
        assertEquals("loaded-user", entry.userId)
    }

    // ==========================================
    // TEST 5: fromMap con datos faltantes
    // ==========================================
    @Test
    fun `fromMap handles missing data gracefully`() {
        // GIVEN: Un Map con datos incompletos (como podría venir de Firestore corrupto)
        val documentId = "doc-with-missing-data"
        val incompleteData = mapOf<String, Any?>(
            "title" to "Only Title"
            // Faltan: content, timestamp, userId
        )

        // WHEN: Creamos la entrada desde el Map incompleto
        val entry = JournalEntry.fromMap(documentId, incompleteData)

        // THEN: Debe usar valores por defecto para los campos faltantes
        assertEquals(documentId, entry.id)
        assertEquals("Only Title", entry.title)
        assertEquals("", entry.content) // Default vacío
        assertEquals("", entry.userId)  // Default vacío
        assertTrue(entry.timestamp > 0) // Default timestamp actual
    }

    // ==========================================
    // TEST 6: fromMap con tipos incorrectos
    // ==========================================
    @Test
    fun `fromMap handles wrong types gracefully`() {
        // GIVEN: Un Map con tipos de datos incorrectos
        val documentId = "doc-wrong-types"
        val wrongTypesData = mapOf<String, Any?>(
            "title" to 12345,        // Debería ser String
            "content" to listOf(1),  // Debería ser String
            "timestamp" to "not a number", // Debería ser Long
            "userId" to null         // Debería ser String
        )

        // WHEN: Creamos la entrada
        val entry = JournalEntry.fromMap(documentId, wrongTypesData)

        // THEN: Debe usar valores por defecto cuando los tipos no coinciden
        assertEquals(documentId, entry.id)
        assertEquals("", entry.title)   // Default porque 12345 no es String
        assertEquals("", entry.content) // Default porque List no es String
        assertEquals("", entry.userId)  // Default porque null no es String
    }

    // ==========================================
    // TEST 7: Round-trip (toMap -> fromMap)
    // ==========================================
    @Test
    fun `round trip toMap and fromMap preserves data`() {
        // GIVEN: Una entrada original
        val original = JournalEntry(
            id = "original-id",
            title = "Round Trip Title",
            content = "Round Trip Content",
            timestamp = 1702300800000L,
            userId = "round-trip-user"
        )

        // WHEN: Convertimos a Map y luego de vuelta a Entry
        val map = original.toMap()
        val restored = JournalEntry.fromMap(original.id, map)

        // THEN: La entrada restaurada debe ser igual a la original
        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.content, restored.content)
        assertEquals(original.timestamp, restored.timestamp)
        assertEquals(original.userId, restored.userId)
    }

    // ==========================================
    // TEST 8: Formato de fecha
    // ==========================================
    @Test
    fun `formattedDate returns readable date`() {
        // GIVEN: Una entrada con timestamp conocido
        // Timestamp: Dec 11, 2023 12:00:00 UTC
        val entry = JournalEntry(
            timestamp = 1702296000000L
        )

        // WHEN: Obtenemos la fecha formateada
        val formatted = entry.formattedDate()

        // THEN: Debe contener "2023" y "Dec" o "dic" (depende del locale)
        assertTrue(
            "Formatted date should contain year: $formatted",
            formatted.contains("2023")
        )
        // Nota: El mes puede variar según el idioma del sistema
    }
}

