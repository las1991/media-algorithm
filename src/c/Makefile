CURRENT_DIR :=$(shell pwd)
CP=cp -rf
RM=rm -rf
all:
	$(RM) include/* lib/*		
	chmod +x ./3rd/build.sh
	sh ./3rd/build.sh
	sh ./run_motion.sh
	$(CP) ./motion/build/lib/* lib/*
	make -C ./encoder 
	make -C ./decoder
clean:
	make -C ./encoder  clean
	make -C ./decoder  clean
