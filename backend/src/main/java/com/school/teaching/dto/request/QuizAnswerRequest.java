package com.school.teaching.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class QuizAnswerRequest implements Serializable {
    private String answerText;
}
