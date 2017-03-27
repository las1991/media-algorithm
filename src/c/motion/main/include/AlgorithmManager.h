#ifndef _ALGORITHMMANAGER_H_
#define _ALGORITHMMANAGER_H_

#include "rv_resource.h"
#include "sengled_algorithm.h"

void DispatchAlgorithm( rvResource *rv, void *frame,int frame_width,
        int frame_height,void *algorithm_param,algorithm_result *result );



#endif  //_ALGORITHMMANAGER_H_



