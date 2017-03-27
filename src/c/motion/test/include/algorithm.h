#ifndef _ALGORITHM_H_
#define _ALGORITHM_H_

/**
   struct for Rect 
**/
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
typedef struct SLS_Zone
{
    int              zone_count;
	int              zone_id[5];
	SLS_RoiRect      zone_pos[5];
}SLS_Zone;

/**
   struct for motion
**/

typedef struct SLS_MotionParams
{
    SLS_Zone motion_params;
	int   zone_id;
	void *bgmode;
	int bgmode_size;
}SLS_MotionParams;



#ifdef __cplusplus
extern "C" {
#endif

/**
   algorithm init
**/
bool SLS_Init();
/**
   motion init
   return motion context
**/
void SLS_InitMotion( SLS_MotionParams *init_motion_params );

/**
  algorithm destory
**/
void SLS_MotionDestory(SLS_MotionParams *init_motion_params);

/**
   algorithm motion
**/
void SLS_MotionUpdateModel( void *frame, int frame_width,int frame_height,SLS_MotionParams *motion_param);

bool SLS_MotionCheck( void *frame, int frame_width,int frame_height,SLS_MotionParams *motion_param);
/**
   algorithm gesture
**/
bool SLS_Gesture( void *frame, int frame_width,int frame_height );

/**
   algorithm expression
**/
bool SLS_Expression( void *frame, int frame_width,int frame_height );

/**
   algorithm human
**/
bool SLS_Human( void *frame, int frame_width,int frame_height );








#ifdef __cplusplus
}
#endif



#endif //_ALGORITHM_H_




