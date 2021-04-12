package com.maoyachen.sfs;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;

public class SimpleFileSystem {
    private final Disk disk;
    private BitSet bitMap;
    private int inodeNum;
    private int blockNum;

    public SimpleFileSystem(Disk disk) {
        this.disk = disk;
    }

    /**
     * 格式化磁盘
     */
    public void format() {
        disk.clear();
        byte[] superBlock = emptyBlock();
        superBlock[0] = (byte) 0xf0;
        superBlock[1] = (byte) 0xf0;
        superBlock[2] = (byte) 0x34;
        superBlock[3] = (byte) 0x10;

        // 256 个 inode blocks
        superBlock[4] = (byte) 0x00;
        superBlock[5] = (byte) 0x01;
        superBlock[6] = (byte) 0x00;
        superBlock[7] = (byte) 0x00;

        int dataBlockNum = disk.getSize() / 512 - 256 - 1;
        byte[] dataBlockNumBytes = Util.intToBytes(dataBlockNum);

        System.arraycopy(dataBlockNumBytes, 0, superBlock, 8, 4);
        IndexNode rootInode = new IndexNode();

        initInode(rootInode, (byte) 1);
        saveInode(0, rootInode);

        disk.write(0, superBlock);
    }

    /**
     * 挂载磁盘
     *
     * @throws Exception 挂载失败错误
     */
    public void mount() throws Exception {

        byte[] block = emptyBlock();
        disk.read(0, block);
        // 比较 MAGIC NUMBER
        if (!(block[0] == (byte) 0xf0 && block[1] == (byte) 0xf0 && block[2] == (byte) 0x34 && block[3] == (byte) 0x10)) {
            throw new Exception("挂载失败,无法识别该磁盘的文件系统");
        }

        inodeNum = Util.bytesToInt(Arrays.copyOfRange(block, 4, 8));
        blockNum = Util.bytesToInt(Arrays.copyOfRange(block, 8, 12));

        bitMap = new BitSet(blockNum);
        // 恢复 bitMap
        IndexNode inode;
        for (int i = 1; i < inodeNum + 1; i++) {
            disk.read(i, block);
            // 遍历所有 inode
            for (int j = 0; j < 512 / 32; j++) {
                if (block[j * 32] == 1) {
                    inode = getInode((i - 1) * 512 / 32 + j);
                    for (int k = 0; k <= 5; k++) {
                        int blockIndex = inode.getDirect(k);
                        if (blockIndex > 0) {
                            bitMap.set(blockIndex - inodeNum - 1);
                        }
                    }
                }
            }
        }

    }

    private int getFirstFreeInumber() {
        for (int i = 1; i < inodeNum + 1; i++) {
            byte[] block = emptyBlock();
            disk.read(i, block);
            // 遍历所有 inode
            for (int j = 0; j < 512 / 32; j++) {
                if (block[j * 32] == 0) {
                    return (i - 1) * 512 / 32 + j;
                }
            }
        }
        return -1;
    }

    private int getFirstFreeDataBlock() {
        for (int i = 0; i < blockNum; i++) {
            if (!bitMap.get(i)) {
                bitMap.set(i);
                return i + inodeNum + 1;
            }
        }
        return -1;
    }

    private byte[] emptyBlock() {
        return new byte[512];
    }

    private IndexNode getRootInode() {
        return getInode(0);
    }

    public IndexNode getInode(int inodeIndex) {
        int blockIndex = inodeIndex * 32 / 512 + 1;
        int blockOffset = inodeIndex * 32 % 512;
        byte[] block = emptyBlock();
        disk.read(blockIndex, block);
        return IndexNode.fromBytes(Arrays.copyOfRange(block, blockOffset, blockOffset + 32));
    }

    private String getParentPath(String filename) {
        if (filename.equals("/") || filename.equals("")) return "/";
        String[] path = splitPath(filename);
        if (path.length == 1) {
            return "/";
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < path.length - 1; i++) {
            ret.append("/").append(path[i]);
        }
        return ret.toString();
    }

