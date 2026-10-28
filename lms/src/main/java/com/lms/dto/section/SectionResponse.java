package com.lms.dto.section;

import com.lms.entity.CourseSection;
import com.lms.entity.User;
import com.lms.entity.enums.SectionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SectionResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseTitle,
        String sectionCode,
        String name,
        String semester,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxStudents,
        long enrolledCount,
        String schedule,
        SectionStatus status,
        boolean registrationOpen,
        Long teacherId,
        String teacherName,
        LocalDateTime createdAt) {

    public static SectionResponse from(CourseSection section, long enrolledCount) {
        User teacher = section.getTeacher();
        return new SectionResponse(
                section.getId(),
                section.getCourse().getId(),
                section.getCourse().getCode(),
                section.getCourse().getTitle(),
                section.getSectionCode(),
                section.getName(),
                section.getSemester(),
                section.getStartDate(),
                section.getEndDate(),
                section.getMaxStudents(),
                enrolledCount,
                section.getSchedule(),
                section.getStatus(),
                section.getStatus() == SectionStatus.OPEN_REGISTRATION,
                teacher == null ? null : teacher.getId(),
                teacher == null ? null : teacher.getFullName(),
                section.getCreatedAt());
    }
}
