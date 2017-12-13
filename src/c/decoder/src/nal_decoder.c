#include <libavformat/avformat.h>
//#include <libswscale/swscale.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

#include "log.h"
#include "nal_decoder.h"
#include "analyse_frame.h"
void (*pp_log_callback)(int level, char* ptr) = NULL;

static int log_convert(int level)
{
    int ret;
    switch(level)
    {
    case AV_LOG_DEBUG:
        ret = SLS_LOG_DEBUG;
        break;
    case AV_LOG_INFO:
        ret = SLS_LOG_INFO;
        break;
    case AV_LOG_WARNING:
        ret = SLS_LOG_WARNING;
        break;
    case AV_LOG_ERROR:
        ret = SLS_LOG_ERROR;
        break;
    case AV_LOG_FATAL:
        ret = SLS_LOG_FATAL;
        break;
    default:
        ret = SLS_LOG_DEBUG;
        break;
    }
    return ret;
}

static void log_default_callback(void* ptr, int level, const char* fmt, va_list vl)
{
    char log_buf[2048];
    memset(log_buf, 0, sizeof(log_buf));
    int out_level;
    out_level = log_convert(level);
    
    vsnprintf(log_buf, sizeof(log_buf), fmt, vl);
    if(pp_log_callback != NULL)
        pp_log_callback(out_level, log_buf);
}

void SetLogCallback(void* log_callback)
{
    pp_log_callback = log_callback;
    av_log_set_callback(log_default_callback);
}


int Init()
{
    av_register_all();
    return 0;
}

#define DECODER_NUM  50
typedef struct InputParams
{
    char        token[64];
    int         width;
    int         height;
    char*       data_bufer;
    int         data_size;
    char        src_url[128];
}InputParams;

typedef struct VideoDecodeCtx
{
    InputParams*     params;

    AVFormatContext* fmt;
    AVCodecContext*  decoder;

    int              width;
    int              height;

    uint8_t*         nal_data;
    uint8_t*         nal_data_ptr;
    int              nal_data_len;

    uint8_t*         nal_buffer;
    uint8_t*         nal_buffer_ptr;
    int              nal_buffer_len;

}VideoDecodeCtx;

typedef struct SLCustomBufferPointer_tag {
    uint8_t *ptr;
    size_t size;
} SLCustomBufferPointer;

static int CustomReadNals(void* opaque, uint8_t* buf, int buf_size)
{
    VideoDecodeCtx* dctx = (VideoDecodeCtx* )opaque;

    buf_size = FFMIN(buf_size, dctx->nal_data_len);

    memcpy(buf, dctx->nal_data, buf_size);
    dctx->nal_data += buf_size;
    dctx->nal_data_len -= buf_size;
    av_log(NULL, AV_LOG_DEBUG, "read size = %d, left = %d\n", buf_size, dctx->nal_data_len);
    return buf_size;
}

