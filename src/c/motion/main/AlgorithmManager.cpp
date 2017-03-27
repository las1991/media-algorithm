#include "AlgorithmManager.h"
#include "motionAction.h"

void LocalPrint( void* context )
{
	rvResource *rv = static_cast<rvResource*>(context);
	if ( rv == NULL )
	{
		return;
	}
	rv->plog->log_print(SLS_LOG_DEBUG,"------- Current video : %s ------", 
		rv->token );
	int zone_count = rv->pAlgoParams->motion_params.zone_count;
	zone_count = zone_count > 3 ? 3:zone_count;
	rv->plog->log_print(SLS_LOG_DEBUG,"motion params zone_count=%d",zone_count );

	for ( int i = 0; i < zone_count; i++ )
	{
		SLS_RoiRect roirect = rv->pAlgoParams->motion_params.zone_pos[i];
		cv::Rect cvrect(roirect.x,roirect.y,roirect.width,roirect.height);
		rv->plog->log_print(SLS_LOG_DEBUG," motion params zone_id=%d,Rect=(%d,%d,%d,%d)",
			rv->pAlgoParams->motion_params.zone_id[i],roirect.x,roirect.y,roirect.width,roirect.height );
	}
	SLS_CommonSettingParams m_setting_params = rv->pAlgoParams->motion_setting_params;
	rv->plog->log_print(SLS_LOG_DEBUG,"motion_setting_params : sensitivity=%d",
		m_setting_params.sensitivity );
}

void CopyAlgorithmParams( rvResource *rv, void *algorithm_param )
{
    SLS_AlgorithmParams *pParams = (SLS_AlgorithmParams*)algorithm_param;
	rv->pAlgoParams->motion_params.zone_count = pParams->motion_params.zone_count;
	int zone_count = rv->pAlgoParams->motion_params.zone_count;
	for ( int i = 0 ; i < zone_count; i++ )
	{
        rv->pAlgoParams->motion_params.zone_id[i] = pParams->motion_params.zone_id[i];
        rv->pAlgoParams->motion_params.zone_pos[i] = pParams->motion_params.zone_pos[i];
	}
	rv->pAlgoParams->motion_setting_params =  pParams->motion_setting_params;
}
/*
bool CompareRect( SLS_RoiRect &last_rect, SLS_RoiRect &cur_rect )
{
	return last_rect.x != cur_rect.x ? false : 
		last_rect.y != cur_rect.y ? false :
		last_rect.width != cur_rect.width ? false :
		last_rect.height != cur_rect.height ? false :
		true;
}

bool CompareCommonParams( SLS_CommonSettingParams &last_param, SLS_CommonSettingParams &cur_param )
{
	return last_param.frame_skip != cur_param.frame_skip ? false :
		last_param.sensitivity != cur_param.sensitivity ? false :
		last_param.sleep_period != cur_param.sleep_period ? false :
		true;
}

bool IsMotionParamsUpdate( rvResource *rv, void *algorithm_param )
{
     SLS_AlgorithmParams *pParams = (SLS_AlgorithmParams*)algorithm_param;
	 int zone_cont = pParams->motion_params.zone_count;
	 
	 if( zone_cont != rv->pAlgoParams->motion_params.zone_count )
	 {
		 rv->plog->log_print( "%s--zone count has been updated",rv->alocontext.token );
		 return true;
	 }
	 for ( int i = 0; i < zone_cont; i++ )
	 {
		 if( !CompareRect( rv->pAlgoParams->motion_params.zone_pos[i], pParams->motion_params.zone_pos[i] ) )
		 {
			 rv->plog->log_print( "%s--zone pos has been updated", rv->alocontext.token );
			 return true;
		 }
	 }
	 if ( !CompareCommonParams(rv->pAlgoParams->motion_setting_params,pParams->motion_setting_params) )
	 {
		 rv->plog->log_print( "%s---motions seting params has been updated ", rv->alocontext.token );
		 return true;
	 }
	 return false;
}
*/

void DispatchAlgorithm( rvResource *rv,void *frame,int frame_width,int frame_height,
        void *algorithm_param, algorithm_result *result )
{
	if ( frame_height == 0 || frame_width == 0 || algorithm_param == NULL )
	{
		rv->plog->log_print(SLS_LOG_DEBUG, "width or height is zero" );
		return;
	}

	//gettimeofday(&(rv->runTime), NULL);
	cv::Mat mat(frame_height*3/2, frame_width, CV_8U, (unsigned char*)frame );
	rv->srcFrame = mat;
	/*if ( (rv->frame_width != 0 && rv->frame_height != 0) &&
		(rv->frame_height != frame_height || rv->frame_width != frame_width) )
    {
		rv->plog->log_print( "frame width and height has changed" );
		rv->firstMotionReport = true;
    }*/
	rv->frame_width = frame_width;
	rv->frame_height = frame_height;
	//rv->frameCount++;
    /*
    bool iscopy = false;
	if ( rv->isParamsUpdate )
	{
		if ( IsMotionParamsUpdate( rv, algorithm_param ) )
		{
			CopyAlgorithmParams(rv,algorithm_param);
            LocalPrint( rv );
			rv->isModelUpdate = true;
			rv->firstMotionReport = true;
			iscopy = true;			
		}        
		rv->isParamsUpdate = false;
		rv->plog->log_print( "----Current Video %s---recive Update message",rv->alocontext.token );
	}
    if ( !iscopy )
    {
        CopyAlgorithmParams(rv,algorithm_param);
    }*/

    CopyAlgorithmParams(rv,algorithm_param);
	mMotionAction(rv,result);
}

