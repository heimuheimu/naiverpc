# NaiveRPC: 简单易用的 Java RPC 框架。

<a href="https://lgtm.com/projects/g/heimuheimu/naiverpc/context:java"><img alt="Language grade: Java" src="https://img.shields.io/lgtm/grade/java/g/heimuheimu/naiverpc.svg?logo=lgtm&logoWidth=18"/></a>

## 使用要求
* JDK 版本：1.8+ 
* 依赖类库：
  * [slf4j-log4j12 1.7.5+](https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12)
  * [naivemonitor 1.1+](https://github.com/heimuheimu/naivemonitor)
  * [compress-lzf 1.0.3+](https://github.com/ning/compress)
  * [spring-context 3.1.4.RELEASE+](https://mvnrepository.com/artifact/org.springframework/spring-context)

## Maven 配置
```xml
    <dependency>
        <groupId>com.heimuheimu</groupId>
        <artifactId>naiverpc</artifactId>
        <version>1.2-SNAPSHOT</version>
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
    <!-- RPC 服务配置，监听端口为 4182，初始化方法由 AutoRpcServiceBeanRegister 进行调用 -->
    <bean id="rpcServer" class="com.heimuheimu.naiverpc.spring.server.RpcServerFactory" destroy-method="close">
        <constructor-arg index="0" value="4182" /> <!-- RPC 服务监听端口，默认为 4182 -->
        <constructor-arg index="1"> <!-- 监听器，允许为 null -->
            <bean class="com.heimuheimu.naiverpc.server.SimpleRpcExecutorListener"/>
        </constructor-arg>
    </bean>
    
    <!-- RPC 服务自动注册配置，自动扫描符合条件的接口实现，对外提供该远程服务 -->
    <bean id="autoRpcServiceBeanRegister" class="com.heimuheimu.naiverpc.spring.server.AutoRpcServiceBeanRegister">
        <constructor-arg index="0" ref="rpcServer" />
        <constructor-arg index="1" value="com.heimuheimu.bookstore" /> <!-- 扫描的包路径，包含子包 -->
        <constructor-arg index="2" value=".*(Remote|Internal)Service$" /> <!-- 接口名字正则表达式，示例为以 "RemoteService" 或 "InternalService" 结尾的所有接口 -->
    </bean>
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

### [推荐使用] Prometheus 监控系统数据采集
#### 1. 实现 Prometheus 监控指标导出 Controller（注意：请勿将此 Controller 暴露给公网访问，需通过策略仅允许 Prometheus 服务器或者内网访问）
```java
@Controller
@RequestMapping("/internal/")
public class PrometheusMetricsController {
    
    private final PrometheusExporter exporter;
    
    @Autowired
    public PrometheusMetricsController(PrometheusExporter exporter) {
        this.exporter = exporter;
    }
    
    @RequestMapping("/metrics")
    public void metrics(HttpServletResponse response) throws IOException {
        PrometheusHttpWriter.write(exporter.export(), response);
    }
}
```

#### 2. 在 Spring 中配置 PrometheusExporter 实例
```xml
    <bean name="prometheusExporter" class="com.heimuheimu.naivemonitor.prometheus.PrometheusExporter">
        <constructor-arg index="0" >
            <list>
                <!-- RPC 服务端信息采集器 -->
                <bean class="com.heimuheimu.naiverpc.monitor.server.prometheus.RpcServerCompositePrometheusCollector">
                    <constructor-arg index="0">
                        <list>
                            <value>4182, bookstore</value> <!-- RPC 监听端口及服务名称-->
                        </list>
                    </constructor-arg>
                </bean>
            </list>
        </constructor-arg>
    </bean>
```

#### 3. 在 Prometheus 服务中配置对应的 Job，并添加以下规则：
```yaml
groups:
  # RPC 服务端报警规则配置
  - name: RpcServerAlerts
    rules:
      # RPC 服务端执行错误报警配置（不包括执行过慢）
      - alert: 'RpcServerExecutionError'
        expr: naiverpc_server_exec_error_count{errorType!~"SlowExecution"} > 0
        annotations:
          summary: "RpcServer 执行错误"
          description: "RpcServer 执行错误，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]，RPC 服务名称：[{{ $labels.name }}]，错误类型：[{{ $labels.errorType }}]"

      # RPC 服务端执行过慢报警配置
      - alert: 'RpcServerSlowExecution'
        expr: naiverpc_server_exec_error_count{errorType="SlowExecution"} > 3
        for: 2m
        annotations:
          summary: "RpcServer 执行过慢"
          description: "RpcServer 执行过慢，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]，RPC 服务名称：[{{ $labels.name }}]，错误类型：[{{ $labels.errorType }}]"

      # RPC 服务端使用的线程池繁忙报警配置
      - alert: 'RpcServerThreadPoolReject'
        expr: naiverpc_server_threadPool_reject_count > 0
        annotations:
          summary: "RpcServer 使用的线程池繁忙"
          description: "RpcServer 使用的线程池繁忙，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]，RPC 服务名称：[{{ $labels.name }}]"

  # RPC 服务端聚合记录配置
  - name: RpcServerRecords
    rules:
      # 相邻两次采集周期内 RPC 方法执行次数，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_exec_count:sum
        expr: sum(naiverpc_server_exec_count) by (job, name)

      # 相邻两次采集周期内每秒最大 RPC 方法执行次数，根据项目名称和 RPC 服务名称进行聚合计算（该值为估算值，实际值一般小于该估算值）
      - record: job:naiverpc_server_exec_peak_tps_count:sum
        expr: sum(naiverpc_server_exec_peak_tps_count) by (job, name)

      # 相邻两次采集周期内单次 RPC 方法最大执行时间，单位：毫秒，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_max_exec_time_millisecond:max
        expr: max(naiverpc_server_max_exec_time_millisecond) by (job, name)

      # 相邻两次采集周期内单次 RPC 方法平均执行时间，单位：毫秒，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_avg_exec_time_millisecond:avg
        expr: avg(naiverpc_server_avg_exec_time_millisecond) by (job, name)

      # 相邻两次采集周期内 RPC 方法执行失败次数（包含执行过慢），根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_exec_error_count:sum
        expr: sum(naiverpc_server_exec_error_count) by (job, name)

      # 相邻两次采集周期内 RPC 服务端使用的线程池拒绝执行的任务总数，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_threadPool_reject_count:sum
        expr: sum(naiverpc_server_threadPool_reject_count) by (job, name)

      # 相邻两次采集周期内 Socket 读取的字节总数，单位：MB，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_socket_read_megabytes:sum
        expr: sum(naiverpc_server_socket_read_bytes) by (job, name) / 1024 / 1024

      # 相邻两次采集周期内 Socket 写入的字节总数，单位：MB，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_server_socket_write_megabytes:sum
        expr: sum(naiverpc_server_socket_write_bytes) by (job, name) / 1024 / 1024

      # 相邻两次采集周期内 Socket 读取的次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_read_count:sum
        expr: sum(naiverpc_server_socket_read_count) by (job, instance, name)

      # 相邻两次采集周期内 Socket 读取的字节总数，单位：MB，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_read_megabytes:sum
        expr: sum(naiverpc_server_socket_read_bytes) by (job, instance, name) / 1024 / 1024

      # 相邻两次采集周期内单次 Socket 读取的最大字节数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_max_read_bytes:max
        expr: max(naiverpc_server_socket_max_read_bytes) by (job, instance, name)

      # 相邻两次采集周期内 Socket 写入的次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_write_count:sum
        expr: sum(naiverpc_server_socket_write_count) by (job, instance, name)

      # 相邻两次采集周期内 Socket 写入的字节总数，单位：MB，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_write_megabytes:sum
        expr: sum(naiverpc_server_socket_write_bytes) by (job, instance, name) / 1024 / 1024

      # 相邻两次采集周期内单次 Socket 写入的最大字节数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_server_socket_max_write_bytes:max
        expr: max(naiverpc_server_socket_max_write_bytes) by (job, instance, name)
```

  完成以上工作后，在 Prometheus 系统中即可找到以下监控指标：
* RPC 服务端执行信息指标：
  * naiverpc_server_exec_count{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法执行次数
  * naiverpc_server_exec_peak_tps_count{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内每秒最大 RPC 方法执行次数
  * naiverpc_server_avg_exec_time_millisecond{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 RPC 方法平均执行时间，单位：毫秒
  * naiverpc_server_max_exec_time_millisecond{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 RPC 方法最大执行时间，单位：毫秒
* RPC 服务端执行错误信息指标：
  * naiverpc_server_exec_error_count{errorCode="-3",errorType="InvocationError",name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法执行出现异常的错误次数
  * naiverpc_server_exec_error_count{errorCode="-4",errorType="SlowExecution",name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法执行出现执行过慢的错误次数
* RPC 服务端 Socket 读、写信息指标：
  * naiverpc_server_socket_read_count{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 读取的次数
  * naiverpc_server_socket_read_bytes{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 读取的字节总数
  * naiverpc_server_socket_max_read_bytes{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 Socket 读取的最大字节数
  * naiverpc_server_socket_write_count{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 写入的次数
  * naiverpc_server_socket_write_bytes{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 写入的字节总数
  * naiverpc_server_socket_max_write_bytes{name="bookstore",port="4182",remoteAddress="127.0.0.1"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 Socket 写入的最大字节数
* RPC 服务端使用的线程池信息指标：
  * naiverpc_server_threadPool_reject_count{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内监控器中所有线程池拒绝执行的任务总数
  * naiverpc_server_threadPool_active_count{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻监控器中的所有线程池活跃线程数近似值总和
  * naiverpc_server_threadPool_pool_size{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻监控器中的所有线程池线程数总和
  * naiverpc_server_threadPool_peak_pool_size{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池出现过的最大线程数总和
  * naiverpc_server_threadPool_core_pool_size{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池配置的核心线程数总和
  * naiverpc_server_threadPool_maximum_pool_size{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池配置的最大线程数总和
* RPC 服务端压缩操作信息指标：
  * naiverpc_server_compression_count{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内已执行的压缩次数
  * naiverpc_server_compression_reduce_bytes{name="bookstore",port="4182"}    &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内通过压缩节省的字节总数
  
  通过 util-grafana 项目可以为 RPC 服务端监控指标快速生成 Grafana 监控图表，项目地址：[https://github.com/heimuheimu/util-grafana](https://github.com/heimuheimu/util-grafana)

### Falcon 监控系统数据采集
#### 1. 在 Spring 中配置 Falcon 数据推送：
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
  完成以上工作后，在 Falcon 系统中可以找到以下数据项
* RPC 服务端方法执行错误数据项：
  * naiverpc_server_error/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 方法执行发生异常的错误次数
  * naiverpc_server_slow_execution/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 方法执行发生的慢执行次数 
* RPC 服务端方法执行数据项： 
  * naiverpc_server_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数
  * naiverpc_server_peak_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数
  * naiverpc_server_avg_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 方法执行平均执行时间
  * naiverpc_server_max_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 方法执行最大执行时间
* RPC 服务端 Socket 数据项：
  * naiverpc_server_socket_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数
  * naiverpc_server_socket_avg_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数
  * naiverpc_server_socket_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数
  * naiverpc_server_socket_avg_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数
* RPC 服务端线程池数据项：
  * naiverpc_server_threadPool_rejected_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内所有线程池拒绝执行的任务总数
  * naiverpc_server_threadPool_active_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池活跃线程数近似值总和
  * naiverpc_server_threadPool_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池线程数总和
  * naiverpc_server_threadPool_peak_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池出现过的最大线程数总和
  * naiverpc_server_threadPool_core_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的核心线程数总和
  * naiverpc_server_threadPool_maximum_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的最大线程数总和
* RPC 服务端压缩数据项： 
  * naiverpc_server_compression_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内压缩操作已节省的字节数
  * naiverpc_server_compression_avg_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内平均每次压缩操作节省的字节数

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
    
    <!-- 远程 RPC 服务自动注册配置，自动扫描符合条件的接口，生成对应的 RPC 服务代理，将其注册在 Spring 中，直接注入即可使用 -->
    <bean id="autoRpcProxyBeanRegister" class="com.heimuheimu.naiverpc.spring.client.AutoRpcProxyBeanRegister">
        <constructor-arg index="0" value="rpcClient" />
        <constructor-arg index="1" value="com.heimuheimu" /> <!-- 扫描的包路径，包含子包 -->
        <constructor-arg index="2" value=".*(Remote|Internal)Service$" /> <!-- 接口名字正则表达式，示例为以 "RemoteService" 或 "InternalService" 结尾的所有接口 -->
    </bean>
```

### [推荐使用] Prometheus 监控系统数据采集
#### 1. 实现 Prometheus 监控指标导出 Controller（注意：请勿将此 Controller 暴露给公网访问，需通过策略仅允许 Prometheus 服务器或者内网访问）
```java
@Controller
@RequestMapping("/internal/")
public class PrometheusMetricsController {
    
    private final PrometheusExporter exporter;
    
    @Autowired
    public PrometheusMetricsController(PrometheusExporter exporter) {
        this.exporter = exporter;
    }
    
    @RequestMapping("/metrics")
    public void metrics(HttpServletResponse response) throws IOException {
        PrometheusHttpWriter.write(exporter.export(), response);
    }
}
```

#### 2. 在 Spring 中配置 PrometheusExporter 实例
```xml
    <bean name="prometheusExporter" class="com.heimuheimu.naivemonitor.prometheus.PrometheusExporter">
        <constructor-arg index="0" >
            <list>
                <!-- RPC 客户端信息采集器 -->
                <bean class="com.heimuheimu.naiverpc.monitor.client.prometheus.RpcClientCompositePrometheusCollector">
                    <constructor-arg index="0">
                        <list>
                            <value>rpc-server-name, 127.0.0.1:4182, 127.0.0.1:4183, 127.0.0.1:4184</value> <!-- RPC 服务名称及 RPC 服务地址数组 -->
                        </list>
                    </constructor-arg>
                </bean>
            </list>
        </constructor-arg>
    </bean>
```

#### 3. 在 Prometheus 服务中配置对应的 Job，并添加以下规则：
```yaml
groups:
  # RPC 客户端报警规则配置
  - name: RpcClientAlerts
    rules:
      # RPC 客户端执行错误报警配置（不包括执行过慢）
      - alert: 'RpcClientExecutionError'
        expr: instance:naiverpc_client_exec_error_count:sum{errorType!~"SlowExecution"} > 0
        annotations:
          summary: "RpcClient 执行错误"
          description: "RpcClient 执行错误，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]，RPC 服务名称：[{{ $labels.name }}]，错误类型：[{{ $labels.errorType }}]"

      # RPC 客户端执行过慢报警配置
      - alert: 'RpcClientSlowExecution'
        expr: instance:naiverpc_client_exec_error_count:sum{errorType="SlowExecution"} > 3
        for: 2m
        annotations:
          summary: "RpcClient 执行过慢"
          description: "RpcClient 执行过慢，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]，RPC 服务名称：[{{ $labels.name }}]，错误类型：[{{ $labels.errorType }}]"

      # RPC 客户端使用的线程池繁忙报警配置
      - alert: 'RpcClientThreadPoolReject'
        expr: naiverpc_client_threadPool_reject_count > 0
        annotations:
          summary: "RpcClient 使用的线程池繁忙"
          description: "RpcClient 使用的线程池繁忙，主机地址：[{{ $labels.instance }}]，项目名称：[{{ $labels.job }}]"

  # RPC 客户端聚合记录配置
  - name: RpcClientRecords
    rules:
      # 相邻两次采集周期内 RPC 方法调用次数，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_exec_count:sum
        expr: sum(naiverpc_client_exec_count) by (job, name)

      # 相邻两次采集周期内每秒最大 RPC 方法调用次数，根据项目名称和 RPC 服务名称进行聚合计算（该值为估算值，实际值一般小于该估算值）
      - record: job:naiverpc_client_exec_peak_tps_count:sum
        expr: sum(naiverpc_client_exec_peak_tps_count) by (job, name)

      # 相邻两次采集周期内单次 RPC 方法调用最大执行时间，单位：毫秒，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_max_exec_time_millisecond:max
        expr: max(naiverpc_client_max_exec_time_millisecond) by (job, name)

      # 相邻两次采集周期内单次 RPC 方法调用平均执行时间，单位：毫秒，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_avg_exec_time_millisecond:avg
        expr: avg(naiverpc_client_avg_exec_time_millisecond) by (job, name)

      # 相邻两次采集周期内 RPC 方法调用失败次数（包含执行过慢），根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_exec_error_count:sum
        expr: sum(naiverpc_client_exec_error_count) by (job, name)

      # 相邻两次采集周期内 RPC 客户端使用的线程池拒绝执行的任务总数，根据项目名称进行聚合计算，不区分具体的 RPC 服务
      - record: job:naiverpc_client_threadPool_reject_count:sum
        expr: sum(naiverpc_client_threadPool_reject_count) by (job)

      # 相邻两次采集周期内 Socket 读取的字节总数，单位：MB，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_socket_read_megabytes:sum
        expr: sum(naiverpc_client_socket_read_bytes) by (job, name) / 1024 / 1024

      # 相邻两次采集周期内 Socket 写入的字节总数，单位：MB，根据项目名称和 RPC 服务名称进行聚合计算
      - record: job:naiverpc_client_socket_write_megabytes:sum
        expr: sum(naiverpc_client_socket_write_bytes) by (job, name) / 1024 / 1024

      # 相邻两次采集周期内 RPC 方法调用次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_exec_count:sum
        expr: sum(naiverpc_client_exec_count) by (job, instance, name)

      # 相邻两次采集周期内每秒最大 RPC 方法调用次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算（该值为估算值，实际值一般小于该估算值）
      - record: instance:naiverpc_client_exec_peak_tps_count:sum
        expr: sum(naiverpc_client_exec_peak_tps_count) by (job, instance, name)

      # 相邻两次采集周期内单次 RPC 方法调平均执行时间，单位：毫秒，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_avg_exec_time_millisecond:avg
        expr: avg(naiverpc_client_avg_exec_time_millisecond) by (job, instance, name)

      # 相邻两次采集周期内单次 RPC 方法调用最大执行时间，单位：毫秒，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_max_exec_time_millisecond:max
        expr: max(naiverpc_client_max_exec_time_millisecond) by (job, instance, name)

      # 相邻两次采集周期内特定类型 RPC 方法调用失败次数，根据项目名称、主机地址、RPC 服务名称和错误类型进行聚合计算
      - record: instance:naiverpc_client_exec_error_count:sum
        expr: sum(naiverpc_client_exec_error_count) by (job, instance, name, errorType)

      # 相邻两次采集周期内 Socket 读取的次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_read_count:sum
        expr: sum(naiverpc_client_socket_read_count) by (job, instance, name)

      # 相邻两次采集周期内 Socket 读取的字节总数，单位：MB，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_read_megabytes:sum
        expr: sum(naiverpc_client_socket_read_bytes) by (job, instance, name) / 1024 / 1024

      # 相邻两次采集周期内单次 Socket 读取的最大字节数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_max_read_bytes:max
        expr: max(naiverpc_client_socket_max_read_bytes) by (job, instance, name)

      # 相邻两次采集周期内 Socket 写入的次数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_write_count:sum
        expr: sum(naiverpc_client_socket_write_count) by (job, instance, name)

      # 相邻两次采集周期内 Socket 写入的字节总数，单位：MB，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_write_megabytes:sum
        expr: sum(naiverpc_client_socket_write_bytes) by (job, instance, name) / 1024 / 1024

      # 相邻两次采集周期内单次 Socket 写入的最大字节数，根据项目名称、主机地址和 RPC 服务名称进行聚合计算
      - record: instance:naiverpc_client_socket_max_write_bytes:max
        expr: max(naiverpc_client_socket_max_write_bytes) by (job, instance, name)
```

  完成以上工作后，在 Prometheus 系统中即可找到以下监控指标：
* RPC 客户端执行信息指标：
  * naiverpc_client_exec_count{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法调用次数
  * naiverpc_client_exec_peak_tps_count{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内每秒最大 RPC 方法调用次数
  * naiverpc_client_avg_exec_time_millisecond{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 RPC 方法调用平均执行时间，单位：毫秒
  * naiverpc_client_max_exec_time_millisecond{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 RPC 方法调用最大执行时间，单位：毫秒
* RPC 客户端执行错误信息指标：
  * naiverpc_client_exec_error_count{errorCode="-1",errorType="Timeout",name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法调用出现执行超时的错误次数
  * naiverpc_client_exec_error_count{errorCode="-2",errorType="TooBusy",name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法调用出现 RPC 服务端繁忙的错误次数
  * naiverpc_client_exec_error_count{errorCode="-3",errorType="InvocationError",name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法调用出现异常的错误次数
  * naiverpc_client_exec_error_count{errorCode="-4",errorType="SlowExecution",name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 RPC 方法调用出现执行过慢的错误次数
* RPC 客户端 Socket 读、写信息指标：
  * naiverpc_client_socket_read_count{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 读取的次数
  * naiverpc_client_socket_read_bytes{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 读取的字节总数
  * naiverpc_client_socket_max_read_bytes{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 Socket 读取的最大字节数
  * naiverpc_client_socket_write_count{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 写入的次数
  * naiverpc_client_socket_write_bytes{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内 Socket 写入的字节总数
  * naiverpc_client_socket_max_write_bytes{name="rpc-server-name",remoteAddress="127.0.0.1:4182"} &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内单次 Socket 写入的最大字节数
* RPC 客户端使用的线程池信息指标：
  * naiverpc_client_threadPool_reject_count &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内监控器中所有线程池拒绝执行的任务总数
  * naiverpc_client_threadPool_active_count &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻监控器中的所有线程池活跃线程数近似值总和
  * naiverpc_client_threadPool_pool_size &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻监控器中的所有线程池线程数总和
  * naiverpc_client_threadPool_peak_pool_size &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池出现过的最大线程数总和
  * naiverpc_client_threadPool_core_pool_size &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池配置的核心线程数总和
  * naiverpc_client_threadPool_maximum_pool_size &nbsp;&nbsp;&nbsp;&nbsp; 监控器中的所有线程池配置的最大线程数总和
* RPC 客户端压缩操作信息指标：
  * naiverpc_client_compression_count &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内已执行的压缩次数
  * naiverpc_client_compression_reduce_bytes &nbsp;&nbsp;&nbsp;&nbsp; 相邻两次采集周期内通过压缩节省的字节总数
  
  通过 util-grafana 项目可以为 RPC 客户端监控指标快速生成 Grafana 监控图表，项目地址：[https://github.com/heimuheimu/util-grafana](https://github.com/heimuheimu/util-grafana)
  
### Falcon 监控系统数据采集
#### 1. 在 Spring 中配置 Falcon 数据推送：
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

  完成以上工作后，在 Falcon 系统中可以找到以下数据项：
* RPC 客户端方法调用错误数据项：
  * naiverpc_client_{groupName}_too_busy/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 服务繁忙的错误次数
  * naiverpc_client_{groupName}_timeout/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生超时的错误次数
  * naiverpc_client_{groupName}_error/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生异常的错误次数
  * naiverpc_client_{groupName}_slow_execution/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 调用发生的慢执行次数
* RPC 客户端方法调用数据项： 
  * naiverpc_client_{groupName}_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数
  * naiverpc_client_{groupName}_peak_tps/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数
  * naiverpc_client_{groupName}_avg_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用平均执行时间
  * naiverpc_client_{groupName}_max_exec_time/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 RPC 调用最大执行时间
* RPC 客户端 Socket 数据项： 
  * naiverpc_client_{groupName}_socket_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数
  * naiverpc_client_{groupName}_socket_avg_read_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数
  * naiverpc_client_{groupName}_socket_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数
  * naiverpc_client_{groupName}_socket_avg_written_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数
* RPC 客户端线程池数据项： 
  * naiverpc_client_threadPool_rejected_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内所有线程池拒绝执行的任务总数
  * naiverpc_client_threadPool_active_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池活跃线程数近似值总和
  * naiverpc_client_threadPool_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池线程数总和
  * naiverpc_client_threadPool_peak_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池出现过的最大线程数总和
  * naiverpc_client_threadPool_core_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的核心线程数总和
  * naiverpc_client_threadPool_maximum_pool_size/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的最大线程数总和
* RPC 客户端压缩数据项： 
  * naiverpc_client_compression_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内压缩操作已节省的字节数
  * naiverpc_client_compression_avg_reduce_bytes/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内平均每次压缩操作节省的字节数
* RPC 集群客户端数据项： 
  * naiverpc_client_cluster_unavailable_client_count/module=naiverpc &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 RPC 集群客户端获取到不可用 RPC 客户端的次数

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
### V1.2-SNAPSHOT
### 新增特性：
 * 支持将监控数据推送至 Prometheus 监控系统。

***

### V1.1
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
* [NaiveRPC v1.1 API Doc](https://heimuheimu.github.io/naiverpc/api/v1.1/)
* [NaiveRPC v1.1 源码下载](https://heimuheimu.github.io/naiverpc/download/naiverpc-1.1-sources.jar)
* [NaiveRPC v1.1 Jar包下载](https://heimuheimu.github.io/naiverpc/download/naiverpc-1.1.jar)