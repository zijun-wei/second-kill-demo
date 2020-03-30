# second-kill-demo

## 介绍
本项目为秒杀想法验证，项目的基础功能实现由**Younjzxx**完成。在基础功能之上，我针对查询接口，秒杀接口做了进一步性能优化。经过检测查询接口部署在两台4核8G内存的服务器上，通过一台带宽为20M的Nginx服务器反向代理，QPS可超过4000；秒杀购买接口Q2.218PS可达到2000左右（演示项目服务器为1核2G，带宽为1M,所以QPS在200左右；秒杀接口利用了验证服务，无法准确测量）。


***演示项目地址：http://47.95.36.145/resources/getotp.html***

## 软件架构
### 查询服务：   
针对查询服务，由于其主要信息短时间内变化不大的特点优化：  
业务处理服务器：避免数据库查询操作带来性能与时间方面的消耗，可以使用**Guava Cache，Caffeine**等内存工具（ConcurrentHashMap使用应该也是可以的），将数据信息存入Java内存结构当中，当然分布式结构中，我们需要数据的一致性，可以使用**Redis**服务器集群来完成。
Nginx服务器端：为了避免Nginx向下游处理服务器发送请求等时间性能上的消耗，我们可以在Nginx服务器端也维持一个缓存空间（Shared dic），但有的时候同样是为了数据的一致性等问题，我们可能需要牺牲一点点性能，将Redis替代Shared dic（*ps：本人基于openresty，编写的lua脚本以共享在docs文件中*）

*本案例中假设商品的相关信息不经常变（注意商品库存等修改次数较多信息可放入另一张表中）*

### 秒杀服务   
秒杀服务中由于MySQL（InnoDB）的行锁关系，对同一件商品在数据库中做修改会降低，所以除了对于一些常用，不经常改变信息需要缓存进入Redis之外，可以利用Redis维护用户存库等内容的临时信息。

异步操作：利用RocketMq的事务性完成库存等信息的异步写入（注意订单创建操作不能异步化，原因：订单之后会进行付完款等操作，这些操作能够进行的基础就是订单存在）。

流量削峰操作：对于大量流量涌入，为了避免秒杀接口瘫痪（毕竟是核心功能）需要对其进行流量削峰操作。本样例提供一种简易的削峰操作模型，利用自制简易令牌机制完成削峰，利用线程池以及其等待队列完成流量的泄洪操作。

防刷机制：为了避免刷单的存在，我才用了一个最简单的办法，交易时加入验证码。


## 安装教程

环境需求：  
java（jdk8+）  
Maven  
RocketMq组件
Redis
MySQL

（本次项目中使用的是application.properties文件完成配置，同时使用了Mybatis逆向工程实现持久层代码）


## 参与贡献

感谢 My roommate——***Younjzxx***提供的基础功能及前端代码。
