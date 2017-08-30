#ifndef __NALDECODER__H
#define __NALDECODER__H
#include <yuv.h>

void SetLogCallback(void* callback);

int Init();

int DecodeNal(char* data_buffer, int len, const char* token, YUVFrame2* yuv_frame);

int Destroy(YUVFrame2* yuv_frame);

#endif
