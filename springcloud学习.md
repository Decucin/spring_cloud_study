# 服务注册

服务注册主要可以通过eureka或者nacos来完成，由于nacos是阿里开发的，因此在国内比较受欢迎。

## Eureka

### Server端

* 记录服务信息
* 心跳监控

### Client端

* 服务提供者：
  * 将自身信息注册到Eureka Server
  * 每隔30s向Eureka Server发送心跳
* 服务消费者
  * 根据服务名称拉取服务列表
  * 基于服务列表发起负载均衡，选择一个微服务后发起远程调用

### 整合过程

#### 搭建server端

1. 创建项目，导入maven依赖

   ````xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
   </dependency>
   ````

2. 编写配置文件application.yml

   ````yaml
   server:
     port: 10086
   
   spring:
     application:
       name: eurekaserver
   
   eureka:
     client:
       service-url:  # eureka地址信息
         defaultZone: http://127.0.0.1:10086
   ````

3. 开启eureka服务器模式：在主启动类上添加@EnableEurekaServer注解

#### 服务注册

1. 在之前的服务中添加依赖：

   ````xml
   <dependency>
        <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
   </dependency>
   ````

2. 更改yml文件

   ````yaml
   server:
     port: 8081
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/cloud_user?useSSL=false
       username: root
       password: Decucin123
       driver-class-name: com.mysql.cj.jdbc.Driver
     application:
       name: userservice
   #  shardingsphere:
   #    sharding:
   #      default-database-strategy:
   #      tables:
   #      discovery:
   #        cluster-name: HZ
   mybatis:
     type-aliases-package: com.decucin.user.pojo
     configuration:
       map-underscore-to-camel-case: true
   
   logging:
     level:
       com.decucin: debug
     pattern:
       dateformat: MM-dd HH:mm:ss:SSS
   
   
   eureka:
     client:
       service-url:  # eureka地址信息
         defaultZone: http://127.0.0.1:10086/eureka
   ````

#### IDEA多实例部署

在对应实例（service窗口）右键选择copy configuration ，修改端口号(添加VM配置-Dserverport:8082)即可。

#### 在order-service完成服务拉取

1. 在order-service中用服务名代替ip地址
2. 在restTemplete bean上加上@LoadBalanced注解

**eureka的负载均衡是通过ribbon实现的**

### ribbon负载均衡

1. 首先请求对应的服务路径: http://userservice/user/1，之后会被LoadBalancerInterceptor拦截器拦截
2. 在该拦截器中有一个RibbonLoadBalancerClient，该client会获取服务id(userservice)
3. 把该服务id交给DynamicServerLoadBalancer，该组件从eureka-server中拉取得到服务列表
4. IRule实现具体的负载均衡选择具体的某个服务地址
5. 修改url并发起请求

#### 各负载均衡策略汇总

|        负载均衡实现        |                             策略                             |
| :------------------------: | :----------------------------------------------------------: |
|         RandomRule         |                             随机                             |
|       RoundRobinRule       |                             轮询                             |
| AvailabilityFilteringRule  | 先过滤掉由于多次访问故障的服务，以及并发连接数超过阈值的服务，然后对剩下的服务按照轮询策略进行访问 |
|  WeightedResponseTimeRule  | 根据平均响应时间计算所有服务的权重，响 应时间越快服务权重就越大被选中的概率即 越高，如果服务刚启动时统计信息不足，则 使用RoundRobinRule策略，待统计信息足够会切换到该WeightedResponseTimeRule策略 |
|         RetryRule          | 先按照RoundRobinRule策略分发，如果分发到的服务不能访问，则在指定时间内进行重试，然后分发其他可用的服务 |
|     BestAvailableRule      | 先过滤掉由于多次访问故障的服务，然后选择一个并发量最小的服务 |
| ZoneAvoidanceRule （默认） | 综合判断服务节点所在区域的性能和服务节点的可用性，来决定选择哪个服务 |

#### 调整负载均衡策略的方法

1. 在主启动类中定义一个新的bean对象IRule，可选择其任意一种实现类	-->	默认全局配置。

