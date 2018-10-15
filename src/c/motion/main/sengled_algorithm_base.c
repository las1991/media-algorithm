#include"sengled_algorithm_base.h"
#include"sengled_algorithm.h"
#include<string.h>

void* g_log_callback;

void SetLogCallback(void* log_callback)
{
    g_log_callback = log_callback;
}

SLSHandle create_instance(const char* token)
{
    common_params params;
    memset(&params, 0, sizeof(common_params));
    memcpy(params.token, token, strlen(token));
    params.log_callback = g_log_callback;
    return create_algorithm_instance(&params);
}

void feed(SLSHandle handle, void* frame, int frame_width, int frame_height, void* algorithm_params, algorithm_base_result* result)
{
    feed_frame(handle, frame, frame_width, frame_height, algorithm_params, (algorithm_result* )result);
}

void feed2(SLSHandle handle, void* frame, int frame_width, int frame_height, void* algorithm_params, algorithm_base_result2* result)
{
    result->size = ALGORITHM_MAX_RESULT_LENGTH;
    result->result = malloc(result->size);
    memset(result->result, 0, result->size);
    algorithm_result res;
    memset(&res, 0, sizeof(algorithm_result));
    feed_frame(handle, frame, frame_width, frame_height, algorithm_params, &res);
    memcpy(result->result, res.result, result->size); 
}

void destroy_result(algorithm_base_result2* result)
{
    free(result->result);
}

void delete_instance(SLSHandle handle)
{
    delete_algorithm_instance(handle);
}
