package io.yggdrasil.labs.midgard.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.alibaba.cola.exception.BizException;

import io.yggdrasil.labs.midgard.adapter.web.dto.ErrorResponse;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/customers/42";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = request();

    @Test
    void handleValidation_returnsFieldMessagesAndRequestMetadata() {
        BeanPropertyBindingResult binding = bindingResult();
        binding.addError(new FieldError("request", "name", "名称不能为空"));
        binding.addError(new FieldError("request", "email", "邮箱格式不合法"));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(binding);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request);

        assertError(
                response,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "name: 名称不能为空; email: 邮箱格式不合法");
    }

    @Test
    void handleBindException_returnsFieldMessageAndRequestMetadata() {
        BeanPropertyBindingResult binding = bindingResult();
        binding.addError(new FieldError("request", "page", "页码必须大于零"));

        ResponseEntity<ErrorResponse> response =
                handler.handleBindException(new BindException(binding), request);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "page: 页码必须大于零");
    }

    @Test
    void handleTypeMismatch_returnsBadRequestWithParameterName() {
        MethodArgumentTypeMismatchException exception =
                mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "参数 'id' 类型不合法");
    }

    @Test
    void handleDataIntegrity_emailIndexConflictReturnsDuplicateEmail() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("constraint idx_email violated");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(exception, request);

        assertError(response, HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "邮箱已被占用");
    }

    @Test
    void handleDataIntegrity_otherConflictReturnsGenericConflict() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("constraint idx_name violated");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(exception, request);

        assertError(response, HttpStatus.CONFLICT, "DATA_CONFLICT", "数据冲突");
    }

    @Test
    void handleDataIntegrity_nullExceptionMessageReturnsGenericConflict() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException((String) null);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(exception, request);

        assertError(response, HttpStatus.CONFLICT, "DATA_CONFLICT", "数据冲突");
    }

    @Test
    void handleBiz_notFoundCodeMapsToNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBiz(new BizException("CUSTOMER_NOT_FOUND", "客户不存在"), request);

        assertError(response, HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "客户不存在");
    }

    @Test
    void handleBiz_duplicateCodeMapsToConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBiz(new BizException("DUPLICATE_EMAIL", "邮箱已被占用"), request);

        assertError(response, HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "邮箱已被占用");
    }

    @Test
    void handleBiz_otherCodeMapsToBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBiz(new BizException("VALIDATION_ERROR", "输入无效"), request);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "输入无效");
    }

    @Test
    void handleBiz_nullCodeMapsToBadRequestAndPreservesNullCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBiz(new BizException(null, "业务错误"), request);

        assertError(response, HttpStatus.BAD_REQUEST, null, "业务错误");
    }

    @Test
    void handleExceptionHidesInternalMessageAndReturnsServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleException(
                        new IllegalStateException("database password leaked"), request);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务内部错误");
        assertFalse(response.getBody().getMessage().contains("database password"));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(REQUEST_PATH);
        return request;
    }

    private static BeanPropertyBindingResult bindingResult() {
        return new BeanPropertyBindingResult(new Object(), "request");
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus status,
            String code,
            String message) {
        assertEquals(status, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(code, body.getCode());
        assertEquals(message, body.getMessage());
        assertEquals(REQUEST_PATH, body.getPath());
        assertNotNull(body.getTimestamp());
        assertFalse(body.getTimestamp().isBlank());
        Instant.parse(body.getTimestamp());
    }
}
