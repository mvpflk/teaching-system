package com.school.teaching.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class TermRequest implements Serializable {
    private String termName;

    private String startDate;

    private String endDate;

    private Integer status;
}
