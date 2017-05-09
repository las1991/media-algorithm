#!/bin/sh
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

SENGLED_APP_HOME=$(cd `dirname $0`/..; pwd)
ps ax | grep -i  media-algorithm-v3 | grep java | grep -v grep | awk '{print $1}' | xargs kill -SIGTERM

echo -n "media-algorithm-v3 Stopping ."
for i in {1..20}
do
  if [[ $(jps | grep AlgorithmServer | wc -l) -lt 1 ]];then
    echo  -e ". STOPPED\n"
    break
  else
    echo -n "."
    sleep 1
  fi
done

