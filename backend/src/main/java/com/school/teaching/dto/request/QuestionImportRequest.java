package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class QuestionImportRequest implements Serializable {
    private List<Map<String, Object>> rows;
}
