#ifndef __YUVENCODER__H
#define __YUVENCODER__H

typedef struct JPGFrame
{
    char* data;
    int   size;
}JPGFrame;

int Init(void (*log_callback )(int level, char* ));

int EncodeJPG(char* data_buffer, int src_width, int src_height, int dst_width, int dst_height, const char* token, JPGFrame* jpg_frame);

int Destroy(JPGFrame* jpg_frame);

#endif
