/*
   Copyright 2012-now $(whois jexler.net)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package net.jexler.tool

import net.jexler.test.FastTests

import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * Tests the respective class.
 *
 * @author $(whois jexler.net)
 */
@Category(FastTests.class)
class StringObfuscatorToolSpec extends Specification {

    def 'TEST default instance, plain fits'() {
        given:
        final def tool = new StringObfuscatorTool()
        final int byteBufferPadLen = 64
        final int blockSize = 16
        final int expectedObfuscatedLen = 2 * (byteBufferPadLen + blockSize)

        expect:
        final def obfus1 = tool.obfuscate(plain)
        final def obfus2 = tool.obfuscate(plain)
        obfus1.length() == expectedObfuscatedLen
        obfus2.length() == expectedObfuscatedLen
        obfus1 != obfus2
        tool.deobfuscate(obfus1) == plain
        tool.deobfuscate(obfus2) == plain

        where:
        plain << [ 'test', 'longer string with unicode \u03D4', '',
                   '12345678901234567890123456789012345678901234567' ] // maximal length
    }

    def 'TEST default instance, plain too long (in UTF-8)'() {
        given:
        final def tool = new StringObfuscatorTool()

        when:
        tool.obfuscate('12345678901234567890123456789012345678901234567\u1234')

        then:
        IllegalArgumentException e = thrown()
        e.message == 'Input string too long (50 bytes UTF-8 encoded, max allowed: 47)'
    }

    def 'TEST custom instance, plain fits'() {
        given:
        final def tool = new StringObfuscatorTool()
        tool.setParameters('0011223344556677', 'aabbccddeeff0011', 'DES', 'DES/CBC/PKCS5Padding')
        final int byteBufferPadLen = 128
        tool.byteBufferPadLen = byteBufferPadLen
        final int blockSize = 8
        final int expectedObfuscatedLen = 2 * (byteBufferPadLen + blockSize)

        expect:
        final def obfus1 = tool.obfuscate(plain)
        final def obfus2 = tool.obfuscate(plain)
        obfus1.length() == expectedObfuscatedLen
        obfus2.length() == expectedObfuscatedLen
        obfus1 != obfus2
        tool.deobfuscate(obfus1) == plain
        tool.deobfuscate(obfus2) == plain

        where:
        plain << [ 'test', 'longer string with unicode \u03D4', '',
                   '12345678901234567890123456789012345678901234567890' +
                   '12345678901234567890123456789012345678901234567890' +
                   '12345678901' ] // maximal length
    }

    def 'TEST custom instance, plain too long (in UTF-8)'() {
        given:
        final def tool = new StringObfuscatorTool()
        tool.setParameters('0011223344556677', 'aabbccddeeff0011', 'DES', 'DES/CBC/PKCS5Padding')
        final int byteBufferPadLen = 128
        tool.byteBufferPadLen = byteBufferPadLen

        when:
        final def plain = '12345678901234567890123456789012345678901234567890' +
                '12345678901234567890123456789012345678901234567890' +
                '1234567890\u1234'
        tool.obfuscate(plain)

        then:
        final IllegalArgumentException e = thrown()
        e.message == 'Input string too long (113 bytes UTF-8 encoded, max allowed: 111)'
    }

    def 'TEST custom instance, different byte buffer pad len'() {
        given:
        final def tool = new StringObfuscatorTool()
        tool.setParameters('0011223344556677', 'aabbccddeeff0011', 'DES', 'DES/CBC/PKCS5Padding')
        final int byteBufferPadLen = 128
        tool.byteBufferPadLen = byteBufferPadLen

        when:
        final def obfuscated = tool.obfuscate('test')
        tool.byteBufferPadLen = 64
        tool.deobfuscate(obfuscated)

        then:
        final IllegalArgumentException e = thrown()
        e.message == 'Illegal length of deciphered buffer (128 bytes, expected 64)'
    }

}
