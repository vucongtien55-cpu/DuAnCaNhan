package com.lms.dto.grade;

import com.lms.entity.SectionGradeWeight;
import com.lms.entity.enums.GradeType;

import java.math.BigDecimal;

public record GradeWeightResponse(GradeType gradeType, BigDecimal weight) {

    public static GradeWeightResponse from(SectionGradeWeight weight) {
        return new GradeWeightResponse(weight.getGradeType(), weight.getWeight());
    }
}
