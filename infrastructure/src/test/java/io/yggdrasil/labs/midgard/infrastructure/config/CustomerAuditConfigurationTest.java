package io.yggdrasil.labs.midgard.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yggdrasil.labs.mybatis.audit.AuditMetaObjectHandler;
import com.yggdrasil.labs.mybatis.audit.AuditorProvider;

import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;

class CustomerAuditConfigurationTest {

    private TableInfo tableInfo;

    private final AuditorProvider auditorProvider =
            new CustomerAuditConfiguration().customerAuditorProvider();
    private final AuditMetaObjectHandler handler = new AuditMetaObjectHandler(auditorProvider);

    @BeforeEach
    void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(false);
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, "customer-audit");
        assistant.setCurrentNamespace(CustomerAuditConfigurationTest.class.getName());
        tableInfo = TableInfoHelper.initTableInfo(assistant, CustomerDO.class);
    }

    @Test
    void tableMetadata_shouldMapMimirAuditFieldsAndVersion() {
        assertThat(tableInfo.getFieldList())
                .extracting(TableFieldInfo::getColumn)
                .contains("create_time", "update_time", "create_by", "update_by", "version")
                .doesNotContain("created_at", "updated_at");
        assertThat(tableInfo.isWithVersion()).isTrue();
        assertThat(tableInfo.getVersionFieldInfo().getPropertyType()).isEqualTo(Integer.class);
    }

    @Test
    void creationAuditFields_shouldBeExcludedFromUpdateSql() {
        assertThat(tableInfo.getFieldList())
                .filteredOn(field -> field.getProperty().startsWith("create"))
                .hasSize(2)
                .allSatisfy(
                        field -> {
                            assertThat(field.getFieldFill()).isEqualTo(FieldFill.INSERT);
                            assertThat(field.getUpdateStrategy()).isEqualTo(FieldStrategy.NEVER);
                        });
        assertThat(tableInfo.getAllSqlSet(false, "et."))
                .contains("update_by=", "update_time=")
                .doesNotContain("create_by=", "create_time=");
    }

    @Test
    void auditorProvider_shouldUseSystemByDefault() {
        assertThat(auditorProvider.currentAuditor()).isEqualTo("system");
    }

    @Test
    void insertFill_shouldPopulateAllAuditFieldsUsingMimirHandler() {
        CustomerDO dataObject = new CustomerDO();
        LocalDateTime before = LocalDateTime.now();

        handler.insertFill(SystemMetaObject.forObject(dataObject));

        assertThat(dataObject.getCreateBy()).isEqualTo("system");
        assertThat(dataObject.getUpdateBy()).isEqualTo("system");
        assertThat(dataObject.getCreateTime()).isBetween(before, LocalDateTime.now());
        assertThat(dataObject.getUpdateTime()).isEqualTo(dataObject.getCreateTime());
    }

    @Test
    void updateFill_shouldPopulateUpdateFieldsAndPreserveCreationAudit() {
        CustomerDO dataObject = new CustomerDO();
        LocalDateTime created = LocalDateTime.of(2020, 1, 2, 3, 4);
        dataObject.setCreateTime(created);
        dataObject.setCreateBy("original-user");
        LocalDateTime before = LocalDateTime.now();

        handler.updateFill(SystemMetaObject.forObject(dataObject));

        assertThat(dataObject.getCreateTime()).isEqualTo(created);
        assertThat(dataObject.getCreateBy()).isEqualTo("original-user");
        assertThat(dataObject.getUpdateBy()).isEqualTo("system");
        assertThat(dataObject.getUpdateTime()).isBetween(before, LocalDateTime.now());
    }
}
