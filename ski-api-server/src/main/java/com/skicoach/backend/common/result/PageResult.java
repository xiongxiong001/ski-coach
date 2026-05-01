package com.skicoach.backend.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应包装
 */
@Data
@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "当前页")
    private Long pageNum;

    @Schema(description = "每页条数")
    private Long pageSize;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResult<T> from(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }
}
