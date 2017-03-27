from  encodejpg import EncodeJPG 
import os
import ctypes

def log_print(level, log_message):
    print(log_message)
    pass

CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

if __name__ == "__main__":
    os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__)
    print os.environ['PYTHON_C_LIB']
    
    encoder = EncodeJPG(CALL_BACK_FUN(log_print))
    b = open('yuv_data', "rb").read()
    print ("b len = %d"%len(b))
    token = "ABCDE"
    encode_results = encoder.encode(token, 1280, 720, 640, 360, str(b), True)
    if encode_results:
        jpg_data = encode_results
    
        f = open('data.jpg', 'wb')
        f.write(jpg_data)
        f.close()
