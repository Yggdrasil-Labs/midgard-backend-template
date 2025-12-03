package com.yggdrasil.labs.domain.customer.repository;

import com.yggdrasil.labs.domain.customer.model.Credit;

/**
 * 信用额度领域仓储接口.
 *
 * <p>命名遵循领域层仓储命名规范：使用 Repository 结尾。</p>
 */
public interface CreditRepository {
  Credit getCredit(String customerId);
}

