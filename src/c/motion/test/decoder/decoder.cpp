//
//  decoder.cpp
//  robotVision_no_zone
//
//  Created by zhongjywork on 15-5-22.
//  Copyright (c) 2015年 zhongjywork. All rights reserved.
//
#include "decoder.h"
#include <stdlib.h>
#include <string.h>
#include "debug.h"
#include "rv_resource.h"
using namespace std;
using namespace cv;

// decoder log callback function

void ffmpeg_log(void *ptr, int level, const char *fmt, va_list vl)
{
	char log[2048];
	if (level <= 32) {
		vsnprintf(log, sizeof(log), fmt, vl);
		//fprintf(stderr, "%s", log);
		printf("%s\n",log);
	}
}



//ready to decode
int decodeVideo(SLStreamParameter &stream_parameter_, const char *stream_url )
{

    RegisterFFMPEG();

	memset(&stream_parameter_, 0, sizeof(stream_parameter_));
	strncpy(stream_parameter_.url_name, stream_url, SL_MAX_PATH_LENGTH);

	stream_parameter_.url_name[SL_MAX_PATH_LENGTH - 1] = '\0';
	stream_parameter_.profile = 0;
	stream_parameter_.save_image_type = SLSAVE_IMAGE_TYPE_NONE;
	stream_parameter_.ffmpeg_log_callback = ffmpeg_log;            //9.8 modify log function
	

    printf("------ decode : begin decode for url : %s ------\n", stream_parameter_.url_name);

	int ret = OpenStream(&stream_parameter_);

	printf("------ decode : end of OpenStream() ------\n");
	if (ret < 0)
    	{
            printf("It is failed to open stream.\n");
            if (stream_parameter_.scaled_frame_data) 
	        {
              //  free(stream_parameter_.scaled_frame_data);
               //stream_parameter_.scaled_frame_data = NULL;
            }
	    
        UnregisterFFMPEG();
        return false;
        }

	
	return true;

}

// get frame from decoder
int getFrameFromDecoder(SLStreamParameter &stream_parameter_)
{
	
	int ret = 0;
	ret = DecodeStream(&stream_parameter_);
        if (ret < 0)
        {
            printf("It is failed to decode stream.\n");
	        return false;
        }

	Mat mat(SRC_FRAME_HEIGHT, SRC_FRAME_WIDTH, CV_8U, (unsigned char*)stream_parameter_.scaled_frame_data);


	return true;

}


void stopGetFrame(SLStreamParameter &stream_parameter_)
{

    ControlStream(&stream_parameter_);

}


void closeVideoDecoder(SLStreamParameter &stream_parameter_)
{       
    CloseStream(&stream_parameter_);
    UnregisterFFMPEG();
   
    if (stream_parameter_.scaled_frame_data)
    {
        free(stream_parameter_.scaled_frame_data);
        stream_parameter_.scaled_frame_data = NULL;
    }
}




















