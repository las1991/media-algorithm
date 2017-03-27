#ifndef _ROBOT_VISION_H_
#define _ROBOT_VISION_H_

#include <sys/types.h>
#include <stdarg.h>

         
#define ALGORITHM_MAX_PATH_LENGTH             1024


#ifdef __cplusplus
extern "C"{
#endif

/*
** struct for interest rect
*/
typedef struct SLS_RoiRect
{
	int   x;
	int   y;
	int   width;
	int   height;
}SLS_RoiRect;


/**
    struct for motion params
**/
typedef struct SLS_MotionParams
{
    int              zone_count;
	int              zone_id[3];
	SLS_RoiRect      zone_pos[3];
}SLS_MotionParams;

/**
    struct for algorithm common params
**/

typedef struct SLS_CommonSettingParams
{
	int sensitivity;
}SLS_CommonSettingParams;


/**
struct for Algorithm params
**/
typedef struct SLS_AlgorithmParams
{
    SLS_MotionParams            motion_params;
	SLS_CommonSettingParams     motion_setting_params;
}SLS_AlgorithmParams;

#ifdef __cplusplus
}
#endif

#endif //_ROBOT_VISION_H_
