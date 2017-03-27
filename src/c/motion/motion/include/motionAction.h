//
//  motionAction.h
//  RobotVision
//
//  Created by zhongjywork on 15-5-15.
//  Copyright (c) 2015年 zhongjywork. All rights reserved.
//

#ifndef __RobotVision__motionAction__
#define __RobotVision__motionAction__

#include "rv_resource.h"
#include <stdio.h>
#include <sys/time.h>

#define MOTION_REPORT_SLEEP_TIME    5

void mMotionAction(rvResource* rv,algorithm_result *result);

#endif /* defined(__RobotVision__motionAction__) */

