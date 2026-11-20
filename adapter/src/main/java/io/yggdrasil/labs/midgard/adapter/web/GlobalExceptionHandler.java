package io.yggdrasil.labs.midgard.adapter.web;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.adapter.web.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

/** 将 Web 层异常转换为统一的错误响应。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

    /** 将请求体校验错误转换为 HTTP 400 响应。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(VALIDATION_ERROR_CODE, message, request));
    }

    /** 将参数类型不匹配错误转换为 HTTP 400 响应。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "参数 '" + ex.getName() + "' 类型不合法";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(VALIDATION_ERROR_CODE, message, request));
    }

    /** 将绑定错误转换为 HTTP 400 响应。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(VALIDATION_ERROR_CODE, message, request));
    }

    /** 将数据库完整性冲突转换为 HTTP 409 响应。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = "数据冲突";
        if (ex.getMessage() != null && ex.getMessage().contains("idx_email")) {
            message = "邮箱已被占用";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildResponse("DUPLICATE_EMAIL", message, request));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildResponse("DATA_CONFLICT", message, request));
    }

    /** 按业务错误码映射 HTTP 状态并生成错误响应。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBiz(BizException ex, HttpServletRequest request) {
        log.warn("Business exception: [{}] {}", ex.getErrCode(), ex.getMessage());
        HttpStatus status = mapStatus(ex.getErrCode());
        return ResponseEntity.status(status)
                .body(buildResponse(ex.getErrCode(), ex.getMessage(), request));
    }

    /** 将未处理异常转换为 HTTP 500 响应。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse("INTERNAL_ERROR", "服务内部错误", request));
    }

    private ErrorResponse buildResponse(String code, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now().toString())
                .path(request.getRequestURI())
                .build();
    }

    private HttpStatus mapStatus(String errCode) {
        if (errCode != null) {
            if (errCode.contains("NOT_FOUND")) return HttpStatus.NOT_FOUND;
            if (errCode.contains("DUPLICATE")) return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
