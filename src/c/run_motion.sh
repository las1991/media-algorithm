#! /bin/sh
basepath=$(cd `dirname $0`; pwd)
cd ${basepath}/motion
chmod +x run.sh
sh ./run.sh
