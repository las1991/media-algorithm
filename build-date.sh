#!/bin/sh

basepath=$(cd `dirname $0`; pwd)
cd src/java/algorithm-assembly/
currdate=`date "+%Y-%m-%d %H:%M:%S"`
echo "build date : "$currdate >>release_note