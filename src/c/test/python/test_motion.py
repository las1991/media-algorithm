import os

from tests_common import NewAlgorithmTests
from motion import motion #import H264NalDecoder 
from motion import motion_types

class motion_test(NewAlgorithmTests):

    def test_motion(self):
        print ""
        print "test motion start..."
        os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__) + '/clib/'

        names = ['yuv_9A1_', 'yuv_813_']
        sizes = ['1280x720']
        token = "ABCDE"
        
        result = "{\n\t\"zone_id\":\t-1\n}"

        motion_sample = motion.MotionDetect(self.log_callback)
        uuid = motion_sample.init(token) 
        
        motion_params = {}
        zone_id  = []
        zone_pos = []
        motion_params["zone_count"] = 0
        zone_id.append(297)
        zone_id.append(298)
        zone_pos.append("17,27,62,72")
        zone_pos.append("17,27,62,72")
        motion_params["id"] = zone_id
        motion_params["pos"] = zone_pos
        motion_params["sensitivity"] = 1000 
       
        img = self.get_sample(names[0] + sizes[0])
        result1 = motion_sample.feed(str(img), int(sizes[0].split('x')[0]), int(sizes[0].split('x')[1]), motion_params)
        
        img = self.get_sample(names[1] + sizes[0])
        result2 = motion_sample.feed(str(img), int(sizes[0].split('x')[0]), int(sizes[0].split('x')[1]), motion_params)
        
        self.assertEqual(str(result2).strip('\0'), result)
        print "test motion [0] ok" 
        motion_sample.close()
