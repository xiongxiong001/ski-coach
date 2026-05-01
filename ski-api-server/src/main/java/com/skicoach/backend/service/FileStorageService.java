package com.skicoach.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储服务
 *
 * 抽象出"保存/获取/删除"等接口,后续从本地存储切换到 OSS 时,
 * 只需提供新的实现类,业务代码不用改。
 */
public interface FileStorageService {

    /**
     * 保存上传的视频文件
     *
     * @param file   上传的文件
     * @param userId 用户ID
     * @return 相对路径(相对于 storage.local-base-path),示例: "videos/123/2026_05/xxx.mp4"
     */
    String saveVideo(MultipartFile file, Long userId) throws IOException;

    /**
     * 把相对路径转成可访问的绝对路径
     * @param relativePath 数据库里存的相对路径
     * @return 绝对路径
     */
    String resolveAbsolutePath(String relativePath);

    /**
     * 删除文件(物理删除)
     */
    boolean delete(String relativePath);

    /**
     * 检查文件是否存在
     */
    boolean exists(String relativePath);
}
