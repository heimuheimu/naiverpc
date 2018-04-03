# NaiveRPC: 简单易用的 Java RPC 框架。

## 使用要求
* JDK 版本：1.8+ 
* 依赖类库：
  * [slf4j-log4j12 1.7.5+](https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12)
  * [naivemonitor 1.0+](https://github.com/heimuheimu/naivemonitor)
  * [compress-lzf 1.0.3+](https://github.com/ning/compress)
  * [spring-context 3.1.4.RELEASE+](https://mvnrepository.com/artifact/org.springframework/spring-context)

## Maven 配置
```xml
    <dependency>
        <groupId>com.heimuheimu</groupId>
        <artifactId>naiverpc</artifactId>
        <version>1.1.0-SNAPSHOT</version>
    </dependency>
```

## RPC 服务端

### Log4J 配置
```
# RPC 错误信息根日志
log4j.logger.com.heimuheimu.naiverpc=ERROR, NAIVERPC
log4j.additivity.com.heimuheimu.naiverpc=false
log4j.appender.NAIVERPC=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC.file=${log.output.directory}/naiverpc/naiverpc.log
log4j.appender.NAIVERPC.encoding=UTF-8
log4j.appender.NAIVERPC.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n

# RPC 服务注册信息日志
log4j.logger.com.heimuheimu.naiverpc.spring.server=INFO, RPC_REGISTER_SERVICE
log4j.additivity.com.heimuheimu.naiverpc.spring.server=false
log4j.appender.RPC_REGISTER_SERVICE=org.apache.log4j.DailyRollingFileAppender
log4j.appender.RPC_REGISTER_SERVICE.file=${log.output.directory}/naiverpc/register.log
log4j.appender.RPC_REGISTER_SERVICE.encoding=UTF-8
log4j.appender.RPC_REGISTER_SERVICE.DatePattern=_yyyy-MM-dd
log4j.appender.RPC_REGISTER_SERVICE.layout=org.apache.log4j.PatternLayout
log4j.appender.RPC_REGISTER_SERVICE.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n

# RPC 连接信息日志
log4j.logger.NAIVERPC_CONNECTION_LOG=INFO, NAIVERPC_CONNECTION_LOG
log4j.additivity.NAIVERPC_CONNECTION_LOG=false
log4j.appender.NAIVERPC_CONNECTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_CONNECTION_LOG.file=${log.output.directory}/naiverpc/connection.log
log4j.appender.NAIVERPC_CONNECTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_CONNECTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_CONNECTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_CONNECTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n

# RPC 方法执行错误日志
log4j.logger.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=INFO, NAIVERPC_SERVER_ERROR_EXECUTION_LOG
log4j.additivity.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=false
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.file=${log.output.directory}/naiverpc/server_error.log
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_SERVER_ERROR_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n

# RPC 方法慢执行日志
log4j.logger.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=INFO, NAIVERPC_SERVER_SLOW_EXECUTION_LOG
log4j.additivity.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=false
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.file=${log.output.directory}/naiverpc/server_slow_execution.log
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_SERVER_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n
```

### Spring 配置
```xml
    <!-- RPC 服务配置，监听端口为 4182 -->
    <bean id="rpcServer" class="com.heimuheimu.naiverpc.spring.server.RpcServerFactory" destroy-method="close">
        <constructor-arg index="0" value="4182" /> <!-- RPC 服务监听端口，默认为 4182 -->
        <constructor-arg index="1"> <!-- 监听器，允许为 null -->
            <bean class="com.heimuheimu.naiverpc.server.SimpleRpcExecutorListener"/>
        </constructor-arg>
    </bean>
    
    <!-- RPC 服务自动注册配置 -->
    <bean id="autoRpcServiceBeanRegister" class="com.heimuheimu.naiverpc.spring.server.AutoRpcServiceBeanRegister">
        <constructor-arg index="0" ref="rpcServer" />
        <constructor-arg index="1" value="com.heimuheimu" /> <!-- 扫描的包路径，包含子包 -->
        <constructor-arg index="2" value=".*(Remote|Internal)Service$" /> <!-- 接口名字正则表达式，示例为以 "RemoteService" 或 "InternalService" 结尾的所有接口 -->
    </bean>
```

### Falcon 监控数据上报 Spring 配置
```xml
    <!-- 监控数据采集器列表 -->
    <util:list id="falconDataCollectorList">
        <!-- RPC 服务端监控数据采集器 -->
        <bean class="com.heimuheimu.naiverpc.monitor.server.falcon.RpcServerCompressionDataCollector"></bean>
        <bean class="com.heimuheimu.naiverpc.monitor.server.falcon.RpcServerSocketDataCollector"></bean>
        <bean class="com.heimuheimu.naiverpc.monitor.server.falcon.RpcServerThreadPoolDataCollector"></bean>
        <bean class="com.heimuheimu.naiverpc.monitor.server.falcon.RpcServerExecutionDataCollector"></bean>
    </util:list>
    
    <!-- Falcon 监控数据上报器 -->
    <bean id="falconReporter" class="com.heimuheimu.naivemonitor.falcon.FalconReporter" init-method="init" destroy-method="close">
        <constructor-arg index="0" value="http://127.0.0.1:1988/v1/push" /> <!-- Falcon 监控数据推送地址 -->
        <constructor-arg index="1" ref="falconDataCollectorList" />
    </bean>
```

### Falcon 上报数据项说明（上报周期：30秒）
 * naiverpc_server_error/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 方法执行发生异常的错误次数
 * naiverpc_server_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数
 * naiverpc_server_peak_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数
 * naiverpc_server_avg_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 方法执行平均执行时间
 * naiverpc_server_max_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 方法执行最大执行时间
 * naiverpc_server_socket_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数
 * naiverpc_server_socket_avg_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数
 * naiverpc_server_socket_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数
 * naiverpc_server_socket_avg_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数
 * naiverpc_server_threadPool_rejected_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内所有线程池拒绝执行的任务总数
 * naiverpc_server_threadPool_active_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池活跃线程数近似值总和
 * naiverpc_server_threadPool_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池线程数总和
 * naiverpc_server_threadPool_peak_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池出现过的最大线程数总和
 * naiverpc_server_threadPool_core_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的核心线程数总和
 * naiverpc_server_threadPool_maximum_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的最大线程数总和
 * naiverpc_server_compression_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内压缩操作已节省的字节数
 * naiverpc_server_compression_avg_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内平均每次压缩操作节省的字节数
 
### 示例代码
RPC 服务接口定义示例：
```java
package com.heimuheimu.naiverpc.demo;

public interface UserRemoteService {
    
    User get(long uid);
    
}
```

RPC 服务实现类示例（NaiveRPC 会自动扫描符合条件的接口实现，对外提供该远程服务）：
```java
package com.heimuheimu.naiverpc.demo.impl;

@Service
public class UserRemoteServiceImpl implements UserRemoteService {
    
    public User get(long uid) {
        User user = new User();
        user.setUid(uid);
        user.setNickname("alice");
        return user;
    }
    
}
```

### 关闭 RPC 服务
为避免直接关闭 RPC 服务导致正在执行中的客户端调用失败，应先执行 RPC 服务下线操作，等待一段时间（可设置为客户端执行超时时间），再关闭应用。

**WEB 项目** RPC 服务下线示例代码：
```java
@Controller
@RequestMapping("/internal/rpc-server")
public class RpcServerController {
    
    @Autowired
    private RpcServer rpcServer;
    
    @RequestMapping("/offline.htm")
    @ResponseBody
    public String offline() {
        rpcServer.offline();
        return "ok";
    }
}
```

**WEB 项目**关闭步骤（建议通过自动化运维脚本实现以下步骤）
 * 调用下线 URL： /internal/rpc-server/offline.htm，等待返回 "ok" 输出
 * 等待一段时间（可设置为客户端执行超时时间）
 * 关闭 Tomcat

**JAR 项目** RPC 服务下线示例代码（使用 [naivecli](https://github.com/heimuheimu/naivecli) 实现）：
```java
public class RpcServerOfflineCommand implements NaiveCommand {

    @Autowired
    private RpcServer rpcServer;

    @Override
    public String getName() {
        return "offline";
    }

    @Override
    public List<String> execute(String[] args) {
        rpcServer.offline();
        return Collections.singletonList("ok");
    }
}
```
**JAR 项目**关闭步骤（建议通过自动化运维脚本实现以下步骤）
 * 执行 "telnet your-project-ip 4183"，打开 naivecli 命令行工具，输入 "offline" 命令后回车，等待返回 "ok" 输出
 * 等待一段时间（可设置为客户端执行超时时间）
 * 关闭 JAR 项目


## RPC 客户端

### Log4J 配置
```
# RPC 错误信息根日志
log4j.logger.com.heimuheimu.naiverpc=ERROR, NAIVERPC
log4j.additivity.com.heimuheimu.naiverpc=false
log4j.appender.NAIVERPC=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC.file=${log.output.directory}/naiverpc/naiverpc.log
log4j.appender.NAIVERPC.encoding=UTF-8
log4j.appender.NAIVERPC.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n

# RPC 客户端连接信息日志
log4j.logger.NAIVERPC_CONNECTION_LOG=INFO, NAIVERPC_CONNECTION_LOG
log4j.additivity.NAIVERPC_CONNECTION_LOG=false
log4j.appender.NAIVERPC_CONNECTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_CONNECTION_LOG.file=${log.output.directory}/naiverpc/connection.log
log4j.appender.NAIVERPC_CONNECTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_CONNECTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_CONNECTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_CONNECTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n

# RPC 远程调用错误日志
log4j.logger.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=INFO, NAIVERPC_CLIENT_ERROR_EXECUTION_LOG
log4j.additivity.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=false
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.file=${log.output.directory}/naiverpc/client_error.log
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_CLIENT_ERROR_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n

# RPC 远程调用慢执行日志
log4j.logger.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=INFO, NAIVERPC_CLIENT_SLOW_EXECUTION_LOG
log4j.additivity.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=false
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.file=${log.output.directory}/naiverpc/client_slow_execution.log
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.encoding=UTF-8
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVERPC_CLIENT_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n
```

### Spring 配置
```xml
    <!-- RPC 集群客户端事件监听器配置，在 RPC 服务不可用时进行实时通知 -->
    <bean id="rpcClientListListener" class="com.heimuheimu.naiverpc.facility.clients.NoticeableDirectRpcClientListListener">
        <constructor-arg index="0" value="your-project-name" /> <!-- 当前项目名称 -->
        <constructor-arg index="1" value="rpc-server-name" /> <!-- RPC 服务名称 -->
        <constructor-arg index="2" ref="notifierList" /> <!-- 报警器列表，报警器的信息可查看 naivemonitor 项目 -->
    </bean>
    
    <!-- RPC 集群客户端配置 -->
    <bean id="rpcClient" class="com.heimuheimu.naiverpc.spring.client.RpcClusterClientFactory" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:4182,127.0.0.1:4183,127.0.0.1:4184" /> <!-- RPC 服务地址列表，使用 "," 分割 -->
        <constructor-arg index="1">
            <bean class="com.heimuheimu.naiverpc.client.SimpleDirectRpcClientListener">
                <constructor-arg index="0" value="rpc-server-name" /> <!-- RPC 服务名称 -->
            </bean>
        </constructor-arg>
        <constructor-arg index="2" ref="rpcClientListListener" />
    </bean>
    
    <!-- 远程 RPC 服务自动注册配置 -->
    <bean id="autoRpcProxyBeanRegister" class="com.heimuheimu.naiverpc.spring.client.AutoRpcProxyBeanRegister">
        <constructor-arg index="0" value="rpcClient" />
        <constructor-arg index="1" value="com.heimuheimu" /> <!-- 扫描的包路径，包含子包 -->
        <constructor-arg index="2" value=".*(Remote|Internal)Service$" /> <!-- 接口名字正则表达式，示例为以 "RemoteService" 或 "InternalService" 结尾的所有接口 -->
    </bean>
```

### Falcon 监控数据上报 Spring 配置
```xml
    <!-- 监控数据采集器列表 -->
    <util:list id="falconDataCollectorList">
        <!-- RPC 客户端监控数据采集器 -->
        <bean class="com.heimuheimu.naiverpc.monitor.client.falcon.RpcClientCompressionDataCollector"></bean>
        <bean class="com.heimuheimu.naiverpc.monitor.client.falcon.RpcClientThreadPoolDataCollector"></bean>
        <bean class="com.heimuheimu.naiverpc.monitor.client.falcon.RpcClientSocketDataCollector">
            <constructor-arg index="0" value="rpc-server-name" /> <!-- groupName: RPC 服务名称 -->
            <constructor-arg index="1" value="127.0.0.1:4182,127.0.0.1:4183,127.0.0.1:4184" /> <!-- RPC 服务地址列表，使用 "," 分割 -->
        </bean>
        <bean class="com.heimuheimu.naiverpc.monitor.client.falcon.RpcClientExecutionDataCollector">
            <constructor-arg index="0" value="rpc-server-name" /> <!-- groupName: RPC 服务名称 -->
            <constructor-arg index="1" value="127.0.0.1:4182,127.0.0.1:4183,127.0.0.1:4184" /> <!-- RPC 服务地址列表，使用 "," 分割 -->
        </bean>
        <bean class="com.heimuheimu.naiverpc.monitor.client.falcon.RpcClusterClientDataCollector"></bean>
    </util:list>
    
    <!-- Falcon 监控数据上报器 -->
    <bean id="falconReporter" class="com.heimuheimu.naivemonitor.falcon.FalconReporter" init-method="init" destroy-method="close">
        <constructor-arg index="0" value="http://127.0.0.1:1988/v1/push" /> <!-- Falcon 监控数据推送地址 -->
        <constructor-arg index="1" ref="falconDataCollectorList" />
    </bean>
```

### Falcon 上报数据项说明（上报周期：30秒）
 * naiverpc_client_{groupName}_too_busy/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 服务繁忙的错误次数
 * naiverpc_client_{groupName}_timeout/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生超时的错误次数
 * naiverpc_client_{groupName}_error/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生异常的错误次数
 * naiverpc_client_{groupName}_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数
 * naiverpc_client_{groupName}_peak_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数
 * naiverpc_client_{groupName}_avg_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用平均执行时间
 * naiverpc_client_{groupName}_max_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用最大执行时间
 * naiverpc_client_{groupName}_socket_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数
 * naiverpc_client_{groupName}_socket_avg_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数
 * naiverpc_client_{groupName}_socket_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数
 * naiverpc_client_{groupName}_socket_avg_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数
 * naiverpc_client_threadPool_rejected_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内所有线程池拒绝执行的任务总数
 * naiverpc_client_threadPool_active_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池活跃线程数近似值总和
 * naiverpc_client_threadPool_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池线程数总和
 * naiverpc_client_threadPool_peak_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池出现过的最大线程数总和
 * naiverpc_client_threadPool_core_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的核心线程数总和
 * naiverpc_client_threadPool_maximum_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的最大线程数总和
 * naiverpc_client_compression_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内压缩操作已节省的字节数
 * naiverpc_client_compression_avg_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内平均每次压缩操作节省的字节数
 * naiverpc_client_cluster_unavailable_client_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 集群客户端获取到不可用 RPC 客户端的次数
 
### 示例代码
RPC 远程服务调用示例代码（NaiveRPC 会自动扫描符合条件的接口，生成对应的 RPC 服务代理，将其注册在 Spring 中)：
```java
public class RpcClientDemo {
    
    @Autowired
    private UserRemoteService userRemoteService; //NaiveRPC 框架会自动注册其实现，直接调用即可
    
    public void test() {
        System.out.println(userRemoteService.get(100L));
    }
}
```

### RPC 广播客户端

广播客户端可以同时向所有 RPC 服务方发起同一 RPC 调用请求，通常作为消息分发使用。 
    
#### Spring 配置
```xml
    <!-- RPC 集群客户端事件监听器配置，在 RPC 服务不可用时进行实时通知 -->
    <bean id="rpcClientListListener" class="com.heimuheimu.naiverpc.facility.clients.NoticeableDirectRpcClientListListener">
        <constructor-arg index="0" value="your-project-name" /> <!-- 当前项目名称 -->
        <constructor-arg index="1" value="rpc-server-name" /> <!-- RPC 服务名称 -->
        <constructor-arg index="2" ref="notifierList" /> <!-- 报警器列表，报警器的信息可查看 naivemonitor 项目 -->
    </bean>

    <!-- RPC 广播客户端事件监听器配置，在 RPC 服务调用失败时进行实时通知 -->
    <bean id="rpcBroadcastClientListener" class="com.heimuheimu.naiverpc.client.broadcast.NoticeableRpcBroadcastClientListener">
        <constructor-arg index="0" value="your-project-name" /> <!-- 当前项目名称 -->
        <constructor-arg index="1" value="rpc-server-name" /> <!-- RPC 服务名称 -->
        <constructor-arg index="2" ref="notifierList" /> <!-- 报警器列表，报警器的信息可查看 naivemonitor 项目 -->
    </bean>
    
    <!-- RPC 广播客户端配置 -->
    <bean id="broadcastRpcClient" class="com.heimuheimu.naiverpc.spring.client.ParallelRpcBroadcastClientFactory" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:4182,127.0.0.1:4183,127.0.0.1:4184" /> <!-- RPC 服务地址列表，使用 "," 分割 -->
        <constructor-arg index="1"><null/></constructor-arg> <!-- Socket 配置，允许为 null -->
        <constructor-arg index="2" value="5000" /> <!-- RPC 调用超时时间，单位：毫秒，默认为 5 秒 -->
        <constructor-arg index="3" value="65536" /> <!-- 最小压缩字节数，默认为 64 KB -->
        <constructor-arg index="4" value="1000" /> <!-- RPC 调用过慢最小时间，单位：毫秒，默认为 1 秒 -->
        <constructor-arg index="5" value="30" /> <!-- 心跳检测时间，单位：秒，默认为 30 秒 -->
        <constructor-arg index="6">
            <bean class="com.heimuheimu.naiverpc.client.SimpleDirectRpcClientListener">
                <constructor-arg index="0" value="rpc-server-name" /> <!-- RPC 服务名称 -->
            </bean>
        </constructor-arg>
        <constructor-arg index="7" ref="rpcClientListListener" />
        <constructor-arg index="8" ref="rpcBroadcastClientListener" />
        <constructor-arg index="9" value="500" />  <!-- 线程池大小，默认为 500 -->
    </bean>
```

#### 代码示例
```java
public class BroadcastRpcClientDemo {
    
    private static final Logger LOG = LoggerFactory.getLogger(BroadcastRpcClientDemo.class);
    
    private static final Method USER_GET_METHOD;
    
    static {
        try {
            USER_GET_METHOD = UserRemoteService.class.getMethod("get", Long.class);
        } catch (NoSuchMethodException e) {
            LOG.error("No such method: `UserRemoteService#get(Long)`.", e);
            throw new IllegalStateException("No such method: `UserRemoteService#get(Long)`.", e);
        }
    }
    
    @Autowired
    private RpcBroadcastClient rpcBroadcastClient;
    
    public void test() {
        Object[] args = new Object[]{100L};
        Map<String, BroadcastResponse> broadcastResponseMap = rpcBroadcastClient.execute(USER_GET_METHOD, args);
        System.out.println(broadcastResponseMap);
    }
}
```

## 版本发布记录
### V1.1.0-SNAPSHOT
### 新增特性：
 * RPC 集群客户端自动移除不可用 RPC 客户端，不再依赖下一次 RPC 调用进行触发。
 * 提供 RPC 服务是否成功下线判断。
 * 更加健壮的心跳检测机制。
 * 增加 RPC 广播调用恢复通知。

***

### V1.0
### 特性：
 * 配置简单。
 * 支持 RPC 广播调用。
 * 通过 Falcon 可快速实现 RPC 数据监控。
 * 通过钉钉实现 RPC 服务故障实时报警。

## 更多信息
* [NaiveMonitor 项目主页](https://github.com/heimuheimu/naivemonitor)
* [NaiveRPC v1.0 API Doc](https://heimuheimu.github.io/naiverpc/api/v1.0/)
* [NaiveRPC v1.0 源码下载](https://heimuheimu.github.io/naiverpc/download/naiverpc-1.0-sources.jar)
* [NaiveRPC v1.0 Jar包下载](https://heimuheimu.github.io/naiverpc/download/naiverpc-1.0.jar)