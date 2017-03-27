from py4j.java_gateway import (JavaGateway, CallbackServerParameters, GatewayParameters)
import sys,os

import ctypes
from ctypes import *
from ctypes import c_int
from ctypes import c_char
from ctypes import POINTER


import time

logger = None

def log_print(log_message):
    logger.info(log_message);
    #print log_message

def create_funptr():
    CALL_BACK_FUN = CFUNCTYPE(None, c_char_p)
    return CALL_BACK_FUN(log_print)

log_callback = create_funptr()

class OutFrame(Structure):
    _fields_ = [("data", ctypes.POINTER(c_char)), ("size", c_int)]

class Function(object):
    def __init__(self):
        #global logger 
        #logger.info( "init...")
        #logger.info("c lib path = %s", os.getenv('PYTHON_C_LIB'))
        self.so = ctypes.CDLL(str(os.getenv('PYTHON_C_LIB')) + "/libconvertjpg.so")
        self.so.GlobleInit(log_callback)
    
    def hello(self):
        logger.info("receive heartbeat hello!")
        return "hello"

    def apply(self, token, data = None):

        #logger.info("get data... len = %d"%len(data))
        if len(data) == 0:
            logger.info("receive data len = 0!")
            return
        frame = OutFrame()
        ret = self.so.ConvertJpg(str(data), len(data), token, byref(frame))
        if int(ret) != 0:
            logger.info("decode frame failed!")
            self.so.Destroy(byref(frame))
            return
        #logger.info("convertjpg ret = %d, frame len = %d", ret, int(frame.size))
        frame_data = string_at(frame.data, frame.size)
        frame_tmp = bytearray(frame_data) 
        self.so.Destroy(byref(frame))
        #logger.info("ret size = %d"%len(frame_tmp))

        return frame_tmp

    class Java:
        implements = ["com.sengled.media.worker.Function"]
        
if __name__ == "__main__":

    java_port = sys.argv[1]
    function = Function()
    gateway = JavaGateway(
        gateway_parameters = GatewayParameters(port = int(java_port)),
        callback_server_parameters = CallbackServerParameters(port = 0),
        python_server_entry_point = function)
    
    #global logger
    logger = gateway.jvm.com.sengled.media.worker.log.PythonLogger.LOGGER
    logger.info("start...")
    python_port = gateway.get_callback_server().get_listening_port()
    #logger.info("get python port...  port = %d"%python_port)
    java_gateway_server = gateway.java_gateway_server
    java_gateway_server.resetCallbackClient(
        gateway.java_gateway_server.getCallbackClient().getAddress(),
        python_port)

