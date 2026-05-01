package com.skicoach.backend.service.impl;

import com.skicoach.backend.common.util.FileUtil;
import com.skicoach.backend.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘文件存储实现
 *
 * 文件路径规则:
 *   {storage.local-base-path}/videos/{user_id}/{yyyy_MM}/{yyyyMMddHHmmss}_{uuid8}.{ext}
 *
 * 数据库存储的是相对路径(不含 base-path),换服务器/迁OSS 不影响数据库。
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${ski.storage.local-base-path}")
    private String basePath;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final DateTimeFormatter FILENAME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String saveVideo(MultipartFile file, Long userId) throws IOException {
        // 1. 生成相对路径
        String extension = FileUtil.getExtension(file.getOriginalFilename());
        String month = LocalDate.now().format(MONTH_FMT);
        String filename = LocalDateTime.now().format(FILENAME_FMT) + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        // 相对路径(数据库存这个)
        String relativePath = String.join("/", "videos", String.valueOf(userId), month, filename);

        // 2. 转绝对路径
        Path absolutePath = Paths.get(basePath, relativePath);

        // 3. 确保父目录存在
        Files.createDirectories(absolutePath.getParent());

        // 4. 保存文件
        try {
            Files.copy(file.getInputStream(), absolutePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存文件失败: {}", absolutePath, e);
            throw e;
        }
        log.info("视频已保存: userId={}, relativePath={}, size={}KB",
                userId, relativePath, file.getSize() / 1024);

        return relativePath;
    }

    @Override
    public String resolveAbsolutePath(String relativePath) {
        return Paths.get(basePath, relativePath).toAbsolutePath().toString();
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            Path path = Paths.get(basePath, relativePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("文件已删除: {}", relativePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("删除文件失败: {}", relativePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(Paths.get(basePath, relativePath));
    }
}
