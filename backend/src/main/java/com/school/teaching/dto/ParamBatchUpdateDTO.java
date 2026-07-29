package com.school.teaching.dto;

import java.util.List;
import lombok.Data;

@Data
public class ParamBatchUpdateDTO {
    private List<ParamUpdateDTO> updates;
    private String password;  // 修改重要参数时的二次确认密码
}
