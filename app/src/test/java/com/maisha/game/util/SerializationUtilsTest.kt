package com.maisha.game.util

import com.maisha.game.data.model.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationUtilsTest {

    @Test
    fun safeDeserialize_malformedJson_returnsDefault() {
        val result = SerializationUtils.safeDeserialize(
            raw = "{not valid json",
            fieldName = "family",
            default = emptyList<Person>(),
            slotId = 1
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun safeDeserialize_validJson_returnsParsedValue() {
        val json = """[{"id":"p1","name":"Pat","relation":"SIBLING","gender":"FEMALE","age":20,"relationshipLevel":60,"interactedThisYear":false,"countryCode":"KE","isMarried":false}]"""
        val result = SerializationUtils.safeDeserialize(
            raw = json,
            fieldName = "family",
            default = emptyList<Person>(),
            slotId = 0
        )
        assertEquals(1, result.size)
        assertEquals("Pat", result.first().name)
    }

    @Test
    fun safeDeserialize_shapeMismatch_returnsDefault() {
        val json = """[{"unexpectedField": true}]"""
        val result = SerializationUtils.safeDeserialize(
            raw = json,
            fieldName = "family",
            default = emptyList<Person>(),
            slotId = 2
        )
        assertTrue(result.isEmpty())
    }
}
