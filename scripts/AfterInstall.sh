#!/bin/sh

target_dir="/opt/sengled/apps"
basepath=$(cd `dirname $0`; pwd)

################ install py4j ##################
mkdir -p /tmp/py4j
cd /tmp/py4j

cp $basepath/../content/py4j-0.10.3.tar.gz ./
tar -zxvf py4j-0.10.3.tar.gz 
cd py4j-0.10.3
python setup.py build
python setup.py install

cd ${basepath}
################## END ########################


exit 0