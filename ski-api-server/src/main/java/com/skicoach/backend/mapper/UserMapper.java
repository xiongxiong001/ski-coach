package com.skicoach.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skicoach.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 * 继承 BaseMapper 自动获得 selectById/insert/update/delete 等基础方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
