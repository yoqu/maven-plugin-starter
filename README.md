<p align="center">
    <h3 align="center">Maven Plugin Stater</h3>
    <p align="center">
        A spring boot applicaiton quick running script.
        <br>
        <a href="https://maven-badges.herokuapp.com/maven-central/com.uyoqu.framework/maven-plugin-starter/">
            <img src="https://maven-badges.herokuapp.com/maven-central/com.uyoqu.framework/maven-plugin-starter/badge.svg" >
        </a>
         <a href="https://github.com/yoqu/maven-plugin-starter/releases">
             <img src="https://img.shields.io/github/release/yoqu/maven-plugin-starter.svg" >
         </a>
         <a href="http://www.apache.org/licenses/LICENSE-2.0">
             <img src="https://img.shields.io/badge/license-APACHEv2-blue.svg" >
         </a>
    </p> 
</p>

# 为什么做这款工具
> 随着spring boot的快速发展，现在一个服务的的部署方式越来越简单，轻松，特别是微服务的兴起，docker容器化。使得spring boot的jar优势越来越大仅需使用`java -jar xxx.jar`即可启动。
但同时，对于传统企业和公司，未引入docker容器化的部署方案,又想使用jar包独立启动会遇到以下几个问题
1. 对于需要自定义jvm参数或者后台挂起等需求需要手动写脚本（每次控制台写太累了）
2. jar内的配置文件修改异常麻烦（有时需要运维做维护配置，假设你的配置有问题，还需要重新打包，遇到龟速网络拷包太浪费时间了）
3. 一台服务器有多个微服务运行，不知道某个进程是什么服务（通过端口检查也知道，不过这好像有点麻烦）。

综上，这款小工具通过预置项目启动、停止、重启脚本。用户在编译后生成的部署包运维只需两行命令即可运行。1: `unzip xxx.war` 2：`sh xxx/bin/start.sh`。

**注**：war包使用tomcat部署不在本次讨论范围内。

项目编译后的目录结构：

```
├── META-INF
├── WEB-INF
│   ├── classes
│   └── lib
└── bin
    ├── restart.sh
    ├── start.sh
    └── stop.sh
```

# 特性
- 自动在打包过程中生成启动脚本
- 无入侵，用户在代码中无感知
- 支持jvm参数自定义配置
- 支持个性化启动类查找
- 支持remote debug，jmx
- jps命令可显示服务名称*（妈妈再也不用担心我的进程是什么服务啦）*

# 快速集成
1. 在pom文件中设置打包方式为war
```xml
	<packaging>war</packaging>
```

2. 设置pom.xml文件的plugins下引入以下配置
```xml
<plugin>
    <groupId>com.uyoqu.framework</groupId>
	<artifactId>maven-plugin-starter</artifactId>
	<version>1.0.0</version>
    <executions>
        <execution>
        <phase>package</phase>
        <goals>
            <goal>bin</goal>
        </goals>
        </execution>
    </executions>
</plugin> 
```

插件本身是支持配置，可指定启动主函数以及jvm参数，对于Spring boot支持自动查询MainClass，无需用户手动填写。

配置示例：

```xml
<plugin>
    <groupId>com.uyoqu.framework</groupId>
	<artifactId>maven-plugin-starter</artifactId>
	<version>1.0.0</version>
    <executions>
        <execution>
        <phase>package</phase>
        <goals>
            <goal>bin</goal>
        </goals>
        </execution>      
    </executions>
  	<configuration>
            <jvms>
                <jvm>-server</jvm>
                <jvm>-Xmx512m</jvm>
            </jvms>
            <mainClass>com.xxx.xxx</mainClass>
            <matchClass>App</matchClass>
     </configuration>
</plugin>        

```



**注**：

- jvms中配置jvm参数，如果用户不进行该项配置则bin脚本中就会填充默认参数
- mainClass中可直接填入启动类全限定名来进行参数配置
- 若mainClass不进行配置，则会启动启动类自动查找功能
- 可通过matchClass参数对查找匹配规则进行配置
- 若mainClass不进行配置则默认查找拥有@SpringBootApplication注解的类
- 若配置mainClass，则会进行包含匹配
- 比如:matchClass配置为App，则类名为App，Application，父类名为App，Application的类都会被匹配成功
- 启动类只能有一个，若匹配到多个则会报错