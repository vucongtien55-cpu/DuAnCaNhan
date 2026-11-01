# LMS – Tài liệu API (đối chiếu với code thực tế)

Nguồn: các `*Controller`, `SecurityConfig`, DTO trong `com.lms.dto` và phần kiểm tra quyền ở tầng service.
Tài liệu này **không** suy diễn endpoint nào ngoài những gì có trong code.

## 1. Quy ước chung

| Mục | Giá trị |
|---|---|
| Base URL | `http://localhost:8080` (`server.port=8080`), mọi API dưới `/api` |
| Xác thực | `Authorization: Bearer <accessToken>` (JWT, TTL 1 giờ). Không nhận token qua query/form |
| Công khai duy nhất | `POST /api/auth/login` (và `OPTIONS /api/**`, `/error`). Mọi `/api/**` khác cần đăng nhập; đường dẫn ngoài `/api` bị `denyAll` |
| Vai trò | `ADMIN`, `LECTURER`, `STUDENT` (một người có thể có nhiều vai trò) |
| CORS | Cho phép origin trong `LMS_CORS_ALLOWED_ORIGINS` (mặc định `http://localhost:3000`, `http://localhost:5173`); methods GET/POST/PUT/PATCH/DELETE/OPTIONS; header `Authorization`, `Content-Type`, `Accept`; expose `Content-Disposition` |
| Phân trang | `?page=0&size=20&sort=field,asc`. `size` tối đa 100. Kết quả là `Page` của Spring (`content`, `totalElements`, `number`, `size`, …) |
| Sort | Chỉ field trong danh sách cho phép (xem từng API); field khác → `400 VALIDATION_ERROR` |
| Upload | `multipart/form-data`, tối đa 20 MB/file (request 21 MB). Chỉ `pdf, docx, pptx, xlsx, zip`, kiểm tra cả đuôi lẫn chữ ký nội dung. Quá cỡ → `413 PAYLOAD_TOO_LARGE`; sai định dạng → `400` |
| Rate limit | Ghi (POST/PUT/PATCH/DELETE): burst 60, 120/phút/người dùng. Upload: burst 10, 20/phút. Đăng nhập: 5 lần sai/cặp (IP, username) và 100 lần sai/IP trong 15 phút. Vượt → `429 TOO_MANY_REQUESTS` + header `Retry-After` (giây). GET không bị đếm |
| Kiểu thời gian | `LocalDateTime` ISO không múi giờ (`2026-11-05T09:00:00`), `LocalDate` `yyyy-MM-dd` |

**Danh tính luôn lấy từ JWT** (`CurrentUser`); không API nào nhận `userId` của người gọi từ client.

### Định dạng lỗi (`ApiError`)

```json
{ "timestamp": "...", "status": 400, "code": "VALIDATION_ERROR", "message": "Dữ liệu không hợp lệ",
  "path": "/api/courses", "details": [ { "field": "code", "message": "..." } ] }
```

| HTTP | `code` | Khi nào |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Body/tham số sai, sort không hợp lệ |
| 400 | `BUSINESS_RULE_VIOLATION` | Vi phạm quy tắc nghiệp vụ (trùng mã, sai trạng thái, …) |
| 401 | `BAD_CREDENTIALS` / `UNAUTHORIZED` | Sai đăng nhập / thiếu hoặc token không hợp lệ, đã thu hồi |
| 403 | `FORBIDDEN` | Không đủ quyền, tài khoản bị khóa |
| 404 | `NOT_FOUND` | Không tồn tại (hoặc cố ý che giấu: khóa học chưa ACTIVE với non-Admin, quiz chưa công bố với SV) |
| 409 | `CONFLICT` / `ATTEMPT_EXPIRED` | Xung đột dữ liệu / lượt làm quiz đã hết hạn |
| 413 | `PAYLOAD_TOO_LARGE` | File > 20 MB |
| 429 | `TOO_MANY_REQUESTS` | Rate limit |
| 500 | `INTERNAL_ERROR` / `STORAGE_ERROR` | Lỗi máy chủ |

### Cách kiểm soát quyền
- Chỉ `UserController` (phần Admin) và `CourseController` (ghi) dùng `@PreAuthorize("hasRole('ADMIN')")`.
- Mọi controller còn lại **không có `@PreAuthorize`**; `SecurityConfig` chỉ yêu cầu "đã đăng nhập". Quyền được kiểm tra trong service bằng `SectionAccessChecker`:
  - **Manager của lớp** = Admin, hoặc giảng viên *được gán* làm `teacher` của lớp đó.
  - **Reader của lớp** = Manager hoặc sinh viên có đăng ký trạng thái `ENROLLED` trong lớp.
