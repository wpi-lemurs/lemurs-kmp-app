package com.lemurs.lemurs_app.survey

import kotlin.random.Random

/**
 * Generates the identifier that makes a submission retry-safe.
 *
 * The value only has to be unique per participant, since the server scopes its uniqueness check to
 * the user. 122 random bits makes an accidental repeat for one participant effectively impossible,
 * which is why this does not need a real UUID library or a platform-specific implementation.
 *
 * The shape is a v4 UUID so the values are recognisable in the database and in logs.
 */
object SubmissionId {

    fun generate(random: Random = Random.Default): String {
        val bytes = ByteArray(16) { random.nextInt(0, 256).toByte() }

        // Set the version (4) and variant bits, so the result is a well-formed
        // v4 UUID rather than 16 arbitrary bytes formatted to look like one.
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        val hex = bytes.joinToString("") { byte ->
            val value = byte.toInt() and 0xFF
            HEX_DIGITS[value shr 4].toString() + HEX_DIGITS[value and 0x0F]
        }

        return buildString {
            append(hex, 0, 8)
            append('-')
            append(hex, 8, 12)
            append('-')
            append(hex, 12, 16)
            append('-')
            append(hex, 16, 20)
            append('-')
            append(hex, 20, 32)
        }
    }

    private const val HEX_DIGITS = "0123456789abcdef"
}
