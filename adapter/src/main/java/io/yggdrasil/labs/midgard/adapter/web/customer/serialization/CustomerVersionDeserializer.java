package io.yggdrasil.labs.midgard.adapter.web.customer.serialization;

import java.io.IOException;
import java.io.Serial;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/** 仅允许客户版本使用 JSON 整数，null 留给字段约束处理。 */
public class CustomerVersionDeserializer extends StdDeserializer<Integer> {

    @Serial private static final long serialVersionUID = 1L;

    /** 创建仅接受整数 token 的客户版本反序列化器。 */
    public CustomerVersionDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return context.reportInputMismatch(Integer.class, "客户版本必须为JSON整数");
        }
        return parser.getIntValue();
    }
}
