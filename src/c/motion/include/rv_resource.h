#ifndef _RV_RESOURCE_H_
#define _RV_RESOURCE_H_

#include <opencv2/opencv.hpp>
#include <map>
#include <deque>
#include <vector>
#include <sys/time.h>
#include "robot_vision.h"
#include "sengled_algorithm.h"
#include "vibe-background-sequential.h"
using namespace cv;
using namespace std;
#include "debug.h"
#define  MASK_ARRAY_COUNT    2
//picture name
#define PICTURE_TYPE_MOTION         "_motion_"

#define SLS_EVENT_TYPE_MOTION				"event_motion"	
#define SLS_MAX_BUFFER_LENGTH        512
//max number of processor in a machine
#define    GLOBAL_HANDLE_MAP_LEN		1024

#define    SRC_FRAME_WIDTH				 960
#define    SRC_FRAME_HEIGHT			     540

#define    MINMUM_FRAME_WIDTH            640
#define    MINMUM_FRAME_HEIGHT           360

class CLogPrint;

struct rvResource
{

    char token[SLS_MAX_BUFFER_LENGTH];	

	//src frame
	cv::Mat srcFrame;
	int  frame_width;
	int  frame_height;
    //frame count
	uint64_t frameCount;
	//vibe algorithm 
	vibeModel_Sequential_t *model[3];
	Mat bg_frame[3];
	bool isModelUpdate;

	//for gesture
    //flag for params update
	bool isParamsUpdate;
	
	
	timeval runTime;

    //algorithm param
	SLS_AlgorithmParams* pAlgoParams;
	
	CLogPrint  *plog;  //for log output
	//initiatial function
	rvResource()/*:myHOG(cv::Size(64, 64), cv::Size(16, 16), cv::Size(8, 8), cv::Size(8, 8), 9),
		humanHOG(Size(64, 128), Size(16, 16), Size(8, 8), Size(8, 8), 9)*/
	{
		frameCount = 0;

        frame_width = 0;
		frame_height = 0;
        
		//vibe algorithm
		memset( model,0,sizeof(model) );
        memset(token,0,SLS_MAX_BUFFER_LENGTH);
		//memset( motion_count,0,sizeof(motion_count));
		isModelUpdate = true;


        isParamsUpdate = true;

		pAlgoParams = new SLS_AlgorithmParams();
        memset( pAlgoParams,0,sizeof(SLS_AlgorithmParams) );
		pAlgoParams->motion_params.zone_count = -1;
		plog = NULL;
	}

	~rvResource()
	{
		delete pAlgoParams;

		for ( int i = 0; i < 3; i++ )
		{
			if ( model[i] != NULL )
			{
				libvibeModel_Sequential_Free(model[i]);
				model[i]=NULL;
			}
		}
	}
	
};


#endif  //_RV_RESOURCE_H_




