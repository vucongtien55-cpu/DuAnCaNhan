package com.lms.dto.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code parentReplyId} null là trả lời trực tiếp chủ đề; có giá trị là trả lời một phản hồi cấp một. */
public record ReplyRequest(
        @NotBlank @Size(max = 10000) String content,

        Long parentReplyId) {
}
