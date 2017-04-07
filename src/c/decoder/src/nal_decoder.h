#ifndef __CONVERTJPG__H
#define __CONVERTJPG__H
#include <yuv.h>

int Init(void (*log_callback )(int level, char* ));

int DecodeNal(char* data_buffer, int len, const char* token, YUVFrame* yuv_frame);

int Destroy(YUVFrame* yuv_frame);

#endif
