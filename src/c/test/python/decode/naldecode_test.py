from  naldecode import H264NalDecoder 
import os
import ctypes
import sys

def log_print(level, log_message):
    print(log_message)
    pass

CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

if __name__ == "__main__":

    os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__)
    print os.environ['PYTHON_C_LIB']
    
    decode = H264NalDecoder(CALL_BACK_FUN(log_print))

    #b = open('./../data/nal_data_1280x720', "rb").read()
    b = open('./../data/nal_813_1280x720', "rb").read()
    print ("b len = %d"%len(b))
    token = "ABCDE"
    decode_results = decode.decode(token, str(b), True)
    if decode_results:
        width, height, yuv_data = decode_results
        print("width = %d, height = %d"%(width, height))
    
        f = open('data.yuv', 'wb')
        f.write(yuv_data)
        f.close()
