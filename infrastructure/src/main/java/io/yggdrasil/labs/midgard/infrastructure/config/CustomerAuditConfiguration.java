package io.yggdrasil.labs.midgard.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yggdrasil.labs.mybatis.audit.AuditorProvider;

/** 客户持久化的审计主体示例。 */
@Configuration(proxyBeanMethods = false)
public class CustomerAuditConfiguration {

    /** 接入认证时替换此处的主体来源，主体标识须符合数据库 64 字符上限。 */
    @Bean
    public AuditorProvider customerAuditorProvider() {
        return () -> "system";
    }
}
