#include "robot_vision.h"
#include <opencv2/opencv.hpp>
#include <stdio.h>
#include "debug.h"
#include "decoder/include/decoder.h"
#include <pthread.h>
#include <unistd.h>
#include <string>
#include <sys/types.h>
#include <dirent.h>
#include "sengled_algorithm.h"

using namespace std;
using namespace cv;


#define URL_1   "rtsp://54.223.51.168:554/140A4524FC57C21DA0768EBEAB4AA392.sdp"
#define URL_2   "rtsp://54.223.51.168:554/D438197D40E7B7E121B1306D98A75FFC.sdp"
#define URL_3   "rtsp://54.223.54.82:554/D438197D40E7B7E121B1306D98A75FFC.sdp"
#define URL_4   "/mnt/hgfs/code/01921.mp4"
#define URL_12  "/mnt/hgfs/code/snow.mp4"
#define URL_5   "rtsp://54.222.135.41:554/80F4480334D01582E922056B83E51527.sdp"
#define URL_6   "rtsp://54.223.51.168:554/C0FDC947EC72040A496D317CE8FBA854.sdp"
#define URL_7   "rtsp://52.35.165.152:554/BA4A6BE1D03B8AB7CE186C2878620E8A.sdp"
#define URL_8   "rtsp://52.35.165.152:554/9BC2524DE47FE4997875CB4D6E958997.sdp"
#define URL_9   "rtsp://52.35.165.152:554/F9090331A11E69043B4267E70C1A13C2.sdp"
#define URL_10  "rtsp://52.35.185.115:554/362B4C972489930694CD474A0460D1BC.sdp"
#define URL_11  "rtsp://52.36.178.107:554/1D09D746052CB5CFE53B3CFDB0FEDDA2.sdp"
#define TOKEN_1  "140A4524FC57C21DA0768EBEAB4AA392"
#define TOKEN_2  "D438197D40E7B7E121B1306D98A75FFC"
#define TOKEN_3  "D438197D40E7B7E121B1306D98A75FFC"
#define TOKEN_4  "LOCAL"
#define TOKEN_5  "80F4480334D01582E922056B83E51527"
#define TOKEN_6  "C0FDC947EC72040A496D317CE8FBA854"
#define TOKEN_7  "BA4A6BE1D03B8AB7CE186C2878620E8A"
#define TOKEN_8  "9BC2524DE47FE4997875CB4D6E958997"
#define TOKEN_9  "F9090331A11E69043B4267E70C1A13C2"
#define TOKEN_10 "362B4C972489930694CD474A0460D1BC"
#define TOKEN_11 "1D09D746052CB5CFE53B3CFDB0FEDDA2"

typedef struct ThreadParam
{
	char url[512];
	char tocken[256];
	char storage[256];
}ThreadParam;



void alo_log_callback (int level, const char* fmt)
{
	char sztmp[1024] = { 0 };
	sprintf( sztmp,"%s\n",fmt );
    printf("%s\n",sztmp);
}