static int CreateDecoderCtxAVIO(VideoDecodeCtx* decodectx)
{
    int ret = 0;
    AVDictionary *opts = NULL;
    AVCodecContext *context = NULL;
    AVCodec *codec = NULL;
    AVIOContext* nal_ioc = NULL;

    decodectx->fmt = avformat_alloc_context();
    if(decodectx->fmt == NULL){
        av_log(NULL, AV_LOG_ERROR, "avformat alloc context failed! token = %s\n", decodectx->params->token);
        return -1;
    }

    SLCustomBufferPointer bd = { 0 };
    decodectx->nal_buffer_len = 4096;
    bd.ptr  = decodectx->nal_data;
    bd.size = decodectx->nal_data_len;
    decodectx->nal_buffer = av_mallocz(decodectx->nal_buffer_len);
    decodectx->nal_buffer_ptr = decodectx->nal_buffer;

    nal_ioc = avio_alloc_context(decodectx->nal_buffer, decodectx->nal_buffer_len, 0, decodectx, &CustomReadNals, NULL, NULL);
    if(nal_ioc == NULL){
        av_log(NULL, AV_LOG_ERROR, "avio alloc context failed! token = %s\n", decodectx->params->token);
        goto err;
    }

    decodectx->fmt->pb = nal_ioc;

    if(avformat_open_input(&(decodectx->fmt), NULL, NULL, NULL) < 0){
        av_log(NULL, AV_LOG_ERROR, "Cannot open input file, token = %s\n", decodectx->params->token);
        goto err;
    }
    else
        av_log(NULL, AV_LOG_DEBUG, "Open input file ok, token = %s\n", decodectx->params->token);

    codec = avcodec_find_decoder_by_name("h264");
    if (!codec){
        av_log(NULL, AV_LOG_ERROR, "video decoder AVCodec is not be fould, token = %s", decodectx->params->token);
        goto err;
    }
    context = avcodec_alloc_context3(codec);
    if (!context){
        av_log(NULL, AV_LOG_ERROR, "video decoder AVCodecContext alloc no succss, token = %s", decodectx->params->token);
        goto err;
    }

    if (!av_dict_get(opts, "threads", NULL, 0))
        av_dict_set(&opts, "threads", "1", 0);
    if (context->codec_type == AVMEDIA_TYPE_VIDEO || context->codec_type == AVMEDIA_TYPE_AUDIO)
        av_dict_set(&opts, "refcounted_frames", "1", 0);

    context->codec_type = codec->type;
    context->codec_id = codec->id;
    context->width = decodectx->width;
    context->height = decodectx->height;

    context->flags |= CODEC_FLAG_GLOBAL_HEADER;

    ret = avcodec_open2(context, codec, &opts);
    if(ret < 0){
        av_log(NULL, AV_LOG_ERROR, "video decoder avcodec open failed, token = %s", decodectx->params->token);
        goto err;
    }
    decodectx->decoder = context;
    av_log(NULL, AV_LOG_DEBUG, "init video decoder ok, token = %s", decodectx->params->token);

    return 0;
err:
    if(context)
        avcodec_free_context(&context);
    if(decodectx->fmt){
        if(decodectx->fmt->pb) {
            av_freep(&decodectx->fmt->pb->buffer);
            av_freep(&decodectx->fmt->pb);
        }
        avformat_close_input(&(decodectx->fmt));
        decodectx->fmt = NULL;
    }
    av_log(NULL, AV_LOG_ERROR, "init video decoder failed! token = %s", decodectx->params->token);
    return -1;
}

static int CreateDecoderCtxURL(VideoDecodeCtx* decodectx, const char* src_url)
{
    int ret = 0;
    AVDictionary *opts = NULL;
    AVCodecContext *context = NULL;
    AVCodec *codec = NULL;

    decodectx->fmt = avformat_alloc_context();
    if(decodectx->fmt == NULL){
        av_log(NULL, AV_LOG_ERROR, "avformat alloc context failed! token = %s\n", decodectx->params->token);
        return -1;
    }

    if(avformat_open_input(&(decodectx->fmt), src_url, NULL, NULL) < 0){
        av_log(NULL, AV_LOG_ERROR, "Cannot open input file, token = %s\n", decodectx->params->token);
        goto err;
    }
    else
        av_log(NULL, AV_LOG_DEBUG, "Open input file ok, token = %s\n", decodectx->params->token);

    if(avformat_find_stream_info(decodectx->fmt, NULL) < 0){
        avformat_close_input(&decodectx->fmt);
        av_log(NULL, AV_LOG_ERROR, "Cannot find stream information, token = %s\n", decodectx->params->token);
        goto err;
    }
    else
        av_log(NULL, AV_LOG_DEBUG, "find stream information ok, token =  %s\n", decodectx->params->token);

    codec = avcodec_find_decoder_by_name("h264");
    if (!codec){
        av_log(NULL, AV_LOG_ERROR, "video decoder AVCodec is not be fould, token = %s", decodectx->params->token);
        goto err;
    }
    context = avcodec_alloc_context3(codec);
    if (!context){
        av_log(NULL, AV_LOG_ERROR, "video decoder AVCodecContext alloc no succss, token = %s", decodectx->params->token);
        goto err;
    }

    if (!av_dict_get(opts, "threads", NULL, 0))
        av_dict_set(&opts, "threads", "1", 0);
    if (context->codec_type == AVMEDIA_TYPE_VIDEO || context->codec_type == AVMEDIA_TYPE_AUDIO)
        av_dict_set(&opts, "refcounted_frames", "1", 0);

    context->codec_type = codec->type;
    context->codec_id = codec->id;
    context->width = decodectx->width;
    context->height = decodectx->height;

    context->flags |= CODEC_FLAG_GLOBAL_HEADER;

    ret = avcodec_open2(context, codec, &opts);
    if(ret < 0){
    	av_dict_free(&opts);
    	av_log(NULL, AV_LOG_ERROR, "video decoder avcodec open failed, token = %s", decodectx->params->token);
        goto err;
    }
    av_dict_free(&opts);
    decodectx->decoder = context;
    av_log(NULL, AV_LOG_DEBUG, "init video decoder ok, token = %s", decodectx->params->token);
    av_dump_format(decodectx->fmt, 0, NULL, 0);
    return 0;

err:

    if(context)
        avcodec_free_context(&context);
    if(decodectx->fmt){
        if(decodectx->fmt->pb) {
            av_freep(&decodectx->fmt->pb->buffer);
            av_freep(&decodectx->fmt->pb);
        }
        avformat_close_input(&(decodectx->fmt));
        decodectx->fmt = NULL;
    }
    av_log(NULL, AV_LOG_ERROR, "init video decoder failed! token = %s", decodectx->params->token);
    return -1;
}

