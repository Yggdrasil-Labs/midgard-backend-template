package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;

/** 仅识别异常链中明确属于邮箱唯一键的 MySQL 重复错误。 */
final class EmailUniqueConstraint {

    private static final Pattern DUPLICATE_ENTRY =
            Pattern.compile("(?i)^\\s*duplicate entry\\s+[^\\r\\n]+\\s+for key\\s+[^\\r\\n]+$");
    private static final Pattern EMAIL_KEY =
            Pattern.compile(
                    "(?i)\\bfor key\\s+(?:'(?:[\\w$]+\\.)*idx_email'"
                            + "|\"(?:[\\w$]+\\.)*idx_email\""
                            + "|(?:`[\\w$]+`\\.)*`(?:[\\w$]+\\.)*idx_email`"
                            + "|(?:[\\w$]+\\.)*idx_email)\\s*$");

    private EmailUniqueConstraint() {}

    static boolean matches(DataIntegrityViolationException failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean hasSqlCause = false;
        boolean duplicateMessage = false;
        for (Throwable cause = failure;
                cause != null && visited.add(cause);
                cause = cause.getCause()) {
            String message = cause.getMessage();
            if (cause instanceof SQLException sqlException) {
                hasSqlCause = true;
                if (hasEmailKey(message)
                        && (sqlException.getErrorCode() == 1062 || isDuplicateEntry(message))) {
                    return true;
                }
            } else if (isDuplicateEntry(message) && hasEmailKey(message)) {
                duplicateMessage = true;
            }
        }
        // 有驱动异常时以它为准，不能把 Spring/MyBatis 包装消息中的 SQL 当作实际索引。
        return !hasSqlCause && duplicateMessage;
    }

    private static boolean isDuplicateEntry(String message) {
        return message != null && DUPLICATE_ENTRY.matcher(message).matches();
    }

    private static boolean hasEmailKey(String message) {
        return message != null && EMAIL_KEY.matcher(message).find();
    }
}
