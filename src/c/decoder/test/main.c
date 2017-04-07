#include<stdio.h>
#include<nal_decoder.h>
#include<stdarg.h>
#include<stdlib.h>
#include<sys/resource.h>
#include<stdlib.h>
#include<memory.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/time.h>
typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
typedef unsigned long long uint64_t;
void log_callback(int level, char* buf)
{
    printf("%s\n", buf);
}

void debugCore()
{
    struct rlimit r;
    if(getrlimit(RLIMIT_CORE, &r) < 0)
    {
        printf("getrlimit error\n");
        return ;
    }
    r.rlim_cur = -1;
    r.rlim_max = -1;
    if(setrlimit(RLIMIT_CORE, &r) < 0)
    {
        printf("setrlimit error\n");
        return ;
    }
}

int main(int argc, char* argv[])
{
    YUVFrame outdata;
   //outdata.data = malloc(100*1024);
    //char dst_jpg[100*1024];
    int dst_size;
    //char h264_data[] = "/home/pang/git/media-algorithm-v3/python/decode/176_144_1490260801604";
    char h264_data[] = "data";
    char token[] = "ABCDEFG";
    FILE* fp = NULL;
    int file_size;
    int file_ret;
    char* data_buffer = NULL;
    debugCore();
    fp = fopen(h264_data, "r");
    if(!fp)
    {
        fprintf(stderr, "It is fail to open h264_data file %s.\n", h264_data);
        return -1;
    }
    printf("open data file ok\n");
    fseek(fp, 0, SEEK_END);
    file_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    data_buffer = (uint8_t *) malloc(file_size);
    //metadata_len = file_size;
    file_ret = fread(data_buffer, sizeof(char), file_size, fp);
    if (file_ret != file_size) 
    {
        fprintf(stderr, "sdp file read is error.\n");
        return -1;
    }
    Init(log_callback);
    int i = 0;

    static int number = 0;
    char jpg_name[1024];
    char jpg[] = "./";

    for(;; i++)
    {
        //sleep(1);
        DecodeNal(data_buffer, file_size, token, &outdata);
        
        number++;
        sprintf(jpg_name, "%s%s_%d.yuv", jpg, token, number);
        fp = fopen(jpg_name, "wr");
        fwrite(outdata.data, 1, outdata.size, fp);
        fclose(fp);
        
        Destroy(&outdata);
    }

    return 0;
}

