package com.yggdrasil.labs.client.dto.cmd;

import com.alibaba.cola.dto.Command;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CreateCustomerCmd：创建客户命令
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CreateCustomerCmd extends Command {
    private String customerId;
    private String memberId;
    private String customerName;
    private String customerType;
    private String companyName;
    private String source;
}
