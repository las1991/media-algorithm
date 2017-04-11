from  naldecode import H264NalDecoder 
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
    
    b = open('data', "rb").read()
    print ("b len = %d"%len(b))
    token = "ABCDE"
    tsum = 0
    deal_num = 0
    while 1:
        #time.sleep(0.1)
        t1 = time.time()    
        decode = H264NalDecoder(CALL_BACK_FUN(log_print))
    
        decode_results = decode.decode(token, str(b), True)
        t2 = time.time()
        tsum = t2 - t1 + tsum
        deal_num = deal_num + 1
        if deal_num == 10000:
            print "deal result :" + str(deal_num / tsum) + " f/s"
            import sys
            sys.exit()
        if decode_results:
            width, height, yuv_data = decode_results
            #print("width = %d, height = %d"%(width, height))
    
            '''
            f = open('data.yuv', 'wb')
            f.write(yuv_data)
            f.close()
            '''
