package com.nsms.batch.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * ファイル・フォルダ操作ユーティリティ
 */
public class FileManager {
    private static final Logger logger = new Logger(FileManager.class);

    /**
     * フォルダを作成
     * @param folderPath フォルダパス
     * @throws IOException
     */
    public static void createFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.info("フォルダを作成しました: " + folderPath);
            } else {
                throw new IOException("フォルダの作成に失敗しました: " + folderPath);
            }
        }
    }

    /**
     * ファイルを削除
     * @param filePath ファイルパス
     * @throws IOException
     */
    public static void deleteFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                logger.info("ファイルを削除しました: " + filePath);
            } else {
                throw new IOException("ファイルの削除に失敗しました: " + filePath);
            }
        }
    }

    /**
     * ファイルを移動
     * @param sourcePath 移動元パス
     * @param targetPath 移動先パス
     * @throws IOException
     */
    public static void moveFile(String sourcePath, String targetPath) throws IOException {
        Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        logger.info("ファイルを移動しました: " + sourcePath + " -> " + targetPath);
    }

    /**
     * フォルダ内のファイル一覧を取得
     * @param folderPath フォルダパス
     * @return ファイル配列
     */
    public static File[] listFiles(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            return folder.listFiles();
        }
        return null;
    }
}
