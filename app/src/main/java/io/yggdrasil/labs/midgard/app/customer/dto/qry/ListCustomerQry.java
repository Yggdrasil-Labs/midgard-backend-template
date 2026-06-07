package io.yggdrasil.labs.midgard.app.customer.dto.qry;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;

@Data
public class ListCustomerQry {

    @Min(value = 1, message = "page必须大于0")
    private int page = 1;

    @Min(value = 1, message = "size必须大于0")
    @Max(value = 100, message = "size不能超过100")
    private int size = 10;
}
