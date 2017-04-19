#include "AlgorithmManager.h"
#include "motionAction.h"
#include "cJSON.h"
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
    char *json_message = (char*)algorithm_param;
    rv->plog->log_print(SLS_LOG_DEBUG,"params=%s",json_message);

    cJSON *pjson = cJSON_Parse(json_message);
    if( pjson == NULL )
    {
        rv->plog->log_print(SLS_LOG_ERROR,"parse json error");
        return;
    }
    cJSON *psensi = cJSON_GetObjectItem(pjson,"sensitivity");
    if( psensi == NULL )
    {
        rv->plog->log_print(SLS_LOG_ERROR,"parse json sensitivity error");
        return;
    }
	rv->pAlgoParams->motion_setting_params.sensitivity = psensi->valueint;

    cJSON *pparam_array= cJSON_GetObjectItem(pjson,"dataList");
    if(pparam_array == NULL || cJSON_Array != pparam_array->type)
    {
        rv->plog->log_print(SLS_LOG_ERROR,"parse json dataList error");
        return;
    } 
    int size = cJSON_GetArraySize(pparam_array);
    if(size > 3 )
    {
        rv->plog->log_print(SLS_LOG_ERROR,"params number overflow");
        return;
    }
	rv->pAlgoParams->motion_params.zone_count = size;
    for( int i = 0; i < size; i++ )
    {
        cJSON *item = cJSON_GetArrayItem(pparam_array,i);
        if( item==NULL ) return;
        cJSON *subitem = item->child;
        if(subitem == NULL || subitem->next == NULL )
        {
            rv->plog->log_print(SLS_LOG_ERROR,"parse dataList element error");
            return;
        }
        rv->plog->log_print(SLS_LOG_DEBUG,"%s:%d\n",subitem->string,subitem->valueint);
        rv->pAlgoParams->motion_params.zone_id[i] = subitem->valueint;

        rv->plog->log_print(SLS_LOG_DEBUG,"%s:%s\n",subitem->next->string,subitem->next->valuestring);
        char *str = subitem->next->valuestring;
        char *token = strtok(str,",");
        int z_pos[4]={0},count=0;
        while( token != NULL && count < 4)
        {
            int p = atoi(token);
            rv->plog->log_print(SLS_LOG_DEBUG,"pos=%d\n",p);
            z_pos[count]=p;
            token = strtok(NULL,",");
            count++;
        }
        rv->pAlgoParams->motion_params.zone_pos[i].x = z_pos[0];
        rv->pAlgoParams->motion_params.zone_pos[i].y = z_pos[1];
        rv->pAlgoParams->motion_params.zone_pos[i].width = z_pos[2];
        rv->pAlgoParams->motion_params.zone_pos[i].height = z_pos[3];
    }
    cJSON_Delete(pjson);
     
}

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

