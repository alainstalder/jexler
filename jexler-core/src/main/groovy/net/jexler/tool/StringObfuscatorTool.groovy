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

import groovy.transform.CompileStatic

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

/**
 * Tool for (de-)obfuscating strings.
 * 
 * Uses by default 128 bit AES in CBC mode with PKCS#5 padding
 * with a hard-coded key and iv.
 * 
 * See user's guide for more information and source code for full details.
 *
 * @author $(whois jexler.net)
 */
@CompileStatic
class StringObfuscatorTool {
    
    private static final int DEFAULT_BYTE_BUFFER_PAD_LEN = 64
    private static final int MIN_SALT_LEN = 16
    private static final int LEN_BYTES_LEN = 1

    private SecretKeySpec key
    private IvParameterSpec iv
    private Cipher cipher
    private int byteBufferPadLen
    
    /**
     * Default constructor.
     * Chooses 128 bit AES (AES/CBC/PKCS5Padding) with a hard-coded default key and iv,
     * and sets byteBufferPadLen to 64, which limits plain strings to max 47 characters
     * (resp. less if some plain string characters need more than one byte UTF-8 encoded).
     */
    StringObfuscatorTool() throws NoSuchAlgorithmException, NoSuchPaddingException {
        setParameters('62e0c45a20dfe429543212be640c3254', 'b42de953243ab9edf03bdac61344bec5',
                'AES', 'AES/CBC/PKCS5Padding')
        byteBufferPadLen = DEFAULT_BYTE_BUFFER_PAD_LEN
    }
    
    /**
     * Set key, iv, algorithm and transformation.
     * @return this (for chaining calls)
     */
    StringObfuscatorTool setParameters(final String hexKey, final String hexIv,
                                       final String algorithm, final String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        key = new SecretKeySpec(hexKey.decodeHex(), algorithm)
        iv = new IvParameterSpec(hexIv.decodeHex())
        cipher = Cipher.getInstance(transformation)
        return this
    }
    
    /**
     * Set the length to which to pad the plain string as UTF-8 encoded byte buffer.
     * @return this (for chaining calls)
     */
    StringObfuscatorTool setByteBufferPadLen(final int len) {
        byteBufferPadLen = len
        return this
    }
            
    /**
     * UTF-8 encode, pad with random bytes, encipher and hex encode given string.
     * @throws IllegalArgumentException if the string is too long (byteBufferPadLen)
     * @return obfuscated string
     */
    String obfuscate(final String plain)
            throws InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException {
        final byte[] plainBytes = plain.getBytes('UTF-8')
        final int lenActual = plainBytes.length
        final int lenMaxAllowed = byteBufferPadLen - MIN_SALT_LEN - LEN_BYTES_LEN
        if (lenActual > lenMaxAllowed) {
            throw new IllegalArgumentException(
                    "Input string too long ($lenActual bytes UTF-8 encoded, max allowed: $lenMaxAllowed)")
        }
        final byte[] plainPaddedBytes = new byte[byteBufferPadLen]
        final int lenSaltBytes = byteBufferPadLen - lenActual - LEN_BYTES_LEN
        final byte[] saltBytes = new byte[lenSaltBytes]
        final SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(saltBytes)
        System.arraycopy(saltBytes, 0, plainPaddedBytes, 0, lenSaltBytes)
        System.arraycopy(plainBytes, 0, plainPaddedBytes, lenSaltBytes, lenActual)
        plainPaddedBytes[byteBufferPadLen-1] = (byte)lenActual
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        final byte[] enc = cipher.doFinal(plainPaddedBytes)
        return enc.encodeHex().toString()
    }
    
    /**
     * Hex decode, decipher, unpad and UTF-8 decode given string.
     * @return deobfuscated string
     */
    String deobfuscate(final String encHex)
            throws InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException {
        final byte[] enc = encHex.decodeHex()
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        final byte[] plain = cipher.doFinal(enc)
        if (plain.length != byteBufferPadLen) {
            throw new IllegalArgumentException(
                    "Illegal length of deciphered buffer ($plain.length bytes, expected $byteBufferPadLen)")
        }
        final int lenPlainBytes = plain[byteBufferPadLen-1] & 0xff
        final int offs = byteBufferPadLen - LEN_BYTES_LEN - lenPlainBytes
        return new String(plain, offs, lenPlainBytes, 'UTF-8')
    }

}