- Cột "Quyền" bên dưới dùng đúng hai khái niệm này. Frontend không được tự suy ra quyền chỉ từ vai trò: giảng viên không phụ trách lớp sẽ nhận 403.

---

## 2. Authentication – `/api/auth`

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/auth/login` | Công khai | `{username*, password*}` | 200 `LoginResponse {accessToken, tokenType:"Bearer", expiresIn, expiresAt, user}`; header `Cache-Control: no-store`. 401 sai thông tin; 403 tài khoản bị khóa; 429 nếu bị chặn |
| GET | `/api/auth/me` | Đã đăng nhập | – | 200 `UserResponse {id, username, email, fullName, phone, avatarUrl, enabled, roles, createdAt}` |
| POST | `/api/auth/logout` | Đã đăng nhập | – | 204; thu hồi **chỉ token đang dùng** (token ở thiết bị khác vẫn sống tới khi hết hạn) |
| PUT | `/api/users/me/password` | Đã đăng nhập | `{oldPassword*, newPassword*(8–100)}` | 204. 400 nếu `oldPassword` sai hoặc mật khẩu mới trùng cũ |

## 3. Users – `/api/users`

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| GET | `/api/users/me` | Đã đăng nhập | – | 200 `UserResponse` |
| PUT | `/api/users/me` | Đã đăng nhập | `{email*, fullName*, phone, avatarUrl}` | 200 `UserResponse` |
| POST | `/api/users` | ADMIN | `{username*(3–50, [A-Za-z0-9._-]), email*, password*(8–100), fullName*, phone, roles*(set không rỗng)}` | 201 `UserResponse`; 400 trùng username/email |
| GET | `/api/users` | ADMIN | `keyword?` (khớp một phần, không phân biệt hoa thường, trên username/họ tên/email; tối đa 100 ký tự), `role?` (`ADMIN/LECTURER/STUDENT`), `enabled?` (true/false), `page,size,sort` (sort: `id, username, email, fullName, enabled, createdAt`) | 200 `Page<UserResponse>`. Ví dụ chọn giảng viên cho lớp: `?role=LECTURER&enabled=true&keyword=nguyen`. `role` sai → 400 |
| GET | `/api/users/{id}` | ADMIN | – | 200 / 404 |
| PUT | `/api/users/{id}` | ADMIN | như `PUT /me` | 200 |
| PUT | `/api/users/{id}/password` | ADMIN | `{newPassword*(8–100)}` | 204 |
| PUT | `/api/users/{id}/enabled` | ADMIN | `{enabled*}` | 200; 400 nếu khóa Admin cuối cùng |
| PUT | `/api/users/{id}/roles` | ADMIN | `{roles*}` | 200; 400 nếu bỏ vai trò Admin cuối cùng |

> Không có `DELETE /api/users/{id}` (chỉ khóa bằng `enabled`).

## 4. Courses – `/api/courses`

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/courses` | ADMIN | `{code*(≤30, [A-Za-z0-9._-]), title*, description, credits(1–20), thumbnailUrl}` | 201 `CourseResponse`, `status=DRAFT`; 400 trùng mã |
| GET | `/api/courses` | Đã đăng nhập | `status?`, phân trang (sort: `id, code, title, credits, status, createdAt, updatedAt`) | Admin: tất cả/ lọc status. Người khác: chỉ `ACTIVE` (lọc status khác ACTIVE → 403) |
| GET | `/api/courses/{courseId}` | Đã đăng nhập | – | Non-Admin chỉ thấy `ACTIVE`, còn lại 404 |
| PUT | `/api/courses/{courseId}` | ADMIN | như tạo + `status*` (`DRAFT/ACTIVE/ARCHIVED`) | 200 |
| DELETE | `/api/courses/{courseId}` | ADMIN | – | 204; 400 nếu khóa học đã có lớp học phần (hãy chuyển ARCHIVED) |

