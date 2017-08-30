#include <libavformat/avformat.h>
//#include <libswscale/swscale.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

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
        av_log(avctx, AV_LOG_DEBUG, "The maximum value for lowres supported by the decoder is %d, token = %s", codec->max_lowres, decodectx->token);
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

static int Decode1(DecodeContext* decodectx)
{
    int ret;
    int got_frame;
    int got_packet;
    ret = avcodec_decode_video2(decodectx->decoder, decodectx->frame, &got_frame, (const AVPacket*)&decodectx->pkt);
    if (ret < 0 || !got_frame)
    {
        av_log(NULL, AV_LOG_DEBUG, "ADT Decode video failed! token = %s", decodectx->token);
        return -1;
    }
    else if (got_frame)
    {
/*      if(decodectx->frame->width != decodectx->frame->linesize[0])
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
            av_log(NULL, AV_LOG_DEBUG, "get a frame!\n");
            save_yuv(decodectx->frame->data, decodectx->frame->linesize, decodectx->width, decodectx->height);
        }
        return 0;
    }
    return -1;
}
static int Decode(DecodeContext* decodectx, AVPacket* pkt, AVFrame* frame)
{
    int ret;
    int got_frame;
    int got_packet;
    ret = avcodec_decode_video2(decodectx->decoder, frame, &got_frame, (const AVPacket*)pkt);
    if (ret < 0 || !got_frame)
    {
        av_log(NULL, AV_LOG_DEBUG, "ADT Decode video failed! token = %s", decodectx->token);
        return -1;
    }
    else if (got_frame)
    {
        decodectx->width = decodectx->decoder->width;
        decodectx->height = decodectx->decoder->height;

        if (got_frame)
        {
            av_log(NULL, AV_LOG_DEBUG, "get a frame!\n");
            //save_yuv(frame->data, frame->linesize, decodectx->width, decodectx->height);
        }
        return 0;
    }
    return -1;
}

static int DestroyContext(DecodeContext** opaque)
{
    DecodeContext* decodectx = (DecodeContext*)*opaque;
    //if(decodectx->frame)
    //  av_frame_free(&(decodectx->frame));
    
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

#define AV_RB32(x)  ((((const uint8_t*)(x))[0] << 24) | (((const uint8_t*)(x))[1] << 16) | (((const uint8_t*)(x))[2] <<  8) |((const uint8_t*)(x))[3])
#include <libavutil/avassert.h>

enum {
    NAL_SLICE           = 1,
    NAL_DPA             = 2,
    NAL_DPB             = 3,
    NAL_DPC             = 4,
    NAL_IDR_SLICE       = 5,
    NAL_SEI             = 6,
    NAL_SPS             = 7,
    NAL_PPS             = 8,
    NAL_AUD             = 9,
    NAL_END_SEQUENCE    = 10,
    NAL_END_STREAM      = 11,
    NAL_FILLER_DATA     = 12,
    NAL_SPS_EXT         = 13,
    NAL_AUXILIARY_SLICE = 19,
    NAL_FF_IGNORE       = 0xff0f001,
};

const uint8_t *find_start_code(const uint8_t * p, const uint8_t *pend, uint32_t * state)
{
    int i;

    av_assert0(p <= pend);
    if (p >= pend)
        return pend;

    for (i = 0; i < 3; i++) {
        uint32_t tmp = *state << 8;
        *state = tmp + *(p++);
        if (tmp == 0x100 || p == pend)
            return p;
    }

    while (p < pend) {
        if      (p[-1] > 1      ) p += 3;
        else if (p[-2]          ) p += 2;
        else if (p[-3]|(p[-1]-1)) p++;
        else {
            p++;
            break;
        }
    }

    p = FFMIN(p, pend) - 4;
    *state = AV_RB32(p);

    return p + 4;
}

static int find_frame_location(const uint8_t* data_buffer, int data_size)
{
    uint32_t state = -1;
    int has_sps = 0;
    int has_pps = 0;
    int has_sei = 0;
    int last_location = 0;
    const uint8_t *ptr = data_buffer, *end = data_buffer + data_size;
    int nalu_type;

    while (ptr < end) {
        ptr = find_start_code(ptr, end, &state);
        if ((state & 0xFFFFFF00) != 0x100)
            break;
        nalu_type = state & 0x1F;
        if (nalu_type == NAL_SPS) {
            has_sps = 1;
        } else if (nalu_type == NAL_PPS)
            has_pps = 1;
        else if (nalu_type == NAL_SEI) {
            has_sei = 1;
        } else {

            while (ptr - 4 > data_buffer && ptr[-5] == 0)
                ptr--;
            return ptr - 4 - data_buffer;
        }
    }
    return 0;
}

void SplitNalBuffer(const char* data_buffer, int data_size, char* data[], int* size)
{
    const uint8_t *ptr = data_buffer;
    int start_location = 0;
    int i = 0;
    int spspps_len = 0;
    int i_frame_len = 0;
    int p_frame_len1 = 0;
    int p_frame_len2 = 0;
    data[0] = data_buffer;

    spspps_len = find_frame_location(ptr, data_size);

    ptr = ptr + spspps_len + 4;
    av_log(NULL, AV_LOG_DEBUG, "ptr = %d %d %d %d\n", ptr[-4], ptr[-3], ptr[-2], ptr[-1]);
    i_frame_len = find_frame_location(ptr, data_size - spspps_len - 4);
    if(i_frame_len == 0)
    {
        //break
        size[0] = data_size;
        data[0] = data_buffer;
        av_log(NULL, AV_LOG_DEBUG, "data0 = %d %d %d %d\n", data[0][0], data[0][1], data[0][2], data[0][3]);
        goto end;
    }
    data[0] = data_buffer;
    size[0] = i_frame_len + 4 + spspps_len;
    av_log(NULL, AV_LOG_DEBUG, "data0 = %d %d %d %d\n", data[0][0], data[0][1], data[0][2], data[0][3]);
    ptr = ptr + i_frame_len + 4;
    av_log(NULL, AV_LOG_DEBUG, "ptr = %d %d %d %d\n", ptr[-4], ptr[-3], ptr[-2], ptr[-1]);
    p_frame_len1 = find_frame_location(ptr, data_size - spspps_len - (i_frame_len + 4));
    if(p_frame_len1 == 0)
    {
        data[1] = ptr - 4;
        av_log(NULL, AV_LOG_DEBUG, "data1 = %d %d %d %d\n", data[1][0], data[1][1], data[1][2], data[1][3]);
        size[1] = data_size - spspps_len - (i_frame_len + 4);
        goto end;
    }
    data[1] = ptr - 4;
    size[1] = p_frame_len1 + 4;
    av_log(NULL, AV_LOG_DEBUG, "data1 = %d %d %d %d\n", data[1][0], data[1][1], data[1][2], data[1][3]);
    av_log(NULL, AV_LOG_DEBUG, "ptr = %d %d %d %d\n", ptr[-4], ptr[-3], ptr[-2], ptr[-1]);
    /*ptr = ptr + p_frame_len1 + 4;

    p_frame_len2 = find_frame_location(ptr, data_size - spspps_len - (i_frame_len + 4) - (p_frame_len1 + 4));
    if(p_frame_len2 == 0)
    {
        data[2] = ptr - 4;
        av_log(NULL, AV_LOG_INFO, "data2 = %d %d %d %d\n", data[2][0], data[2][1], data[2][2], data[2][3]);
        size[2] = data_size - spspps_len - (i_frame_len + 4) - (p_frame_len1 + 4);
    }
    av_log(NULL, AV_LOG_INFO, "data2 = %d %d %d %d\n", data[2][0], data[2][1], data[2][2], data[2][3]);
    av_log(NULL, AV_LOG_INFO, "ptr = %d %d %d %d\n", ptr[-4], ptr[-3], ptr[-2], ptr[-1]);
    */
end:
    av_log(NULL, AV_LOG_DEBUG, "size = %d %d\n", size[0], size[1]);
    av_log(NULL, AV_LOG_DEBUG, "data = %d %d\n", data[0], data[1]);
    return;
}

int DecodeNal(char* data_buffer, int len, const char* token, YUVFrame2* yuv_frame)
{
    int ret = 0;
    int tmp_size;
    char* dst_ptr;

    // debug 
    char* data[2] = {0};
    int   size[2] = {0};
    SplitNalBuffer(data_buffer, len, data, size);
    // debug end

    memset(yuv_frame, 0, sizeof(YUVFrame2));
    DecodeContext* decodectx = (DecodeContext* )av_mallocz(sizeof(DecodeContext));

    //decodectx->frame = av_frame_alloc();
    //decodectx->scale_frame = av_frame_alloc();
    memcpy(decodectx->token, token, strlen(token));
    //av_init_packet(&decodectx->pkt);

    ret = CreateDecoderContext(decodectx);
    if(ret < 0)
        goto failed;
    int i = 0;

    AVPacket pkt;
    AVFrame* frame = 0;

    for(; i < 2; i++)
    {
        if(size[i] != 0)
        {
            frame = av_frame_alloc();
            av_init_packet(&pkt);
            pkt.data = data[i];
            pkt.size = size[i];

            ret = Decode(decodectx, &pkt, frame);
            if(ret < 0)
            {
                av_free_packet(&decodectx->pkt);
                av_frame_free(&frame);
                goto failed;
            }

            yuv_frame->size[i] = decodectx->width * decodectx->height * 3 / 2;

            yuv_frame->data[i] = av_mallocz(decodectx->width * decodectx->height * 3 / 2);
            yuv_frame->width = decodectx->width;
            yuv_frame->height = decodectx->height;

            tmp_size = decodectx->width * decodectx->height;
            dst_ptr = yuv_frame->data[i];
            memcpy(dst_ptr, frame->data[0], tmp_size);
            dst_ptr = yuv_frame->data[i] + tmp_size;
            tmp_size = tmp_size / 4;
            memcpy(dst_ptr, frame->data[1], tmp_size);
            dst_ptr += tmp_size;
            memcpy(dst_ptr, frame->data[2], tmp_size);

            av_free_packet(&decodectx->pkt);
            av_frame_free(&frame);
        }
    }
    //decodectx->pkt.data = data_buffer;
    //decodectx->pkt.size = len;

    //ret = Decode(decodectx);
    //av_free_packet(&decodectx->pkt);
    //if(ret < 0)
    //  goto failed;

    /*
    yuv_frame->data = av_mallocz(decodectx->width * decodectx->height * 3 / 2);
    yuv_frame->size = decodectx->width * decodectx->height * 3 / 2;
    yuv_frame->width = decodectx->width;
    yuv_frame->height = decodectx->height;

    tmp_size = decodectx->width * decodectx->height;
    dst_ptr = yuv_frame->data;
    memcpy(dst_ptr, decodectx->frame->data[0], tmp_size);
    dst_ptr = yuv_frame->data + tmp_size;
    tmp_size = tmp_size / 4;
    memcpy(dst_ptr, decodectx->frame->data[1], tmp_size);
    dst_ptr += tmp_size;
    memcpy(dst_ptr, decodectx->frame->data[2], tmp_size);
    */
    if(decodectx)
        DestroyContext(&decodectx);
    return 0;

failed:
    if(decodectx)
        DestroyContext(&decodectx);
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
