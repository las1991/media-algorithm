#!/bin/bash
basepath=$(cd `dirname $0`; pwd)
cd ${basepath}/ffmpeg
chmod +x configure
chmod +x *.sh
make clean
make distclean
#--extra-cflags="-fvisibility=hidden" 
./configure  --prefix="./../install" --enable-gpl  --disable-shared --enable-static --enable-nonfree --enable-pic  --enable-decoder=h264 --enable-swscale  --enable-swresample --disable-optimizations --disable-stripping  --enable-debug --disable-podpages --disable-doc

make -j4 install


