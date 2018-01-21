这是一个基于raft协议的分布式内存KV数据库

原先准备将raft层和kv层放在同一个进程中,去除kafka这样需要依赖zookeeper的特性.
但是实际实现起来有各种问题,主要问题因为raft的append Entry需要有一个client去主动调用,
如果KV Server和作为leader的raft一起挂掉.那么无法确定raft的log是否成功commit.

作为解决方法,只能回到原点,模仿kafka的做法.将raft的同步剥离出来作为一个独立的服务.

因此这个项目最后会导出四个jar包  
1.EasyRaft.jar作为Raft的Server  
2.EasyRaftClient.jar作为Raft的Client  
2.KVServer.jar作为KV数据库的Server  
3.KVClient.jar作为KV数据库的Client,可以给其他的client程序调用  

可以支持Repilca,但一旦使用了Replica无法保证强一致性(Strong Consistence).
关于无法保证强一致性的问题,可以参考Redis Cluster对强一致性的讨论.

KVServer如何使用EasyRaft?
KVServer的配置文件会要求用户写明开放给KVClient的端口.
KVServer调用RaftClient的joinCluster的API,该API在Raft的Log中写下join:Ip:Port的日志后返回.
根据在Log中留下日志的顺序,KVServer按照相同的顺序在用户配置的端口上开Server服务.例如现在有两条日志
join:192.168.1.102:55564
join:192.168.1.105:55570
用户配置的端口[12000,12001]  
那么192.168.1.102:55564所在的进程会自动在12000端口上开启KV服务
    192.168.1.105:55570所在的进程会自动在12001端口上开启KV服务
    
按照这条规则,KV用户所在的进程在得到log后也知道,192.168.1.102:12000端口上有KV服务,192.168.1.105:12001端口上
也有KV服务

集群如何感知KVServer的crash并作出反映.
这里就引入Leader的概念了,先写入Raft日志的server,默认为Leader.在上述例子中就是192.168.1.102:55564
当一台KVServer被Raft提醒Crash了之后,
如果Crash的Server不是Leader,那么由Leader写入Leave:{Crash Ip}:{Crash Port}
如果Crash的Server是Leader,那么由Leader后一个Ip:Port所在的进程写入Leave:{Crash Ip}:{Crash Port},
并且后面一个Ip自动晋升为Leader

如果所有的KV Server同时一起挂掉了?
每一次新的Server加入Cluster都会得到当前cluster存活的Ip:Port以及所有的日志,这样就可以推断出哪些server crash的
日志没有及时加入.集群中只要有存活的机器就会使得日志正确.

快速使用: