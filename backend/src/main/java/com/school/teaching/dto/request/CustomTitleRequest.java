package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class CustomTitleRequest implements Serializable {
    private String titleCode;
}