## 5. Course Sections – `/api/sections`

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/sections` | ADMIN | `{courseId*, sectionCode?(≤40), name*, semester, startDate, endDate, maxStudents*(1–1000), schedule, teacherId?}` | 201, `status=DRAFT`; không gửi `sectionCode` thì tự sinh |
| GET | `/api/sections` | Đã đăng nhập | `courseId?, status?`, phân trang (sort: `id, sectionCode, name, semester, startDate, endDate, maxStudents, status, createdAt`) | Admin: tất cả. Giảng viên: lớp mình phụ trách. Sinh viên/khác: chỉ `OPEN_REGISTRATION` (status khác → 403) |
| GET | `/api/sections/by-code/{sectionCode}` | Đã đăng nhập (phải "nhìn thấy" lớp) | – | 200 `SectionResponse` |
| GET | `/api/sections/{sectionId}` | như trên | – | 200; 404 nếu không có quyền nhìn thấy |
| PUT | `/api/sections/{sectionId}` | ADMIN | `{name*, semester, startDate, endDate, maxStudents*, schedule}` | 200 |
| PATCH | `/api/sections/{sectionId}/info` | Manager | `{name*, schedule}` | 200 |
| PUT | `/api/sections/{sectionId}/teacher` | ADMIN | `{teacherId*}` (phải là LECTURER đang hoạt động) | 200 |
| PUT | `/api/sections/{sectionId}/status` | ADMIN (mọi chuyển hợp lệ); GV phụ trách chỉ → `IN_PROGRESS`/`ENDED` | `{status*}` | 200. Chuyển trạng thái: `DRAFT→OPEN_REGISTRATION`, `OPEN_REGISTRATION→DRAFT/IN_PROGRESS`, `IN_PROGRESS→ENDED`; mở đăng ký cần đã có giảng viên. Sai → 400 |
| POST | `/api/sections/{sectionId}/regenerate-code` | ADMIN | – | 200, đổi `sectionCode` |

`SectionResponse`: `id, courseId, courseCode, courseTitle, sectionCode, name, semester, startDate, endDate, maxStudents, enrolledCount, schedule, status, registrationOpen, teacherId, teacherName, createdAt`.

## 6. Enrollments

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/enrollments` | STUDENT | `{sectionCode*}` | 201 `EnrollmentResponse`. 400 nếu lớp không mở đăng ký / đã đăng ký / đã rút / hết chỗ |
| GET | `/api/enrollments/me` | Đã đăng nhập | – | 200 `List<EnrollmentResponse>` của chính mình |
| GET | `/api/sections/{sectionId}/enrollments` | Manager | – | 200 danh sách |
| GET | `/api/sections/{sectionId}/student-candidates` | Manager (Admin hoặc giảng viên phụ trách lớp đó) | `keyword*` (≥ 2 ký tự sau khi bỏ khoảng trắng), `page,size,sort` (sort: `id, username, fullName`; mặc định `fullName`) | 200 `Page<{id, username, fullName}>`: chỉ sinh viên **đang hoạt động** và **chưa có đăng ký còn hiệu lực** trong lớp (sinh viên đã `DROPPED` vẫn hiện để thêm lại). Thiếu/ngắn từ khóa → 400; không phụ trách → 403; lớp không có → 404. Dùng `id` trả về cho `POST /api/sections/{sectionId}/enrollments` |
| POST | `/api/sections/{sectionId}/enrollments` | Manager | `{studentId*}` | 201; sinh viên phải đang hoạt động và có vai trò STUDENT |
| POST | `/api/sections/{sectionId}/enrollments/{studentId}/withdraw` | Chính sinh viên đó hoặc Manager | `{reason?(≤255)}` (body tùy chọn) | 200, `status=DROPPED`; 400 nếu không còn `ENROLLED` hoặc lớp đã `ENDED` |

