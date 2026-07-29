package com.school.teaching.service.impl;

import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

    @Mock private StudentMapper studentMapper;
    @Mock private CreditRuleMapper creditRuleMapper;
    @Mock private CreditTransactionMapper transactionMapper;
    @Mock private CreditShopItemMapper shopItemMapper;
    @Mock private SignRecordMapper signRecordMapper;
    @Mock private TitleLevelMapper titleLevelMapper;
    @Mock private UserMapper userMapper;
    @Mock private ClassesMapper classesMapper;
    @Mock private RedeemDeliveryMapper redeemDeliveryMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks private CreditServiceImpl creditService;

    // ── getCreditInfo ─────────────────────────────────

    @Test @DisplayName("getCreditInfo: 返回学生积分摘要")
    void getCreditInfo_shouldReturnStudentInfo() {
        Student stu = new Student();
        stu.setId(1L); stu.setUserId(10L); stu.setTotalCredits(150);
        stu.setTitleLevel(3); stu.setCurrentStreak(5); stu.setClassId(1L);
        when(studentMapper.selectById(1L)).thenReturn(stu);
        when(signRecordMapper.isSignedToday(1L)).thenReturn(1);

        Map<String, Object> info = creditService.getCreditInfo(1L);
        assertNotNull(info);
        assertEquals(150, info.get("totalCredits"));
        assertEquals(5, info.get("currentStreak"));
    }

    @Test @DisplayName("getCreditInfo: 学生不存在返回空 Map")
    void getCreditInfo_shouldReturnEmptyForUnknown() {
        when(studentMapper.selectById(999L)).thenReturn(null);
        assertTrue(creditService.getCreditInfo(999L).isEmpty());
    }

    // ── getShopItems ──────────────────────────────────

    @Test @DisplayName("getShopItems: 返回商城商品列表")
    void getShopItems_shouldReturnItems() {
        CreditShopItem item = new CreditShopItem();
        item.setId(1L); item.setItemName("钢笔"); item.setCreditPrice(50);
        item.setStatus(1);
        when(shopItemMapper.selectList(any())).thenReturn(List.of(item));

        List<CreditShopItem> items = creditService.getShopItems();
        assertEquals(1, items.size());
        assertEquals("钢笔", items.get(0).getItemName());
    }

    // ── redeemItem ────────────────────────────────────

    @Test @DisplayName("redeemItem: 积分不足抛异常")
    void redeemItem_shouldThrowWhenNotEnoughCredits() {
        Student stu = new Student(); stu.setId(1L); stu.setTotalCredits(10);
        CreditShopItem item = new CreditShopItem();
        item.setId(1L); item.setItemName("钢笔"); item.setCreditPrice(50);
        item.setStatus(1); // 必须为已上架
        when(studentMapper.selectById(1L)).thenReturn(stu);
        when(shopItemMapper.selectById(1L)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> creditService.redeemItem(1L, 1L));
        assertTrue(ex.getMessage().contains("积分不足"));
    }

    @Test @DisplayName("redeemItem: 积分充足正常兑换")
    void redeemItem_shouldSucceedWhenEnoughCredits() {
        Student stu = new Student(); stu.setId(1L); stu.setUserId(10L);
        stu.setTotalCredits(100); stu.setCurrentStreak(1);
        CreditShopItem item = new CreditShopItem();
        item.setId(1L); item.setItemName("钢笔"); item.setCreditPrice(50);
        item.setStatus(1); item.setSortOrder(1);
        when(studentMapper.selectById(1L)).thenReturn(stu);
        when(shopItemMapper.selectById(1L)).thenReturn(item);
        // 乐观锁更新成功
        when(studentMapper.update(isNull(), any())).thenReturn(1);
        when(transactionMapper.insert(any())).thenReturn(1);
        when(shopItemMapper.updateById(any())).thenReturn(1);
        when(redeemDeliveryMapper.insert(any())).thenReturn(1);

        Map<String, Object> r = creditService.redeemItem(1L, 1L);
        assertNotNull(r.get("remainingCredits"));
    }

    // ── getTitleLevels ────────────────────────────────

    @Test @DisplayName("getTitleLevels: 返回称号等级列表")
    void getTitleLevels_shouldReturnLevels() {
        TitleLevel t = new TitleLevel(); t.setId(1L); t.setLevelName("学霸");
        when(titleLevelMapper.selectList(any())).thenReturn(List.of(t));
        assertEquals(1, creditService.getTitleLevels().size());
    }
}
