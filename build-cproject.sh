#!/bin/sh


basepath=$(cd `dirname $0`; pwd)
cd src/c
make

cd $basepath
cd src/c/log4c
sh build.sh


cd $basepath
cp  $basepath/src/c/lib/*  $basepath/src/java/screenshot-assembly/libc/
cp  $basepath/src/c/log4c/*.so  $basepath/src/java/screenshot-assembly/libc/