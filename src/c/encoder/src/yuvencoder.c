#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "log.h"
#include "yuvencoder.h"
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
        ret = SLS_LOG_INFO;
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
    /*
    if (level <= 32)
    {
        vsnprintf(log_buf, sizeof(log_buf), fmt, vl);
        if(pp_log_callback != NULL)
            pp_log_callback(0, log_buf);
    }
    */
}

int Init(void (*log_callback)(int level, char* buf))
{
    pp_log_callback = log_callback;
    av_log_set_callback(log_default_callback);
    
    av_register_all();
    return 0;
}

typedef struct EncodeContext
{
	AVCodecContext*    encoder;
    struct SwsContext* converter;
    char               token[1024];
	int                src_width;
	int                src_height;
	int                dst_width;
	int                dst_height;
	int                need_scale;
    int                format;
    AVFrame*           frame;
    AVFrame*           scale_frame;
	AVPacket           pkt;
}EncodeContext;

static int CreateEncoderContext(EncodeContext* encode)
{
	AVCodec *codec = NULL;
	AVCodecContext *c = NULL;

	/* find the jpg encoder */
	if (encode->dst_width == 0 || encode->dst_height == 0) {
		av_log(NULL, AV_LOG_ERROR, "video encoder width or height error! token = %s", encode->token);
	}
	codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
	if (!codec) {
		av_log(NULL, AV_LOG_ERROR, "video encoder Codec not found, token = %s", encode->token);
		goto err;
	}
	//av_log(NULL, AV_LOG_INFO, "codec name=%s,exist_fmt %p, token = %s", codec->name, codec->pix_fmts, encode->token);
	c = avcodec_alloc_context3(codec);
	if (!c) {
		av_log(NULL, AV_LOG_ERROR, "video encoder AVCodecCOntext alloc not sucess, token = %s", encode->token);
		goto err;
	}

	/* put sample parameters */
	//c->bit_rate = encode->bitrate;	//400000;
	/* resolution must be a multiple of two */
	c->width = encode->dst_width;
	c->height = encode->dst_height;

    c->pix_fmt = AV_PIX_FMT_YUVJ420P;
    c->time_base = (AVRational ) { 1, 25 };
	//c->rc_min_rate = encode->bitrate;
	//c->rc_max_rate = encode->bitrate;
	//c->bit_rate_tolerance = encode->bitrate;
	//c->rc_buffer_size = encode->bitrate;
	//c->rc_initial_buffer_occupancy = c->rc_buffer_size * 3 / 4;

	if (avcodec_open2(c, codec, NULL) < 0) {
		av_log(NULL, AV_LOG_ERROR, "encoder Could not open codec, token = %s", encode->token);
		goto err;
	}

	//av_log(NULL, AV_LOG_INFO, "init mjpeg encoder ok, token = %s", encode->token);

	encode->encoder = c;
	return 0;
err:
    if (c)
		avcodec_free_context(&c);
	av_log(NULL, AV_LOG_ERROR, "init mjpeg encoder failed! token = %s", encode->token);
	return -1;
}

static int Encode(EncodeContext* encodectx)
{
    int ret = 0;
    int got_packet = 0;
    ret = avcodec_encode_video2(encodectx->encoder, &encodectx->pkt, encodectx->need_scale ? encodectx->scale_frame : encodectx->frame, &got_packet);
    if (ret < 0 || got_packet == 0)
	{
		av_log(NULL, AV_LOG_WARNING, "ADT Encoder video failed! token = %s", encodectx->token);
		return -1;
	}
	else if (got_packet)
	{
#if 0
		FILE* in_file = NULL;
		char filename[1024]= {0};
		static int number = 0;
		number++;
		sprintf(filename, "/home/pang/git/screenshot-v3/source/example/%s_%d.jpg", transcode->token, number);
		in_file = fopen(filename, "w+");
		if(in_file == NULL)
		{
			av_log(NULL, AV_LOG_ERROR, "Open jpeg file failed, token = %s\n", transcode->token);
			return -1;
     	}
		int size = fwrite(transcode->out_pkt.data, 1, transcode->out_pkt.size, in_file);
		if(size != transcode->out_pkt.size)
		{
			av_log(NULL, AV_LOG_ERROR, "write jpeg picture failed, token = %s\n", transcode->token);
			fclose(in_file);
			return -1;
		}
		fclose(in_file);
#endif
	}
	return 0;
}

