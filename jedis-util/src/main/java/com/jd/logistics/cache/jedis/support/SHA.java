package com.jd.logistics.cache.jedis.support;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA
 *
 * @author Y.Y.Zhao
 */
class SHA {
    private static final char[] LOWER = "0123456789abcdef".toCharArray();

    static String digest(byte[] script) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(script);
            return new String(encode(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encode bytes to base16 chars.
     *
     * @param src Bytes to encode.
     * @return Encoded chars.
     */
    private static char[] encode(byte[] src) {
        char[] dst = new char[src.length * 2];
        byte b;
        for (int si = 0, di = 0; si < src.length; si++) {
            b = src[si];
            dst[di++] = SHA.LOWER[(b & 0xf0) >>> 4];
            dst[di++] = SHA.LOWER[(b & 0x0f)];
        }

        return dst;
    }
}