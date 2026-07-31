package com.lemurs.lemurs_app.survey

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmissionIdTest {

    @Test
    fun `has the shape of a v4 uuid`() {
        val id = SubmissionId.generate()

        assertEquals(36, id.length)
        assertTrue(
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
                .matches(id),
            "not a well-formed v4 uuid: $id"
        )
    }

    @Test
    fun `successive ids differ`() {
        val ids = List(1000) { SubmissionId.generate() }
        assertEquals(1000, ids.toSet().size, "generated a repeated id")
    }

    @Test
    fun `the version and variant bits are set regardless of the random source`() {
        // An all-zero source would produce an invalid uuid if the bits were not
        // forced, and an all-ones source would too.
        val allZero = SubmissionId.generate(object : Random() {
            override fun nextBits(bitCount: Int) = 0
            override fun nextInt(from: Int, until: Int) = 0
        })
        val allMax = SubmissionId.generate(object : Random() {
            override fun nextBits(bitCount: Int) = 0
            override fun nextInt(from: Int, until: Int) = until - 1
        })

        assertEquals('4', allZero[14], "version nibble not set for a zero source")
        assertEquals('4', allMax[14], "version nibble not set for a max source")
        assertTrue(allZero[19] in "89ab", "variant nibble wrong: ${allZero[19]}")
        assertTrue(allMax[19] in "89ab", "variant nibble wrong: ${allMax[19]}")
    }

    @Test
    fun `fits the column the server stores it in`() {
        // survey_response.client_submission_id is VARCHAR(64).
        assertTrue(SubmissionId.generate().length <= 64)
    }
}
