#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#    http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ $# -lt 1 ];
then
	echo "USAGE: $0 [-daemon] [-loggc] [-name DAEMON_NAME]"
	exit 1
fi

SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)

if [ "x$JMEDIA_HEAP_OPTS" = "x" ]; then
    export JMEDIA_HEAP_OPTS="-Xmx2G -Xms512M"
fi

echo "
# server work mode
server.mode=clust
">$SENGLED_APP_HOME/config/server.properties



for i in `find ${SENGLED_APP_HOME}/config  -maxdepth 1 -name "*.properties"`
do
        CONFIGS="file:"$i",$CONFIGS"
done


export JMEDIA_OPTS=" -Dserver.name=media-algorithm-v3 -Dspring.config.location=${CONFIGS}file:/etc/sengled/sengled.properties"
export JMEDIA_OPTS=" -Djni.library.path=$SENGLED_APP_HOME/libc -Djna.library.path=$SENGLED_APP_HOME/libc $JMEDIA_OPTS"
export LD_LIBRARY_PATH=$SENGLED_APP_HOME/libc:$LD_LIBRARY_PATH


EXTRA_ARGS="-name media-algorithm-v3 -loggc "
COMMAND=$1
case $COMMAND in
  -daemon)
    EXTRA_ARGS="-daemon "$EXTRA_ARGS
    shift
    ;;
  *)
    ;;
esac

exec $SENGLED_APP_HOME/bin/run-class.sh $EXTRA_ARGS com.sengled.mediaworker.AlgorithmServer $@
