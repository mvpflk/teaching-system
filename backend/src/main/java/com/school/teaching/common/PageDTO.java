package com.school.teaching.common;

import java.io.Serializable;
import lombok.Data;

@Data
public class PageDTO implements Serializable {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy;
    private String sort = "desc";

    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
