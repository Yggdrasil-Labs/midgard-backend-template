package io.yggdrasil.labs.midgard.adapter.web;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.adapter.web.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse("VALIDATION_ERROR", message, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "参数 '" + ex.getName() + "' 类型不合法";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildResponse("VALIDATION_ERROR", message, request));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBiz(BizException ex, HttpServletRequest request) {
        HttpStatus status = mapStatus(ex.getErrCode());
        return ResponseEntity.status(status)
                .body(buildResponse(ex.getErrCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
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