void test( const char *url, const char *token, const char *storage )
{
	common_params  initcontext;
	initcontext.log_callback = alo_log_callback;
	//strcpy(initcontext.dest_storage_floder,"/opt/sengled/tmp/test_bin");
	strcpy(initcontext.token,token);
    SLSHandle handle = create_algorithm_instance( &initcontext ); 
	
	SLS_AlgorithmParams algorithmparams;

	algorithmparams.motion_params.zone_count = 0;
	algorithmparams.motion_setting_params.sensitivity = 1500;
    
	SLS_RoiRect rect1={10,10,37,63};
	SLS_RoiRect rect2={53,0,47,98};
	SLS_RoiRect rect4={15,18,52,79};
    SLS_RoiRect rect3={53,0,47,97};
	//SLS_RoiRect rect2={15,18,52,79};
	algorithmparams.motion_params.zone_id[0] = 1;
	algorithmparams.motion_params.zone_id[1] = 2;
	algorithmparams.motion_params.zone_id[2] = 3;
	algorithmparams.motion_params.zone_pos[0] = rect1;
	algorithmparams.motion_params.zone_pos[1] = rect2;
    algorithmparams.motion_params.zone_pos[2] = rect3;

	SLStreamParameter stream_parameter;
    
    int opencount = 0;
    algorithm_result *al_result=new algorithm_result;
	while ( opencount < 50  )
	{
		opencount++;
		if ( !decodeVideo(stream_parameter,url) )
		{
			printf("------open video failed:%d------\n", opencount++ );
			continue;
		}      
		int count_error = 0;
		stream_parameter.scaled_frame_data = (char*)malloc(1920*1080*2);
		//SLSSetMDFlag( private_param,MD_TRUE);
        int i = 0;
		while ( 1 )
		{
			i++;
			bool ret = getFrameFromDecoder(stream_parameter);
			if ( !ret )
			{
				count_error++;
				printf("getFrameFromDecoder failed %d\n",count_error);			
			}
			if ( count_error >= 20 )
			{
				break;
			}

            if( (i%25) != 0 )
            {
                continue;
            }

			if ( ret &&  stream_parameter.error_frame !=-1 )
			{
                memset(al_result,0,sizeof(algorithm_result));
				feed_frame(handle,stream_parameter.scaled_frame_data,stream_parameter.frame_width,
					stream_parameter.frame_height,&algorithmparams,al_result );
                if(al_result->bresult)
                {
                    printf("sucess:%s\n",al_result->result);
                }
                else
                {
                    printf("failed:%s\n",al_result->result);
                }

			}			
		}
		stopGetFrame( stream_parameter );
		closeVideoDecoder(stream_parameter);
	}
	
	delete_algorithm_instance( handle );	
    free(al_result);    
}

void *thread_fun( void *param )
{
	ThreadParam *pinfo = static_cast<ThreadParam*>(param);
	test( pinfo->url,pinfo->tocken,pinfo->storage );
	delete pinfo;
}

void start( const char *url, const char *tocken, const char *storage )
{
    ThreadParam *pinfo = new ThreadParam;
	strcpy(pinfo->url,url);
	strcpy(pinfo->tocken,tocken);
	strcpy(pinfo->storage,storage );
	pthread_t threadid;
	pthread_create( &threadid,NULL,thread_fun, pinfo );
	pthread_detach(threadid);
}

int main( int argc, char *argv[] )
{
	if( argc < 3 )
	{
		printf("Usage--{ test_output media-address  picture-storage-floder }\n");
		printf("Example-- { test_output rtsp://54.222.135.41:554/140A4524FC57C21DA0768EBEAB4AA392.sdp /tmp/picture } \n");
		return -1;
	}
    string str_address(argv[1]);
	string str_token;
	if(str_address.length() <= 0 ) return -1;
	if( str_address.find( "rtsp" ) != string::npos )
	{
        size_t p = str_address.find_last_of( '/' );
		size_t l = str_address.find( ".sdp" );
		if( p != string::npos && l != string::npos )
		{
            str_token = str_address.substr( p+1,l-p-1 );
		}
	}
	else
	{
		str_token.assign( "LOCAL" );
	}
	string picture_storage(argv[2]);
    if( (opendir( picture_storage.c_str())) == NULL ) 
	{
		printf("floder is invalide!!!!\n");
		return -1;
	}
	//start( URL_2, TOKEN_2 );
	//start( URL_7, TOKEN_7 );
	//start( URL_4, TOKEN_2 );
	//start( URL_9, TOKEN_9 );
	int countNum=0;
	if( argc == 4 )
	{
		countNum = atoi( argv[3]);
	}
	printf("countNum=%d\n",countNum);
	for( int i = 0; i <= countNum; i++ )
	{
	   start( str_address.c_str(),str_token.c_str(),picture_storage.c_str() );
	}
	while ( 1 )
	{
		sleep(10);
	}
	return 0;
}


