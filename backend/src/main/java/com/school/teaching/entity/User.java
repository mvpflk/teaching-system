package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("users")
@Schema(description = "用户实体")
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户名", required = true)
    private String username;
    @JsonIgnore
    @Schema(description = "密码", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "头像URL")
    private String avatarUrl;
    @Deprecated
    @Schema(description = "角色ID(已废弃)")
    private Long roleId;
    @Schema(description = "角色名称", example = "TEACHER")
    private String roleName;
    @Schema(description = "学校ID")
    private Long schoolId;
    @Schema(description = "当前学段ID")
    private Long currentStageId;
    @Schema(description = "外部系统ID")
    private String externalId;
    @Schema(description = "外部来源")
    private String externalSource;
    @Schema(description = "状态: 1=启用, 0=禁用")
    private Integer status;
    @Schema(description = "最后登录时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lastLoginTime;
    @Schema(description = "最后登录IP", accessMode = Schema.AccessMode.READ_ONLY)
    private String lastLoginIp;
    @Schema(description = "是否强制修改密码")
    private Integer mustChangePassword;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
