package vn.hoidanit.jobhunter.util.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS) // Trả về 403
public class SessionLimitExceededException extends RuntimeException {

    // ========== BẠN BỊ THIẾU CONSTRUCTOR NÀY ==========
    public SessionLimitExceededException(String message) {
        super(message); // Truyền message lên lớp cha (RuntimeException)
    }
    // ==================================================

}