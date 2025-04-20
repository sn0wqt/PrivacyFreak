package com.sn0wqt.privacyfreak.utils;

public class FileUtils {
    /**
     * Gets the file extension from a filename.
     */
    public static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}