package io.yggdrasil.labs.midgard.adapter.web.dto;

import lombok.Builder;
import lombok.Data;

/** API 错误响应的数据结构。 */
@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private String timestamp;
    private String path;
}
