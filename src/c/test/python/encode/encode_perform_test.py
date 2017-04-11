from  encodejpg import EncodeJPG 
import os
import ctypes
import time

def log_print(level, log_message):
    print(log_message)
    pass

CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

if __name__ == "__main__":
    os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__)
    print os.environ['PYTHON_C_LIB']
    
    b = open('yuv_data', "rb").read()
    print ("b len = %d"%len(b))
    token = "ABCDE"
    tsum = 0
    deal_num = 0
    while 1:
        #time.sleep(0.1)
        t1 = time.time()    
        encoder = EncodeJPG(CALL_BACK_FUN(log_print))
        
        encode_results = encoder.encode(token, 1280, 720, 1280, 720, str(b), True)
        
        t2 = time.time()
        
        tsum = t2 - t1 + tsum
        deal_num = deal_num + 1
        if deal_num == 10000:
            print "deal result :" + str(deal_num / tsum) + " f/s"
            import sys
            sys.exit()
        if encode_results:
            jpg_data = encode_results
            
            ''' 
            f = open('data.jpg', 'wb')
            f.write(jpg_data)
            f.close()
            '''
