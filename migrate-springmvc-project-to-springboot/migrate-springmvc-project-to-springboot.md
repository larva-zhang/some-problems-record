# 背景

作为Java后端开发人员，Spring必定是开发者最常用的框架。基于底层的ioc和aop机制衍生了一系列的扩展框架：spring-orm、spring-data家族、spring-cloud微服务全家桶等等。spring-boot
在spring生态圈中的定位是辅助快速开发，同时也是spring-cloud框架的基石。很多企业在往微服务的转型过程中会选择spring-cloud，而要迁移至spring-cloud就必须先迁移到spring-boot。

# 原始spring-mvc工程

原始的spring-mvc工程是一个`spring-mvc + mybatis + mysql`的典型web工程，为了能更好的切合实际工作当中的情况（企业倾向稳定性因此框架不太会是最新版本，参考了我公司里的工程），对工程中使用的部分框架做了版本限制：

1. spring版本为4.3.21.RELEASE，相关上层框架也调整到相应的版本
2. mybatis版本为3.2.8，mybatis-spring版本为1.2.3
3. hibernate-validator版本为5.4.2.Final
4. servlet-api版本为3.0
5. lombok版本为1.6.16

# 迁移

## Step 1. 用spring-boot-starters替换spring相关依赖项

spring-boot定义了一系列的starter，每个starter都有明确的目标及命名，并且都定义了相关传递依赖以及其版本，使用时只需引入需要的starter即可，而不需要所有的依赖都声明一遍，包括特殊的自动配置依赖。例如`mybatis-spring-boot-starter`的`pom.xml`声明如下：
```xml
<parent>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot</artifactId>
    <version>1.3.2</version>
  </parent>
  <artifactId>mybatis-spring-boot-starter</artifactId>
  <name>mybatis-spring-boot-starter</name>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mybatis</groupId>
      <artifactId>mybatis</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mybatis</groupId>
      <artifactId>mybatis-spring</artifactId>
    </dependency>
  </dependencies>
```
一个starter就可以替代`spring-orm`、`mybaits`以及`mybatis-spring`，并且还添加了`mybatis-spring-boot-autoconfigure`。

这一步的主要目的是利用starters避免依赖地狱，同时也是为了获得各种autoconfigure支持，具体要使用哪些starter替换依赖，视你的工程需求而定，甚至你也可以自定义一个starter来替换所有依赖。

### 使用的starter

+ `mybatis-spring-boot-starter`替换`spring-orm`、`mybaits`、`mybatis-spring`
+ `spring-boot-starter-aop`替换`spring-aspects`、`spring-aop`
+ `spring-boot-starter-data-redis`替换`spring-data-redis`
   
   + 因为`spring-boot-starter-data-redis`默认使用`lettuce`作为redis客户端，所以并不能替换`jedis`依赖
+ ``

## Step 2. 用Java Annotation Config配置Spring容器



## Step 3. 用Java Annotation Config配置Servlet容器

## Step 4. 用spring-boot启动类配置Servlet容器

## Step 5. 切换成jar的打包方式

# 问题

## 使用`spring-boot-starter-parent`后导致maven打包未替换resource变量

## 使用`mybatis-spring-boot-stater`替换`mybatis`以及`mybatis-spring`依赖后启动报错`org.apache.ibatis.binding.BindingException: Parameter '0' not found. Available parameters are [arg1, arg0, param1, param2]`

## 使用`spring-boot-stater-validation`替换`hibernate-validator`依赖以`tomcat7-maven-plugin`启动报错`java.lang.NoClassDefFoundError: javax/el/ELManager`

## 使用`lombok 1.8.10`后，jackson反序列化报错`com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Cannot construct instance of PracticeResults (no Creators, like default construct, exist): cannot deserialize from Object value (no delegate- or property-based Creator)`

## 使用spring-boot-starter-web后报错`Cannot forward to error page for request [/wechat/portal] as the response has already been commit`

## 使用`@EnableAutoConfiguration`后启动报错`Failed to instantiate [org.springframework.web.servlet.HandlerMapping]: Factory method 'resourceHandlerMapping' threw exception; nested exception is java.lang.IllegalStateException: No ServletContext set`

## 使用`spring-boot-devtools`后启动报错`java.lang.LinkageError: loader constraint violation: loader (instance of org/springframework/boot/devtools/restart/classloader/RestartClassLoader) previously initiated loading for a different type`

## 使用jar方式打包后，启动时对依赖的类报`java.lang.ClassNotFound`异常

# 参考

