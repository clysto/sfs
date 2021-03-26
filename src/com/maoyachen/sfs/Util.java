package com.maoyachen.sfs;

import java.nio.charset.StandardCharsets;

public class Util {
    static byte[] intToBytes(int num) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (num);
        ret[1] = (byte) (num >> 8);
        ret[2] = (byte) (num >> 16);
        ret[3] = (byte) (num >> 24);
        return ret;
    }

    static int bytesToInt(byte[] bytes) {
        int ret = 0;
        for (int i = 3; i >= 0; i--) {
            ret <<= 8;
            ret |= ((int) bytes[i] & 0xff);
        }
        return ret;
    }

    static String buildString(byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; i++) {
            if (bytes[offset + i] == '\0') {
                length = i;
            }
        }
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
}
