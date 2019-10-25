package com.github.larva.zhang.problems;

import java.io.IOException;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import lombok.Data;

/**
 * SerializationUseJacksonReplaceFastJsonTest
 *
 * 仅对fastjson序列化特性在jackson中并未提供等价特性，但是可以通过某种方法达到相同效果时的测试
 *
 * @see SerializerFeature
 * @see JsonGenerator.Feature
 * @see SerializationFeature
 * @author zhanghan
 * @date 2019/9/23
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class SerializationUseJacksonReplaceFastJsonTest {

    @Test
    public void printFastJsonDefaultGenerateFeature() {
        for (SerializerFeature feature : SerializerFeature.values()) {
            if (SerializerFeature.isEnabled(JSON.DEFAULT_GENERATE_FEATURE, feature)) {
                System.out.println(feature);
            }
        }
    }

    @Test
    public void testJacksonReplaceFastJsonWriteNullListAsEmptyUsePropertyFilter() throws JsonProcessingException {
        WriteNullListAsEmptyUsePropertyFilterBean writeNullListAsEmptyUsePropertyFilterBean =
                new WriteNullListAsEmptyUsePropertyFilterBean();
        ObjectMapper objectMapper = new ObjectMapper();
        SerializationConfig serializationConfig = objectMapper.getSerializationConfig().withFilters(
                new SimpleFilterProvider().addFilter("nullListAsEmptyFilter", new SimpleBeanPropertyFilter() {

                    @Override
                    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                            PropertyWriter writer) throws Exception {
                        if (include(writer)) {
                            if (writer.getName().equals("asEmptyField")
                                    && pojo instanceof WriteNullListAsEmptyUsePropertyFilterBean) {
                                List<Boolean> asEmptyField =
                                        ((WriteNullListAsEmptyUsePropertyFilterBean) pojo).getAsEmptyField();
                                if (asEmptyField == null) {
                                    jgen.writeFieldName(writer.getName());
                                    jgen.writeStartArray();
                                    jgen.writeEndArray();
                                    return;
                                }
                            }
                            writer.serializeAsField(pojo, jgen, provider);
                        } else if (!jgen.canOmitFields()) { // since 2.3
                            writer.serializeAsOmittedField(pojo, jgen, provider);
                        }
                    }
                }));
        objectMapper.setConfig(serializationConfig);
        Assert.assertEquals("{\"asEmptyField\":[],\"asNullField\":null}",
                objectMapper.writeValueAsString(writeNullListAsEmptyUsePropertyFilterBean));
    }

    @Data
    @JsonFilter("nullListAsEmptyFilter")
    public static class WriteNullListAsEmptyUsePropertyFilterBean {

        private List<Boolean> asEmptyField;

        private List<Boolean> asNullField;
    }

    @Test
    public void testJacksonReplaceFastJsonWriteNullListAsEmptyUseBeanSerializerModifier()
            throws JsonProcessingException {
        WriteNullListAsEmptyUseBeanSerializerModifierBean writeNullListAsEmptyUseBeanSerializerModifierBean =
                new WriteNullListAsEmptyUseBeanSerializerModifierBean();
        ObjectMapper objectMapper = new ObjectMapper();
        SerializerFactoryConfig factoryConfig =
                new SerializerFactoryConfig().withSerializerModifier(new BeanSerializerModifier() {

                    @Override
                    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                            BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                        for (BeanPropertyWriter beanPropertyWriter : beanProperties) {
                            JavaType type = beanPropertyWriter.getType();
                            if (isCollectionType(type) && beanPropertyWriter.getName().equals("asEmptyField")) {
                                beanPropertyWriter.assignNullSerializer(new JsonSerializer<Object>() {
                                    public void serialize(Object value, JsonGenerator gen,
                                            SerializerProvider serializers) throws IOException {
                                        gen.writeStartArray();
                                        gen.writeEndArray();
                                    }
                                });
                            }
                        }
                        return beanProperties;
                    }

                    private boolean isCollectionType(JavaType type) {
                        return Collection.class.isAssignableFrom(type.getRawClass());
                    }
                });
        objectMapper.setSerializerFactory(BeanSerializerFactory.instance.withConfig(factoryConfig));
        Assert.assertEquals("{\"asEmptyField\":[],\"asNullField\":null}",
                objectMapper.writeValueAsString(writeNullListAsEmptyUseBeanSerializerModifierBean));
    }

    @Data
    public static class WriteNullListAsEmptyUseBeanSerializerModifierBean {

        private List<Boolean> asEmptyField;

        private List<Boolean> asNullField;
    }
}
