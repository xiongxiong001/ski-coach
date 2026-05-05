package com.skicoach.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skicoach.backend.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