## 7. Materials

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/sections/{sectionId}/materials` | Manager | multipart: `title*`, `description`, `file*` | 201 `MaterialResponse` (bucket rate-limit upload) |
| GET | `/api/sections/{sectionId}/materials` | Reader | – | 200 danh sách |
| GET | `/api/materials/{materialId}` | Reader | – | 200 |
| GET | `/api/materials/{materialId}/download` | Reader | – | 200 stream, `Content-Disposition` |
| DELETE | `/api/materials/{materialId}` | Manager | – | 204 |

## 8. Assignments & Submissions

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/assignments` | Manager (lớp chưa `ENDED`) | `{sectionId*, title*, description, dueDate(tương lai), maxScore*(0–1000], maxSubmissions, allowLateSubmission, gradingPolicy(LATEST/FIRST/MANUAL/HIGHEST)}` | 201 `AssignmentResponse` |
| GET | `/api/sections/{sectionId}/assignments` | Reader | – | 200 danh sách (sắp theo hạn nộp) |
| GET | `/api/assignments/{assignmentId}` | Reader | – | 200 |
| PUT | `/api/assignments/{assignmentId}` | Manager | `{title*, description, dueDate, maxScore*, maxSubmissions, allowLateSubmission*, gradingPolicy*}` | 200 |
| DELETE | `/api/assignments/{assignmentId}` | Manager | – | 204; 400 nếu đã có bài nộp |
| PUT | `/api/assignments/{assignmentId}/attachment` | Manager | multipart `file*` | 200 (thay file cũ) |
| GET | `/api/assignments/{assignmentId}/attachment` | Reader | – | 200 stream; 404 nếu không có file |
| DELETE | `/api/assignments/{assignmentId}/attachment` | Manager | – | 200 `AssignmentResponse` |
| POST | `/api/assignments/{assignmentId}/submissions` | STUDENT đã đăng ký lớp | multipart: `file*`, `content?` | 201 `SubmissionResponse`; 400 nếu quá số lần nộp/quá hạn không cho nộp muộn |
| GET | `/api/assignments/{assignmentId}/submissions` | Manager | – | 200 mọi bài nộp |
| GET | `/api/assignments/{assignmentId}/students/{studentId}/submissions` | Chính `studentId` hoặc Manager | – | 200 lịch sử nộp (mới nhất trước) |
| GET | `/api/assignments/{assignmentId}/students/{studentId}/submission-to-grade` | Manager | – | 200 bài nộp dùng để chấm theo `gradingPolicy` |
| GET | `/api/submissions/{submissionId}` | Chủ bài nộp hoặc Manager | – | 200 |
| GET | `/api/submissions/{submissionId}/download` | Chủ bài nộp hoặc Manager | – | 200 stream |
| PUT | `/api/submissions/{submissionId}/select-for-grading` | Manager; chỉ khi `gradingPolicy=MANUAL` | – | 200; policy khác → 400 |

## 9. Grades – bài tập, điểm tổng kết

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| PUT | `/api/submissions/{submissionId}/grade` | Manager | `{score*(≥0, ≤ maxScore), feedback(≤5000), internalNote(≤5000)}` | 200 `GradeResponse`; vượt maxScore → 400 |
| GET | `/api/submissions/{submissionId}/grade` | Manager | – | 200 / 404 |
| GET | `/api/assignments/{assignmentId}/grades` | Manager | – | 200 danh sách |
| GET | `/api/assignments/{assignmentId}/students/{studentId}/grade` | Manager | – | 200 điểm chính thức |
| POST | `/api/assignments/{assignmentId}/grades/publish` | Manager | – | 200 `{count}` |
| POST | `/api/grades/{gradeId}/publish` | Manager | – | 200 `GradeResponse`; đã công bố → 400 |
| POST | `/api/grades/{gradeId}/unpublish` | Manager | – | 200; chưa công bố → 400 |
| GET | `/api/assignments/{assignmentId}/my-grade` | STUDENT đã đăng ký | – | 200 `StudentGradeResponse` (chưa công bố thì không lộ điểm) |
| GET | `/api/sections/{sectionId}/my-grades` | STUDENT đã đăng ký | – | 200 danh sách |
| PUT | `/api/sections/{sectionId}/grade-weights` | Manager | mảng `[{gradeType*(ASSIGNMENT/QUIZ), weight*(0–100)}]`, tổng = 100 | 200 `List<GradeWeightResponse>`; sai → 400 |
| GET | `/api/sections/{sectionId}/grade-weights` | Reader | – | 200 |
| POST | `/api/sections/{sectionId}/final-grades/calculate` | Manager | – | 200 `List<FinalGradeResponse>`; cần đã cấu hình trọng số hợp lệ |
| GET | `/api/sections/{sectionId}/final-grades` | Manager | – | 200 `List<GradeResponse>` |
| POST | `/api/sections/{sectionId}/final-grades/publish` | Manager | – | 200 `{count}` |
| POST | `/api/sections/{sectionId}/final-grades/unpublish` | Manager | – | 200 `{count}` |
| GET | `/api/sections/{sectionId}/my-final-grade` | STUDENT đã đăng ký | – | 200 `{sectionId, published, score, maxScore, publishedAt}` |

