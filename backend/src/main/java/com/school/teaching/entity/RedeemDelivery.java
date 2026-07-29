package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("redeem_deliveries")
public class RedeemDelivery implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transactionId;
    private Long studentId;
    private Long itemId;
    private String itemName;
    private Integer creditCost;
    private String status;
    private Long deliveredBy;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getCreditCost() { return creditCost; }
    public void setCreditCost(Integer creditCost) { this.creditCost = creditCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(Long deliveredBy) { this.deliveredBy = deliveredBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
