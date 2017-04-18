#ifndef __CONVERTJPG__H
#define __CONVERTJPG__H
#include <yuv.h>

void SetLogCallback(void* callback);

int Init();

int DecodeNal(char* data_buffer, int len, const char* token, YUVFrame* yuv_frame);

int Destroy(YUVFrame* yuv_frame);

#endif
