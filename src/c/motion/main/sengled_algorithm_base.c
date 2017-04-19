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

void delete_instance(SLSHandle handle)
{
    delete_algorithm_instance(handle);
}
