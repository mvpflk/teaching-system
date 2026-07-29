package com.school.teaching.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class PageResult<T> implements Serializable {
    private Long total;
    private List<T> records;
    private Long current;
    private Long size;
    private Long pages;

    public PageResult() {}

    public PageResult(IPage<T> page) {
        this.total = page.getTotal();
        this.records = page.getRecords();
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.pages = page.getPages();
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page);
    }
}
