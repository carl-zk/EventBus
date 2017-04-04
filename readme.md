> 这是个publish/subscribe框架,借鉴了spring自带的和[EventBus 源码解析](http://a.codekk.com/detail/Android/Trinea/EventBus%20%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90)


## EventBus tutorial
1.
在spring项目pom.xml中引入
```xml
<dependency>
    <groupId>moc.oreh</groupId>
    <artifactId>event-bus</artifactId>
    <version>1.2</version>
</dependency>
```
在applicationContext.xml文件中加入EventBus bean
```xml
<bean id="springEventBus" class="moc.oreh.eventbus.spring.SpringEventBus" destroy-method="destroy">
    <constructor-arg name="corePoolSize" value="8"/>
    <constructor-arg name="maximumPoolSize" value="32"/>
    <constructor-arg name="keepAliveTime" value="300"/>
</bean>
```

2.
新建xxxEvent和xxxEventHandler
```java
public class OnlineEvent {
    private User user;

    public OnlineEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}

@Component
public class OnlineEventHandler {

    @Transactional
    @Subscribe
    public void fun(OnlineEvent e) {
        System.out.println("follow you");
        userDao.save(new User("jj", 23));
        try {
            int t = 9;
            t /= 0;
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }

    @Transactional(timeout=1)
    @Subscribe(mode = SubscribeMode.SYNC)
    public void handle(OnlineEvent event) throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        System.out.print("handle event: " + event.getClass().getName() + " ---> ");
        System.out.println(event.getUser());
    }
}
```

3.
通过EventBus发布事件,带有Subscribe标签的EventHandler会同步/异步来处理这个事件.
```java
@Controller
public class MyController {
    @Autowired
    private EventBus eventBus;

    @Transactional
    @RequestMapping("/online")
    public User online() {
        OnlineEvent onlineEvent = new OnlineEvent(new User("小红", 17));
        eventBus.publish(onlineEvent);
        return new User("xfdosa", 3);
    }
}
```

[演示代码](https://github.com/carl-zk/JavaJava/tree/master/SpringEventBus)


## 功能
1. 订阅方式
主要有3中订阅方式:
```java
public enum SubscribeMode {
    FOLLOW,    // publisher直接调subscriber
    SYNC,      // 交给一个单独线程
    ASYNC      // 交给一个线程池
}
```
Follow 是在当前线程中顺序执行.
SYNC 是在另一个单独的线程中执行,所有SYNC订阅事件都会等待在这个线程的等待队列上等待执行.
ASYNC 默认订阅模式,发布的事件由一个线程池去执行.此模式在项目中经常遇到.

要想用好EventBus,先要清楚spring事务的传播特性.

FOLLOW模式下,publisher所处的事务上下文传递给FOLLOW注释的方法.若publisher的@Transactional没有时间限制,
那么即使FOLLOW注释的方法有@Transactional(timeout=1)也不会超时.

SYNC和ASYNC模式下,timeout生效.因为是一个单独的新事务.

在SYNC和ASYNC的方法内操作数据库,必须在该方法上添加@Transactional注释.否则如果出现异常,dao操作不会回滚,
并且,还容易出现数据库内存泄漏.

@Subscribe注解很自由,你可以对同一个Event采用不同的监听模式.
举个例子,如果员工a要辞职,只需他项目经理PM同意后就可以走,而部门经理boss并不会立即过问,但是a辞职的事必须告诉boss.
所以,对辞职事件的监听,PM监听的模式是FOLLOW,boss监听的模式是ASYNC/SYNC.
当a要走,发出辞职事件Event,PM会立即处理,a这时在等PM的处理结果.等PM处理完成,Event所在的事务commit了,这个时候
才会通知boss.

由此可见,通过配置不同的@Subscribe监听模式,让事件处理变得非常灵活.


2. 优先级
支持优先级,priority越小排序越靠前.默认=0.


## Spring中的事务
**参考文章**
[Spring 事务管理高级应用难点剖析: 第 3 部分](https://www.ibm.com/developerworks/cn/java/j-lo-spring-ts3/)这篇文章的1,2,3部分都很精彩.
[http://developer.51cto.com/art/200902/109303_all.htm](http://developer.51cto.com/art/200902/109303_all.htm)

1.
在一个事务中发布一个事件,必须等该事务commit之后事件才能发布.
为什么?
举个场景:如果user要注销账户,可能会发出注销账户Event,如果不等该user账户注销事务commit就直接通知监听者,
例如会员信息清除Subscriber,该Subscriber就直接将该user的会员信息清除了. 结果user账户注销事务中断,user
并没成功注销,但是这个时候该user的会员信息已经删了.
可能这个例子有漏洞,但是足以说明问题了.

2.
@Transactional标签会使bean初始化时自动交给代理类,代理类后缀一般都是$$EnhancerBySpringCGLIB$$xxxxxx,例如
service.myeventbus.OnlineEventHandler$$EnhancerBySpringCGLIB$$eb2b402d
代理就是对bean的AOP增强.然后使用DaoUtils工具类也不必担心有内存泄漏.

一个方法上有@Transactional标签不代表一定有事务上下文.它只会作用于方法开始到最后一个数据库操作完成.如果方法中没有
对数据库操作,那么该方法的事务注解不生效.

@Transactional标签并不一定作用到方法结束,它会在最后一个对数据库操作结束后立刻commit然后结束事务.例
```java
@Transactional(timeout = 1)
public void handle(OnlineEvent event) throws InterruptedException {
    TimeUnit.SECONDS.sleep(3);  // timeout    1
    daoService.getUser("1");
    // TimeUnit.SECONDS.sleep(3);  // 不会timeout   2
}
```
1处的会超时,而2处不会超时.


## Spring AOP代理
[http://jinnianshilongnian.iteye.com/blog/1613222](http://jinnianshilongnian.iteye.com/blog/1613222)
[http://jinnianshilongnian.iteye.com/blog/1894465](http://jinnianshilongnian.iteye.com/blog/1894465)
[Spring AOP 实现原理与 CGLIB 应用](https://www.ibm.com/developerworks/cn/java/j-lo-springaopcglib/)


**[欢迎大家使用和完善EventBus]**

spring不会对引入的类有BeanPostProcessor方法postProcessAfterInitialization处理.