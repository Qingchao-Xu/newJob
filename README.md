# newJob

newJob就业信息分享平台，旨在为高校学生与招聘公司提供更便利的信息分享服务。

**技术栈：Spring Boot、Redis、Kafka、ElasticSearch、SpringSecurity、Caffeine**

## 项目简介


## 开发记录

### 平台首页

1）首页显示10条招聘帖子

1. 创建帖子实体类，用户实体类；
2. 开发Dao层，从数据库中分页查询帖子（按照类型和创建时间降序），从数据库中查询帖子总数，
根据userId从数据库查询用户信息； 
3. 开发Service层，调用Dao层，分页查询帖子，查询帖子总数，根据userId查询用户信息；
4. 开发Controller层，添加首页请求方法（GET），查询前10条帖子，
将帖子和对应的用户放到List中（List存储的Map，一个Map中包含一个帖子、一个用户），
将List放到Model中；
5. 使用thymeleaf模板展示首页数据。

2）分页组件，分页显示所有的招聘帖子

1. 封装一个Page实体类，包含当前页码、每页上限、总行数、请求路径。根据以上属性，
Page类能够计算当前页的起始行，总的页数回前端能够显示的起始页和结束页码。
当前页码和每页上限由前端传递。总行数和请求路径在Controller中设置。
2. 在首页请求方法中设置Page的总页数，和访问路径。
3. 按照Page的 起始行 和 每页上限 查询帖子。
4. 将Page对象页放到Model中，返回给前端。
5. 使用thymeleaf模板处理展示分页信息。

### 登录模块

1）注册功能

1. 开发 Dao 层，对用户表的增删改查。
2. 发送邮件工具
   - 添加 Spring Mail 依赖；
   - 配置 spring.mail 邮箱参数：host、port、username、password、protocol（协议）、
     ssl.enable（采用ssl发送邮件）
   - 创建发送邮件的工具类MailClient，构建MimeMessage，使用JavaMailSender发送邮件
3. 访问注册页面。Controller 层添加请求方法，返回注册页面的 thymeleaf 模板；
4. Service 层添加注册方法。
   - 空值处理，判断用户名、密码、邮箱是否为空。
   - 验证用户名、邮箱是否已存在。 
   - 补充用户其它属性：随机生成用户的 salt（盐，通过UUID生成，长度为5）。对用户密码使用 salt 进行 md5 加密。
   设置用户类型为 0 （普通用户），用户状态为 0（未激活），生成用户激活吗（通过UUID），用户头像，创建时间。
   - 将用户插入数据库中。
   - 使用模板引擎创建 HTML 邮件内容。通过邮件工具发送激活邮件。
   - 返回 Map 类型, 如果 Map 为空代表没有问题，如果 Map 不为空，则会携带相应的错误信息。
5. Controller 层添加请求方法，处理前端提交的注册数据。
   - 调用Service层注册方法进行注册。如果返回空的 Map 说明注册成功，跳转到首页。
   - 否则，说明注册失败，返回 Map 中的错误信息给前端。
6. 激活注册账号。
   - Service 层添加激活方法。如果已经激活，该方法返回表示重复激活的常量。如果激活码不正确
该方法返回表示激活失败的常量。否则，修改用户状态，返回激活成功的常量。
   - Controller 层添加请求方法，调用 Service 层激活方法处理激活请求。激活成功跳转到登录页面，
重复激活或激活失败跳转到首页。

2）登录、退出功能

1. 访问登录页面。Controller 层添加请求方法，返回登录页面的 thymeleaf 模板；
2. 生成验证码
   - 添加 Kaptcha 依赖
   - 新建配置类，声明一个 Bean，将 Kaptcha 核心接口 Producer 的实现类交给 Spring 容器管理。
   实例化 Producer 的实现类 DefaultKaptcha，并配置其高度、宽度、字体等配置。
   - Controller 层添加请求方法，返回一个验证码图片。使用 Producer 生成一个验证码，将验证码存入 Session
   将图片通过 HttpServletResponse 返回给前端。


### 使用AOP记录日志

## 未来优化


## 运行项目
### 数据库
在命令行启动MySQL之后使用如下命令：

1、创建并使用数据库
```sql
create database newjob;
use newjob;
```
2、通过sql文件建表
```sql
source path/init_schema.sql;
```
3、插入测试数据
```sql
source path/init_data.sql;
```

项目配置文件`application.properties`中的MySQL用户名和密码改为自己的
```properties
spring.datasource.username=用户名
spring.datasource.password=密码
```

### 邮箱
项目需要一个邮箱来发送注册激活邮件

在个人邮箱 -> 客户端设置 -> 开启POP/SMTP服务

项目配置文件`application.properties`中的mail配置改为自己的
```properties
spring.mail.host=邮箱smtp服务器地址（新浪的是：smtp.163.com 163的是：smtp.163.com qq邮箱：smtp.qq.com）
spring.mail.port=邮箱smtp端口（大部分邮箱的端口都是465）
spring.mail.username=邮箱名称
spring.mail.password=邮箱密码
```