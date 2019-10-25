package com.github.larva.zhang.problems;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import lombok.Data;

/**
 * DeserializationUseJacksonReplaceFastJsonTest
 *
 * 仅对fastjson反序列化特性在jackson中并未提供等价特性，但是可以通过某种方法达到相同效果时的测试
 *
 * @see Feature
 * @see JsonParser.Feature
 * @see DeserializationFeature
 * @see com.fasterxml.jackson.databind.MapperFeature
 * @author zhanghan
 * @date 2019/9/18
 * @since 1.0
 */
@RunWith(JUnit4.class)
@SuppressWarnings("WeakerAccess")
public class DeserializationUseJacksonReplaceFastJsonTest {

    @Test
    public void printFastJsonDefaultParserFeature() {
        for (Feature feature : Feature.values()) {
            if (Feature.isEnabled(JSON.DEFAULT_PARSER_FEATURE, feature)) {
                System.out.println(feature);
            }
        }
    }

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/fastjson/deserializer/TestISO8601Date.java">TestISO8601Date.java</a>
     */
    @Test
    public void testJacksonReplaceAllowISO8601DateFormat() throws IOException {
        Calendar gmtCale = Calendar.getInstance();
        gmtCale.clear();
        gmtCale.setTimeZone(TimeZone.getTimeZone("GMT"));
        gmtCale.set(2018, Calendar.MAY, 31, 19, 13, 42);
        Date gmtDate = gmtCale.getTime();
        String gmtText = "[\"2018-05-31T19:13:42Z\",\"2018-05-31T19:13:42.000Z\"]";
        ObjectMapper objectMapper = new ObjectMapper();
        List<Date> gmtList = objectMapper.readValue(gmtText, new TypeReference<List<Date>>() {});
        Assert.assertEquals(gmtList.get(0), gmtDate);
        Assert.assertEquals(gmtList.get(1), gmtDate);
        Calendar gmt7Cale = Calendar.getInstance();
        gmt7Cale.clear();
        gmt7Cale.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        gmt7Cale.set(2018, Calendar.MAY, 31, 19, 13, 42);
        Date gmt7Date = gmt7Cale.getTime();
        String gmt7Text = "[\"2018-05-31T19:13:42+07:00\",\"2018-05-31T19:13:42.000+07:00\"]";
        List<Date> gmt7List = objectMapper.readValue(gmt7Text, new TypeReference<List<Date>>() {});
        Assert.assertEquals(gmt7Date, gmt7List.get(0));
        Assert.assertEquals(gmt7Date, gmt7List.get(1));
    }

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/json/bvt/parser/TestInitStringFieldAsEmpty2.java">TestInitStringFieldAsEmpty2.java</a>
     * @throws IOException
     */
    @Test
    public void testJacksonReplaceInitStringFieldAsEmpty() throws IOException {
        String text = "{\"strEmptyField\":null,\"collectionEmptyField\":[\"1\",null,\"3\"]}";
        ObjectMapper objectMapper = new ObjectMapper();
        InitStringFieldAsEmptyBean initStringFieldAsEmptyBean =
                objectMapper.readValue(text, InitStringFieldAsEmptyBean.class);
        Assert.assertNotNull(initStringFieldAsEmptyBean.getStrEmptyField());
        Assert.assertEquals("", initStringFieldAsEmptyBean.getStrEmptyField());
        Assert.assertNotNull(initStringFieldAsEmptyBean.getCollectionEmptyField());
        Assert.assertEquals("1", initStringFieldAsEmptyBean.getCollectionEmptyField().get(0));
        Assert.assertEquals("", initStringFieldAsEmptyBean.getCollectionEmptyField().get(1));
        Assert.assertEquals("3", initStringFieldAsEmptyBean.getCollectionEmptyField().get(2));
    }

    @Data
    public static class InitStringFieldAsEmptyBean {
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private String strEmptyField;

        @JsonSetter(contentNulls = Nulls.AS_EMPTY)
        private List<String> collectionEmptyField;
    }

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/json/bvt/bug/Issue900.java">Issue900.java</a>
     * @throws IOException
     */
    @Test
    public void testJacksonReplaceSupportNonPublicField() throws IOException {
        String text = "{\"id\":123}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        SupportNonPublicFieldBean supportNonPublicFieldBean =
                objectMapper.readValue(text, SupportNonPublicFieldBean.class);
        Assert.assertNotNull(supportNonPublicFieldBean);
        Assert.assertEquals(123, supportNonPublicFieldBean.id);
    }

    public static class SupportNonPublicFieldBean {
        private int id;
    }

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/json/bvt/feature/DisableFieldSmartMatchTest.java">DisableFieldSmartMatchTest.java</a>
     * @throws IOException
     */
    @Test
    public void testJacksonReplaceDisableFieldSmartMatch() throws IOException {
        String text = "{\"person_id\":123}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        DisableFieldSmartMatchBean disableFieldSmartMatchBean =
                objectMapper.readValue(text, DisableFieldSmartMatchBean.class);
        Assert.assertEquals(123, disableFieldSmartMatchBean.personId);
    }

    public static class DisableFieldSmartMatchBean {
        public int personId;
    }

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/json/bvt/issue_1600/Issue1653.java">Issue1653.java</a>
     */
    @Test
    public void testJacksonReplaceCustomMapDeserializer() throws IOException {
        String text = "{\"val\":{}}";
        ObjectMapper objectMapper = new ObjectMapper();
        CaseInsensitiveMap map = objectMapper.readValue(text, CaseInsensitiveMap.class);
        Assert.assertNotNull(map.get("val"));
        Assert.assertEquals("{}", map.get("val").toString());
    }

    @Rule
    public ExpectedException ReplaceErrorOnEnumNotMatchExpectedEx = ExpectedException.none();

    /**
     * @see <a href=
     *      "https://github.com/alibaba/fastjson/tree/master/src/test/java/com/alibaba/json/bvt/issue_2200/Issue2249.java">Issue2249.java</a>
     * @throws IOException
     */
    @Test
    public void testJacksonReplaceErrorOnEnumNotMatch() throws IOException {
        String text = "[\"M\", \"MATCH\"]";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        List<ErrorOnEnumNotMatchEnums> list =
                objectMapper.readValue(text, new TypeReference<List<ErrorOnEnumNotMatchEnums>>() {});
        Assert.assertNull(list.get(0));
        Assert.assertEquals(ErrorOnEnumNotMatchEnums.MATCH, list.get(1));
        ReplaceErrorOnEnumNotMatchExpectedEx.expect(JsonProcessingException.class);
        objectMapper.disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        objectMapper.readValue(text, new TypeReference<List<ErrorOnEnumNotMatchEnums>>() {});
    }

    public enum ErrorOnEnumNotMatchEnums {
        MATCH
    }
}
