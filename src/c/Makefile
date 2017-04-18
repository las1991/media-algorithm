CURRENT_DIR :=$(shell pwd)
CP=cp -rf
RM=rm -rf
all:
	$(RM) include/* lib/*		
	chmod +x ./3rd/build.sh
	#sh ./3rd/build.sh
	sh ./run_motion.sh
	$(CP) motion/build/lib/*.so lib/
	make -C ./encoder 
	make -C ./decoder
	rm -rf test/python/clib/*.so
	cp lib/*.so test/python/clib/
	#python ./test/python/test.py --repo ./test/python/data/
   
clean:
	make -C ./encoder  clean
	make -C ./decoder  clean
