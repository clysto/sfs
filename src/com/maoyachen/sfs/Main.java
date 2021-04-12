package com.maoyachen.sfs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Disk disk = null;
        boolean exist = false;

        if (new File(args[0]).isFile()) {
            exist = true;
        }

        try {
            disk = new Disk(args[0], 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        SimpleFileSystem sfs = new SimpleFileSystem(disk);
        if (!exist) {
            sfs.format();
            System.out.println("监测到为新的硬盘,格式化......");
        }
        try {
            sfs.mount();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        Scanner scanner = new Scanner(System.in);
        String line = "";
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Welcome to Simple File System (SFS), type `quit` to quit.       │");
        System.out.println("│ Login time: " + dtf.format(now) + "                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        while (!line.strip().equals("quit")) {
            System.out.print("$ ");
            line = scanner.nextLine();
            String[] parsed = line.strip().split("\\s+");
            switch (parsed[0]) {
                case "ls":
                    sfs.ls(parsed.length > 1 ? parsed[1] : "");
                    break;
                case "format":
                    sfs.format();
                    break;
                case "cat":
                    sfs.cat(parsed.length > 1 ? parsed[1] : "", 0);
                    break;
                case "touch": {
                    String filename = parsed[1];
                    if (sfs.touch(filename) >= 0) {
                        System.out.println("success.");
                    } else {
                        System.out.println("fail.");
                    }
                    break;
                }
                case "mkdir": {
                    String filename = parsed[1];
                    if (sfs.mkdir(filename) >= 0) {
                        System.out.println("success.");
                    } else {
                        System.out.println("fail.");
                    }
                    break;
                }
                case "write": {
                    ArrayList<String> arrayLines = new ArrayList<>();
                    while (true) {
                        line = scanner.nextLine();
                        if (line.equals("")) {
                            break;
                        } else {
                            arrayLines.add(line);
                        }
                    }
                    if (sfs.write(parsed[1], String.join("\n", arrayLines).getBytes(StandardCharsets.US_ASCII), 0)) {
                        System.out.println("success.");
                    } else {
                        System.out.println("fail.");
                    }
                    break;
                }
                case "hexdump": {
                    sfs.cat(parsed.length > 1 ? parsed[1] : "", 1);
                    break;
                }
                case "rm": {
                    if (sfs.unlink(parsed[1])) {
                        System.out.println("success.");
                    } else {
                        System.out.println("fail.");
                    }
                    break;
                }
            }
        }
    }
}
