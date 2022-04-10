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

# Http客户端Feigh

RestTemplete方式是在请求中将URL作为变量传入，但若是遇到较为复杂的路径，那么维护起来就会比较困难。

## 定义和使用Feign

1. 引入依赖

   ````xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-openfeign</artifactId>
   </dependency>
   ````

2. 在主启动类中通过@EnableFeignClients开启feign的功能。

3. 编写feign客户端：需要包含的信息有服务名称、请求方式、请求路径、请求参数以及返回值类型

## Feign的自定义配置

|        类型         |       作用       |                          说明                          |
| :-----------------: | :--------------: | :----------------------------------------------------: |
| feign.logger.level  |   修改日志级别   |            None(默认), Basic, Headers, Full            |
| feign.codec.Decoder | 响应结果的解析器 |   http远程调用的结果做解析，例如解析json字符串为对象   |
| feign.codec.Encoder |   请求参数编码   |          将请求参数编码，便于通过http请求发送          |
|   feign.Contract    |  支持的注解格式  |                 默认是SpringMVC的注解                  |
|    feign.Retryer    |   失败重试机制   | 请求的失败重试机制，默认是没有，不过会使用Ribbon的重试 |

### 修改日志界别的方式

1. 配置文件中修改

   * 全局生效

     ````yaml
     feign:
       client:
         config: 
           default:  # default表示全局
             loggerLevel: FULL
     ````

   * 局部生效

     ````yaml
     feign:
       client:
         config: 
           userservice :  # 针对某个服务
             loggerLevel: FULL
     ````

2. 使用Java代码实现

   1. 先声明一个bean

      ````java
      public class DefaultFeignConfiguration {
      
          @Bean
          public Logger.Level logLevel(){
              return Logger.Level.BASIC;
          }
      
      }
      ````

   2. 如果是全局配置，把它配置到@EnableFeignClients之中，这个注解是在主启动类上

      ````java
      @EnableFeignClients(defaultConfiguration = DefaultFeignConfiguration.class)
      ````

      如果是局部配置，把它配置到@FeignClient上

      ````java
      @FeignClient(value = "userservice", configuration = DefaultFeignConfiguration.class)
      ````

## Feign的性能优化

Fiegn底层客户端实现：

* URLConnection：默认实现，不支持连接池
* Apache HttpClient：支持连接池
* OKHttp：支持连接池

Feign的性能优化主要包括以下几个方面：

1. 使用连接池代替URLConnection
2. 日志最好使用Basic或者None

### 使用连接池代替URLConnection（HttpClient为例）

1. 引入依赖

   ````xml
   <dependency>
       <groupId>io.github.openfeign</groupId>
       <artifactId>feign-httpclient</artifactId>
   </dependency>
   ````

2. 配置连接池

   ````yaml
   feign:
     httpclient:
       enabled: true # 开启
       max-connections: 200
       max-connections-per-route: 50
   ````

## Feign最佳实践

1. 继承：给消费者的Client和提供者的Controller定义统一的父接口作为标准	--->	耦合度过高，此外方法参数无法继承
2. 抽取：将FeighClient单独抽取为独立模块，并把接口相关的POJO，默认的Feign配置都放到这里，提供给所有消费者使用。

### 抽取FeignClient

1. 创建module，命名为feign-api并引入Feign的starter依赖
2. 将orderservice中编写的UserClient，User，DefaultConfiguration复制到其中
3. orderservice中引入依赖
4. 修改order-service的代码，将其改为feign-api中的部分

这里会出现问题：当FeignClient不在扫描包范围内时，这些FeignClient无法使用，有两种方式解决：

1. 指定FeignClient所在的包

   ```java
   @EnableFeignClients(defaultConfiguration = DefaultFeignConfiguration.class, basePackages = "com.decucin.feign.clients")
   ```

2. 指定FeignClient字节码文件

   ```java
   @EnableFeignClients(defaultConfiguration = DefaultFeignConfiguration.class, clients = UserClient.class)
   ```

# 统一网关Gateway

网关的必要性：直接将服务队外暴露有很多安全隐患，比如说有的服务只允许内部调用。

网关的作用：

* 身份认证和权限校验
* 服务路由、负载均衡
* 请求限流

springcloud网关的选择：

* Gateway：基于Webflux实现，属于响应式编程
* Zuul：基于servlet实现，属于阻塞式编程

### 基于Gateway的实现

1. 创建相应module，引入Gateway和nacos发现依赖

   ```xml
   <dependencies>
   
       <!--    nacos服务发现依赖 -->
       <dependency>
           <groupId>com.alibaba.cloud</groupId>
           <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
       </dependency>
   
   		<!--	gateway依赖	-->
       <dependency>
           <groupId>org.springframework.cloud</groupId>
           <artifactId>spring-cloud-starter-gateway</artifactId>
       </dependency>
   </dependencies>
   ```

