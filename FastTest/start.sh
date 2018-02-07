#!/bin/bash

trap 'onCtrlC' INT
function onCtrlC () {
	for id in ${proc_id_RaftServer[*]}
	     do
		kill -9 ${id}
		echo "kill Raft Server pid "${id}
	     done

	for id in ${proc_id_KVServer[*]}
	     do
		kill -9 ${id}
		echo "kill KV Server pid "${id}
	     done
    exit 0
}

nohup java -jar RaftServer.jar RaftServer2.xml 1>/dev/null 2>/dev/null &
echo "init Raft Server1 port:50000"
sleep 4s
nohup java -jar RaftServer.jar RaftServer1.xml 1>/dev/null 2>/dev/null &
echo "init Raft Server2 port:50001"
sleep 4s
nohup java -jar KVServer.jar KVServer1.xml 1>/dev/null 2>/dev/null &
echo "init KV Server1 port:30000 shard 0"
sleep 2s
nohup java -jar KVServer.jar KVServer2.xml 1>/dev/null 2>/dev/null &
echo "init KV Server2 port:30001 shard 1"
sleep 2s

proc_name="RaftServer.jar"
proc_name2="KVServer.jar"

name_suffixx="\>"

proc_id_RaftServer=`ps -ef|grep -i ${proc_name}${name_suffixx}|grep -v "grep"|awk '{print $2}'`
proc_id_KVServer=`ps -ef|grep -i ${proc_name2}${name_suffixx}|grep -v "grep"|awk '{print $2}'`


java -jar KVClient.jar KVClient.xml
