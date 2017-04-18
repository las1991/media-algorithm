#ifndef __YUVENCODER__H
#define __YUVENCODER__H

#include <yuv.h>
#include <log.h>

typedef struct JPGFrame
{
    char* data;
    int   size;
}JPGFrame;

void SetLogCallback(void* callback);

int Init();

int EncodeJPG(const YUVFrame* yuv_frame, int dst_width, int dst_height, const char* token, JPGFrame* jpg_frame);

int Destroy(JPGFrame* jpg_frame);

#endif
