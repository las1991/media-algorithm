#! /bin/sh

# find gfortran builder
#if [ ! -f /usr/bin/gfortran ]; 
#then
#	echo "Need install gfortran ..."
#	exit
#fi

# build project
if [ -d build ];
then
	cd build
	rm -rf *
	cmake ..
	make -j 4
else
	mkdir build
	cd build
	cmake ..
	make -j 4
fi
