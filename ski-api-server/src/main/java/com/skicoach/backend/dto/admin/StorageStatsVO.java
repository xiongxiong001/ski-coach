package com.skicoach.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "存储使用情况")
public class StorageStatsVO {

    @Schema(description = "存储根目录")
    private String basePath;

    @Schema(description = "视频总数(数据库统计,不含逻辑删除)")
    private Long activeVideos;

    @Schema(description = "已删除视频数(逻辑删除)")
    private Long deletedVideos;

    @Schema(description = "总存储字节数(数据库 file_size 总和)")
    private Long totalSizeBytes;

    @Schema(description = "总存储 GB(便于阅读)")
    private String totalSizeGB;

    @Schema(description = "存储目录是否存在")
    private Boolean storageDirExists;

    @Schema(description = "存储目录可用空间(字节,系统级)")
    private Long freeSpaceBytes;

    @Schema(description = "存储目录可用空间(GB)")
    private String freeSpaceGB;
}
