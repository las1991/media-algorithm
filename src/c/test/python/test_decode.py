#from __future__ import print_function
import os
import ctypes

from tests_common import NewAlgorithmTests
from decode import naldecode #import H264NalDecoder 

class decode_test(NewAlgorithmTests):

    os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__) + '/clib/'
 
    def test_decode(self):
        print ""
        print "test decode start..."
        os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__) + '/clib/'

        samples = ['nal_A47_1920x1080', 'nal_9A1_1280x720', 'nal_813_640x360']
        results = ['yuv_A47_1920x1080', 'yuv_9A1_1280x720', "yuv_813_640x360"]
        token = "ABCDE"

        for index, sample in enumerate(samples):
            img = self.get_sample(sample)
            #img = open("data/nal_data_1280x720", "rb").read()
            decode = naldecode.H264NalDecoder(self.log_callback)
    
            decode_results = decode.decode(token, str(img), True)
            if decode_results != -1 and decode_results != 0:
                width, height, yuv_data = decode_results
                src_md5 = self.hashimg(yuv_data)
                
                dst_img = self.get_sample(results[index])
                dst_md5 = self.hashimg(dst_img)

                self.assertEqual(src_md5, dst_md5)
                print "test decode [%d] ok"%index
            else:
                self.assertEqual(0, 1)
                print "test decode [%d] failed"%index

    def test_decode_err(self): 
        # err test
        print ""
        print "test decode err sample start..."
        err_samples = ['nal_A47_1920x1080_err', 'err']
        token = "ABCD"
        
        for index, sample in enumerate(err_samples):

            img = self.get_sample(sample)
            decode = naldecode.H264NalDecoder(self.log_callback)
            decode_results = decode.decode(token, str(img), True)
            if decode_results == -1:
                self.assertEqual(0, decode_results)
                print "test decode err smaple [%d] failed"%index
            elif decode_results == 0:
                self.assertEqual(0, decode_results)
                print "test decode err sample [%d] ok"%index

