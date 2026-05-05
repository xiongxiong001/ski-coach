package com.skicoach.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈
 */
@Data
@TableName("feedbacks")
public class Feedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 反馈类型: bug / feature / performance / other */
    private String type;

    /** 反馈内容 */
    private String content;

    /** 联系方式(手机号/邮箱) */
    private String contact;

    /** 逗号分隔的图片相对路径 */
    private String images;

    /** APP版本号 */
    private String appVersion;

    /** 状态: 0=未处理 1=已查看 2=已回复 */
    private Integer status;

    /** 官方回复内容 */
    private String reply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
