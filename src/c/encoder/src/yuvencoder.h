#ifndef __YUVENCODER__H
#define __YUVENCODER__H

typedef struct OUTDATA
{
	char* data;
	int   size;
}OUTDATA;

int Init(void (*log_callback )(int level, char* ));

int EncodeJPG(char* data_buffer, int src_width, int src_height, int dst_width, int dst_height, char* token, OUTDATA* outdata);

int Destroy(OUTDATA* outdata);

#endif