## 10. Quizzes

Manager (Admin / GV phụ trách):

| Method | Endpoint | Request | Kết quả |
|---|---|---|---|
| POST | `/api/quizzes` | `{sectionId*, title*, description, durationMinutes*(1–600), startTime, endTime, maxAttempts(1–10), shuffleQuestions, questions:[{content*, score(≤100), explanation, options:[{content*, correct}]}]}` | 201 `QuizResponse` (có đáp án đúng) |
| GET | `/api/quizzes/{quizId}` | – | 200 `QuizResponse` đầy đủ (chỉ Manager) |
| PUT | `/api/quizzes/{quizId}` | `{title*, description, durationMinutes*, startTime, endTime, maxAttempts*, shuffleQuestions}` | 200 |
| PUT | `/api/quizzes/{quizId}/questions` | mảng `QuizQuestionRequest` (thay toàn bộ); 400 nếu đã có lượt làm | 200 |
| POST | `/api/quizzes/{quizId}/publish` | – | 200; cần ≥ 1 câu hỏi; đã công bố → 400 |
| POST | `/api/quizzes/{quizId}/unpublish` | – | 200 |
| DELETE | `/api/quizzes/{quizId}` | – | 204 |
| GET | `/api/quizzes/{quizId}/attempts` | – | 200 `List<QuizAttemptStaffResponse>` |
| POST | `/api/quizzes/{quizId}/grades/publish` | – | 200 `{count}` |

Dùng chung: `GET /api/sections/{sectionId}/quizzes` – Manager thấy tất cả, sinh viên đã đăng ký chỉ thấy quiz đã công bố, người khác 403 → `List<QuizSummaryResponse>`.

Sinh viên (STUDENT đã đăng ký):

| Method | Endpoint | Request | Kết quả |
|---|---|---|---|
| POST | `/api/quizzes/{quizId}/attempts` | – | 200 `QuizAttemptResponse {attemptId, deadlineAt, remainingSeconds, questions[{id, content, score, options[{id, content}]}], savedAnswers}`; **không có đáp án đúng**. Đang có lượt dở thì trả lại lượt đó. Hết lượt/chưa mở/đã đóng → 400; quiz chưa công bố → 404 |
| GET | `/api/quizzes/{quizId}/my-attempts` | – | 200 `List<QuizResultResponse>` |
| GET | `/api/quiz-attempts/{attemptId}` | – | 200 (chỉ lượt của mình) |
| PUT | `/api/quiz-attempts/{attemptId}/answers` | `{questionId*, selectedOptionId?}` | 200 `SaveAnswerResponse`; hết giờ → 409 `ATTEMPT_EXPIRED` |
| POST | `/api/quiz-attempts/{attemptId}/submit` | – | 200 `QuizResultResponse` |
| GET | `/api/quiz-attempts/{attemptId}/result` | – | 200 `{attemptId, quizId, status, submittedAt, gradePublished, score, maxScore, …}` |

## 11. Discussions & Notifications

| Method | Endpoint | Quyền | Request | Kết quả |
|---|---|---|---|---|
| POST | `/api/discussions` | Reader (lớp chưa `ENDED`, trừ Admin) | `{sectionId*, title*(≤255), content*(≤10000)}` | 201 `DiscussionDetailResponse` |
| GET | `/api/sections/{sectionId}/discussions` | Reader | – | 200 `List<DiscussionSummaryResponse>` (ghim trước, mới nhất trước) |
| GET | `/api/discussions/{discussionId}` | Reader | – | 200 kèm `replies` |
| PUT | `/api/discussions/{discussionId}` | Tác giả hoặc Admin | `{title*, content*}` | 200 |
| DELETE | `/api/discussions/{discussionId}` | Tác giả hoặc Admin (non-Admin: không xóa được khi đã có trả lời) | – | 204 |
| PUT | `/api/discussions/{discussionId}/pinned` | Manager | `{pinned*}` | 200 |
| PUT | `/api/discussions/{discussionId}/locked` | Manager | `{locked*}` | 200 |
| POST | `/api/discussions/{discussionId}/replies` | Reader (chủ đề chưa khóa, trừ Admin) | `{content*, parentReplyId?}` | 201 `ReplyResponse` |
| PUT | `/api/discussion-replies/{replyId}` | Tác giả hoặc Admin | `{content*}` | 200 |
| DELETE | `/api/discussion-replies/{replyId}` | Tác giả hoặc Admin | – | 204 |
| GET | `/api/notifications` | Đã đăng nhập (của chính mình) | `unreadOnly=false`, `page,size` (sort từ client bị bỏ qua, luôn mới nhất trước) | 200 `Page<NotificationResponse>` |
| GET | `/api/notifications/unread-count` | như trên | – | 200 `{count}` |
| GET | `/api/notifications/{notificationId}` | Chủ thông báo | – | 200; người khác 403 |
| PUT | `/api/notifications/{notificationId}/read` | Chủ thông báo | – | 200 |
| PUT | `/api/notifications/read-all` | Đã đăng nhập | – | 200 `{count}` |
| POST | `/api/sections/{sectionId}/announcements` | Manager | `{title*(≤255), message*(≤2000)}` | 201 `{count}` số người nhận (sinh viên đang học) |

