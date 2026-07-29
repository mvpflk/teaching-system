package com.school.teaching.service;

import java.math.BigDecimal;
import java.util.List;

public interface PrecisionProgressService {

    void markWeakIfNeeded(Long studentId, Long nodeId, Long configId);

    List<Long> findWeakNodeIds(Long studentId, BigDecimal threshold);
}
