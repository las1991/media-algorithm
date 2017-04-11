import os
import unittest
import hashlib
import ctypes

CALL_BACK_FUN = ctypes.CFUNCTYPE(None, ctypes.c_int, ctypes.c_char_p)

'''
c log level
SLS_LOG_INFO     0x0001
SLS_LOG_DEBUG    0x0002 
SLS_LOG_WARNING  0x0003
SLS_LOG_ERROR    0x0004
SLS_LOG_FATAL    0x0005
'''


def log_print(level, log_message):
    if level == 1 or level == 3 or level == 4 or level == 5:
        pass
        #print(log_message)

class NewAlgorithmTests(unittest.TestCase):

    # path to local repository folder containing 'samples' folder
    repoPath = None
    extraTestDataPath = None
    log_callback = CALL_BACK_FUN(log_print) 

    def get_sample(self, filename):

        if not filename in self.image_cache:

            filedata = None
            if NewAlgorithmTests.repoPath is not None:
                candidate = NewAlgorithmTests.repoPath + '/' + filename
                if os.path.isfile(candidate):
                    with open(candidate, 'rb') as f:
                        filedata = f.read()
            if NewAlgorithmTests.extraTestDataPath is not None:
                candidate = NewAlgorithmTests.extraTestDataPath + '/' + filename
                if os.path.isfile(candidate):
                    with open(candidate, 'rb') as f:
                        filedata = f.read()

            if filedata is None:
                return None

            self.image_cache[filename] = str(filedata)

        return self.image_cache[filename]

    def setUp(self):

        self.image_cache = {}
    
    def hashimg(self, im):

        return hashlib.md5(im).hexdigest()
