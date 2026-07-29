package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class QuestionBankImportRequest implements Serializable {
    private List<Integer> questionIds;
}
