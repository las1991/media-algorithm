#include "robot_vision.h"
#include "rv_resource.h"
#include "debug.h"
#include "AlgorithmManager.h"
#include "sengled_algorithm.h"

SLSHandle create_algorithm_instance(common_params* params)
{
    if(params == NULL)
    {
		printf("init params is null\n");		
		return NULL;
    }
    rvResource *prv_resource = new rvResource();
	if ( prv_resource == NULL )
	{
		return NULL;
	}
	strncpy( prv_resource->token,params->token,strlen(params->token));
    if ( params->log_callback == NULL )
    {
		printf("log_callback is NULL  !!!\n");
        return NULL;
    }
	CLogPrint *plogprint = new CLogPrint( params->log_callback );
	prv_resource->plog = plogprint;
		
	prv_resource->plog->log_print(SLS_LOG_DEBUG,"%s->init success !!!!",params->token);
    
    return (SLSHandle)prv_resource;    
}

void feed_frame(SLSHandle handle, void* frame, int frame_width, int frame_height, void* algorithm_params, algorithm_result* result)
{
    rvResource *rv = static_cast<rvResource*>(handle);
	if ( rv == NULL || algorithm_params == NULL || frame == NULL || frame_width == 0
      || frame_height==0 || algorithm_params == NULL )
	{
		rv->plog->log_print(SLS_LOG_ERROR,"context param error!!!!" );
		return;
	}

    DispatchAlgorithm( rv, frame,frame_width, frame_height,(void*)algorithm_params,result);
}

void delete_algorithm_instance(SLSHandle handle)
{
	rvResource *rv = static_cast<rvResource*>(handle);
    if ( rv != NULL )
    {
		rv->plog->log_print(SLS_LOG_DEBUG,"%s:Recive stop message\n",rv->token );
		delete rv->plog;
		delete rv;	
    }
}




