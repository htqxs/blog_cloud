+ blog_admin：提供admin端API接口服务；


+ blog_oss： 图片储存服务，使用阿里云OSS进行图片上传；

- blog_sms：消息服务，邮件发送；
- blog_gateway：网关服务；
- blog_common：公共模块，存放Entity实体类、Feign远程调用接口、公共config配置；
- blog_system：存放service、dao、dto、vo；
- blog_auth：权限模块。

**Nacos中的配置：**

**blog-admin-dev.yaml**

```yaml
#配置端口
server:
  port: 8061

#配置mysql数据库
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/blog?useSSL=false&serverTimezone=GMT%2B8
    username: root
    password: 94264944htz
    hikari:
      minimum-idle: 5
      # 空闲连接存活最大时间，默认600000（10分钟）
      idle-timeout: 180000
      # 连接池最大连接数，默认是10
      maximum-pool-size: 10
      # 此属性控制从池返回的连接的默认自动提交行为,默认值：true
      auto-commit: true
      # 连接池名称
      pool-name: MyHikariCP
      # 此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认1800000即30分钟
      max-lifetime: 1800000
      # 数据库连接超时时间,默认30秒，即30000
      connection-timeout: 30000
      connection-test-query: SELECT 1
  #redis配置
  redis:
    host: localhost
    port: 6379
    password: 94264944htz
  #图片大小限制
  servlet:
    multipart:
      max-file-size: 40MB
      max-request-size: 100MB

#配置MybatisPlus
mybatis-plus:
  mapper-locations: classpath*:mapper/*.xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

#第三方配置信息
qq:
  app-id: "101948161"
  user-info-url: "https://graph.qq.com/user/get_user_info?openid={openid}&access_token={access_token}&oauth_consumer_key={oauth_consumer_key}"
```

**blog-oss-dev.yaml**

```yaml
server:
  port: 8062
aliyun:
  url: ***
  endpoint: ***
  accessKeyId: ***
  accessKeySecret: ***
  bucketName: ***
```

**blog-gateway-dev.yaml**

```yaml
server:
  port: 8080
spring:
  profiles:
    active: dev
  application:
    name: blog-gateway
  cloud:
    gateway:
      globalcors:
        # gateway 跨域设置
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedHeaders: "*"
            allowCredentials: true
            allowedMethods:
              - GET
              - POST
              - PUT
              - OPTIONS
      # 设置与服务注册发现组件结合，这样可以采用服务名的路由策略
      discovery:
        locator:
          enabled: true
      # 配置路由规则
      routes:
        - id: blog_admin
          # 采用 LoadBalanceClient 方式请求，以 lb:// 开头，后面的是注册在 Nacos 上的服务名
          uri: lb://blog-admin
          # Predicate 翻译过来是“谓词”的意思，必须，主要作用是匹配用户的请求，有很多种用法
          predicates:
            # 路径匹配，以 admin 开头
            - Path=/admin/**,/login
        - id: blog_oss
          uri: lb://blog-oss
          predicates:
            - Path=/upload/**
```

**blog-sms-dev.yaml**

```yaml
server:
  port: 8063
spring:  
  #mq配置
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  #邮箱配置
  mail:
    host: smtp.qq.com
    # 邮箱号
    username: 1693740714@qq.com
    # 邮箱授权码
    password: qcahpeenlktscjdi
    default-encoding: UTF-8
    port: 587
    properties:
      mail:
      smtp:
      auth: true
      socketFactory:
      class: javax.net.ssl.SSLSocketFactory
```

