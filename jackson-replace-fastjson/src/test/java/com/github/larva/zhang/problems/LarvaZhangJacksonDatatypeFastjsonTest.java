package com.github.larva.zhang.problems;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * LarvaZhangJacksonDatatypeFastjsonTest
 *
 * @author zhanghan
 * @date 2019/10/25
 * @since 1.0
 */
public class LarvaZhangJacksonDatatypeFastjsonTest extends TestCase {

    public void testReadObject() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JSONObject ob = mapper.readValue("{\"a\":{\"b\":3}, \"c\":[9, -4], \"d\":null, \"e\":true}", JSONObject.class);
        assertEquals(4, ob.size());
        JSONObject ob2 = ob.getJSONObject("a");
        assertEquals(1, ob2.size());
        assertEquals(3, ob2.getIntValue("b"));
        JSONArray array = ob.getJSONArray("c");
        assertEquals(2, array.size());
        assertEquals(9, array.getIntValue(0));
        assertEquals(-4, array.getIntValue(1));
        assertNull(ob.get("d"));
        assertTrue(ob.getBoolean("e"));
    }

    public void testReadArray() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONArray jacksonParsedArray =
                mapper.readValue("[null, 13, false, 1.25, \"abc\", {\"a\":13}, [ ] ]", JSONArray.class);
        assertEquals(7, jacksonParsedArray.size());
        assertNull(jacksonParsedArray.get(0));
        assertEquals(13, jacksonParsedArray.getIntValue(1));
        assertFalse(jacksonParsedArray.getBoolean(2));
        assertEquals(Double.valueOf(1.25), jacksonParsedArray.getDouble(3));
        assertEquals("abc", jacksonParsedArray.getString(4));
        JSONObject ob = jacksonParsedArray.getJSONObject(5);
        assertEquals(1, ob.size());
        assertEquals(13, ob.getIntValue("a"));
        JSONArray array2 = jacksonParsedArray.getJSONArray(6);
        assertEquals(0, array2.size());
    }

    public void testWriteObject() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Ok: let's create JSONObject from jsonString text
        String jsonString = "{\"a\":{\"b\":3}}";
        JSONObject jsonObject = JSON.parseObject(jsonString);
        assertEquals(jsonString, mapper.writeValueAsString(jsonObject));
    }

    public void testWriteArray() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Ok: let's create JSONObject from jsonString text
        String jsonString = "[1,true,\"text\",[null,3],{\"a\":[1.25]}]";
        JSONArray jsonArray = JSON.parseArray(jsonString);
        assertEquals(jsonString, mapper.writeValueAsString(jsonArray));
    }
}
