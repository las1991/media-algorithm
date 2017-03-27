//
//  decoder.h
//  robotVision_no_zone
//
//  Created by zhongjywork on 15-5-22.
//  Copyright (c) 2015年 zhongjywork. All rights reserved.
//

#ifndef __robotVision_no_zone__decoder__
#define __robotVision_no_zone__decoder__

#include <stdio.h>
#include "debug.h"
#include <opencv2/opencv.hpp>
#include "led_decoder.h"
#define DECODER_TRY_REOPEN_COUNT	30
#define WAIT_CLOSEED_TIME			5


#ifdef __cplusplus
extern "C" {
#endif

int decodeVideo(SLStreamParameter &stream_parameter_, const char *stream_url);
int getFrameFromDecoder(SLStreamParameter &stream_parameter_);
void stopGetFrame(SLStreamParameter &stream_parameter_);
void closeVideoDecoder(SLStreamParameter &stream_parameter_);

#ifdef __cplusplus
}
#endif
#endif /* defined(__robotVision_no_zone__decoder__) */
