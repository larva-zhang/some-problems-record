# 背景
公司线上一个运行了很久的定时任务突然报警，报警日志如下：

```java
java.lang.NullPointerException: null
        at java.util.HashMap.merge(HashMap.java:1225)
        at java.util.stream.Collectors.lambda$toMap$58(Collectors.java:1320)
        at java.util.stream.ReduceOps$3ReducingSink.accept(ReduceOps.java:169)
        at java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1380)
        at java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:481)
        at java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:471)
        at java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:708)
        at java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
        at java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:499)
        at com.xxx.web.controller.TaskController.getAccidUserIdMap(TaskController.java:648)

```

对应出错的代码：

```java
accidUserIdMap = administratorList.stream().collect(Collectors.toMap(Administrator::getAccid,
                    administrator -> bridgeIdUserIdMap.get(administrator.getBridgeId())));
```

# 解决过程

首先想到的是半年前将jdk版本升级到1
.8后，ide提示有优化，就一键将原来的Guava库的Function写法转成lambda表达式，想着是否是这里的一键转换出了问题（肯定不是我的问题:)）。翻出git 
log一看不是相关代码出现的问题，出问题的代码一直都是用lambda表达式书写的。这就奇怪了，因为``Administrator::getAccid``可以确定不会返回``null``，且administratorList
中也不包含``null``元素，能想到的就只有``bridgeIdUserIdMap.get(administrator.getBridgeId())``返回了``null``，但在我的印象中，HashMap是允许一个 null 
key以及多个null value的，查看openjdk的HashMap源码，看到javadoc也是如此说明的：

```text
Hash table based implementation of the Map interface. This implementation provides all of the optional map operations, and permits null values and the null key. (The HashMap class is roughly equivalent to Hashtable, except that it is unsynchronized and permits nulls.) This class makes no guarantees as to the order of the map; in particular, it does not guarantee that the order will remain constant over time.
```

那究竟是哪里出了问题呢，继续看HashMap的put方法源码：
```java
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
```
原来jdk8对HashMap的put方法做了修改，不允许直接通过put方法设置 null key or null value，如果需要设置 null key or value 需要使用 putIfAbsent方法。
