import ctypes 

ALGORITHM_MAX_LENGTH = 1024
CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_char_p)

class Rect(ctypes.Structure):
    _fields_ = [("x",            ctypes.c_int),
                ("y",            ctypes.c_int),
                ("width",        ctypes.c_int),
                ("height",       ctypes.c_int)
               ]

class Zone(ctypes.Structure):
    _fields_ = [("zone_count",   ctypes.c_int),
                ("zone_id",      ctypes.c_int * 3),
                ("roi_rect",     Rect * 3)
               ]

class MotionCommonParams(ctypes.Structure):
    _fields_ = [("sensitivity",  ctypes.c_int)]

class MotionParams(ctypes.Structure):
    _fields_ = [("zoneparams",   Zone),
                ("commonparams", MotionCommonParams)
               ]
