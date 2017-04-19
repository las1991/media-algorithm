#ifndef SENGLED_ALGORITHM_BASE_H_
#define SENGLED_ALGORITHM_BASE_H_

#ifdef __cplusplus
extern "C" {
#endif 

#ifndef  ALGORITHM_MAX_LENGTH
#define  ALGORITHM_MAX_LENGTH                 1024
#define  ALGORITHM_MAX_RESULT_LENGTH          10*1024
#endif

#ifndef SLSHandle
#define SLSHandle  void*
#endif

#ifndef bool
#define bool       int
#endif

typedef struct algorithm_base_result
{
	/**
	 * bresult  whether detected  a object
	 **/
    bool bresult;
	/**
	 * result   The result is the json format and been used to describe some detection result infomation; eg. detection rect
	 **/
	char result[ALGORITHM_MAX_RESULT_LENGTH];

}algorithm_base_result;

void      SetLogCallback(void* callback);
 
SLSHandle create_instance(const char* token);
 
void      feed(SLSHandle handle, void* frame, int frame_width, int frame_height, void* algorithm_params, algorithm_base_result* result);
 
void      delete_instance(SLSHandle handle);

#ifdef __cplusplus
}
#endif //end __cplusplus

#endif //end SENGLED_ALGORITHM_BASE_H_
