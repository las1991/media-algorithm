CURRENT_DIR :=$(shell pwd)
CP=cp -rf
RM=rm -rf
all:
	make -C ./src 
	$(CP) $(CURRENT_DIR)/src/lib*.so $(CURRENT_DIR)/../lib		
	$(CP) $(CURRENT_DIR)/src/*.h $(CURRENT_DIR)/../include		
	make -C ./test 
clean:
	make -C ./src  clean
	make -C ./test  clean