static int save_yuv(uint8_t *frame[], int linesize[], int width, int height)
{
	int ret = 0;
	FILE *fp;
	char filename[80];
	static int frame_num = 0;
	int y = 0;
	sprintf(filename, "frame_%d_%d.yuv", (int)(getpid()), frame_num++);
    fp = fopen(filename, "wb");
    if (fp == NULL) {
    	return -1;
    }
    for (y = 0; y < height; y++) {
    	fwrite(frame[0] + linesize[0] * y, sizeof(uint8_t), width, fp);
    }
    for (y = 0; y < height / 2; y++) {
    	fwrite(frame[1] + linesize[1] * y, sizeof(uint8_t), width / 2, fp);
    }
    for (y = 0; y < height / 2; y++) {
    	fwrite(frame[2] + linesize[2] * y, sizeof(uint8_t), width / 2, fp);
    }
    fclose(fp);
    return ret;
}

static int DecodeVideoPkt(AVCodecContext* decoder, AVPacket* pkt, AVFrame* frame, int* width, int* height)
{
    int ret;
    int got_frame;

    ret = avcodec_decode_video2(decoder, frame, &got_frame, (const AVPacket*)pkt);

    if (ret < 0 || !got_frame){
        av_log(NULL, AV_LOG_DEBUG, "ADT Decode video failed!\n");
        return -1;
    }else if (got_frame){

        *width  = decoder->width;
        *height = decoder->height;

        if (got_frame){
            av_log(NULL, AV_LOG_DEBUG, "get a frame!\n");
            //save_yuv(frame->data, frame->linesize, *width, *height);
        }
        return 0;
    }
    return -1;
}

static int DestroyVideoDecodeCtx(VideoDecodeCtx** opaque)
{
    VideoDecodeCtx* decodectx = (VideoDecodeCtx*)*opaque;
    if(decodectx->nal_data_ptr){
        av_free(decodectx->nal_data_ptr);
        decodectx->nal_data_ptr = NULL;
    }
    if(decodectx->fmt && decodectx->fmt->pb) {
        av_freep(&decodectx->fmt->pb->buffer);
        av_freep(&decodectx->fmt->pb);
    }

    if(decodectx->decoder){
        //avcodec_flush_buffers(decodectx->decoder);
        avcodec_free_context(&decodectx->decoder);
        av_free(decodectx->decoder);
    }

    if(decodectx->fmt){
        AVFormatContext *ic;
        unsigned int i = 0;
        ic = decodectx->fmt;
        for (i = 0; i < ic->nb_streams; i++)
            avcodec_close(ic->streams[i]->codec);
        avformat_close_input(&(decodectx->fmt));
    }
    if(decodectx->params){
       av_free(decodectx->params);
    }
    av_log(NULL, AV_LOG_DEBUG, "destroy video decoder ok, token = %s", decodectx->params->token);
    av_free(decodectx);
    decodectx = NULL;
    return 0;
}

