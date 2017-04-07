CURRENT_DIR:=$(shell pwd)
#LD_LIB= $(CURRENT_DIR)/../../install_md/lib/libavfilter.a	$(CURRENT_DIR)/../../install_md/lib/libavformat.a $(CURRENT_DIR)/../../install_md/lib/libavcodec.a $(CURRENT_DIR)/../../install_md/lib/libavutil.a $(CURRENT_DIR)/../../install_md/lib/libswresample.a $(CURRENT_DIR)/../../install_md/lib/libswscale.a
#LD_LIB1=	$(CURRENT_DIR)/../lib/libavformat.a $(CURRENT_DIR)/../lib/libavcodec.a $(CURRENT_DIR)/../lib/libavutil.a $(CURRENT_DIR)/../lib/libswresample.a 
LD_INCLUDE=$(CURRENT_DIR)/../../include
LD_LIB=$(CURRENT_DIR)/../../lib
COMMON_INCLUDE=$(CURRENT_DIR)/../../common
#FILE_OUT=librtpdecoder.so
BIN=decode
CC=gcc -g
CC_OUT=gcc -o
CP=cp -rf
CC_FLAGS= -fPIC -I$(LD_INCLUDE) -I$(COMMON_INCLUDE)
LD_FLAGS= -L$(LD_LIB) -lnal_decoder # -lm -lz -lpthread#-L/usr/lib/x86_64-linux-gnu/
#LD_FLAGS += -Wl,-rpath,$(LD_LIB)
CC_DEPFLAGS=-MMD -MF $(@:.o=.d) -MT $@
all:$(BIN)  
FILE_C+=$(wildcard *.c)
FILE_O=$(patsubst %.c,%.o,$(FILE_C))
FILE_D=$(patsubst %.c,%.d,$(FILE_C))

%.o:%.c
	$(CC) $(CC_FLAGS) $(CC_DEPFLAGS) -c $< -o $@

$(BIN):$(FILE_O)
	$(CC_OUT) $(BIN) $(FILE_O) $(LD_FLAGS)
clean:
	rm -f $(FILE_O) $(FILE_D) $(FILE_OUT) $(BIN)
