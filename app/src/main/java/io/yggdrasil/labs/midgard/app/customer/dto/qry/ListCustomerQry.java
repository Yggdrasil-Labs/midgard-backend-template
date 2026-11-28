package io.yggdrasil.labs.midgard.app.customer.dto.qry;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;

import com.yggdrasil.labs.common.page.PageRequest;

import lombok.Data;

/** 分页查询客户用例的筛选参数。 */
@Data
public class ListCustomerQry implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Max(value = Integer.MAX_VALUE, message = "pageIndex超出支持范围")
    private Long pageIndex = 1L;

    private Long pageSize = 10L;

    private String keyword;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)?$", message = "status只能是ACTIVE或INACTIVE")
    private String status;

    /** 转换为已纠正的分页请求；不改写原查询对象。 */
    public PageRequest toPageRequest() {
        return PageRequest.of(pageIndex, pageSize);
    }
}
