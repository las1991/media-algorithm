#include <libavformat/avformat.h>
//#include <libswscale/swscale.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "log.h"
#include "nal_decoder.h"
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
    }*/
}

void SetLogCallback(void* log_callback)
{
    pp_log_callback = log_callback;
    av_log_set_callback(log_default_callback);
}

//int Init(void (*log_callback)(int level, char* buf))
int Init()
{
    //pp_log_callback = log_callback;
    //av_log_set_callback(log_default_callback);
    
    av_register_all();
    return 0;
}

typedef struct DecodeContext
{
	AVCodecContext* decoder;
	char            token[1024];
	int             width;
	int             height;
	AVFrame*        frame;
	//AVFrame*        scale_frame;
	//int             need_scale;
	AVPacket        pkt;
}DecodeContext;

static int CreateDecoderContext(DecodeContext* decodectx)
{
	int ret = 0;
	AVDictionary *opts = NULL;
	AVCodecContext *context = NULL, *avctx = NULL;
	AVCodec *codec = NULL;

	codec = avcodec_find_decoder_by_name("h264");
	if (!codec)
	{
		av_log(NULL, AV_LOG_ERROR, "video decoder AVCodec is not be fould, token = %s", decodectx->token);
		goto err;
	}
	avctx = context = avcodec_alloc_context3(codec);
	if (!context)
	{
		av_log(NULL, AV_LOG_ERROR, "video decoder AVCodecContext alloc no succss, token = %s", decodectx->token);
		goto err;
	}
	avctx->workaround_bugs = 1;
	avctx->lowres = 0;
	if (avctx->lowres > codec->max_lowres) {
		av_log(avctx, AV_LOG_WARNING, "The maximum value for lowres supported by the decoder is %d, token = %s", codec->max_lowres, decodectx->token);
		avctx->lowres = codec->max_lowres;
	}
	avctx->idct_algo = 0;
	avctx->skip_frame = AVDISCARD_DEFAULT;
	avctx->skip_idct = AVDISCARD_DEFAULT;
	avctx->skip_loop_filter = AVDISCARD_DEFAULT;
	avctx->error_concealment = 3;
	if (avctx->lowres)
		avctx->flags |= CODEC_FLAG_EMU_EDGE;
	if (codec->capabilities & CODEC_CAP_DR1)
		avctx->flags |= CODEC_FLAG_EMU_EDGE;
	if (!av_dict_get(opts, "threads", NULL, 0))
		av_dict_set(&opts, "threads", "1", 0);
	if (avctx->codec_type == AVMEDIA_TYPE_VIDEO || avctx->codec_type == AVMEDIA_TYPE_AUDIO)
		av_dict_set(&opts, "refcounted_frames", "1", 0);

	context->codec_type = codec->type;
	context->codec_id = codec->id;
	context->width = decodectx->width;
	context->height = decodectx->height;

	context->flags |= CODEC_FLAG_GLOBAL_HEADER;

    ret = avcodec_open2(context, codec, &opts);
    if(ret < 0)
	{
    	av_log(NULL, AV_LOG_ERROR, "video decoder avcodec open failed, token = %s", decodectx->token);
    	goto err;
    }
    decodectx->decoder = context;
	//av_log(NULL, AV_LOG_INFO, "init video decoder ok, token = %s", decodectx->token);

	return 0;
err:
	if(context)
		avcodec_free_context(&context);
	av_log(NULL, AV_LOG_ERROR, "init video decoder failed! token = %s", decodectx->token);
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

static int Decode(DecodeContext* decodectx)
{
	int ret;
	int got_frame;
	int got_packet;
	ret = avcodec_decode_video2(decodectx->decoder, decodectx->frame, &got_frame, (const AVPacket*)&decodectx->pkt);
	if (ret < 0 || !got_frame)
	{
		av_log(NULL, AV_LOG_WARNING, "ADT Decode video failed! token = %s", decodectx->token);
		return -1;
	}
	else if (got_frame)
	{
/*		if(decodectx->frame->width != decodectx->frame->linesize[0])
		{
			decodectx->need_scale = 1;
			decodectx->scale_frame->format = AV_PIX_FMT_YUV420P;
			decodectx->scale_frame->width = dst_width;
			decodectx->scale_frame->height = dst_height;
			decodectx->scale_frame->data[0] = av_malloc(dst_width * dst_height);
			decodectx->scale_frame->data[1] = av_malloc(dst_width * dst_height / 4);
			decodectx->scale_frame->data[2] = av_malloc(dst_width * dst_height / 4);
			decodectx->scale_frame->linesize[0] = dst_width;
			decodectx->scale_frame->linesize[1] = dst_width / 2;
			decodectx->scale_frame->linesize[2] = dst_width / 2;
			struct SwsContext* swscontext;
			swscontext = sws_getCachedContext(swscontext,
					decodectx->frame->linesize[0], decodectx->frame->height, AV_PIX_FMT_YUV420P, decodectx->frame->width,
					decodectx->frame->height, AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
			sws_scale(swscontext, (const uint8_t * const *) (decodectx->frame->data), decodectx->frame->linesize, 0, decodectx->frame->height,
					decodectx->scale_frame->data, decodectx->scale_frame->linesize);
			sws_freeContext(swscontext);
		}*/
		decodectx->width = decodectx->decoder->width;
		decodectx->height = decodectx->decoder->height;
		
        if (got_frame)
		{
			//save_yuv(decodectx->frame->data, decodectx->frame->linesize, decodectx->width, decodectx->height);
		}
        return 0;
	}
	return -1;
}

static int DestroyContext(DecodeContext** opaque)
{
	DecodeContext* decodectx = (DecodeContext*)*opaque;

	av_free_packet(&decodectx->pkt);
	if(decodectx->frame)
		av_frame_free(&(decodectx->frame));
    
    if(decodectx->decoder)
    {
    	avcodec_flush_buffers(decodectx->decoder);
    	avcodec_free_context(&decodectx->decoder);
    	av_free(decodectx->decoder);
    }

	//av_log(NULL, AV_LOG_INFO, "destroy video decoder ok, token = %s", decodectx->token);
	av_free(decodectx);
	decodectx = NULL;
	return 0;
}

int DecodeNal(char* data_buffer, int len, const char* token, YUVFrame* yuv_frame)
{
    int ret = 0;
    int tmp_size;
    char* dst_ptr;
    memset(yuv_frame, 0, sizeof(YUVFrame));
    DecodeContext* decodectx = (DecodeContext* )av_mallocz(sizeof(DecodeContext));

    decodectx->frame = av_frame_alloc();
    //decodectx->scale_frame = av_frame_alloc();
    memcpy(decodectx->token, token, strlen(token));
    av_init_packet(&decodectx->pkt);

    ret = CreateDecoderContext(decodectx);
    if(ret < 0)
    	goto failed;

    decodectx->pkt.data = data_buffer;
    decodectx->pkt.size = len;

    ret = Decode(decodectx);
    if(ret < 0)
    	goto failed;

    yuv_frame->data = av_mallocz(decodectx->width * decodectx->height * 3 / 2);
    yuv_frame->size = decodectx->width * decodectx->height * 3 / 2;
    yuv_frame->width = decodectx->width;
    yuv_frame->height = decodectx->height;

    tmp_size = decodectx->width * decodectx->height;
    dst_ptr = yuv_frame->data;
    memcpy(dst_ptr, /*decodectx->need_scale ? decodectx->scale_frame->data[0] : */decodectx->frame->data[0], tmp_size);
    dst_ptr = yuv_frame->data + tmp_size;
    tmp_size = tmp_size / 4;
    memcpy(dst_ptr, /*decodectx->need_scale ? decodectx->scale_frame->data[1] : */decodectx->frame->data[1], tmp_size);
    dst_ptr += tmp_size;
    memcpy(dst_ptr, /*decodectx->need_scale ? decodectx->scale_frame->data[2] : */decodectx->frame->data[2], tmp_size);

    if(decodectx)
        DestroyContext(&decodectx);
    return 0;

failed:
    if(decodectx)
        DestroyContext(&decodectx);
    return -1;
}

int Destroy(YUVFrame* yuv_frame)
{
    if(yuv_frame->data)
        av_free(yuv_frame->data);
    return 0;
}
