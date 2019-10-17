
# 效果预览
目录结构：
>项目名
>>target目录
>>>项目生成的包
>>>>bin目录
>>>>>start,stop,restart脚本
# 特性
- 自动在打包过程中生成启动脚本
- 无入侵，用户在代码中无感知
- 支持jvm参数自定义配置
- 支持个性化启动类查找
- 支持remote debug，jmx

注：
  - jvms中配置jvm参数，如果用户不进行该项配置则bin脚本中就会填充默认参数
  - mainClass中可直接填入启动类全限定名来进行参数配置
  - 若mainClass不进行配置，则会启动启动类自动查找功能
  - 可通过matchClass参数对查找匹配规则进行配置
  - 若mainClass不进行配置则默认查找拥有@SpringBootApplication注解的类
  - 若配置mainClass，则会进行包含匹配
  - 比如:matchClass配置为App，则类名为App，Application，父类名为App，Application的类都会被匹配成功
  - 启动类只能有一个，若匹配到多个则会报错
# java插件集成
1. 在pom文件中设置打包方式为war
```xml
	<packaging>jar</packaging>
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
        <configuration>
            <jvms>
                <jvm>-server</jvm>
                <jvm>-Xmx512m</jvm>
            </jvms>
            <mainClass>com.xxx.xxx</mainClass>
            <matchClass>App</matchClass>
        </configuration>
        </execution>
    </executions>
</plugin> 
```


具体生成脚本示例展示： 
- start脚本：
``` bash
#!/bin/bash

source /etc/profile
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CLASSES=$DEPLOY_DIR/WEB-INF/classes

JMX_HOST_NAME=0.0.0.0
JMX_PORT=1099

BITS=`java -version 2>&1 | grep -i 64-bit`

MAIN=xxx
JAVA_MEM_OPTS=" -xxx "
SERVER_NAME="xxx"
if [ -z "$SERVER_NAME" ]; then
    SERVER_NAME=`hostname`
fi

PIDS=`ps aux | grep java | grep "$CLASSES" |awk '{print $2}'`
if [ -n "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

if [ -n "$SERVER_PORT" ]; then
    SERVER_PORT_COUNT=`netstat -tln | grep $SERVER_PORT | wc -l`
    if [ $SERVER_PORT_COUNT -gt 0 ]; then
        echo "ERROR: The $SERVER_NAME port $SERVER_PORT already used!"
        exit 1
    fi
fi


LIB_DIR=$DEPLOY_DIR/WEB-INF/lib
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Duser.timezone=GMT+08"

JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
fi

if [ "$APM_AGENT_HOME" ]; then
    echo "APM plugin detected,auto enable APM plugin,apm path:$APM_AGENT_HOME"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:$APM_AGENT_HOME/apm-agent-2.1.6-rc2.jar"s
fi

JAVA_JMX_OPTS=""
if [ "$1" = "jmx" ]; then
    JAVA_JMX_OPTS=" -Djava.rmi.server.hostname=$JMX_HOST_NAME -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
fi

echo -e "Starting the $SERVER_NAME ...\c"
nohup java $JAVA_OPTS $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_JMX_OPTS -classpath $CLASSES:$LIB_JARS $MAIN > nohup.out 2>&1 < /dev/null &

echo "OK!"
PIDS=`ps aux | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
echo "PID: $PIDS"
```
- stop脚本：
``` bash
#!/bin/bash

source /etc/profile
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CLASSES=$DEPLOY_DIR/WEB-INF/classes

SERVER_NAME="xxx"
if [ -z "$SERVER_NAME" ]; then
    SERVER_NAME=`hostname`
fi

PIDS=`ps aux | grep java | grep "$CLASSES" |awk '{print $2}'`
if [ -z "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME does not started!"
    exit 1
fi

echo -e "Stopping the $SERVER_NAME ...\c"
for PID in $PIDS ; do
    kill $PID > /dev/null 2>&1
done

COUNT=0
while [ $COUNT -lt 1 ]; do
    echo -e ".\c"
    sleep 1
    COUNT=1
    for PID in $PIDS ; do
        PID_EXIST=`ps -f -p $PID | grep java`
        if [ -n "$PID_EXIST" ]; then
            COUNT=0
            break
        fi
    done
done

echo "OK!"
echo "PID: $PIDS"
```
- restart.sh:
``` bash
#!/bin/bash
cd `dirname $0`
./stop.sh
./start.sh
```
注：示例中的xxx部分根据项目实际情况进行生成

