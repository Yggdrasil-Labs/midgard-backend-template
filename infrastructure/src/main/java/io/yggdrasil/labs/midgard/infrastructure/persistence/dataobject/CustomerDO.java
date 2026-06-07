package io.yggdrasil.labs.midgard.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yggdrasil.labs.mimir.mybatis.annotation.AutoMybatis;
import lombok.Data;

import java.time.LocalDateTime;

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
