import os
import ctypes 
import uuid
import motion_types 

#CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

#def log_print(level, log_message):
#    print "level = %s log_message = %s"%(level, log_message)
#    #pass
ALGORITHM_MAX_LENGTH = 1024
CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

class CommonParams(ctypes.Structure):
    _fields_ = [("token",        ctypes.c_char * ALGORITHM_MAX_LENGTH),
                ("log_callback", CALL_BACK_FUN)
               ]

class Recognition_Result(ctypes.Structure):
    _fields_ = [("bresult",      ctypes.c_int),
                ("result",       ctypes.c_char * ALGORITHM_MAX_LENGTH * 10)
               ]

def create_motion_params(zone_count, zone_id_list, rect_list, sensitivity):

    zone_pos  = (motion_types.Rect * 3)
    zone_ids  = (ctypes.c_int * 3)
    zone_info = None

    if zone_count == 0:
        return motion_types.MotionParams(motion_types.Zone(0, zone_ids(0, 0, 0), 
                        zone_pos(motion_types.Rect(1, 1, 1, 1), 
                                 motion_types.Rect(1, 1, 1, 1),
                                 motion_types.Rect(1, 1, 1, 1))), motion_types.MotionCommonParams(sensitivity))
    elif zone_count == 1:
        zone_info = motion_types.Zone(zone_count, zone_ids(zone_id_list[0], 0, 0),
                zone_pos(motion_types.Rect(rect_list[0][0], rect_list[0][1], rect_list[0][2], rect_list[0][3]),
                         motion_types.Rect(0, 0, 0, 0),
                         motion_types.Rect(0, 0, 0, 0)))
    elif zone_count == 2:
        zone_info = motion_types.Zone(zone_count, zone_ids(zone_id_list[0], zone_id_list[1], 0),
                zone_pos(motion_types.Rect(rect_list[0][0], rect_list[0][1], rect_list[0][2], rect_list[0][3]),
                         motion_types.Rect(rect_list[1][0], rect_list[1][1], rect_list[1][2], rect_list[1][3]),
                         motion_types.Rect(0, 0, 0, 0)))
    elif zone_count == 3:
        zone_info = motion_types.Zone(zone_count, zone_ids(zone_id_list[0], zone_id_list[1], zone_id_list[2]),
                zone_pos(motion_types.Rect(rect_list[0][0], rect_list[0][1], rect_list[0][2], rect_list[0][3]),
                         motion_types.Rect(rect_list[1][0], rect_list[1][1], rect_list[1][2], rect_list[1][3]),
                         motion_types.Rect(rect_list[2][0], rect_list[2][1], rect_list[2][2], rect_list[2][3])))

    return motion_types.MotionParams(zone_info, motion_types.MotionCommonParams(sensitivity))

class my_void_p(ctypes.c_void_p):
    pass

class MotionDetect(object):

    def __init__(self, log_callback):
        self.motion = ctypes.CDLL(str(os.environ['PYTHON_C_LIB']) + "/../clib/libmotion.so")
        #self.motion = ctypes.CDLL("./libmotion.so")
        #self.log_fun = CALL_BACK_FUN(log_print)
        self.log_fun = log_callback

    def init(self, token):
        common_params = CommonParams(token, self.log_fun)
        
        init_fun = self.motion.create_algorithm_instance
        init_fun.restype = my_void_p
        self.context = init_fun(ctypes.byref(common_params))
        self.token = token
        if not self.context:
            return None
        else:
            return str(uuid.uuid1()) 

    def feed(self, frame, frame_width, frame_height, motion_params):
        
        recognition_result = Recognition_Result()
        zone_count = motion_params["zone_count"]
        
        zone_ids = []
        for j in range(zone_count):
            zone_id = int(motion_params["id"][j])
            zone_ids.append(zone_id)

        zone_pos = []
        for i in range(zone_count):
           pos = motion_params["pos"][i]
           points = pos.split(',')
           point_num = (int(points[0]), int(points[1]), int(points[2]), int(points[3]))
           zone_pos.append(point_num)
        
        params = create_motion_params(int(motion_params["zone_count"]), zone_ids, zone_pos, int(motion_params["sensitivity"]))

        self.motion.feed_frame(self.context, str(frame), frame_width, frame_height, ctypes.byref(params), ctypes.byref(recognition_result))
        if int(recognition_result.bresult) == 0:
            return 0
        elif int(recognition_result.bresult) == 1:
            result = ctypes.string_at(recognition_result.result, ALGORITHM_MAX_LENGTH * 10)
            return result 
        else:
            return -1

    def close(self):

        self.motion.delete_algorithm_instance(self.context)