---

## 12. API còn thiếu / chưa đủ cho frontend

Đây là các điểm **thực sự không có trong code**; chưa sửa gì (đúng yêu cầu).

**Ảnh hưởng nặng (nên giải quyết trước hoặc song song khi làm frontend):**
1. **Không có refresh token.** Access token sống 1 giờ; hết hạn thì frontend chỉ có thể đăng nhập lại. Cần quyết định chiến lược (giữ nguyên + redirect login, hoặc bổ sung refresh).
2. ~~Không lọc/tìm người dùng~~ **Đã bổ sung:** `GET /api/users?keyword&role&enabled` (Admin) và `GET /api/sections/{id}/student-candidates?keyword` (Admin/giảng viên phụ trách). Còn lại: giảng viên không tra cứu được *giảng viên* hay người dùng khác (chủ ý), và tìm sinh viên cần từ khóa ≥ 2 ký tự.
3. **Không có API tạo tài khoản công khai, quên/đặt lại mật khẩu qua email.** Chỉ Admin tạo user và đặt lại mật khẩu.

**Ảnh hưởng trung bình:**
4. Danh sách `enrollments`, `submissions`, `grades`, `materials`, `assignments`, `discussions`, `quizzes` trả `List` không phân trang/không lọc; lớp đông sẽ nặng.
5. Kết quả quiz chỉ có điểm; **không có API xem lại đề + đáp án đúng/đã chọn sau khi nộp**.
6. Không có API thống kê/dashboard (số lớp, bài chưa nộp, hạn sắp tới…); frontend phải tự gọi nhiều API và gộp.
7. Không có API sinh viên xem toàn bộ bài tập/hạn nộp trên các lớp của mình; phải đi `GET /api/enrollments/me` rồi gọi từng `/sections/{id}/assignments`.
8. Không có `DELETE` cho lớp học phần và người dùng, không có API sửa/xóa từng câu hỏi quiz (chỉ thay cả danh sách).
9. Không có API upload avatar (`avatarUrl`, `thumbnailUrl` chỉ là chuỗi).
10. ~~Không có OpenAPI/Swagger~~ **Đã bổ sung** (springdoc, mặc định tắt) — xem mục 14.
11. `Enrollment` có `PENDING`/`COMPLETED` nhưng không có endpoint nào chuyển sang hai trạng thái này.

**Lưu ý khi tích hợp (không phải thiếu sót):**
- Hai endpoint trùng chức năng: `GET /api/auth/me` và `GET /api/users/me`.
- Không có `@PreAuthorize` trên phần lớn controller; frontend cần gọi thử/xử lý 403 thay vì chỉ ẩn nút theo vai trò.
- `server.forward-headers-strategy` mặc định `none`; đặt `LMS_FORWARD_HEADERS=native` khi chạy sau reverse proxy, nếu không rate-limit đăng nhập sẽ coi mọi người cùng một IP.
- Login sai quá 5 lần/IP+username khóa tạm tối đa 15 phút (429 + `Retry-After`); frontend nên đọc header này.

## 13. Cách chạy collection Postman

File: `docs/postman/LMS.postman_collection.json` và `docs/postman/LMS.local.postman_environment.json`.

