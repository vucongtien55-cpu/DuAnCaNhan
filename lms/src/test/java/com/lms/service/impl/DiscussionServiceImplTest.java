package com.lms.service.impl;

import com.lms.dto.discussion.CreateDiscussionRequest;
import com.lms.dto.discussion.DiscussionDetailResponse;
import com.lms.dto.discussion.DiscussionSummaryResponse;
import com.lms.dto.discussion.ReplyContentRequest;
import com.lms.dto.discussion.ReplyRequest;
import com.lms.dto.discussion.ReplyResponse;
import com.lms.dto.discussion.UpdateDiscussionRequest;
import com.lms.entity.CourseSection;
import com.lms.entity.Discussion;
import com.lms.entity.DiscussionReply;
import com.lms.entity.Enrollment;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.enums.EnrollmentStatus;
import com.lms.entity.enums.RoleName;
import com.lms.entity.enums.SectionStatus;
import com.lms.exception.BusinessException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseSectionRepository;
import com.lms.repository.DiscussionReplyRepository;
import com.lms.repository.DiscussionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.support.SectionAccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LECTURER_ID = 2L;
    private static final Long OTHER_LECTURER_ID = 3L;
    private static final Long STUDENT_ID = 4L;
    private static final Long OTHER_STUDENT_ID = 5L;
    private static final Long SECTION_ID = 20L;
    private static final Long DISCUSSION_ID = 60L;
    private static final Long REPLY_ID = 70L;

    @Mock
    private DiscussionRepository discussionRepository;
    @Mock
    private DiscussionReplyRepository replyRepository;
    @Mock
    private CourseSectionRepository sectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private DiscussionServiceImpl service;

    @BeforeEach
    void setUp() {
        SectionAccessChecker access = new SectionAccessChecker(userRepository, enrollmentRepository);
        service = new DiscussionServiceImpl(discussionRepository, replyRepository, sectionRepository, access);
    }

    // ---------- helpers ----------

    private User user(Long id, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        return user;
    }

    private User asUser(Long id, RoleName roleName) {
        User user = user(id, roleName);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    /** Đăng ký sinh viên vào lớp (cần cho quyền đọc của sinh viên). */
    private void enroll(Long studentId, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(status);
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, SECTION_ID))
                .thenReturn(Optional.of(enrollment));
    }

    private CourseSection section(SectionStatus status, User teacher) {
        CourseSection section = new CourseSection();
        section.setId(SECTION_ID);
        section.setStatus(status);
        section.setTeacher(teacher);
        return section;
    }

    private CourseSection openSection() {
        return section(SectionStatus.IN_PROGRESS, user(LECTURER_ID, RoleName.LECTURER));
    }

    private Discussion discussion(CourseSection section, User author, boolean locked) {
        Discussion discussion = new Discussion();
        discussion.setId(DISCUSSION_ID);
        discussion.setSection(section);
        discussion.setAuthor(author);
        discussion.setTitle("Hỏi bài 1");
        discussion.setContent("Nội dung");
        discussion.setLocked(locked);
        return discussion;
    }

    private Discussion stubDiscussion(CourseSection section, User author, boolean locked) {
        Discussion discussion = discussion(section, author, locked);
        when(discussionRepository.findById(DISCUSSION_ID)).thenReturn(Optional.of(discussion));
        return discussion;
    }

    private DiscussionReply reply(long id, Discussion discussion, User author, DiscussionReply parent) {
        DiscussionReply reply = new DiscussionReply();
        reply.setId(id);
        reply.setDiscussion(discussion);
        reply.setAuthor(author);
        reply.setParent(parent);
        reply.setContent("Phản hồi " + id);
        return reply;
    }

    private DiscussionReply stubReply(DiscussionReply reply) {
        when(replyRepository.findById(reply.getId())).thenReturn(Optional.of(reply));
        return reply;
    }

    private CreateDiscussionRequest createRequest() {
        return new CreateDiscussionRequest(SECTION_ID, "  Hỏi bài 1 ", "  Nội dung câu hỏi ");
    }

    // ---------- create ----------

    @Test
    @DisplayName("Sinh viên đang theo học tạo chủ đề thảo luận")
    void create_enrolledStudent() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscussionDetailResponse response = service.create(STUDENT_ID, createRequest());

        ArgumentCaptor<Discussion> captor = ArgumentCaptor.forClass(Discussion.class);
        verify(discussionRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(student);
        assertThat(captor.getValue().isPinned()).isFalse();
        assertThat(captor.getValue().isLocked()).isFalse();
        assertThat(response.title()).isEqualTo("Hỏi bài 1");
        assertThat(response.content()).isEqualTo("Nội dung câu hỏi");
        assertThat(response.authorId()).isEqualTo(STUDENT_ID);
        assertThat(response.replies()).isEmpty();
    }

    @Test
    @DisplayName("Giảng viên phụ trách và Admin tạo chủ đề")
    void create_lecturerAndAdmin() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(LECTURER_ID, createRequest()).authorId()).isEqualTo(LECTURER_ID);
        assertThat(service.create(ADMIN_ID, createRequest()).authorId()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("Sinh viên chưa đăng ký lớp tạo chủ đề -> ForbiddenException")
    void create_notEnrolled() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));

        assertThatThrownBy(() -> service.create(STUDENT_ID, createRequest())).isInstanceOf(ForbiddenException.class);

        verify(discussionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sinh viên đã rút khỏi lớp tạo chủ đề -> ForbiddenException")
    void create_droppedStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.DROPPED);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));

        assertThatThrownBy(() -> service.create(STUDENT_ID, createRequest())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Giảng viên không phụ trách tạo chủ đề -> ForbiddenException")
    void create_otherLecturerForbidden() {
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));

        assertThatThrownBy(() -> service.create(OTHER_LECTURER_ID, createRequest()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Lớp đã ENDED: sinh viên không tạo được chủ đề mới, Admin vẫn tạo được")
    void create_endedSection() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        asUser(ADMIN_ID, RoleName.ADMIN);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        when(sectionRepository.findById(SECTION_ID))
                .thenReturn(Optional.of(section(SectionStatus.ENDED, user(LECTURER_ID, RoleName.LECTURER))));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.create(STUDENT_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
        assertThat(service.create(ADMIN_ID, createRequest()).authorId()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("Tạo chủ đề cho lớp không tồn tại -> ResourceNotFoundException")
    void create_sectionNotFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ADMIN_ID, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- list / get ----------

    @Test
    @DisplayName("Sinh viên đang theo học xem danh sách chủ đề kèm số phản hồi, giữ thứ tự từ repository")
    void list_enrolledStudent() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        CourseSection section = openSection();
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));
        Discussion pinned = discussion(section, user(LECTURER_ID, RoleName.LECTURER), false);
        pinned.setId(61L);
        pinned.setPinned(true);
        Discussion normal = discussion(section, user(STUDENT_ID, RoleName.STUDENT), false);
        when(discussionRepository.findBySectionIdOrderByPinnedDescCreatedAtDesc(SECTION_ID))
                .thenReturn(List.of(pinned, normal));
        when(replyRepository.countByDiscussionId(61L)).thenReturn(3L);
        when(replyRepository.countByDiscussionId(DISCUSSION_ID)).thenReturn(0L);

        List<DiscussionSummaryResponse> result = service.listBySection(STUDENT_ID, SECTION_ID);

        assertThat(result).extracting(DiscussionSummaryResponse::pinned).containsExactly(true, false);
        assertThat(result).extracting(DiscussionSummaryResponse::replyCount).containsExactly(3L, 0L);
    }

    @Test
    @DisplayName("Người ngoài lớp xem danh sách chủ đề -> ForbiddenException")
    void list_notReader() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(openSection()));

        assertThatThrownBy(() -> service.listBySection(STUDENT_ID, SECTION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(discussionRepository, never()).findBySectionIdOrderByPinnedDescCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Xem chi tiết chủ đề kèm các phản hồi theo thứ tự thời gian")
    void get_withReplies() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = stubDiscussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply first = reply(70L, discussion, user(LECTURER_ID, RoleName.LECTURER), null);
        DiscussionReply nested = reply(71L, discussion, user(STUDENT_ID, RoleName.STUDENT), first);
        when(replyRepository.findByDiscussionIdOrderByCreatedAtAsc(DISCUSSION_ID)).thenReturn(List.of(first, nested));

        DiscussionDetailResponse response = service.get(STUDENT_ID, DISCUSSION_ID);

        assertThat(response.replies()).extracting(ReplyResponse::id).containsExactly(70L, 71L);
        assertThat(response.replies().get(1).parentReplyId()).isEqualTo(70L);
    }

    @Test
    @DisplayName("Người ngoài lớp xem chi tiết chủ đề -> ForbiddenException")
    void get_notReader() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(OTHER_STUDENT_ID, SECTION_ID))
                .thenReturn(Optional.empty());
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.get(OTHER_STUDENT_ID, DISCUSSION_ID)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Xem chủ đề không tồn tại -> ResourceNotFoundException")
    void get_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(discussionRepository.findById(DISCUSSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ADMIN_ID, DISCUSSION_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- update ----------

    @Test
    @DisplayName("Tác giả sửa chủ đề của mình")
    void update_author() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        DiscussionDetailResponse response = service.update(STUDENT_ID, DISCUSSION_ID,
                new UpdateDiscussionRequest(" Tiêu đề mới ", " Nội dung mới "));

        assertThat(discussion.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(response.content()).isEqualTo("Nội dung mới");
    }

    @Test
    @DisplayName("Admin sửa chủ đề của người khác")
    void update_adminEditsOthers() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        service.update(ADMIN_ID, DISCUSSION_ID, new UpdateDiscussionRequest("Tiêu đề kiểm duyệt", "Nội dung"));

        assertThat(discussion.getTitle()).isEqualTo("Tiêu đề kiểm duyệt");
    }

    @Test
    @DisplayName("Sinh viên sửa chủ đề của người khác -> ForbiddenException, nội dung giữ nguyên")
    void update_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        enroll(OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.update(OTHER_STUDENT_ID, DISCUSSION_ID,
                new UpdateDiscussionRequest("Sửa trái phép", "Nội dung")))
                .isInstanceOf(ForbiddenException.class);

        assertThat(discussion.getTitle()).isEqualTo("Hỏi bài 1");
    }

    @Test
    @DisplayName("Giảng viên phụ trách (không phải tác giả) không được sửa bài của sinh viên")
    void update_lecturerNotAuthorForbidden() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.update(LECTURER_ID, DISCUSSION_ID,
                new UpdateDiscussionRequest("Sửa", "Nội dung")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Chủ đề đã khóa: tác giả không sửa được, Admin vẫn sửa được")
    void update_lockedThread() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        asUser(ADMIN_ID, RoleName.ADMIN);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), true);

        assertThatThrownBy(() -> service.update(STUDENT_ID, DISCUSSION_ID, new UpdateDiscussionRequest("A", "B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("khóa");
        assertThat(service.update(ADMIN_ID, DISCUSSION_ID, new UpdateDiscussionRequest("A", "B")).title())
                .isEqualTo("A");
    }

    @Test
    @DisplayName("Lớp đã ENDED: tác giả không sửa được chủ đề")
    void update_endedSection() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(section(SectionStatus.ENDED, user(LECTURER_ID, RoleName.LECTURER)),
                user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.update(STUDENT_ID, DISCUSSION_ID, new UpdateDiscussionRequest("A", "B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kết thúc");
    }

    @Test
    @DisplayName("Sửa chủ đề không tồn tại -> ResourceNotFoundException")
    void update_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(discussionRepository.findById(DISCUSSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ADMIN_ID, DISCUSSION_ID, new UpdateDiscussionRequest("A", "B")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- delete ----------

    @Test
    @DisplayName("Tác giả xóa chủ đề chưa có phản hồi")
    void delete_authorNoReplies() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        when(replyRepository.findByDiscussionIdOrderByCreatedAtAsc(DISCUSSION_ID)).thenReturn(List.of());

        service.delete(STUDENT_ID, DISCUSSION_ID);

        verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("Tác giả xóa chủ đề đã có phản hồi -> BusinessException, giữ lại cuộc thảo luận")
    void delete_authorWithReplies() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        when(replyRepository.findByDiscussionIdOrderByCreatedAtAsc(DISCUSSION_ID))
                .thenReturn(List.of(reply(70L, discussion, user(LECTURER_ID, RoleName.LECTURER), null)));

        assertThatThrownBy(() -> service.delete(STUDENT_ID, DISCUSSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phản hồi");

        verify(discussionRepository, never()).delete(any());
        verify(replyRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Admin xóa chủ đề có phản hồi: xóa phản hồi con, rồi phản hồi cha, rồi chủ đề")
    void delete_adminRemovesWholeThreadChildrenFirst() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply parent = reply(70L, discussion, user(LECTURER_ID, RoleName.LECTURER), null);
        DiscussionReply child = reply(71L, discussion, user(STUDENT_ID, RoleName.STUDENT), parent);
        when(replyRepository.findByDiscussionIdOrderByCreatedAtAsc(DISCUSSION_ID)).thenReturn(List.of(parent, child));

        service.delete(ADMIN_ID, DISCUSSION_ID);

        InOrder order = inOrder(replyRepository, discussionRepository);
        order.verify(replyRepository).deleteAll(List.of(child));
        order.verify(replyRepository).flush();
        order.verify(replyRepository).deleteAll(List.of(parent));
        order.verify(replyRepository).flush();
        order.verify(discussionRepository).delete(discussion);
    }

    @Test
    @DisplayName("Sinh viên xóa chủ đề của người khác -> ForbiddenException")
    void delete_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        enroll(OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.delete(OTHER_STUDENT_ID, DISCUSSION_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(discussionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Chủ đề đã khóa: tác giả không xóa được")
    void delete_lockedThread() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), true);

        assertThatThrownBy(() -> service.delete(STUDENT_ID, DISCUSSION_ID)).isInstanceOf(BusinessException.class);

        verify(discussionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa chủ đề không tồn tại -> ResourceNotFoundException")
    void delete_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(discussionRepository.findById(DISCUSSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ADMIN_ID, DISCUSSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- ghim / khóa ----------

    @Test
    @DisplayName("Giảng viên phụ trách ghim và khóa chủ đề")
    void moderation_assignedLecturer() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        when(replyRepository.countByDiscussionId(DISCUSSION_ID)).thenReturn(2L);

        DiscussionSummaryResponse pinned = service.setPinned(LECTURER_ID, DISCUSSION_ID, true);
        DiscussionSummaryResponse locked = service.setLocked(LECTURER_ID, DISCUSSION_ID, true);

        assertThat(pinned.pinned()).isTrue();
        assertThat(locked.locked()).isTrue();
        assertThat(discussion.isPinned()).isTrue();
        assertThat(discussion.isLocked()).isTrue();
        assertThat(locked.replyCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Admin ghim chủ đề")
    void moderation_admin() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThat(service.setPinned(ADMIN_ID, DISCUSSION_ID, true).pinned()).isTrue();
    }

    @Test
    @DisplayName("Sinh viên (kể cả tác giả) hoặc giảng viên không phụ trách ghim/khóa -> ForbiddenException")
    void moderation_forbidden() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        asUser(OTHER_LECTURER_ID, RoleName.LECTURER);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.setPinned(STUDENT_ID, DISCUSSION_ID, true))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.setLocked(OTHER_LECTURER_ID, DISCUSSION_ID, true))
                .isInstanceOf(ForbiddenException.class);

        assertThat(discussion.isPinned()).isFalse();
        assertThat(discussion.isLocked()).isFalse();
    }

    // ---------- trả lời ----------

    @Test
    @DisplayName("Sinh viên đang theo học trả lời chủ đề")
    void reply_toThread() {
        User student = asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(inv -> inv.getArgument(0));

        ReplyResponse response = service.reply(STUDENT_ID, DISCUSSION_ID, new ReplyRequest("  Cảm ơn bạn ", null));

        ArgumentCaptor<DiscussionReply> captor = ArgumentCaptor.forClass(DiscussionReply.class);
        verify(replyRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(student);
        assertThat(captor.getValue().getParent()).isNull();
        assertThat(response.content()).isEqualTo("Cảm ơn bạn");
        assertThat(response.parentReplyId()).isNull();
        assertThat(response.discussionId()).isEqualTo(DISCUSSION_ID);
    }

    @Test
    @DisplayName("Trả lời một phản hồi cấp một (lồng hai cấp)")
    void reply_toReply() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply parent = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(inv -> inv.getArgument(0));

        ReplyResponse response = service.reply(LECTURER_ID, DISCUSSION_ID, new ReplyRequest("Giải đáp", REPLY_ID));

        assertThat(response.parentReplyId()).isEqualTo(REPLY_ID);
        assertThat(parent.getParent()).isNull();
    }

    @Test
    @DisplayName("Trả lời lồng quá hai cấp -> BusinessException")
    void reply_tooDeep() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        Discussion discussion = stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply top = reply(71L, discussion, user(STUDENT_ID, RoleName.STUDENT), null);
        stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), top));

        assertThatThrownBy(() -> service.reply(LECTURER_ID, DISCUSSION_ID, new ReplyRequest("Quá sâu", REPLY_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hai cấp");

        verify(replyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phản hồi cha thuộc chủ đề khác -> BusinessException")
    void reply_parentFromAnotherThread() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        Discussion other = discussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        other.setId(999L);
        stubReply(reply(REPLY_ID, other, user(STUDENT_ID, RoleName.STUDENT), null));

        assertThatThrownBy(() -> service.reply(LECTURER_ID, DISCUSSION_ID, new ReplyRequest("Lạc chủ đề", REPLY_ID)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Trả lời phản hồi không tồn tại -> ResourceNotFoundException")
    void reply_parentNotFound() {
        asUser(LECTURER_ID, RoleName.LECTURER);
        stubDiscussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        when(replyRepository.findById(REPLY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reply(LECTURER_ID, DISCUSSION_ID, new ReplyRequest("Nội dung", REPLY_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Trả lời chủ đề đã khóa: sinh viên bị từ chối, Admin vẫn được")
    void reply_lockedThread() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        asUser(ADMIN_ID, RoleName.ADMIN);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), true);
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.reply(STUDENT_ID, DISCUSSION_ID, new ReplyRequest("Nội dung", null)))
                .isInstanceOf(BusinessException.class);
        assertThat(service.reply(ADMIN_ID, DISCUSSION_ID, new ReplyRequest("Thông báo", null)).authorId())
                .isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("Lớp đã ENDED: sinh viên không trả lời được")
    void reply_endedSection() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        stubDiscussion(section(SectionStatus.ENDED, user(LECTURER_ID, RoleName.LECTURER)),
                user(OTHER_STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.reply(STUDENT_ID, DISCUSSION_ID, new ReplyRequest("Nội dung", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Người ngoài lớp trả lời -> ForbiddenException")
    void reply_notReader() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(enrollmentRepository.findByStudentIdAndSectionId(STUDENT_ID, SECTION_ID)).thenReturn(Optional.empty());
        stubDiscussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);

        assertThatThrownBy(() -> service.reply(STUDENT_ID, DISCUSSION_ID, new ReplyRequest("Nội dung", null)))
                .isInstanceOf(ForbiddenException.class);

        verify(replyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trả lời chủ đề không tồn tại -> ResourceNotFoundException")
    void reply_discussionNotFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(discussionRepository.findById(DISCUSSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reply(STUDENT_ID, DISCUSSION_ID, new ReplyRequest("Nội dung", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- sửa / xóa phản hồi ----------

    @Test
    @DisplayName("Tác giả sửa phản hồi của mình")
    void updateReply_author() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply reply = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));

        ReplyResponse response = service.updateReply(STUDENT_ID, REPLY_ID, new ReplyContentRequest(" Đã sửa "));

        assertThat(reply.getContent()).isEqualTo("Đã sửa");
        assertThat(response.content()).isEqualTo("Đã sửa");
    }

    @Test
    @DisplayName("Admin sửa phản hồi của người khác; sinh viên khác không được sửa")
    void updateReply_adminOkOthersForbidden() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        enroll(OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply reply = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));

        assertThatThrownBy(() -> service.updateReply(OTHER_STUDENT_ID, REPLY_ID, new ReplyContentRequest("Sửa lén")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(reply.getContent()).isEqualTo("Phản hồi 70");

        service.updateReply(ADMIN_ID, REPLY_ID, new ReplyContentRequest("Kiểm duyệt"));
        assertThat(reply.getContent()).isEqualTo("Kiểm duyệt");
    }

    @Test
    @DisplayName("Chủ đề đã khóa: tác giả không sửa được phản hồi")
    void updateReply_lockedThread() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), true);
        stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));

        assertThatThrownBy(() -> service.updateReply(STUDENT_ID, REPLY_ID, new ReplyContentRequest("Sửa")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Sửa phản hồi không tồn tại -> ResourceNotFoundException")
    void updateReply_notFound() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        when(replyRepository.findById(REPLY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReply(STUDENT_ID, REPLY_ID, new ReplyContentRequest("Sửa")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tác giả xóa phản hồi chưa có ai trả lời")
    void deleteReply_authorLeaf() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply reply = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));
        when(replyRepository.findByParentId(REPLY_ID)).thenReturn(List.of());

        service.deleteReply(STUDENT_ID, REPLY_ID);

        verify(replyRepository).delete(reply);
    }

    @Test
    @DisplayName("Tác giả xóa phản hồi đã có người trả lời -> BusinessException, giữ lại cuộc thảo luận")
    void deleteReply_authorWithChildren() {
        asUser(STUDENT_ID, RoleName.STUDENT);
        enroll(STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply reply = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));
        when(replyRepository.findByParentId(REPLY_ID))
                .thenReturn(List.of(reply(71L, discussion, user(LECTURER_ID, RoleName.LECTURER), reply)));

        assertThatThrownBy(() -> service.deleteReply(STUDENT_ID, REPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trả lời");

        verify(replyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Admin xóa phản hồi có phản hồi con: xóa con trước rồi mới xóa cha")
    void deleteReply_adminRemovesChildrenFirst() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        Discussion discussion = discussion(openSection(), user(OTHER_STUDENT_ID, RoleName.STUDENT), false);
        DiscussionReply parent = stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));
        DiscussionReply child = reply(71L, discussion, user(LECTURER_ID, RoleName.LECTURER), parent);
        when(replyRepository.findByParentId(REPLY_ID)).thenReturn(List.of(child));

        service.deleteReply(ADMIN_ID, REPLY_ID);

        InOrder order = inOrder(replyRepository);
        order.verify(replyRepository).deleteAll(List.of(child));
        order.verify(replyRepository).flush();
        order.verify(replyRepository).delete(parent);
    }

    @Test
    @DisplayName("Sinh viên xóa phản hồi của người khác -> ForbiddenException")
    void deleteReply_otherStudentForbidden() {
        asUser(OTHER_STUDENT_ID, RoleName.STUDENT);
        enroll(OTHER_STUDENT_ID, EnrollmentStatus.ENROLLED);
        Discussion discussion = discussion(openSection(), user(STUDENT_ID, RoleName.STUDENT), false);
        stubReply(reply(REPLY_ID, discussion, user(STUDENT_ID, RoleName.STUDENT), null));

        assertThatThrownBy(() -> service.deleteReply(OTHER_STUDENT_ID, REPLY_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(replyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa phản hồi không tồn tại -> ResourceNotFoundException")
    void deleteReply_notFound() {
        asUser(ADMIN_ID, RoleName.ADMIN);
        when(replyRepository.findById(REPLY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReply(ADMIN_ID, REPLY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
