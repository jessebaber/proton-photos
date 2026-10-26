/*
 * Photos for Proton
 * Copyright (C) 2026 Akoos <https://akoos.eu>
 *
 * Source:  https://github.com/gitakoos/proton-photos
 * Website: https://www.photosforproton.eu
 *
 * This file is part of Photos for Proton.
 *
 * Photos for Proton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package eu.akoos.photos.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks [encodeHexLower] to lowercase two-digit hex without Formatter, matching the prior
 * `"%02x".format` behavior used by [DriveCryptoHelper.sha256Hex] / [DriveCryptoHelper.computeNameHash].
 */
class EncodeHexLowerTest {

    @Test
    fun `known digest bytes encode to expected lowercase hex`() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val digest = byteArrayOf(
            0xe3.toByte(), 0xb0.toByte(), 0xc4.toByte(), 0x42.toByte(),
            0x98.toByte(), 0xfc.toByte(), 0x1c.toByte(), 0x14.toByte(),
            0x9a.toByte(), 0xfb.toByte(), 0xf4.toByte(), 0xc8.toByte(),
            0x99.toByte(), 0x6f.toByte(), 0xb9.toByte(), 0x24.toByte(),
            0x27.toByte(), 0xae.toByte(), 0x41.toByte(), 0xe4.toByte(),
            0x64.toByte(), 0x9b.toByte(), 0x93.toByte(), 0x4c.toByte(),
            0xa4.toByte(), 0x95.toByte(), 0x99.toByte(), 0x1b.toByte(),
            0x78.toByte(), 0x52.toByte(), 0xb8.toByte(), 0x55.toByte(),
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            encodeHexLower(digest),
        )
    }

    @Test
    fun `boundary nibbles stay lowercase and zero-padded`() {
        assertEquals("00010a0fff", encodeHexLower(byteArrayOf(0x00, 0x01, 0x0a, 0x0f, 0xff.toByte())))
        assertEquals("", encodeHexLower(byteArrayOf()))
    }
}