2. 编写路由配置及nacos地址

   ````yaml
   server:
     port: 10010
   
   spring:
     application:
       name: gateway
     cloud:
       nacos:
         server-addr: 8848
       gateway:
         routes:
           - id: user-service # 自定义的唯一标识，必须唯一
             uri: lb://userservice # 路由的目标地址，也可使用http://localhost:8081写死，但不推荐
             predicates:   # 判断请求是否符合规则
               - Path=/user/** # 路径断言，判断是否以/user开头，如果是则符合
           - id: order-service
             uri: lb://orderservice
             predicates:
               - Path=/order/**
             # 这里还可以对路由过滤器进行配置
   ````

请求过程解析：

1. 用户请求网关
2. 网关根据路由规则判断选择路由
3. 网关从nacos拉取服务列表
4. 负载均衡发送请求

#### 路由断言及断言工厂

配置对断言会被断言工厂解析并处理，转换为路由判断的条件。

Spring中基本的断言工厂：

|    名称    |           说明           |                             实例                             |
| :--------: | :----------------------: | :----------------------------------------------------------: |
|   After    |  是某个时间点之后的请求  |    \- After=2017-01-20T17:42:47.789-07:00[America/Denver]    |
|   Before   |  是某个时间点之前的请求  |   \- Before=2017-01-20T17:42:47.789-07:00[America/Denver]    |
|  Between   | 是某两个时间点之间的请求 | \- Between=2017-01-20T17:42:47.789-07:00[America/Denver], 2017-01-21T17:42:47.789-07:00[America/Denver] |
|   Cookie   |  请求必须包含某些cookie  |                  \- Cookie=chocolate, ch.p                   |
|   Header   |  请求必须包含某些header  |                 \- Header=X-Request-Id, \d+                  |
|    Host    |  请求必须是访问某些host  |        \- Host=** .somehost.org, ** .anotherhost.org         |
|   Method   |    请求必须是指定方式    |                      \- Method=GET,POST                      |
|    Path    | 请求路径必须符合指定规则 |            \- Path=/red/{segment},/blue/{segment}            |
|   Query    | 请求参数必须包含指定参数 |                        \- Query=green                        |
| RemoteAddr | 请求者的ip必须是指定范围 |                 \- RemoteAddr=192.168.1.1/24                 |
|   Weight   |         权重处理         |                     \- Weight=group1, 2                      |

#### 路由过滤器及过滤器工厂

对进入网关的请求和微服务返回的响应做处理(spring提供了三十多种)。

* 默认过滤器：对所有路由都生效

* 局部过滤器：只对局部请求有效

  ````xml
  cloud:
      nacos:
        server-addr: localhost:8848
      gateway:
        routes:
          - id: user-service # 自定义的唯一标识，必须唯一
            uri: lb://userservice # 路由的目标地址，也可使用http://localhost:8081写死，但不推荐
            predicates:   # 判断请求是否符合规则
              - Path=/user/** # 路径断言，判断是否以/user开头，如果是则符合
  #          filters: # 过滤器
  #            - AddRequestHeader=Truth,Decucin is awesome!  # 添加请求头
          - id: order-service
            uri: lb://orderservice
            predicates:
              - Path=/order/**
  #            - After=2031-01-20T17:42:47.789-07:00[America/Denver]
        default-filters: # 注意这里和routes一级
          - AddRequestHeader=Truth,Decucin is awesome!  # 添加请求头
  ````

* 全局过滤器：与默认过滤器类似，但与前两者不同的是可以实现自定义逻辑操作 ---->实现GlobalFilter接口

  ````java
  //@Order(-1)
  @Component
  public class AuthorizeFilter implements GlobalFilter, Ordered {
      @Override
      public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
          //  1.获取请求参数
          ServerHttpRequest request = exchange.getRequest();
          MultiValueMap<String, String> params = request.getQueryParams();
          //  2.获取参数中的对应字段
          String auth = params.getFirst("authorization");
  
          //  3.判断该字段是否合理，合理就放行
          if("admin".equals(auth)){
              return chain.filter(exchange);
          }
  
          //  4.不合理就拦截，并设置状态码
          exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
          return exchange.getResponse().setComplete();
      }
  
      @Override
      public int getOrder() {
          return -1;
      }
  }
  ````

过滤器执行顺序：

默认过滤器和局部过滤器的order值是spring指定的，按照配置文件中的声明顺序从1开始递增，如果order值相同，那么默认过滤器>路由过滤器（局部过滤器）>全局过滤器。

### 网关跨域问题处理

跨域：域名不同、域名相同端口不同。浏览器禁止请求的发起者与服务端发生跨域ajax请求，该请求会被浏览器拦截。

解决方案：CORS，网关也是类似，只需要进行相应配置即可。

````yaml
default-filters: # 注意这里和routes一级
        - AddRequestHeader=Truth,Decucin is awesome!  # 添加请求头
      globalcors:
        add-to-simple-url-handler-mapping: true # 防止option请求被拦截
        cors-configurations:
          '[/**]':
            allowedOrigins: # 允许哪些网站的跨域请求
              - "http://localhost:8090"
              - "http://www.leyou.com"
            allowedMethods:
              - "GET"
              - "POST"
              - "DELETE"
              - "PUT"
              - "OPTIONS"
            allowedHeaders: "*" # 允许在请求中携带的头信息
            allowCredentials: true  # 是否允许携带cookie
            maxAge: 360000  # 本次跨域的有效期
````

