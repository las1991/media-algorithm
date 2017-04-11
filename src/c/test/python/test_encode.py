import os

from tests_common import NewAlgorithmTests
from encode import encodejpg

class encode_test(NewAlgorithmTests):

    def test_encode(self):
        print "test encode start..."
        os.environ['PYTHON_C_LIB'] = os.path.dirname(__file__) + '/clib/'

        sample = ['yuv_A47_1920x1080']
        
        token = "ABCDE"
        
        scales = ['1920x1080', '1280x720', '640x360']

        for index, size in enumerate(scales):
            img  = self.get_sample(sample[0])
            encode = encodejpg.EncodeJPG(self.log_callback)
            encode_results = encode.encode(token, 1920, 1080, int(scales[index].split('x')[0]), int(scales[index].split('x')[1]), str(img), True)
            if encode_results:
                jpg_data = encode_results
                src_md5 = self.hashimg(jpg_data)
                
                dst_img = self.get_sample("jpg_A47_" + size)
                dst_md5 = self.hashimg(dst_img)

                self.assertEqual(src_md5, dst_md5)
                print "test encode [%d] ok"%index
            else:
                self.assertEqual(0, 1)
                print "test encode [%d] failed"%index
