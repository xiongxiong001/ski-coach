package com.skicoach.backend.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件工具类
 */
public final class FileUtil {

    private FileUtil() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * 计算 MultipartFile 的 MD5
     */
    public static String md5(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return md5(is);
        }
    }

    /**
     * 计算流的 MD5
     * 调用方负责关闭流
     */
    public static String md5(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                md.update(buffer, 0, n);
            }
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用(JVM问题)", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            result[i * 2] = HEX[v >>> 4];
            result[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(result);
    }

    /**
     * 提取文件扩展名(不含点),全部转小写
     * 例如 "video.MP4" -> "mp4"
     * 没有扩展名返回空字符串
     */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * 判断扩展名是否在允许列表中
     * @param extension 扩展名(不含点),如 "mp4"
     * @param allowedExtensions 允许的扩展名,如 "mp4,mov,m4v"
     */
    public static boolean isExtensionAllowed(String extension, String allowedExtensions) {
        if (extension == null || extension.isEmpty()) return false;
        if (allowedExtensions == null || allowedExtensions.isEmpty()) return false;
        for (String allowed : allowedExtensions.split(",")) {
            if (extension.equalsIgnoreCase(allowed.trim())) return true;
        }
        return false;
    }
}