2. 修改服务调用者(orderservice)的yml文件

   ````yaml
   userservice:
     ribbon:
       NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
   ````

   **这种方式是可以对于不同的服务启用不同的负载均衡策略的。**

#### 饥饿加载

Ribbon默认采用的加载方式是懒加载，即在第一次访问时才会去创建LoadBalanceClient，这导致第一次请求的时间很长。

饥饿加载则是在项目启动时就创建该client，降低第一次访问的耗时。

开启方式：在对应服务的配置文件中进行修改

````yaml
ribbon:
  eager-load:
    enabled: true # 开启指定加载
    clients:
      - userservice
````

## Nacos

### 安装启动

进入官网https://nacos.io，进入github下载release，之后进行相应版本的下载，下载完成之后解压并根据操作系统的不同执行不同的启动策略，mac安装运行出错可参考[博客](https://blog.csdn.net/lvhonglei1987/article/details/113913373?utm_medium=distribute.pc_aggpage_search_result.none-task-blog-2~aggregatepage~first_rank_ecpm_v1~rank_v31_ecpm-2-113913373.pc_agg_new_rank&utm_term=mac+nacos+运行&spm=1000.2123.3001.4430)，还有[这篇](https://blog.csdn.net/weixin_44607626/article/details/123466881?spm=1001.2101.3001.6661.1&utm_medium=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7ERate-1.pc_relevant_paycolumn_v3&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7ERate-1.pc_relevant_paycolumn_v3&utm_relevant_index=1)。

````cmd
sh startup.sh -m standalone
````

### 服务注册发现

1. 在父工程中添加spring-cloud-alibaba的管理依赖

   ````xml
   <dependency>
       <groupId>com.alibaba.cloud</groupId>
       <artifactId>spring-cloud-alibaba-dependencies</artifactId>
       <version>2.2.5.RELEASE</version>
       <type>pom</type>
       <scope>import</scope>
   </dependency>
   ````

   **注意这里是需要加载在dependencyManagement里面的。**

2. 添加对应的nacos-client依赖（若是之前有eureka，需要注释掉）。

   ````xml
   <!--    nacos依赖     -->
   <dependency>
       <groupId>com.alibaba.cloud</groupId>
       <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
   </dependency>
   ````

3. 修改对应的配置文件（若是之前有eureka，需要注释掉）

   ````yaml
   spring:
     cloud:
       nacos:
         server-addr: localhost:8848 # nacos服务端地址（实际上服务端就是之前安装的本机服务）
   ````

### 服务分级存储模型

服务->集群->实例

实际上就是配置实例的集群属性，直接修改配置文件，添加集群属性即可。

````yaml
cloud:
    nacos:
      server-addr: localhost:8848 # nacos服务端地址（实际上服务端就是之前安装的本机服务）
      discovery:
        cluster-name: HZ      # 集群名称
````

### NacosRule负载均衡

1. 首先需要将服务调用者的集群也进行配置

2. 参照ribbon修改负载均衡策略的方案，将对应的规则改为NacosRule

   ````yaml
   userservice:
     ribbon:
       NFLoadBalancerRuleClassName: com.netflix.loadbalancer.NacosRule
   ````

   nacosrule的策略，优先选择相应集群（若是当前集群无实例，那么会访问其它集群的实例，但控制台会提示Warn，说进行了跨集群访问），在同一个集群中的多个实例中随机进行选择。

#### 根据权重

使用情形：在不同设备（实例服务器）之间有性能差异时可以对不同的实例设置权重，那么权重越大被访问的频率越高。

直接在nacos控制台进行配置，权重在0-1之间（权重为0时表示直接不访问），权重为0的使用方式如下：若是需要进行服务更新，那么可以先将一台服务器的权重设置为0，之后对该服务器进行服务升级，升级之后增大权重进行测试（此时建议设一个较小的权重），若是该服务没有问题，那么依次对所有服务器进行升级，直到全部服务器都被升级完成。

### 环境隔离

1. 先在nacos控制台新增命名空间，命名空间名称和描述信息必须要填

