package com.skicoach.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频实体
 * 对应数据库表: videos
 *
 * 此表同时承载"文件信息"和"分析结果",分析数据存在 analysisDataJson 字段(JSON类型)。
 */
@Data
@TableName("videos")
public class Video implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // ============== 文件信息 ==============
    private String originalFilename;

    private String filePath;          // 相对路径(相对于 storage.local-base-path)

    private String fileMd5;

    private Long fileSize;

    // ============== 视频元信息(分析后填充) ==============
    private BigDecimal durationSeconds;

    private Integer width;

    private Integer height;

    private BigDecimal fps;

    // ============== 第1层分析结果 ==============
    /** pending / analyzing / analyzed / failed */
    private String analysisStatus;

    /** 完整的分析数据JSON(姿态指标、动作分割等) */
    private String analysisDataJson;

    private String analysisVersion;

    private BigDecimal detectionRate;

    private Integer turnLeftCount;

    private Integer turnRightCount;

    private LocalDateTime analysisStartedTime;

    private LocalDateTime analysisFinishedTime;

    private String analysisErrorMessage;

    // ============== 逻辑删除(MyBatis-Plus 自动处理) ==============
    /** 删除时间(NULL=未删除),逻辑删除字段 */
    @TableLogic(value = "null", delval = "NOW()")
    private LocalDateTime deletedTime;

    // ============== 时间字段(自动填充) ==============
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
