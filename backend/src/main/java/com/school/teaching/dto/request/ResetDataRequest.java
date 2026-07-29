package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class ResetDataRequest implements Serializable {
    private String confirm;
    private String target;
}
