package com.skicoach.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequest {

    /** 视频文件的绝对路径(由Java端解析后传给Python) */
    @JsonProperty("video_path")
    private String videoPath;
}
