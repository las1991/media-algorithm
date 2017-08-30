#ifndef __YUV__H
#define __YUV__H

typedef struct YUVFrame
{
    int   width;
    int   height;
    char* data[2];
    int   size[2];
}YUVFrame;

#endif
