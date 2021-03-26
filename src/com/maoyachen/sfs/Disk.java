package com.maoyachen.sfs;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Disk {
    private final RandomAccessFile file;
    private final int size;

    public Disk(String path, int size) throws IOException {
        file = new RandomAccessFile(path, "rw");
        this.size = size;
        if (file.length() != size) {
            clear();
        }
    }

    public void clear() {
        byte[] content = new byte[size];
        try {
            file.seek(0);
            file.write(content);
            file.seek(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(int blockIndex, byte[] block) {
        try {
            file.seek(blockIndex * 512L);
            file.write(block);
            file.seek(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read(int blockIndex, byte[] block) {
        try {
            file.seek(blockIndex * 512L);
            file.read(block);
            file.seek(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getSize() {
        return size;
    }
}
