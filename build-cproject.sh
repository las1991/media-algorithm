#!/bin/sh


basepath=$(cd `dirname $0`; pwd)
cd src/c
make

cd $basepath
cp  $basepath/src/c/lib/*  $basepath/src/java/algorithm-assembly/libc/linux-x86-64