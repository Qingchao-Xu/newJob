# newJob

newJob就业信息分享平台，旨在为高校学生与招聘公司提供更便利的信息分享服务。

**技术栈：Spring Boot、Redis、Kafka、ElasticSearch、SpringSecurity、Caffeine**

## 项目简介


## 开发记录

### 平台首页

**1）首页显示10条招聘帖子**

1. 创建帖子实体类，用户实体类；
2. 开发Dao层，从数据库中分页查询帖子（按照类型和创建时间降序），从数据库中查询帖子总数，
根据userId从数据库查询用户信息； 
3. 开发Service层，调用Dao层，分页查询帖子，查询帖子总数，根据userId查询用户信息；
4. 开发Controller层，添加首页请求方法（GET），查询前10条帖子，
将帖子和对应的用户放到List中（List存储的Map，一个Map中包含一个帖子、一个用户），
将List放到Model中；
5. 使用thymeleaf模板展示首页数据。

**2）分页组件，分页显示所有的招聘帖子**

1. 封装一个Page实体类，包含当前页码、每页上限、总行数、请求路径。根据以上属性，
Page类能够计算当前页的起始行，总的页数回前端能够显示的起始页和结束页码。
当前页码和每页上限由前端传递。总行数和请求路径在Controller中设置。
2. 在首页请求方法中设置Page的总页数，和访问路径。
3. 按照Page的 起始行 和 每页上限 查询帖子。
4. 将Page对象页放到Model中，返回给前端。
5. 使用 thymeleaf 模板处理展示分页信息。

### 登录模块

**1）注册功能**

1. 开发 Dao 层，对用户表的增改查。
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
   - 否则，说明注册失败，返回 Map 中的错误信息给前端。thymeleaf 模板展示错误信息。
6. 激活注册账号。
   - Service 层添加激活方法。如果已经激活，该方法返回表示重复激活的常量。如果激活码不正确，
该方法返回表示激活失败的常量。否则，修改用户状态，返回激活成功的常量。
   - Controller 层添加请求方法，调用 Service 层激活方法处理激活请求。激活成功跳转到登录页面，
重复激活或激活失败跳转到首页。

**2）登录、退出功能**

1. 访问登录页面。Controller 层添加请求方法，返回登录页面的 thymeleaf 模板；
2. 生成验证码
   - 添加 Kaptcha 依赖
   - 新建配置类，声明一个 Bean，将 Kaptcha 核心接口 Producer 的实现类交给 Spring 容器管理。
   实例化 Producer 的实现类 DefaultKaptcha，并配置其高度、宽度、字体等配置。
   - Controller 层添加请求方法，返回一个验证码图片。使用 Producer 生成一个验证码，将验证码存入 Session
   将图片通过 HttpServletResponse 返回给前端。
3. 登录
   - 创建登录凭证实体类。
   - Dao 层添加对登录凭证的 增改查。
   - Service 层添加登录方法，返回 Map。判断用户名、密码不为空，用户名存在，用户已激活，密码正确。
登录不成功，返回存储失败原因的 Map。登录成功，生成登录凭证（UUID），存储到数据库中，返回存储凭证的 Map。
   - Controller 层添加请求方法，接收前端的用户名、密码、验证码、是否长期登录。从session中取出验证码
，判断验证码是否正确，不正确返回错误信息，跳转到登录页面。调用 Service 层登录方法，登录成功，将凭证放到cookie中发送给前端，重定向到首页。
登录失败，返回错误信息，回到登录页面，thymeleaf 模板展示错误信息。
4. 退出
   - Service 层添加退出方法，传入凭证。调用 Dao 层修改凭证的状态为 1（失效）。
   - Controller 层添加请求方法，从 Cookie 中获取凭证，调用 Service 层退出方法，重定向到登录页面。


**3）显示登录信息**

由于每一个请求都要判断是否登录，所以使用 Spring 拦截器实现。

1. 新建拦截器，重写 preHandle 方法。
   - 通过 HttpServletRequest 获取 Cookie 中的登录凭证。
   - 如果凭证不为空，并且凭证有效、没有过期，则根据凭证查询对应的用户。
   - 需要在本次请求中持有用户对象。**每次请求访问服务器，服务器会创建一个单独的线程。**
所以应该将用户对象放到 ThreadLocal 中，封装一个工具类来实现对 ThreadLocal 的存取和删除。
   - 调用工具类，将用户对象存放到 ThreadLocal 中。（为什么不放到 Session 中呢？）
