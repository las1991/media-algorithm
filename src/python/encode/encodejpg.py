import os
import ctypes

class EncodeJPGError(Exception):
    pass

class VideoFrame(ctypes.Structure):
    _fields_ = [ ("data", ctypes.POINTER(ctypes.c_char)),
                ("size", ctypes.c_int)]

def save_jpg(jpg_data):
    
    f = open('data.jpg', 'wb')
    f.write(jpg_data)
    f.close()

class EncodeJPG(object):

    def __init__(self, log_callback):
        self.log_callback = log_callback

    def _encode_jpg(self, token, src_width, src_height, dst_width, dst_height, src_data, always_encode=False):

        encoder = ctypes.CDLL(str(os.environ['PYTHON_C_LIB']) + "/libencoder.so")
        encoder.Init(self.log_callback)
        initialized = False

        frame = VideoFrame()
        try:
            if always_encode:
                initialized = True
                ret = encoder.EncodeJPG(str(src_data), src_width, src_height, dst_width, dst_height, token, ctypes.byref(frame))
                if ret < 0:
                    # Failed decoding
                    encoder.Destroy(ctypes.byref(frame))
                    raise NALDecoderError('fail to decode')
                jpg_data = ctypes.string_at(frame.data, frame.size)
               
                #save_jpg(jpg_data)
                return jpg_data

            else:
                return None

        finally:
            encoder.Destroy(ctypes.byref(frame))

    def encode(self, token, src_width, src_height, dst_width, dst_height, src_data, always_encode=False):

        encode_results = self._encode_jpg(token, src_width, src_height, dst_width, dst_height, src_data, always_encode)

        return encode_results
