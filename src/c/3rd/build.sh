#!/bin/bash
basepath=$(cd `dirname $0`; pwd)
cd ${basepath}/ffmpeg

make clean
make distclean
#--extra-cflags="-fvisibility=hidden" 
./configure  --prefix="./../install" --enable-gpl  --disable-shared --enable-static --enable-nonfree --enable-pic  --enable-decoder=h264 --enable-swscale  --enable-swresample --disable-optimizations --disable-stripping  --enable-debug

make -j4 install


