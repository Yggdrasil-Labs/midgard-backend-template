package io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yggdrasil.labs.mybatis.annotation.AutoMybatis;

import lombok.Data;

/** 客户表的持久化记录。 */
@Data
@TableName("customer")
@AutoMybatis
public class CustomerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
