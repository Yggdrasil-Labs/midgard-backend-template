package io.yggdrasil.labs.midgard.infrastructure.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;

import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.CustomerDO;
import io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject.mapper.CustomerMapper;

/** 核对真实 Mapper SQL 和乐观锁插件，不建立数据库连接。 */
class CustomerOptimisticLockTest {

    private static MybatisConfiguration configuration;

    @BeforeAll
    static void initializeMapper() {
        configuration = new MybatisConfiguration();
        configuration.addMapper(CustomerMapper.class);
    }

    @Test
    void generatedUpdateUsesOriginalVersionAndNeverOverwritesCreationAudit() {
        CustomerDO record = new CustomerDO();
        record.setId(8L);
        record.setName("Lin");
        record.setVersion(Integer.MAX_VALUE - 1);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Constants.ENTITY, record);
        MappedStatement statement = statement("updateById");

        new OptimisticLockerInnerInterceptor().beforeUpdate(null, statement, parameters);
        BoundSql sql = statement.getBoundSql(parameters);

        assertThat(record.getVersion()).isEqualTo(Integer.MAX_VALUE);
        assertThat(parameters.get(Constants.MP_OPTLOCK_VERSION_ORIGINAL))
                .isEqualTo(Integer.MAX_VALUE - 1);
        assertThat(sql.getSql())
                .containsPattern("(?s)WHERE\\s+id\\s*=\\s*\\?\\s+AND\\s+version\\s*=\\s*\\?");
        assertThat(sql.getSql()).contains("version=").doesNotContain("create_time=", "create_by=");
        assertThat(statement.isFlushCacheRequired()).isTrue();
    }

    @Test
    void lockingReadKeepsForUpdateAndDoesNotUseSecondLevelCache() {
        LambdaQueryWrapper<CustomerDO> query =
                new LambdaQueryWrapper<CustomerDO>().eq(CustomerDO::getId, 8L).last("FOR UPDATE");
        MappedStatement statement = statement("selectList");

        assertThat(statement.getBoundSql(Map.of(Constants.WRAPPER, query)).getSql().strip())
                .endsWith("FOR UPDATE");
        assertThat(statement.getCache()).isNull();
    }

    private static MappedStatement statement(String method) {
        return configuration.getMappedStatement(CustomerMapper.class.getName() + "." + method);
    }
}
