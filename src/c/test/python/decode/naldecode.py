import os
import ctypes

class NALDecoderError(Exception):
    pass

class VideoFrame(ctypes.Structure):
    _fields_ = [("width",  ctypes.c_int),
                ("height", ctypes.c_int),
                ("data",   ctypes.POINTER(ctypes.c_char)),
                ("size",   ctypes.c_int)]

def save_yuv(yuv_data):
    
    f = open('data.yuv', 'wb')
    f.write(yuv_data)
    f.close()

class H264NalDecoder(object):

    def __init__(self, log_callback):
        
        self.log_callback = log_callback
   
    def _decode_nal(self, token, nal, always_decode=False):
        
        decoder = ctypes.CDLL(str(os.environ['PYTHON_C_LIB']) + "/../clib/libnal_decoder.so")
        decoder.Init(self.log_callback)
        initialized = False
        frame = VideoFrame()
        try:
            if always_decode:
                initialized = True
                ret = decoder.DecodeNal(str(nal), len(nal), token, ctypes.byref(frame))
                if ret < 0:
                    # Failed decoding
                    decoder.Destroy(ctypes.byref(frame))
                    #raise NALDecoderError('fail to decode')
                    return 0 
                yuv_data = ctypes.string_at(frame.data, frame.size)
                
                #save_yuv(yuv_data)
                return frame.width, frame.height, yuv_data

            else:
                return 0
        except:
            return -1
        finally:
            decoder.Destroy(ctypes.byref(frame))

    def decode(self, token, nal, always_decode=False):

        decoding_results = self._decode_nal(token, nal, always_decode)

        return decoding_results
