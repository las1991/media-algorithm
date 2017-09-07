#ifndef SENGLED_ALGORITHM_H_
#define SENGLED_ALGORITHM_H_
 
#ifdef __cplusplus
extern "C" {
#endif 

#define  ALGORITHM_MAX_LENGTH                 1024
#define  ALGORITHM_MAX_RESULT_LENGTH          10*1024

/**
 * Standard information.
 **/
#define  SLS_LOG_DEBUG                         0x0001

/**
 * Stuff which is only useful for algorithm developers.
 **/
#define  SLS_LOG_INFO                        0x0002

/**
 * Something somehow does not look correct
 **/
#define  SLS_LOG_WARNING                      0x0003

/**
 * Something went wrong and cannot losslessly be recovered
 **/
#define  SLS_LOG_ERROR                        0x0004

/**
 * Something went wrong and recovery is not possible
**/
#define  SLS_LOG_FATAL                        0x0005


#define SLSHandle  void*

#ifndef bool
#define bool       int
#endif

typedef struct common_params
{
    /**
     * camera uuid
     **/
    char token[ALGORITHM_MAX_LENGTH];
    /**
     * print log message 
     * @param level   log level by defined in the text
     * @param message log message
     **/
    void (*log_callback)(int level, const char* message);

}common_params;

typedef struct algorithm_result
{
	/**
	 * bresult  whether detected  a object
	 **/
    bool bresult;
	/**
	 * result   The result is the json format and been used to describe some detection result infomation; eg. detection rect
	 **/
	char result[ALGORITHM_MAX_RESULT_LENGTH];

}algorithm_result;
 
/**
* create a algorithm instance
* input
* @param params is a common_params  struct ptr
* ouput
* @param Handle is inner pointer
**/
SLSHandle create_algorithm_instance(common_params* params);
 
/**
* feed a new frame 
 * input
* @param handle is the parameter which is created by create_algorithm_instance
* @param frame is the YUV data
* @param frame_width is the frame width
* @param frame_height is the frame height
* @param algorithm_params is (json or struct) parameters data which is required by algorithm
* output
* @param result is the algorithm detection result
**/
void feed_frame(SLSHandle handle, void* frame, int frame_width, int frame_height, void* algorithm_params, algorithm_result* result);
 
/**
* delete a algorithm instance
* input
* @param handle is the parameter which is created by create_algorithm_instance 
**/
void delete_algorithm_instance(SLSHandle handle);

#ifdef __cplusplus
}
#endif 

#endif //SENGLED_ALGORITHM_H_
