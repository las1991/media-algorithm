from py4j.java_gateway import (JavaGateway, CallbackServerParameters, GatewayParameters, java_import)
import sys,os

import ctypes

from decode.naldecode import H264NalDecoder
from encode.encodejpg import EncodeJPG

from motion.motion import MotionDetect

logger = None
gateway = None
algorithm_context = {}

'''
c log level
SLS_LOG_INFO     0x0001
SLS_LOG_DEBUG    0x0002 
SLS_LOG_WARNING  0x0003
SLS_LOG_ERROR    0x0004
SLS_LOG_FATAL    0x0005
'''

def log_print(level, log_message):

    if level == 1:
        logger.info(log_message)
    elif level == 2:
        logger.debug(log_message)
    elif level == 3:
        logger.warn(log_message)
    elif level == 4:
        logger.error(log_message)
    elif level == 5:
        logger.error(log_message)
    else:
        logger.info(log_message)
    #logger.info(log_message)

CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

log_callback = CALL_BACK_FUN(log_print)

class Function(object):

    def __init__(self):
        pass

    def decode(self, token, nal_data):

        if len(nal_data) == 0:
            log_print(0, "receive data len = 0!")
            return
        h264naldecoder = H264NalDecoder(log_callback)
        decode_results = h264naldecoder.decode(token, str(nal_data), True)
        if decode_results:
            width, height, yuv_data = decode_results
            return gateway.jvm.YUVImage(width, height, bytearray(yuv_data))
        else:
            return None

    def encode(self, token, src_width, src_height, dst_width, dst_height, yuv_data):    

        if len(yuv_data) == 0:
            log_print(0, "receive data len = 0!")
            return
        encodejpg = EncodeJPG(log_callback)
        str2 = "src width = " + str(src_width) + " str height = " + str(src_height) + " dst width =  " +str(dst_width) + " dst height " + str(dst_height)
        log_print(0, str2)

        encode_results = encodejpg.encode(token, src_width, src_height, dst_width, dst_height, str(yuv_data), True) 
        if encode_results:
            jpg_data = encode_results
            return bytearray(jpg_data)
        else:
            return "NORESULT"

    def newAlgorithmModel(self, algorithm_type, token):

        if algorithm_type == "motion":
            context = MotionDetect(log_callback)
            uuid = context.init(str(token))
            algorithm_context[uuid] = context
            log_print(0, "uuid = " + uuid)
            return uuid
        else:
            return None

    def feed(self, java_algorithm_object, yuv_image):
        
        java_uuid = java_algorithm_object.getPythonObjectId()
        try:
            context = algorithm_context[java_uuid]
        except:
            return "NULL_ALGORITHM_MODEL"

        width = yuv_image.getWidth()
        height = yuv_image.getHeight()
        yuv_data = yuv_image.getYUVData()

        java_params = java_algorithm_object.getParameters()
        algorithm_params = {}
        algorithm_params["sensitivity"] = java_params.get("sensitivity")
        algorithm_params["dataList"]  = java_params.get("dataList")
        zone_id  = []
        zone_pos = []
        for zone in algorithm_params["dataList"]:
            zone_id.append(zone["id"])
            zone_pos.append(zone["pos"])
        algorithm_params["id"] = zone_id
        algorithm_params["pos"] = zone_pos
        
        algorithm_params["zone_count"] = len(algorithm_params["dataList"])
        #str1 = "===== zone_count = " + str(algorithm_params["zone_count"]) + " sensitivity = " \
                 #+ str(algorithm_params["sensitivity"]) + " zone_id = " + str(zone_id) + " zone_pos = " + str(zone_pos)
        #log_print(0, str1)
        if context:
            result = context.feed(yuv_data, width, height, algorithm_params)
            if not result:
                return "NORESULT"
            else:
                return result
        else:
            log_print(0, "context not found")
            return "NULL_ALGORITHM_MODEL"

    def close(self, java_algorithm_object):
        
        java_uuid = java_algorithm_object.getPythonObjectId()
        try:
            context = algorithm_context[java_uuid]
        except:
            log_print(0, "context not exist!")
            return
        
        log_print(0, "==========close============")
        if context:
            context.close()
            try:
                algorithm_context.pop(java_uuid)
            except:
                log_print(0, "context not exist!")

    def hello(self):
        log_print(0, "receive heartbeat hello!")
        return "hello"

    def apply(self, token, data = None):
        pass

    class Java:
        implements = ["com.sengled.media.worker.Function"]
        
if __name__ == "__main__":

    java_port = sys.argv[1]
    function = Function()
    gateway = JavaGateway(
        gateway_parameters = GatewayParameters(port = int(java_port)),
        callback_server_parameters = CallbackServerParameters(port = 0),
        python_server_entry_point = function)
    java_import(gateway.jvm, "com.sengled.mediaworker.algorithm.pydto.*") 
    logger = gateway.jvm.PythonLogger

    log_print(0, "start...")
    python_port = gateway.get_callback_server().get_listening_port()
    java_gateway_server = gateway.java_gateway_server
    java_gateway_server.resetCallbackClient(
        gateway.java_gateway_server.getCallbackClient().getAddress(),
        python_port)

