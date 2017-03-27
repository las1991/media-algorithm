#!/bin/sh

project_name='media-algorithm-v3'

ps -ef|grep consul-template | grep -v grep | grep sengled.properties.ctmpl | awk '{print $2}' | xargs kill -9 >/dev/null 2>&1

basepath=$(cd `dirname $0`; pwd)
${basepath}/../content/${project_name}/bin/stop-all.sh


exit 0