2. 重写拦截器 postHandle 方法，从 ThreadLocal 获取用户对象，放到 Model 中。（postHandle 会在模板引擎之前调用）
3. 重写拦截器 afterCompletion 方法，从 ThreadLocal 中删除用户对象。（postHandle 会在请求结束时调用）
4. 在配置类中注册拦截器，过滤静态资源的请求。
5. thymeleaf 模板显示登录信息。

**4）账号设置**

设置头像
1. 需要可以访问设置页面。Controller 层添加请求方法，返回设置页面的 thymeleaf 模板。
2. 配置文件中配置上传文件存放的路径。
3. Service 层添加修改用户头像路径的方法。
4. Controller 层添加请求方法，处理上传的文件，通过 MultipartFile 来接收文件。
   - 判断文件不为空，文件格式正确。
   - 通过 UUID 生成一个随机的文件名。（保证存储的文件名不重复）
   - 将文件写入到对应路径的 File 中。
   - 调用 Service 层修改用户头像的路径。
   - 重定向到首页
5. Controller 层添加请求方法，文件 IO 流读取头像，通过 HttpServletResponse 返回头像。
6. 处理 thymeleaf 模板。（注意表单的 enctype=“multipart/form-data”）

设置密码
1. Service 层添加修改密码的方法，传入旧密码和新密码。
   - 判断旧密码、新密码不为空。
   - 从 ThreadLocal 中获取用户对象，判断旧密码是否正确。
   - 如果有错误，通过 Map 返回错误类型和提示。
   - 如果判断没问题，调用 Dao 层修改用户密码。返回一个空的 Map
2. Controller 层添加请求方法，接收前端传入的旧密码和新密码。
   - 调用 Service 层修改密码。
   - 如果返回的 Map 不为空，说明修改失败，返回错误信息，跳转回修改页面。
   - 如果修改成功，获取 Cookie 中的凭证，调用 Service 层的退出方法。
   - 删除 ThreadLocal 中的用户对象。（如果不删除，前端这次请求仍然认为用户是已登录状态。
也可以用重定向，重定向时发起一次新的请求。这样手动跳转是为了让用户体验更好，可以先提示用户修改成功再跳转）
   - 跳转到登录页面，让用户重新登录。

**5）检查登录状态**

用户在未登录状态下，可以通过地址栏访问需要登录的页面，非常危险。

用拦截器 + 注解实现。在想要拦截的方法上添加注解，不加注解的方法不拦截。

1. 新建一个注解 LoginRequired，用来表示方法是否需要登录才能访问。该注解用于方法，运行时有效。
2. 给账号设置、上传头像、修改密码等方法添加 LoginRequired 注解。
3. 新建一个拦截器，重写 preHandle 方法。
   - 判断拦截的是否时一个方法，不是方法不进行拦截。
   - 获取方法上的 LoginRequired 注解。如果为空，也不进行拦截
   - 如果注解不为空，并且 ThreadLocal 中是没有用户对象。说明没登录，拒绝请求，重定向到登录页面。
4. 在拦截器配置类中，注册刚才的拦截器，并过滤静态资源的请求。

### 核心功能

**1）过滤敏感词工具**

使用前缀树的数据结构，来实现敏感词的过滤。

1. 在 resources 目录下新建一个 txt，存放敏感词。
2. 创建工具类。工具类中定义前缀树数据结构。前缀树节点包含两个属性：关键词结束标志、子节点（Map 类型，Key 为子节点字符，value 为子节点对象）。
3. 工具类中创建一个前缀树根节点。
4. 工具类的 init 方法中初始化前缀树。
   - 通过类加载器获取敏感词文件的字节输入流。
   - 将字节输入流转为字符输入流，再转为缓冲流。
   - 每次读取一个敏感词，添加到前缀树中。添加过程如下：
   - 定义一个引用指向根节点。遍历敏感词中的字符，如果当前引用下级没有该字符节点，则初始化一个子节点，