static int DestroyContext(EncodeContext** opaque)
{
	EncodeContext* encodectx = (EncodeContext*)*opaque;

	av_free_packet(&encodectx->pkt);
	if(encodectx->frame)
    {
        int i;
        for(i = 0; i < 3; i++)
        {
            av_free(encodectx->frame->data[i]);
            encodectx->frame->data[i] = NULL;
        }
		av_frame_free(&(encodectx->frame));
    }
    if(encodectx->scale_frame)
    {
        int i;
        for(i = 0; i < 3; i++)
        {
            av_free(encodectx->scale_frame->data[i]);
            encodectx->scale_frame->data[i] = NULL;
        }
        av_frame_free(&(encodectx->scale_frame));
    }
    
    if(encodectx->converter)
    {
        sws_freeContext(encodectx->converter);
        encodectx->converter = NULL;
    }
    if(encodectx->encoder)
    {
    	avcodec_flush_buffers(encodectx->encoder);
    	avcodec_free_context(&encodectx->encoder);
    	av_free(encodectx->encoder);
    }

	//av_log(NULL, AV_LOG_INFO, "destroy video decoder ok, token = %s", decodectx->token);
	av_free(encodectx);
	encodectx = NULL;
	return 0;
}

int EncodeJPG(char* data_buffer, int src_width, int src_height, int dst_width, int dst_height, char* token, OUTDATA* outdata)
{
    int ret = 0;
    int tmp_size;
    char* dst_ptr;
    memset(outdata, 0, sizeof(OUTDATA));
    EncodeContext* encodectx = (EncodeContext* )av_mallocz(sizeof(EncodeContext));

    encodectx->frame = av_frame_alloc();
    memcpy(encodectx->token, token, strlen(token));
    av_init_packet(&encodectx->pkt);

    if(src_width == dst_width && src_height == dst_height)
        encodectx->need_scale = 0;
    else
        encodectx->need_scale = 1;
    if(encodectx->need_scale == 1)
    {
        encodectx->scale_frame = av_frame_alloc();
        encodectx->scale_frame->format = AV_PIX_FMT_YUV420P;
        encodectx->scale_frame->width = dst_width;
        encodectx->scale_frame->height = dst_height;
        encodectx->scale_frame->data[0] = av_malloc(dst_width * dst_height);
        encodectx->scale_frame->data[1] = av_malloc(dst_width * dst_height / 4);
        encodectx->scale_frame->data[2] = av_malloc(dst_width * dst_height / 4);
        encodectx->scale_frame->linesize[0] = dst_width;
        encodectx->scale_frame->linesize[1] = dst_width / 2;
        encodectx->scale_frame->linesize[2] = dst_width / 2;
        encodectx->converter = sws_getCachedContext(encodectx->converter,
                        src_width, src_height, AV_PIX_FMT_YUV420P, dst_width,
                                        dst_height, encodectx->scale_frame->format, SWS_POINT,
                                                        NULL, NULL, NULL);
    }
    encodectx->dst_width = dst_width;
    encodectx->dst_height = dst_height;
    ret = CreateEncoderContext(encodectx);
    if(ret < 0)
    	goto failed;
    
    encodectx->frame->data[0] = malloc(src_width * src_height);
    memcpy(encodectx->frame->data[0], data_buffer, src_width * src_height);
    encodectx->frame->data[1] = malloc(src_width * src_height / 4);
    memcpy(encodectx->frame->data[1], data_buffer + src_width * src_height, src_width * src_height / 4);
    encodectx->frame->data[2] = malloc(src_width * src_height / 4);
    memcpy(encodectx->frame->data[2], data_buffer + src_width * src_height + src_width * src_height / 4, src_width * src_height / 4);

    encodectx->frame->linesize[0] = src_width;
    encodectx->frame->linesize[1] = src_width / 2;
    encodectx->frame->linesize[2] = src_width / 2;
    encodectx->frame->width = src_width;
    encodectx->frame->height = src_height;

    encodectx->frame->format = AV_PIX_FMT_YUV420P;
    if(encodectx->need_scale == 1)
    {
        sws_scale(encodectx->converter, (const uint8_t * const *) (encodectx->frame->data), encodectx->frame->linesize, 0, encodectx->frame->height,
                                        encodectx->scale_frame->data, encodectx->scale_frame->linesize);
    }
    ret = Encode(encodectx);
    if(ret < 0)
    	goto failed;

    outdata->data = av_malloc(encodectx->pkt.size);
    memcpy(outdata->data, encodectx->pkt.data, encodectx->pkt.size);
    outdata->size = encodectx->pkt.size;

    if(encodectx)
        DestroyContext(&encodectx);
    return 0;

failed:
    if(encodectx)
        DestroyContext(&encodectx);
    return -1;
}

int Destroy(OUTDATA* outdata)
{
    if(outdata->data)
        av_free(outdata->data);
    return 0;
}
