CURRENT_DIR:=$(shell pwd)
#LD_LIB= $(CURRENT_DIR)/../../install_md/lib/libavfilter.a	$(CURRENT_DIR)/../../install_md/lib/libavformat.a $(CURRENT_DIR)/../../install_md/lib/libavcodec.a $(CURRENT_DIR)/../../install_md/lib/libavutil.a $(CURRENT_DIR)/../../install_md/lib/libswresample.a $(CURRENT_DIR)/../../install_md/lib/libswscale.a
LD_LIB=	$(CURRENT_DIR)/../../3rd/install/lib/libavformat.a $(CURRENT_DIR)/../../3rd/install/lib/libavcodec.a $(CURRENT_DIR)/../../3rd/install/lib/libavutil.a $(CURRENT_DIR)/../../3rd/install/lib/libswresample.a $(CURRENT_DIR)/../../3rd/install/lib/libswscale.a 
LD_INCLUDE=$(CURRENT_DIR)/../../3rd/install/include
FILE_OUT=libdecoder.so
CC=gcc -g
CC_OUT=gcc -o
CC_SHARE_OUT=gcc -fPIC -shared -o 
AR=ar crs
CP=cp -rf
CC_FLAGS= -fPIC -I$(LD_INCLUDE)
LD_FLAGS= -Wl,-Bsymbolic #$(LD_LIB)  
LD_FLAGS1=$(LD_LIB)  -lm -lz -lpthread #-L/usr/lib/x86_64-linux-gnu/
CC_DEPFLAGS=-MMD -MF $(@:.o=.d) -MT $@
all:$(FILE_OUT)  
FILE_C+=$(wildcard *.c)
FILE_O=$(patsubst %.c,%.o,$(FILE_C))
FILE_D=$(patsubst %.c,%.d,$(FILE_C))

%.o:%.c
	$(CC) $(CC_FLAGS) $(CC_DEPFLAGS) -c $< -o $@

$(FILE_OUT):$(FILE_O)
	$(CC_SHARE_OUT) $(FILE_OUT) $(FILE_O) $(LD_FLAGS1) $(LD_FLAGS)
	#$(CP) *.h ../include
	#$(CP) $(FILE_OUT) ../lib

#$(BIN):$(FILE_O)
#	$(CC_OUT) $(BIN) $(FILE_O) $(LD_FLAGS1)
#	$(CP) $(BIN) ../demo
clean:
	rm -f $(FILE_O) $(FILE_D) $(FILE_OUT) 
