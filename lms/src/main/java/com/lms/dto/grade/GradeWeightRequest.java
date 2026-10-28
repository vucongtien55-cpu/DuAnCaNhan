package com.lms.dto.grade;

import com.lms.entity.enums.GradeType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Trọng số theo phần trăm, tối đa 2 chữ số thập phân; tổng các thành phần của lớp phải bằng 100. */
public record GradeWeightRequest(
        @NotNull GradeType gradeType,

        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
        @Digits(integer = 3, fraction = 2) BigDecimal weight) {
}
