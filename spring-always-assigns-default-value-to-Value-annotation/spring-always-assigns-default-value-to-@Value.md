# reasons
工作中负责的一套计费系统需要开发一个新通知功能，在扣费等事件触发后发送MQ，然后消费MQ发送邮件或短信通知给客户。因为有多套环境，测试时需要知道是从哪套环境发出的邮件，又不想维护多套通知模板，因此就打算在各环境的properties
中声明不同的title
前缀，实现类似`[DEV]您的xx月账单`、`[TEST]您的xx月账单`的效果，但是这个前缀需要在生产环境中去掉，因此我想到用`Spring @Value`的默认值来实现，伪代码如下：

```java

@Value("${notice.mail.titlePrefix:}")
private String mailTitlePrefix;

public void doSendEmail() {
	...
	String title = "xxx";
	if (StringUtils.isNotBlank(mailTitlePrefix)) {
		title = mailTitlePrefix + title;
	}
	mailSender.send(title, content, recevier);
}

```

# problems
采用上述代码后，运行发现，即使在properties中配置了值，但是`mailTitlePrefix`一直是空字符串`""`，一旦把冒号去掉又能正常读取到配置的值，修改`:`后面的数据为其他值，如`@Value("${notice.mail.titlePrefix:113}")
`，`mailTitlePrefix`的值也为`113`，因此确认是Spring在注入属性的时候出现问题。一番google后，找到这样一篇文章 
[spring-boot-spring-always-assigns-default-value-to-property-despite-of-it-bein](https://stackoverflow.com/questions/28369582/spring-boot-spring-always-assigns-default-value-to-property-despite-of-it-bein)。
检查代码后发现，`applicationContext.xml`中的确声明了两个`property-placeholder`:

```xml
<context:property-placeholder order="0" location="classpath*:db.properties" ignore-unresolvable="true"/>
<context:property-placeholder order="0" location="classpath*:config.properties" ignore-unresolvable="true"/>
```
按照 [SPR-9989](https://jira.spring.io/browse/SPR-9989) 上的说明，Spring在有多个`property-placeholder`时会默认使用第一个来对default 
value进行处理，代码中的`db.properties`里并没有`notice.mail.titlePrefix`。

# solution
合并`property-placeholder`声明：

```xml
<context:property-placeholder order="0" location="classpath*:db.properties,classpath*:config.properties" ignore-unresolvable="true"/>
```

# append
这个bug一直是未修复状态，最新的`spring 5.1.2.RELEASE`版本也有这个问题，demo见 [spring-always-assigns-default-value-to-Value-annotation](https://github.com/zxl2014/some-problems-record/spring-always-assigns-default-value-to-Value-annotation)