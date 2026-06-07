package io.yggdrasil.labs.midgard.app.customer.dto.qry;

import lombok.Data;

@Data
public class ListCustomerQry {

    private int page = 1;
    private int size = 10;
}
