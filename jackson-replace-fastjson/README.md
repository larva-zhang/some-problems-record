# 为什么要替换fastjson
工程里大量使用了fastjson作为序列化和反序列化框架，甚至ORM在处理部分字段也依赖fastjson进行序列化和反序列化。那么作为大量使用的基础框架，为什么还要进行替换呢？

原因有以下几点：
1. fastjson太过于侧重性能，对于部分高级特性支持不够，而且部分自定义特性完全偏离了json和js规范导致和其他框架不兼容；
2. fastjson文档缺失较多，部分Feature甚至没有文档，而且代码缺少注释较为晦涩；
3. fastjson的CVE bug监测较弱，很多CVE数据库网站上有关fastjson的CVE寥寥无几，例如近期的AutoType导致的高危漏洞，虽然和Jackson的PolymorphicDeserialization是同样的bug，但是CVE网站上几乎没有fastjson的bug报告。

# 框架选型
参考[mvnrepository json libraries](https://mvnrepository.com/open-source/json-libraries)，根据流行度排序后前十名框架：
* jackson2(com.fasterxml.jackson)
* gson
* org.json
* jackson1(com.codehuas.jackson)
* fastjson
* cheshire
* json-simple
![](https://img2018.cnblogs.com/blog/464089/201909/464089-20190918180822511-317652641.png)
 
jackson1是已经过时的框架，因此可以忽略，cheshire和json-simple排名尚且不如fastjson，也忽略，剩余jackson2、gson以及org.json，其中org.json的使用量(usage)远小于jackson2(方便起见，下文均以jackson均指代jackson2)和gson，因此org.json也可以排除了。

关于jackson和gson的比较文章有很多，[stackoverflow](https://stackoverflow.com/)上自行搜索，下面仅推荐几篇blog：
* [jackson vs gson](https://www.baeldung.com/jackson-vs-gson)
* [JSON in Java](https://www.baeldung.com/java-json)
* [the ultimate json library json-simple vs gson vs jackson vs json](https://blog.overops.com/the-ultimate-json-library-json-simple-vs-gson-vs-jackson-vs-json/)

在功能特性支持、稳定性、可扩展性、易用性以及社区活跃度上 jackson 和 gson 差不多，入门教程可以分别参考[baeldung jackson系列](https://www.baeldung.com/category/json/jackson/) 以及 [baeldung gson系列](https://www.baeldung.com/tag/gson/)。但是jackson有更多现成的类库兼容支持例如`jackson-datatype-commons-lang3`，以及更丰富的输出数据格式支持例如`jackson-dataformat-yaml`，而且spring框架默认使用jackson，因此最终我选择使用jackson。

PS: Jackson 2.10.0开始尝试基于新的API使用白名单机制来避免RCE漏洞，详见[https://github.com/FasterXML/jackson-databind/issues/2195](https://github.com/FasterXML/jackson-databind/issues/2195)，效果尚待观察。

# 替换fastjson
fastjson常见的使用场景就是序列化和反序列化，偶尔会有`JSONObject`和`JSONArray`实例的相关操作。

以下步骤的源码分析基于以下版本：
* `fastjson v1.2.60`
* `jackson-core v2.9.9`
* `jackson-annotations v2.9.0`
* `jackson-databind v2.9.9.3`


## Deserialization
fastjson将json字符串反序列化成Java Bean通常使用`com.alibaba.fastjson.JSON`的静态方法(`JSONObject`和`JSONArray`的静态方法也是来自于`JSON`)，常用的有以下几个API：
```java
public static JSONObject parseObject(String text);

public static JSONObject parseObject(String text, Feature... features);

public static <T> T parseObject(String text, Class<T> clazz);

public static <T> T parseObject(String text, Class<T> clazz, Feature... features);

public static <T> T parseObject(String text, TypeReference<T> type, Feature... features);

public static JSONArray parseArray(String text);

public static <T> List<T> parseArray(String text, Class<T> clazz);
```
从方法入参就能猜到，fastjson在执行反序列化时的Parse行为由`com.alibaba.fastjson.parser.Feature`指定。研究`parseObject`的源码后，发现底层最终都是使用的以下方法：
```java
public static <T> T parseObject(String input, Type clazz, ParserConfig config, ParseProcess processor, int featureValues, Feature... features) {
        if (input == null) {
            return null;
        }

        // featureValues作为基准解析特性开关值
        // 入参features和featureValues取并集得到最终的解析特性
        if (features != null) {
            for (Feature feature : features) {
                featureValues |= feature.mask;
            }
        }

        DefaultJSONParser parser = new DefaultJSONParser(input, config, featureValues);

        if (processor != null) {
            if (processor instanceof ExtraTypeProvider) {
                parser.getExtraTypeProviders().add((ExtraTypeProvider) processor);
            }

            if (processor instanceof ExtraProcessor) {
                parser.getExtraProcessors().add((ExtraProcessor) processor);
            }

            if (processor instanceof FieldTypeResolver) {
                parser.setFieldTypeResolver((FieldTypeResolver) processor);
            }
        }

        T value = (T) parser.parseObject(clazz, null);

        parser.handleResovleTask(value);

        parser.close();

        return (T) value;
    }
```
通过IDE搜索usage后，发现当没有作为基准解析特性开关的`featureValues`入参时，都是使用的`DEFAULT_PARSE_FEATURE`作为基准解析特性开关，以下是`JSON.DEFAULT_PARSE_FEATURE`的实例化代码：
```java
static {
        int features = 0;
        features |= Feature.AutoCloseSource.getMask();
        features |= Feature.InternFieldNames.getMask();
        features |= Feature.UseBigDecimal.getMask();
        features |= Feature.AllowUnQuotedFieldNames.getMask();
        features |= Feature.AllowSingleQuotes.getMask();
        features |= Feature.AllowArbitraryCommas.getMask();
        features |= Feature.SortFeidFastMatch.getMask();
        features |= Feature.IgnoreNotMatch.getMask();
        DEFAULT_PARSER_FEATURE = features;
}
```
fastjson还会从环境变量中读取配置来修改`DEFAULT_PARSER_FEATURE`(虽然很少会有人这么做)，但最好还是通过实际运行一下程序来确认你的环境中的实际解析特性开关。
```java
    @Test
    public void printFastJsonDefaultParserFeature() {
        for (Feature feature : Feature.values()) {
            if (Feature.isEnabled(JSON.DEFAULT_PARSER_FEATURE, feature)) {
                System.out.println(feature);
            }
        }
    }
```

### fastjson 和 jackson的反序列化特性对照表

| fastjson特性说明 | fastjson枚举 | fastjson默认状态 | jackson枚举 | jackson默认状态 | jackson特性说明 |
|---|---|---|---|---|---|
| Parser close时自动关闭为创建Parser实例而创建的底层InputStream以及Reader等输入流 | Feature.AutoCloseSource | 开启 | JsonParser.Feature.AUTO_CLOSE_SOURCE | 开启 | 保持开启 |
| 允许json字符串中带注释 | Feature.AllowComment | 关闭 | JsonParser.Feature.ALLOW_COMMENTS | 关闭 | 根据系统的json数据情况开启 |
| 允许json字段名不被引号包括起来 | Feature.AllowUnQuotedFieldNames | 开启 | JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES | 关闭 | 根据系统的json数据情况开启 |
| 允许json字段名使用单引号包括起来 | Feature.AllowSingleQuotes | 开启 | JsonParser.Feature.ALLOW_SINGLE_QUOTES | 关闭 | 根据系统的json数据情况开启 |
| 将json字段名作为字面量缓存起来，即`fieldName.intern()` | Feature.InternFieldNames | 开启 | - | - | jackson不支持该特性，会影响内存占用以及解析速度，但影响不大 |
| 识别ISO8601格式的日期字符串，例如：`2018-05-31T19:13:42.000Z`或`2018-05-31T19:13:42.000+07:00` | Feature.AllowISO8601DateFormat | 关闭 | - | - | jackson默认支持ISO8601格式日期字符串的解析，并且也可以通过`ObjectMapper.setDateFormat`指定解析格式 |
| 忽略json中包含的连续的多个逗号，非标准特性 | Feature.AllowArbitraryCommas | 关闭 | - | - | jackson不支持该特性，且该特性是非标准特性，因此可以忽略 |
| 将json中的浮点数解析成BigDecimal对象，禁用后会解析成Double对象 | Feature.UseBigDecimal | 开启 | DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS | 关闭 | 建议开启 |
| 解析时忽略未知的字段继续完成解析 | Feature.IgnoreNotMatch | 开启 | DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES | 开启 | jackson默认开启遇到未知属性需要抛异常，因此如要和fastjson保持一致则需要关闭该特性 |
| 如果你用fastjson序列化的文本，输出的结果是按照fieldName排序输出的，parser时也能利用这个顺序进行优化读取。这种情况下，parser能够获得非常好的性能 | Feature.SortFeidFastMatch | 关闭 | - | - | fastjson内部处理逻辑，jackson不支持该特性，不影响功能 |
| 禁用ASM | Feature.DisableASM | 关闭 | - | - | fastjson内部处理逻辑，jackson不支持该特性，不影响功能 |  
| 禁用循环引用检测 | Feature.DisableCircularReferenceDetect | 关闭 | - | - | fastjson内部处理逻辑，jackson不支持该特性，不影响功能 | 
| 对于没有值的字符串属性设置为空串 | Feature.InitStringFieldAsEmpty | 关闭 | - | - | jackson不支持该特性，但是可以通过`@JsonSetter`的`nulls()`和`contentNulls()`分别设置Bean以及Array/Collection的元素对`null`的处理方式。例如`Nulls.AS_EMPTY`就会将`null`设置为`JsonDeserializer.getEmptyValue` |
| 非标准特性，允许将数组按照字段顺序解析成Java Bean，例如`"[1001,\"xx\",33]"`可以等价为`"{\"id\": 10001, \"name\": \"xx\", \"age\": 33}"` | Feature.SupportArrayToBean | 关闭 | - | - | 非标准特性，且使用场景较少，jackson不支持该特性 |
| 解析后属性保持原来的顺序 | Feature.OrderedField | 关闭 | - | - | - |  
| 禁用特殊字符检查 | Feature.DisableSpecialKeyDetect | 关闭 | - | - | - | 
| 使用对象数组而不是集合 | Feature.UseObjectArray | 关闭 | DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY | 关闭 | 保持关闭 |
| 支持解析没有setter方法的非public属性 | Feature.SupportNonPublicField | 关闭 | - | - | jaskson可以通过`ObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)`来达到相同的目的 |
| 禁用fastjson的AUTOTYPE特性，即不按照json字符串中的`@type`自动选择反序列化类 | Feature.IgnoreAutoType | 关闭 | - | - | jackson的[PolymorphicDeserialization](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)默认是支持`Object.class`、`abstract classes`、`interfaces`属性的AUTO Type，但是该特性容易导致安全漏洞，强烈建议使用`ObjectMapper.disableDefaultTyping()`设置为只允许`@JsonTypeInfo`生效 |
| 禁用属性智能匹配，例如下划线自动匹配驼峰等 | Feature.DisableFieldSmartMatch | 关闭 | - | - | jackson可以通过`ObjectMapper.setPropertyNamingStrategy()`达到相同的目的，但这种是针对一个json串的统一策略，如果要在一个json串中使用不同的策略则可以使用`@JsonProperty.value()`指定字段名 |
| 启用fastjson的autotype功能，即根据json字符串中的`@type`自动选择反序列化的类 | Feature.SupportAutoType | 关闭 | ObjectMapper.DefaultTyping.* | 开启 | jackson的[PolymorphicDeserialization](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)支持不同级别的AUTO TYPE，但是这个功能容易导致安全漏洞，强烈建议使用`ObjectMapper.disableDefaultTyping()`设置为只允许`@JsonTypeInfo`生效 | 
| 解析时将未用引号包含的json字段名作为String类型存储，否则只能用原始类型获取key的值。例如`String text="{123:\"abc\"}"`在启用了`NonStringKeyAsString`后可以通过`JSON.parseObject(text).getString("123")`的方式获取到`"abc"`，而在不启用`NonStringKeyAsString`时，`JSON.parseObject(text).getString("123")`只能得到`null`，必须通过`JSON.parseObject(text).get(123)`的方式才能获取到`"abc"`。| Feature.NonStringKeyAsString | 关闭 | - | - | 非标准特性，jackson并不支持 | 
| 自定义`"{\"key\":value}"`解析成`Map`实例，否则解析为`JSONObject` | Feature.CustomMapDeserializer | 关闭 | - | - | jackson没有相应的全局特性，但是可以通过`TypeReference`达到相同的效果 | 
| 枚举未匹配到时抛出异常，否则解析为`null` | Feature.ErrorOnEnumNotMatch | 关闭 | DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL | 关闭 | fastjson默认解析为`null`，jackson则相反，默认会抛异常，建议采用jackson默认行为 | 

反序列化fastjson和jackson的特性TestCase见[DeserializationUseJacksonReplaceFastJsonTest.java](https://github.com/zhanghan0031/some-problems-record/blob/master/jackson-replace-fastjson/src/test/java/com/zxl/problems/DeserializationUseJacksonReplaceFastJsonTest.java)


## Serialization
fastjson将Java Bean序列化成json字符串通常也是使用`com.alibaba.fastjson.JSON`的静态方法(`JSONObject`和`JSONArray`的静态方法也是来自于`JSON`)，常用的有以下几个API：
```java
public static String toJSONString(Object object);

public static String toJSONString(Object object, SerializerFeature... features);

public static String toJSONStringWithDateFormat(Object object, String dateFormat, SerializerFeature... features);

public static String toJSONString(Object object, boolean prettyFormat);

public static void writeJSONString(Writer writer, Object object, SerializerFeature... features);
```
从方法入参也能看出，在序列化时，fastjson的特性由`SerializerFeature`控制，研究`toJSONString`的源码后，发现最终都会调用以下方法：
```java
 public static String toJSONString(Object object, SerializeConfig config, SerializeFilter[] filters, String dateFormat, int defaultFeatures, SerializerFeature... features) {
         SerializeWriter out = new SerializeWriter(null, defaultFeatures, features);
 
         try {
             JSONSerializer serializer = new JSONSerializer(out, config);
             
             if (dateFormat != null && dateFormat.length() != 0) {
                 serializer.setDateFormat(dateFormat);
                 serializer.config(SerializerFeature.WriteDateUseDateFormat, true);
             }
 
             if (filters != null) {
                 for (SerializeFilter filter : filters) {
                     serializer.addFilter(filter);
                 }
             }
 
             serializer.write(object);
 
             return out.toString();
         } finally {
             out.close();
         }
     }
```
通过IDE搜索usage后，发现当没有作为基准解析特性开关的`defaultFeatures`入参时，都是使用的`DEFAULT_GENERATE_FEATURE`作为基准解析特性开关，以下是`JSON.DEFAULT_GENERATE_FEATURE`的实例化代码：
```java
static {
        int features = 0;
        features |= SerializerFeature.QuoteFieldNames.getMask();
        features |= SerializerFeature.SkipTransientField.getMask();
        features |= SerializerFeature.WriteEnumUsingName.getMask();
        features |= SerializerFeature.SortField.getMask();

        DEFAULT_GENERATE_FEATURE = features;

        config(IOUtils.DEFAULT_PROPERTIES);
    }
```
fastjson还会从环境变量中读取配置来修改`DEFAULT_GENERATE_FEATURE`(虽然很少会有人这么做)，但最好还是通过实际运行一下程序来确认你的环境中的实际解析特性开关。
```java
    @Test
    public void printFastJsonDefaultGenerateFeature() {
        for (SerializerFeature feature : SerializerFeature.values()) {
            if (SerializerFeature.isEnabled(JSON.DEFAULT_GENERATE_FEATURE, feature)) {
                System.out.println(feature);
            }
        }
    }
```

### fastjson 和 jackson的序列化特性对照表
| fastjson特性说明 | fastjson枚举 | fastjson默认状态 | jackson枚举 | jackson默认状态 | jackson特性说明 |
|---|---|---|---|---|---| 
| 输出的json字段名被引号包含 | SerializerFeature.QuoteFieldNames | 开启 | JsonGenerator.Feature.QUOTE_FIELD_NAMES | 开启 | 保持开启 |
| 序列化时使用单引号，而不是使用双引号 | SerializerFeature.UseSingleQuotes | 关闭 | - | - | jackson不支持该特性 |
| 序列化时，value为`null`的key或field也输出 | SerializerFeature.WriteMapNullValue | 关闭 | JsonInclude.Include.ALWAYS | 开启 | 建议按需选择。注意`SerializationFeature.WRITE_NULL_MAP_VALUES`从2.9已废弃，且会被`JsonInclude.Include`给覆盖 |
| 序列化枚举时使用枚举类型的`toString()`方法，和`SerializerFeature.WriteEnumUsingName`互斥 | SerializerFeature.WriteEnumUsingToString | 关闭 | SerializationFeature.WRITE_ENUMS_USING_TO_STRING | 关闭 | 建议关闭，或者和反序列化的`DeserializationFeature.READ_ENUMS_USING_TO_STRING`保持一致 |
| 序列化枚举时使用枚举类型的`name()`方法，和`SerializerFeature.WriteEnumUsingToString`互斥 | SerializerFeature.WriteEnumUsingName | 开启 | - | - | jackson的默认行为，无需配置 |
| 序列化时对Date、Calendar等类型使用ISO8601格式进行格式化，否则以timestamp形式输出Long数字 | SerializerFeature.UseISO8601DateFormat | 关闭 | SerializationFeature.WRITE_DATES_AS_TIMESTAMPS | 开启 | jackson和fastjson的默认行为都是将Date数据输出为Long，建议根据不同的场景选择是否需要格式化日期 |
| 序列化List类型数据时将`null`输出为`"[]"` | SerializerFeature.WriteNullListAsEmpty | 关闭 | - | - | 可以通过`PropertyFilter`/`SerializerFactory.withSerializerModifier(BeanSerializerModifier)`任一一种方式达到相同效果，推荐使用`PropertyFilter` |
| 序列化String类型的field时将`null`输出为`""` | SerializerFeature.WriteNullStringAsEmpty | 关闭 | - | - | 可以通过`PropertyFilter`/`SerializerFactory.withSerializerModifier(BeanSerializerModifier)`任一一种方式达到相同效果，推荐使用`PropertyFilter` |
| 序列化Number类型的field时将`null`输出为`0` | SerializerFeature.WriteNullNumberAsZero | 关闭 | - | - | 可以通过`PropertyFilter`/`SerializerFactory.withSerializerModifier(BeanSerializerModifier)`任一一种方式达到相同效果，推荐使用`PropertyFilter` |
| 序列化Boolean类型的field时将`null`输出为`false` | SerializerFeature.WriteNullBooleanAsFalse | 关闭 | - | - | 可以通过`PropertyFilter`/`SerializerFactory.withSerializerModifier(BeanSerializerModifier)`任一一种方式达到相同效果，推荐使用`PropertyFilter` |
| 序列化时忽略`transient`修饰的field | SerializerFeature.SkipTransientField | 开启 | MapperFeature.PROPAGATE_TRANSIENT_MARKER | 关闭 | 建议保持关闭，通过`@JsonIgnore`或者`FilterProvider`来指定忽略的属性 |
| 序列化时，如果未指定`order`，则将field按照`getter`方法的字典顺序排序 | SerializerFeature.SortField | 开启 | MapperFeature.SORT_PROPERTIES_ALPHABETICALLY | 关闭 | 建议关闭，排序会影响序列化性能（fastjson在反序列化时支持按照field顺序读取解析，因此排序后的json串有利于提高fastjson的解析性能，但jackson并没有该特性） |
| 把`\t`做转义输出，**已废弃，即使开启也无效** | SerializerFeature.WriteTabAsSpecial | 关闭 | - | - | - |
| 格式化json输出 | SerializerFeature.PrettyFormat | 关闭 | SerializationFeature.INDENT_OUTPUT | 关闭 | 建议保持关闭，格式化可以交给前端完成 |
| 序列化时把类型名称写入json | SerializerFeature.WriteClassName | 关闭 | - | - | jackson可以通过`@JsonTypeInfo`达到类似的效果，参见[Jackson Annotation Examples](https://www.baeldung.com/jackson-annotations) |
| 序列化时消除对同一对象循环引用的问题 | SerializerFeature.DisableCircularReferenceDetect | 关闭 | SerializationFeature.FAIL_ON_SELF_REFERENCES | 开启 | 保持开启，避免循环引用 |
| 对斜杠'/'进行转义 | SerializerFeature.WriteSlashAsSpecial | 关闭 | - | - | jackson可以通过自定义`Serializer`实现相同效果，按需设置 |
| 将中文都会序列化为`\uXXXX`格式，字节数会多一些，但是能兼容IE 6 | SerializerFeature.BrowserCompatible | 关闭 | - | - | jackson可以通过自定义`Serializer`实现相同效果，按需设置 |
| 全局修改日期格式，默认使用`JSON.DEFFAULT_DATE_FORMAT` | SerializerFeature.WriteDateUseDateFormat | 关闭 | - | - | jackson可以通过`@JsonFormat.pattern()`、`ObjectMapper.setDateFormat()`等方式实现相同效果 |
| 序列化时不把最外层的类型名称写入json | SerializerFeature.NotWriteRootClassName | 关闭 | - | - | jackson可以通过`@JsonRootName`达到类似的效果，参见[Jackson Annotation Examples](https://www.baeldung.com/jackson-annotations) |
| 不转义特殊字符，**已废弃，即使开启也无效** | SerializerFeature.DisableCheckSpecialChar | 关闭 | - | - | - |
| 将Bean序列化时将field值按顺序当成json数组输出，而不是json object，同时不会输出fieldName，例如：`{"id":123,"name":"xxx"}`会输出成`[123,"xxx"]` | SerializerFeature.BeanToArray | 关闭 | - | - | 非标准特性，jackson并不支持 |
| 序列化Map时将非String类型的key作为String类型输出，例如：`{123:231}`会输出成`{"123":231}` | SerializerFeature.WriteNonStringKeyAsString | 关闭 | - | - | 非标准特性，jackson并不支持 |
| 序列化`Byte、Short、Integer、Long、Float、Double、Boolean`及其对应原始类型field时，如果属性值为各自类型的默认值（如`0、0F、0L`），则不会输出该属性 | SerializerFeature.NotWriteDefaultValue | 关闭 | - | - | 非标准特性，jackson并不支持 |
| 序列化时将`(`、`)`、`>`、`<`以unicode编码输出 | SerializerFeature.BrowserSecure | 关闭 | - | - | jackson可以通过自定义`Serializer`实现相同效果，按需设置，通常可以交给前端处理 |
| 序列化时忽略没有实际属性对应的getter方法 | SerializerFeature.IgnoreNonFieldGetter | 关闭 | - | - | - |
| 序列化时把非String类型数据当作String类型输出 | SerializerFeature.WriteNonStringValueAsString | 关闭 | - | - | jackson有一个类似的特性`JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS`可以将数字作为字符串输出，但没有覆盖所有非String类型  |
| 序列化时忽略会抛异常的getter方法 | SerializerFeature.IgnoreErrorGetter | 关闭 | - | - | - |
| 序列化时将BigDecimal使用toPlainString()输出 | SerializerFeature.WriteBigDecimalAsPlain | 关闭 | JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN | 关闭 | 按需开启 |
| 序列化时对Map按照Key进行排序 | SerializerFeature.MapSortField | 关闭 | SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS | 关闭 | 建议关闭，开启会影响性能 | 

序列化fastjson和jackson的特性TestCase见[SerializationUseJacksonReplaceFastJsonTest.java](https://github.com/zhanghan0031/some-problems-record/blob/master/jackson-replace-fastjson/src/test/java/com/zxl/problems/SerializationUseJacksonReplaceFastJsonTest.java)

## Annotation

## JSONObject & JSONArray

# 参考文档
* [Jackson快速替换Fastjson之道](https://blog.csdn.net/hujkay/article/details/97040048)
* [fastjson Features 说明](https://blog.csdn.net/xiaoliuliu2050/article/details/82356934)
* [fastjson SerializerFeatures 说明](https://blog.csdn.net/zjkyx888/article/details/78673898)
* [Jackson – Decide What Fields Get Serialized/Deserialized](https://www.baeldung.com/jackson-field-serializable-deserializable-or-not)