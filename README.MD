# Mybatis-generator插件

1. CommentPlugin.java
    通用的代码生成  
    MySQLLimitPlugin.java
    生成分页相关代码
2. SpringBootGeneratorPlugin.java
  适用于集成通用mapper及pageHelper(代码简洁)
3. org.mybatis.util.GeneratorUtil用于在2的基础上生成service,只需执行类似main方法去调用即可
## 使用
 需在pom引入mybatis-generator插件,并在相应的xml中配置引用的jar包即可
 
#### 插件2示例:
 ![Aaron Swartz](https://github.com/hiyzx/mybatis-generagor-plugin/blob/master/mybatisPlugin/src/file/example.png?raw=true) 