2. 修改对应服务的配置文件，添加namespace，注意这里填的是id

   ````yaml
   nacos:
         server-addr: localhost:8848 # nacos服务端地址（实际上服务端就是之前安装的本机服务）
         discovery:
           cluster-name: KM
           namespace: 42968204-428b-449e-a9a5-cf2304791142
   ````

   注意在两个命名空间下的服务是无法相互访问的----->不同namespace下的服务不可见。

## Eureka和Nacos的对比

1. Nacos有临时实例的说法，临时实例（可以被关闭），采用心跳检测，若是发现没有心跳则会将该实例剔除，非临时实例则是nacos主动询问，若是发现当前实例已经宕机，则将其标记为不健康，不会从实例列表中剔除。
2. nacos实现了消息推送（服务变更更及时），eureka仅仅是不停的pull（服务消费者向eureka进行拉取），而nacos会把pull和push进行结合（nacos也会主动推送变更消息）
3. Nacos默认采用AP（强调可用性）模式，当有非临时实例时采用CP（强调可靠性）模式，Eureka采用AP模式

临时实例的配置

````yaml
cloud:
    nacos:
      server-addr: localhost:8848 # nacos服务端地址（实际上服务端就是之前安装的本机服务）
      discovery:
        cluster-name: KM
        namespace: 42968204-428b-449e-a9a5-cf2304791142
        ephemeral: false  # 是否是临时实例
````

## 配置管理

1. 这里的配置管理针对的其实是那些需要热更新的配置信息，直接在nacos控制台进行配置，之后发布即可，dataId一般的选择方式是服务名称-环境.yaml，如userservice-dev.yaml

2. 微服务进行配置拉取

   明确一点，bootstrap.yml的优先级比application.yml的高很多，因此考虑将nacos的配置信息放在bootstrap.yml文件中

   * 首先需要引入nacos的配置客户端依赖

     ````xml
     <!--    nacos配置管理依赖 -->
     <dependency>
         <groupId>com.alibaba.cloud</groupId>
         <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
     </dependency>
     ````

   * 在需要进行配置管理的服务中新建bootstrap.yml文件，并标识出相应内容

     ````yaml
     spring:
       application:
         name: userservice # dataId的一部分
       profiles:
         active: dev # 开发环境  # dataId的一部分
       cloud:
         nacos:
           server-addr: localhost:8848 # nacos服务端地址（实际上服务端就是之前安装的本机服务）
           config:
             file-extension: yaml
     ````

   * 在用到热更新的Controller上加上注解@RefreshScope     (使用的是@value获取配置信息)

   * 或者可以在一个专门用于获取配置的类里面加载并设置类为@ConfigurationProperties(prefix = "pattern")

     ````java
     @Data
     @Component
     @ConfigurationProperties(prefix = "pattern")
     public class PatternProperties {
     
         private String dateformat;
     
     }
     ````

     ````java
     @Autowired
         private PatternProperties patternProperties;
     
          @GetMapping("now")
          public String now(){
              return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDateformat()));
          }
     ````

### 配置共享（配置文件重用）

1. 在nocas控制台新建配置文件，使用对应的服务名作为多环境共享的配置文件名称，以yaml作为文件后缀。
2. 以获取配置文件的方式对公共配置文件中的值进行获取。

**注意这里的优先级问题：服务名-profile.yaml > 服务名.yaml > 本地配置；微服务会从nacos读取两个配置文件，一个是服务名-profile.yaml，一个是服务名.yaml**

## Nacos集群搭建

用户请求发送到Nginx，Ngix实现负载均衡并把请求分发到Nacos集群中的某个节点，Nacos集群访问的数据库是mysql的集群。

### 搭建集群

1. 搭建集群，初始化数据库表结构

2. 下载nacos

3. 配置

   * 将nacos/conf/cluster.conf.example文件重命名为cluster.conf

     在该配置文件中配置集群信息（ip地址以及端口号）

   * 修改nacos/conf/application.properties文件

     将关于mysql的那部分注释去掉，其中db.num表示的是有多少个mysql集群，修改数据库部分配置（用户名，密码等）

4. 启动

   这里直接启动即可，无需加-M 这一段（默认集群启动）

5. Nginx反向代理

   * 修改conf/nginx.conf文件并配置
   * 修改bootstrap.yml中的server-addr为localhost:80（看nginx怎么配的）