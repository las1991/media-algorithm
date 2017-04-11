import sys
sys.path.append("..")
import motion_types
import motion
import ctypes
from ctypes import *
import os

def log_print(level, log_message):
    print log_message

CALL_BACK_FUN = CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)
log_callback = CALL_BACK_FUN(log_print)

if __name__ == "__main__":
    
    os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__)
    print os.environ['PYTHON_C_LIB']
    #frame = "asdfasdfa"
    frame = open('../data/yuv_9A1_1280x720', "rb").read()
    print "frame len = %d"%len(frame)

    frame_width = 1280
    frame_height = 720
    token = "DD69960CE1DF6C27EBED2B7889CD8F5A"
    
    frame1 = open('../data/yuv_813_1280x720', 'rb').read()
    motion_sample = motion.MotionDetect(log_callback)
    uuid = motion_sample.init(token)

    if not uuid:
        print "uuid = %s"%uuid
        pass
    else:
        '''
        motion_params = motion_types.MotionParams()
        motion_params.zoneparams.zone_count = 1
        motion_params.zoneparams.zone_id[0] = 297
        motion_params.zoneparams.roi_rect[0].x = 17
        motion_params.zoneparams.roi_rect[0].y = 27
        motion_params.zoneparams.roi_rect[0].width = 62
        motion_params.zoneparams.roi_rect[0].height = 72
        motion_params.commonparams.sensitivity = 1000
        '''
        motion_params = {}
        zone_id  = []
        zone_pos = []
        motion_params["zone_count"] = 0
        zone_id.append(297)
        zone_id.append(298)
        zone_pos.append("17,27,62,72")
        zone_pos.append("17,27,62,72")
        motion_params["id"] = zone_id
        motion_params["pos"] = zone_pos
        motion_params["sensitivity"] = 1000 
        if 1:
            result = motion_sample.feed(str(frame), frame_width, frame_height, motion_params)
            if result == 0:
                print "result : " + str(result)
            else:
                print "result : " + str(result)
             
            result = motion_sample.feed(str(frame1), frame_width, frame_height, motion_params)
            if result == 0:
                print "result1 : " + str(result)
            else:
                print "result1 : " + str(result)
                #print result.strip()
                import json
                d1 = json.dumps(str(result).strip('\0'))
                print "d1 = " + d1
        motion_sample.close()
