package com.maoyachen.sfs;

import java.util.Arrays;

public class IndexNode {
    private byte valid;
    private byte type;
    private int size;
    private int direct0;
    private int direct1;
    private int direct2;
    private int direct3;
    private int direct4;
    private int direct5;

    public int getDirect(int index) {
        switch (index) {
            case 0:
                return direct0;
            case 1:
                return direct1;
            case 2:
                return direct2;
            case 3:
                return direct3;
            case 4:
                return direct4;
            case 5:
                return direct5;
            default:
                return -1;
        }
    }

    public void setDirect(int index, int direct) {
        switch (index) {
            case 0:
                setDirect0(direct);
                break;
            case 1:
                setDirect1(direct);
                break;
            case 2:
                setDirect2(direct);
                break;
            case 3:
                setDirect3(direct);
                break;
            case 4:
                setDirect4(direct);
                break;
            case 5:
                setDirect5(direct);
                break;
        }
    }


    public void setValid(byte valid) {
        this.valid = valid;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setDirect0(int direct0) {
        this.direct0 = direct0;
    }

    public void setDirect1(int direct1) {
        this.direct1 = direct1;
    }

    public void setDirect2(int direct2) {
        this.direct2 = direct2;
    }

    public void setDirect3(int direct3) {
        this.direct3 = direct3;
    }

    public void setDirect4(int direct4) {
        this.direct4 = direct4;
    }

    public void setDirect5(int direct5) {
        this.direct5 = direct5;
    }

    public int getValid() {
        return valid;
    }

    public boolean isDir() {
        return type == 1;
    }

    public byte getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public int getDirect0() {
        return direct0;
    }

    public int getDirect1() {
        return direct1;
    }

    public int getDirect2() {
        return direct2;
    }

    public int getDirect3() {
        return direct3;
    }

    public int getDirect4() {
        return direct4;
    }

    public int getDirect5() {
        return direct5;
    }

    public byte[] toBytes() {
        byte[] ret = new byte[32];
        ret[0] = valid;
        ret[1] = type;
        System.arraycopy(Util.intToBytes(size), 0, ret, 4, 4);
        System.arraycopy(Util.intToBytes(direct0), 0, ret, 8, 4);
        System.arraycopy(Util.intToBytes(direct1), 0, ret, 12, 4);
        System.arraycopy(Util.intToBytes(direct2), 0, ret, 16, 4);
        System.arraycopy(Util.intToBytes(direct3), 0, ret, 20, 4);
        System.arraycopy(Util.intToBytes(direct4), 0, ret, 24, 4);
        System.arraycopy(Util.intToBytes(direct5), 0, ret, 28, 4);
        return ret;
    }

    public static IndexNode fromBytes(byte[] bytes) {
        IndexNode inode = new IndexNode();
        inode.valid = bytes[0];
        inode.type = bytes[1];
        inode.size = Util.bytesToInt(Arrays.copyOfRange(bytes, 4, 8));
        inode.direct0 = Util.bytesToInt(Arrays.copyOfRange(bytes, 8, 12));
        inode.direct1 = Util.bytesToInt(Arrays.copyOfRange(bytes, 12, 16));
        inode.direct2 = Util.bytesToInt(Arrays.copyOfRange(bytes, 16, 20));
        inode.direct3 = Util.bytesToInt(Arrays.copyOfRange(bytes, 20, 24));
        inode.direct4 = Util.bytesToInt(Arrays.copyOfRange(bytes, 24, 28));
        inode.direct5 = Util.bytesToInt(Arrays.copyOfRange(bytes, 28, 32));
        return inode;
    }

}