int DecodeNal(char* data_buffer, int data_size, const char* token, YUVFrame2* yuv_frame)
{
    /* vim: set fdm=indent: */
    if(data_buffer == NULL || token == NULL || data_size == 0){
        av_log(NULL, AV_LOG_ERROR, "Input params have error! Please check, token = %s\n", token);
        return -1;
    }

    int ret = 0;
    int tmp_size;
    char* dst_ptr;
    int yuv_num = 0;
    int i = 0;
    int width = 0, height = 0;

    AVPacket packet;
    AVPacket* pkt = &packet;
    av_init_packet(pkt);

    memset(yuv_frame, 0, sizeof(YUVFrame2));
    VideoDecodeCtx* decodectx = (VideoDecodeCtx* )av_mallocz(sizeof(VideoDecodeCtx));
    decodectx->params = av_mallocz(sizeof(InputParams));
    memcpy(decodectx->params->token, token, sizeof(decodectx->params->token));

    decodectx->nal_data_len = data_size;
    decodectx->nal_data = av_mallocz(data_size);
    memcpy(decodectx->nal_data, data_buffer, data_size);
    decodectx->nal_data_ptr = decodectx->nal_data;

    ret = CreateDecoderCtxAVIO(decodectx);
    if(ret < 0)
        goto failed;
    AVFrame* frame = av_frame_alloc();
    char* data[DECODER_NUM] = {NULL};
    AnalyseFrameGlobalParams afg_params;
    memset(&afg_params, 0, sizeof(AnalyseFrameGlobalParams));
    int isbasePframe = 0;
    do{
        ret = av_read_frame(decodectx->fmt, pkt);
        if(ret < 0){
            av_free_packet(pkt);
            if (ret == AVERROR(EAGAIN))
                continue;
            break;
        }
        if((pkt->flags & AV_PKT_FLAG_KEY) == AV_PKT_FLAG_KEY)
            AnalysePpsSps(&afg_params, pkt->data, pkt->size);
        else{
            if(FindBasePframe(&afg_params, pkt->data, pkt->size) != 1){
                av_free_packet(pkt);
                continue;
            }
        }
        ret = DecodeVideoPkt(decodectx->decoder, pkt, frame, &width, &height);
        if(ret < 0){
            av_free_packet(pkt);
            break;
        }
        //save_yuv(frame->data, frame->linesize, width, height);
        av_log(NULL, AV_LOG_DEBUG, "frame width = %d height = %d\n", frame->linesize[0], frame->linesize[1]);
        yuv_frame->data[yuv_num] = av_mallocz(width * height * 3 / 2);

        tmp_size = width * height;
        dst_ptr = yuv_frame->data[yuv_num];
        memcpy(dst_ptr, frame->data[0], tmp_size);
        dst_ptr += tmp_size;
        tmp_size = tmp_size / 4;
        memcpy(dst_ptr, frame->data[1], tmp_size);
        dst_ptr += tmp_size;
        memcpy(dst_ptr, frame->data[2], tmp_size);
        yuv_frame->size[yuv_num] = width * height * 3 / 2;
        av_free_packet(pkt);
        yuv_num ++;

        if(yuv_num >= DECODER_NUM)
            break;

    }while(1);
    av_frame_free(&frame);
    if(yuv_num == 0)
        goto failed;

    yuv_frame->width = width;
    yuv_frame->height = height;

    av_log(NULL, AV_LOG_DEBUG, "---get %d frame!----\n", yuv_num);
    if(decodectx)
        DestroyVideoDecodeCtx(&decodectx);
    return 0;

failed:
    if(decodectx)
        DestroyVideoDecodeCtx(&decodectx);
    return -1;
}

int Destroy(YUVFrame2* yuv_frame)
{
    int i = 0;
    for(; i < 2; i++)
    {
        if(yuv_frame->data[i])
            av_free(yuv_frame->data[i]);
    }
    return 0;
}
