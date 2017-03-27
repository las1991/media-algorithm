#ifndef __CONVERTJPG__H
#define __CONVERTJPG__H

typedef struct OUTDATA
{
    int   width;
    int   height;
	char* data;
	int   size;
}OUTDATA;

int Init(void (*log_callback )(int level, char* ));

int DecodeNal(char* data_buffer, int len, char* token, OUTDATA* outdata);

int Destroy(OUTDATA* outdata);

#endif
