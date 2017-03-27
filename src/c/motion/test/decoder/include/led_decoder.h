/** Copyright (c) 2015 Sengled Co. Ltd. 
 *  Author: yujun Email: yujun@sengled.com
 */

#ifndef LED_DECODER_H_
#define LED_DECODER_H_

#include <stdarg.h>

#define LIBLEDDECODER_VERSION_MAJOR 0
#define LIBLEDDECODER_VERSION_MINOR 0
#define LIBLEDDECODER_VERSION_MICRO 1

#define SL_MAX_PATH_LENGTH 4096

/**
 * @stream decoder's state
 */

enum SLStreamState {
    SLSTREAM_STATE_CLOSE,
    SLSTREAM_STATE_INIT,
    SLSTREAM_STATE_DECODE,
    SLSTREAM_STATE_PAUSE
};

/**
 * @image frame decoded state
 */
enum SLFrameState {
    SLFRAME_STATE_WAITING,
    SLFRAME_STATE_READY
};

/**
 * @save image type for debug
 */
enum SLSaveImageType {
    SLSAVE_IMAGE_TYPE_NONE,
    SLSAVE_IMAGE_TYPE_YUV,
    SLSAVE_IMAGE_TYPE_PPG,
    SLSAVE_IMAGE_TYPE_PPM
};

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @extern API's parameters struct
 */
typedef struct SLStreamParameter_tag {
    /**
     * input stream's name
     */
    char url_name[SL_MAX_PATH_LENGTH];

    /**
     * stream decoder's state
     */
    enum SLStreamState state;

    /**
     * image frame dedoded state
     */
    enum SLFrameState frame_state;

    /**
     * output grey image's data
     */
    char *scaled_frame_data;

    /**
     * profile performance
     */
    int profile;

    /**
     * save image type
     */
    enum SLSaveImageType save_image_type;

    /**
     * error frame flag: 0 is normal, -1 is error
     */
    int error_frame;

    /**
     * frame type : key frame is 1
     *       none key frame is 0
     */
    int key_frame;

    /**
     * frame time stamp, unit is ms;
     */
    long long frame_time_stamp;

    /**
     * frame's width
     * frame's height
     */
    int frame_width;
    int frame_height;

    /**
     * ffmpeg log callback function pointer
     */
    void (*ffmpeg_log_callback)(void *ptr, int level, const char *fmt, va_list vl);

    /**
     * private data point
     */
    void *private_data;
}SLStreamParameter;

/**
 * register ffmpeg protocal decoder ...
 */

void RegisterFFMPEG(void);

/**
 * Unregister ffmpeg protocal decoder ...
 */

void UnregisterFFMPEG(void);

/**
 * Initialize stream decoder, return 0 if success.
 * @param[in] input parameter for decoder
 */

int OpenStream(SLStreamParameter *para);

/**
 * Decode stream, return 0 if success.
 * It is required to check para->frame_state,
 * when a frame is ready then para->frame_state ==
 * SLFRAME_STATE_READY.
 *
 * @param[inout] stream parameter
 */

int DecodeStream(SLStreamParameter *para);

/**
 * Close strea, return 0 if success.
 * @param[in] stream parameter
 */

int CloseStream(SLStreamParameter *para);

/**
 * Query stream decoder's state, return 0 if success.
 * @param[out] stream parameter.
 */

int QueryStream(SLStreamParameter *para);

/**
 * control stream decoder's state, return 0 if success.
 * @param[in] stream parameter.
 */

int ControlStream(SLStreamParameter *para);

#ifdef __cplusplus
}
#endif
#endif  // LED_DECODER_H_
