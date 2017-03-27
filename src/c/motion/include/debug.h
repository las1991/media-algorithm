#ifndef _DEBUG_H_
#define _DEBUG_H_

#include<stdio.h>
#include<time.h>
#include<stdarg.h>
#include<string.h>
#include<unistd.h>
#include<sys/types.h>
#include<sys/syscall.h>
#include <sys/prctl.h>
#include <string>
using namespace std;
//#include "rv_resource.h"
//switch for printf
#define DEBUG_ENABLE_PRINTF     1

//switch for gui debug
#define DEBUG_ENABLE_GUI       0

//test performance
#define ENABLE_TEST_PERFORMANCE 0

//test debug draw line

#define ENABLE_DRAW_RECT       0

#define SEV_CONFIG_FILE_PATH         "/opt/sengled/apps/media_process_server-0.0.1/configc/data/"
#define SEV_TRAIN_FILE_PATH         "/opt/sengled/apps/media_process_server-0.0.1/configc/train/"

#define DP_PREFIX   "[%s] [threadID:%d] %s %d "
#define RED_COLOR "\033[1;31m"
#define CLOSE_COLOR "\033[0m" 

#define ROBOTVISION_LOG_FLAG       "[ ROBOTVISION ] "
// print function for debug
#if DEBUG_ENABLE_PRINTF
//#define Debug_printf(format, ...)  fprintf(stderr, format, ## __VA_ARGS__)
#define Debug_printf(_fmt,args...) \
	msg_print( RED_COLOR DP_PREFIX _fmt CLOSE_COLOR,pr_time(),pr_pid(),__FUNCTION__,__LINE__,## args) 
#else
#define Debug_printf(format, ...)
#endif

typedef void (*sls_log_callback)( int level, const char* fmt );


class CLogPrint
{
public:  
	CLogPrint( sls_log_callback log_callback ):log_callback(log_callback){}
	void log_print( int log_level, const char *format, ... )
	{
		va_list ap;
		va_start(ap,format);

		string open_flag( ROBOTVISION_LOG_FLAG );
		string str_format(format);
		string str_time(get_time());

      	char sztmp[1024] = {0};
        char log_output[1024]={0};

		sprintf( sztmp,"%s",(open_flag+str_time+" "+str_format).c_str());
        vsprintf(log_output,sztmp,ap);

		if ( log_callback != NULL )
		{
			log_callback(log_level,log_output);
		}
		va_end(ap);
	}
private:
	const char *get_time()
	{
		time_t ltime;
		time(&ltime);
		char *tmp = ctime(&ltime);
		*(tmp+strlen(tmp)-1)='\0';
		return tmp;
	}
private:
	sls_log_callback log_callback;
};


#endif