1. Chạy ứng dụng với biến môi trường như `.env.example` (cần `LMS_DB_*`, `LMS_JWT_SECRET`, và `LMS_ADMIN_USERNAME`/`LMS_ADMIN_PASSWORD` để có Admin).
2. Import cả hai file vào Postman, chọn environment **LMS Local**, điền `adminUsername`, `adminPassword` (biến kiểu secret; **không commit giá trị thật**).
3. Chạy bằng **Collection Runner theo thứ tự thư mục 00 → 09**. Mỗi lượt tự sinh `runId` nên có thể chạy lại.
4. Hai request upload (`03 - Upload tài liệu`, `04 - Sinh viên nộp bài`) cần **chọn file** ở tab Body (PDF nhỏ). Nếu không chọn file, 2 request đó và các bước phụ thuộc (tải tài liệu, chấm điểm…) sẽ fail.
5. Collection ghi token vào collection variable (`adminToken`, `lecturerToken`, `studentToken`) trong lúc chạy; không có token hay mật khẩu thật trong file.

Thư mục: `00 Authentication · 01 Setup bằng Admin · 02 Đăng ký lớp · 03 Tài liệu · 04 Bài tập/bài nộp/chấm điểm · 05 Quiz · 06 Điểm tổng kết · 07 Thảo luận & thông báo · 08 Phân quyền (kết quả âm) · 09 Đổi mật khẩu & đăng xuất` (94 request).

> **Trạng thái:** collection được viết dựa trên code và đã kiểm tra là JSON hợp lệ, nhưng **chưa được chạy** với server thật (cần DB và Admin). Các assertion cần xác nhận lần chạy đầu: nội dung `StudentGradeResponse` khi chưa công bố điểm, 403 khi đăng nhập tài khoản bị khóa, và body mẫu quiz.

## 14. Swagger UI / OpenAPI

Dùng springdoc-openapi `3.2.0` (tương thích Spring Boot 4.1.2).

| Mục | Giá trị |
|---|---|
| Bật/tắt | `LMS_DOCS_ENABLED=true` (thuộc tính `app.docs.enabled`). **Mặc định `false`**: khi tắt, `/v3/api-docs` và `/swagger-ui*` không tồn tại và bị `SecurityConfig` từ chối (401) như mọi đường dẫn ngoài `/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` (OpenAPI 3.1) |
| Xác thực trong UI | Gọi `POST /api/auth/login` ngay trong UI, bấm **Authorize**, dán `accessToken` (không gồm chữ `Bearer`). Token **không** được lưu vào localStorage (`persist-authorization=false`) |
| Phạm vi mở | Chỉ `GET /v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` được `permitAll` và **chỉ khi bật**. Không đường dẫn nào khác được mở; `/api/**` vẫn cần JWT; POST/DELETE tới đường dẫn tài liệu bị từ chối |
| Nội dung | Sơ đồ `bearerAuth` (HTTP bearer, JWT) áp mặc định cho mọi API; `POST /api/auth/login` khai báo không cần token; API gom nhóm theo `@Tag` của controller; phân trang hiển thị `page`, `size`, `sort` |

**Khuyến nghị:** chỉ bật trên môi trường phát triển/kiểm thử. Tài liệu không chứa secret hay dữ liệu người dùng, nhưng lộ toàn bộ cấu trúc API nên không nên công khai ở production.

## 15. Quyết định thiết kế cho API tra cứu người dùng

- **Admin** dùng `GET /api/users` đầy đủ (trả `UserResponse`), thêm các bộ lọc tùy chọn; tham số cũ vẫn hoạt động nguyên vẹn.
- **Giảng viên** không có quyền với `GET /api/users`. Thay vào đó có endpoint theo ngữ cảnh lớp (`student-candidates`), chỉ cho giảng viên **phụ trách chính lớp đó**. Phạm vi (chỉ STUDENT, đang hoạt động, chưa đăng ký lớp) cố định ở máy chủ; `role`, `userId`, `enabled`… do client gửi lên bị bỏ qua.
- Phản hồi cho giảng viên chỉ gồm `id, username, fullName` (không email, số điện thoại, vai trò, trạng thái).
- Từ khóa bắt buộc ≥ 2 ký tự để giảng viên không thể liệt kê hàng loạt toàn bộ sinh viên. `%` và `_` trong từ khóa được coi là ký tự thường.
- **Sinh viên** không có endpoint tra cứu nào.
- **Chọn giảng viên cho lớp:** `POST /api/sections` và `PUT /api/sections/{id}/teacher` vẫn chỉ Admin và vẫn yêu cầu `teacherId` là giảng viên đang hoạt động; Admin lấy danh sách ứng viên bằng `GET /api/users?role=LECTURER&enabled=true&keyword=...`.
