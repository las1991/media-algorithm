#ifndef __CONVERTJPG__H
#define __CONVERTJPG__H

typedef struct YUVFrame
{
    int   width;
    int   height;
	char* data;
	int   size;
}YUVFrame;

int Init(void (*log_callback )(int level, char* ));

int DecodeNal(const char* data_buffer, int len, const char* token, YUVFrame* yuv_frame);

int Destroy(YUVFrame* yuv_frame);

#endif
