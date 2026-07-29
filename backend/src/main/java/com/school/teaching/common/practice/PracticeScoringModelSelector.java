package com.school.teaching.common.practice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PracticeScoringModelSelector {

    private final Map<ScoringModelType, PracticeScoringModel> models;

    @Autowired
    public PracticeScoringModelSelector(List<PracticeScoringModel> modelList) {
        this.models = modelList.stream()
            .collect(Collectors.toMap(PracticeScoringModel::getType, m -> m, (a, b) -> b));
    }

    public PracticeScoringModel get(ScoringModelType type) {
        return models.get(type);
    }
}