    /**
     * 创建一个新的文件
     *
     * @param filename 文件名,不超过28个字节
     */
    private int create(String filename, byte type) {
        if (stat(filename) != -1) {
            // 文件已存在
            return -1;
        }
        String parent = getParentPath(filename);
        String[] path = splitPath(filename);
        int parentInumber = stat(parent);
        if (parentInumber == -1) {
            // 父目录不存在
            return -1;
        }
        IndexNode parentInode = getInode(parentInumber);
        if (!parentInode.isDir()) {
            // 父文件不是目录
            return -1;
        }
        // 分配 inode
        int inumber = getFirstFreeInumber();
        IndexNode inode = getInode(inumber);
        initInode(inode, type);
        saveInode(inumber, inode);
        // 目录项 32 字节
        byte[] dir_item = new byte[32];
        System.arraycopy(path[path.length - 1].getBytes(StandardCharsets.US_ASCII), 0, dir_item, 0, path[path.length - 1].length());
        System.arraycopy(Util.intToBytes(inumber), 0, dir_item, 28, 4);
        // 在目录文件中追加一个目录项
        write(parentInumber, dir_item, parentInode.getSize());
        return inumber;
    }

    public int stat(String filename) {
        if (filename.equals("/")) return 0;
        IndexNode inode = getRootInode();
        int inumber = 0;
        String[] path = splitPath(filename);
        for (String s : path) {
            if (inode.getType() != 1) {
                // 不是目录
                return -1;
            }
            byte[] bytes = new byte[inode.getSize()];
            read(inumber, bytes, 0);
            boolean find = false;
            for (int i = 0; i < inode.getSize() / 32; i++) {
                String searchFilename = Util.buildString(bytes, i * 32, 28);
                if (s.equals(searchFilename)) {
                    inumber = Util.bytesToInt(Arrays.copyOfRange(bytes, i * 32 + 28, i * 32 + 32));
                    inode = getInode(inumber);
                    find = true;
                    break;
                }
            }
            if (!find) {
                return -1;
            }
        }
        return inumber;
    }

    private void read(int inumber, byte[] content, int offset) {
        IndexNode inode = getInode(inumber);
        int length = content.length;
        int blockIndex = inode.getDirect(offset / 512);
        byte[] block = emptyBlock();
        disk.read(blockIndex, block);

        int p = offset;
        while (p < offset + length && p < inode.getSize()) {
            if (inode.getDirect(p / 512) != blockIndex) {
                blockIndex = inode.getDirect(p / 512);
                disk.read(blockIndex, block);
            }
            int blockOffset = p % 512;
            content[p - offset] = block[blockOffset];
            p++;
        }
    }

    private void write(int inumber, byte[] content, int offset) {
        IndexNode inode = getInode(inumber);
        int length = content.length;

        if (offset + length > inode.getSize()) {
            // 如果写入的部分超出文件长度重新分配磁盘块
            for (int i = 0; i <= (offset + length - 1) / 512 && i <= 5; i++) {
                if (inode.getDirect(i) == 0) {
                    inode.setDirect(i, getFirstFreeDataBlock());
                }
            }
        }

        inode.setSize(offset + length);
        saveInode(inumber, inode);

        int blockIndex = inode.getDirect(offset / 512);
        byte[] block = emptyBlock();
        disk.read(blockIndex, block);
        int p = offset;
        while (p < offset + length) {
            if (inode.getDirect(p / 512) != blockIndex) {
                disk.write(blockIndex, block);
                blockIndex = inode.getDirect(p / 512);
                disk.read(blockIndex, block);
            }
            int blockOffset = p % 512;
            block[blockOffset] = content[p - offset];
            p++;
        }
        disk.write(blockIndex, block);
    }

    public boolean write(String filename, byte[] content, int offset) {
        int inumber = stat(filename);
        if (inumber == -1) {
            return false;
        }
        IndexNode inode = getInode(inumber);
        if (!inode.isDir()) {
            write(inumber, content, offset);
            return true;
        }
        return false;
    }

    public boolean read(String filename, byte[] content, int offset) {
        int inumber = stat(filename);
        if (inumber == -1) {
            return false;
        }
        IndexNode inode = getInode(inumber);
        if (!inode.isDir()) {
            read(inumber, content, offset);
            return true;
        }
        return false;
    }

