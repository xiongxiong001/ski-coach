package com.skicoach.backend.service;

import com.skicoach.backend.common.result.PageResult;
import com.skicoach.backend.dto.video.VideoDetailVO;
import com.skicoach.backend.dto.video.VideoListItemVO;
import com.skicoach.backend.dto.video.VideoListQuery;
import com.skicoach.backend.dto.video.VideoUploadResponse;
import com.skicoach.backend.entity.Video;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    /** 上传视频 */
    VideoUploadResponse upload(Long userId, MultipartFile file);

    /** 我的视频列表(分页) */
    PageResult<VideoListItemVO> list(Long userId, VideoListQuery query);

    /** 视频详情(校验归属) */
    VideoDetailVO getDetail(Long userId, Long videoId);

    /** 删除视频(逻辑删除,校验归属) */
    void delete(Long userId, Long videoId);

    /** 校验视频归属并返回(供其他Service内部使用) */
    Video getOwnedVideoOrThrow(Long userId, Long videoId);
}