把子节点挂到当前引用之下。如果已经有该字符节点，直接使用子节点。让当前引用指向子节点，进入下一个循环。
如果遍历结束，需要设置最后一个节点的关键词结束标志为 True。
5. 定义一个公开的方法，过滤文本的敏感词，返回过滤后的文本。
   - 如果文本为空直接返回。
   - 定义 tempNode 指向根节点，begin 和 position 指向字符串最开始的索引 0。定义 StringBuilder 存放结果。
   - 遍历字符串，当 position 指向结尾的时候结束。
   - 如果当前字符为特殊符号。并且 tempNode 指向根节点，则将当前字符添加到结果中，begin++，position++。如果 tempNode 不是指向根节点，则position++，进入下一次循环。
   - 检查 tempNode 是否有包含当前字符的下级节点。
   - 如果不包含，将 begin 指向的字符加入结果中。begin++，position = begin，tempNode = 根节点；
   - 如果包含，并且 tempNode 结束标志为 True。替换 begin-position 之间的字符。position++，begin = position，tempNode = 根节点。
   - 如果包含，tempNode 结束标志不为 true。position++，继续检查下一个字符；
   - 循环结束，将最后一批字符添加到结果中。（就时 position 遍历到终点，begin 还没有遍历到终点的情况）
   - 返回结果。

**2）发布招聘贴**

发布帖子的功能，通过 AJAX 请求的方式来实现。

1. 引入 FastJSON 依赖，工具类中添加对象转 JSON 字符串的功能。先将对象放到 JSON 对象中，然后转为字符串返回。
2. Dao 层添加插入帖子到数据库的方法。
3. Service 层添加发布帖子的功能。如果传入的帖子不为空，将帖子标题和内容中的 HTML 标签进行转义，
使用工具过滤标题和内容的敏感词，调用 Dao 层方法插入帖子到数据库。
4. Controller 层添加请求方法，接收前端传的标题和内容。
   - 尝试从 ThreadLocal 中取用户对象。如果没有，说明未登录，返回错误信息的 JSON 字符串给前端。
   - 根据接收的标题和内容，构建帖子对象。调用 Service 层方法发布帖子。
   - 返回添加成功的 JSON 字符串给前端。
5. 前端使用 Jquery 发布异步请求。
   - 根据 id 从输入框获取标题和内容。
   - Jquery 发送异步请求，发送标题和内容。回调函数中接收 JSON 字符串，转为 JSON 对象。
   - 在提示框中显示JSON对象中的内容。2s 后隐藏提示框，如果后端传的是发布成功，就刷新下首页，显示新的帖子。

**3）查看帖子详情**

1. Dao 层添加根据帖子 id 查询帖子的方法。
2. Service 层添加查询帖子的方法，调用 Dao 层。
3. Controller 层添加请求方法，使用路径参数接收帖子 id。
   - 根据帖子 id 调用 Service 层查询帖子，放到 Model 中。
   - 根据帖子中的 userId，查询用户，放到 Model 中。
   - 返回帖子详情 thymeleaf 模板的路径。
4. thymeleaf 模板中处理并展示数据。

**4）显示帖子评论**

1. 新建评论的实体类。
2. Dao 层添加从数据库中分页查询评论、查询评论数量的方法。
3. Service 层调用 Dao 层，添加分页查询评论，查询评论数量的方法。
4. 修改 Controller 层帖子详情的请求方法。
   - 添加 Page 参数，接收前端的分页信息。设置 Page 的每页显示评论的最大数量，设置 Page 的路径，
从数据库中查询评论总数，并给 Page 设置。
   - 调用 Service 层方法，分页来查询帖子评论。
   - 由于每个评论都有对应的用户，每个评论还有对应的评论（回复）。我们定义一个 List<Map<String, Object>> 结构来存储。
   - 遍历所有的评论，将评论放到 Map 中，根据评论的 userId 查询到用户，放到 Map 中。
   - 根据评论的 id 查询到对应的回复（评论的评论），以及回复对应的用户（包括发表的用户和目标用户），放到一个List<Map<String, Object>>结构中，回复的 List<Map<String, Object>> 在放到 Map 中。
   - 查询评论的回复数量，放到 Map 中。
   - 将这个 Map（包含评论，用户，回复，回复数量）放到 List 中。List 放到 Model 中传给前端。
5. thymeleaf 模板中处理并展示数据。

**5）发布评论**

1. 

**6）显示私信列表**

**7）发送私信**

**8）统一异常处理**

**9）统一记录日志**

### 引入Redis

### 引入Kafka

### 引入ElasticSearch

### 优化安全和性能


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
source $path/init_schema.sql;
``` 
3、插入测试数据
```sql
source $path/init_data.sql;
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