    public boolean unlink(String filename) {
        int inumber = stat(filename);
        if (inumber == -1) {
            return false;
        }

        IndexNode inode = getInode(inumber);

        if (inode.isDir()) {
            return false;
        }

        for (int i = 0; i <= 5; i++) {
            int direct = inode.getDirect(i);
            if (direct > 0) {
                bitMap.clear(direct - inodeNum - 1);
            }
        }

        deleteInode(inumber);

        String parent = getParentPath(filename);
        inumber = stat(parent);
        inode = getInode(inumber);

        // 删除目录项
        byte[] bytes = new byte[inode.getSize()];
        byte[] newBytes = new byte[inode.getSize() - 32];
        read(inumber, bytes, 0);
        int index = 0;
        for (int i = 0; i < inode.getSize() / 32; i++) {
            String searchFilename = Util.buildString(bytes, i * 32, 28);
            if (!filename.equals(searchFilename)) {
                System.arraycopy(bytes, i * 32, newBytes, index * 32, 32);
                index++;
            }
        }
        write(inumber, newBytes, 0);
        return true;
    }

    private String[] splitPath(String path) {
        if (path.equals("")) {
            return new String[]{};
        }
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path.split("/");
    }

    /**
     * DEBUG ONLY
     *
     * @param filename 文件名
     */
    public void ls(String filename) {
        int inumber = stat(filename);
        if (inumber == -1) {
            System.out.println(filename + " is not exist.");
            return;
        }
        IndexNode inode = getInode(inumber);
        if (!inode.isDir()) {
            System.out.println(filename + " is not a directory.");
            return;
        }

        byte[] bytes = new byte[inode.getSize()];
        read(inumber, bytes, 0);
        for (int i = 0; i < inode.getSize() / 32; i++) {
            String searchFilename = Util.buildString(bytes, i * 32, 28);
            int fileInumber = Util.bytesToInt(Arrays.copyOfRange(bytes, i * 32 + 28, i * 32 + 32));
            IndexNode fileInode = getInode(fileInumber);
            System.out.println((fileInode.isDir() ? "d " : "- ") + searchFilename);
        }
    }

    public void cat(String filename, int mode) {
        int inumber = stat(filename);
        if (inumber == -1) {
            System.out.println(filename + " is not exist.");
            return;
        }
        IndexNode inode = getInode(inumber);
        if (inode.isDir()) {
            System.out.println(filename + " is a directory.");
            return;
        }
        byte[] content = new byte[inode.getSize()];
        if (mode == 0) {
            if (read(filename, content, 0)) {
                System.out.println(new String(content, StandardCharsets.US_ASCII));
            } else {
                System.out.println("fail.");
            }
        } else if (mode == 1) {
            int index = 0;
            if (read(filename, content, 0)) {
                for (int i = 0; i < content.length / 16 + 1; i++) {
                    for (int j = 0; j < 16; j++) {
                        if (index >= content.length) {
                            break;
                        }
                        System.out.printf("%02x ", content[index++]);
                    }
                    System.out.print('\n');
                }
            } else {
                System.out.println("fail.");
            }
        }
    }

    private void initInode(IndexNode inode, byte type) {
        inode.setValid((byte) 1);
        inode.setType(type);
        inode.setSize(0);
        inode.setDirect0(0);
        inode.setDirect1(0);
        inode.setDirect2(0);
        inode.setDirect3(0);
        inode.setDirect4(0);
        inode.setDirect5(0);
    }

    private void saveInode(int inumber, IndexNode inode) {
        int blockIndex = inumber * 32 / 512 + 1;
        byte[] block = emptyBlock();
        disk.read(blockIndex, block);
        System.arraycopy(inode.toBytes(), 0, block, inumber * 32, 32);
        disk.write(blockIndex, block);
    }

    private void deleteInode(int inumber) {
        int blockIndex = inumber * 32 / 512 + 1;
        byte[] block = emptyBlock();
        disk.read(blockIndex, block);
        block[inumber * 32] = 0;
        disk.write(blockIndex, block);
    }

    public int mkdir(String dirName) {
        return create(dirName, (byte) 1);
    }

    public int touch(String filename) {
        return create(filename, (byte) 0);
    }

}
