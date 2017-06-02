//
//  motionAction.cpp
//  RobotVision
//
//  Created by zhongjywork on 15-5-15.
//  Copyright (c) 2015年 zhongjywork. All rights reserved.
//

#include <unistd.h>
#include <map>
#include "motionAction.h"
#include "debug.h"
#include "cJSON.h"

using namespace std;
using namespace cv;

//--------------------------------------------------------------------------------

//report motion event
void reportMotionEvent(rvResource* rv, bool is_motion, int zone_id,algorithm_result *res_describe)
{
	cJSON* root = cJSON_CreateObject();
    //cJSON_AddItemToObject(root, "stream_id", cJSON_CreateString(rv->token));
    //cJSON_AddItemToObject(root, "event_type", cJSON_CreateString(SLS_EVENT_TYPE_MOTION));
    if(is_motion)
    {
        cJSON_AddItemToObject(root, "zone_id", cJSON_CreateNumber(zone_id));
    }
    
    char* json_info = cJSON_Print(root);
    strncpy(res_describe->result,json_info,strlen(json_info));
    res_describe->bresult=is_motion;

	cJSON_Delete(root);
	free(json_info);
}

//--------------------------------------------------------------------------------

// get Boundary by sobel
void getBoundaryBySobel(Mat& src, Mat& dst)
{
    int scale = 1;
    int delta = 0;
    int ddepth = CV_8U;

    Mat grad_x, grad_y;
    Mat abs_grad_x, abs_grad_y;

    Sobel( src, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT );
    convertScaleAbs( grad_x, abs_grad_x );

    Sobel( src, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT );
    convertScaleAbs( grad_y, abs_grad_y );

    addWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0, dst );
}

//get foreground mask
int getForegroundMask(rvResource* rv, Mat& srcImg, Mat& fg_mask, int index)
{
    if (index > 2 )
    {
        return -1;
    }
	
    if ( rv->isModelUpdate )
    {
		
		rv->model[index] = (vibeModel_Sequential_t*)libvibeModel_Sequential_New();
		libvibeModel_Sequential_AllocInit_8u_C1R(rv->model[index], srcImg.data, srcImg.cols, srcImg.rows);
        if(srcImg.data == NULL || srcImg.cols <= 0 || srcImg.rows <= 0 )
        {
            rv->plog->log_print(SLS_LOG_ERROR,"fatal error  width=%d,height=%d\n",srcImg.cols,srcImg.rows);
            return -1;
        }
		srcImg.copyTo(rv->bg_frame[index]);
	    
		return 0;
    }
    
	libvibeModel_Sequential_Segmentation_8u_C1R(rv->model[index], srcImg.data, fg_mask.data);
	libvibeModel_Sequential_Update_8u_C1R(rv->model[index], srcImg.data, fg_mask.data);    
   
    return 1;
}

//get contours from foreground mask
void getValidContours( Mat &grad_img,Mat& fg_mask, vector<vector<Point> >& contours, int limit,rvResource* rv, int index )
{
	if ( fg_mask.empty() || index < 0 || index > 2 )
	{
		return;
	}
    
    vector< vector<Point> > allContours;
    vector< Vec4i > hierarchy,subHierarchy;
    vector< vector<Point> > validContours;
	vector< vector<Point> > invalidContours;
    vector< vector<Point> > snowContours;

	Mat fillContoursMat;
	fg_mask.copyTo(fillContoursMat);
    findContours(fg_mask, allContours, hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
    int allConSize = allContours.size();
    int valideMotionNum = 0;
    for (size_t i = 0; i < allConSize; i++)
    {
        if ( fabs(contourArea(Mat(allContours[i]))) >= limit ) //问题
        {
			valideMotionNum++;
        }
		else
		{
            invalidContours.push_back( allContours[i] );
		}
    }    
	
	if ( invalidContours.size() > 0 )
	{
		drawContours(fillContoursMat, invalidContours, -1, Scalar(0), CV_FILLED, 8);
	}
	
	
	if ( valideMotionNum > 0 )
	{
		//two frame and operator
		Mat diff,diff_mask;			
		absdiff(grad_img,rv->bg_frame[index],diff);
		threshold(diff,diff_mask,120,255,CV_THRESH_BINARY );
		//Mat element = getStructuringElement(MORPH_RECT,Size(4,4));
		//dilate(diff_mask,diff_mask,element,Point(-1,-1),1 );
		//two foreground frame and operator
		Mat result;	
		bitwise_and(fillContoursMat,diff_mask,result);
		Mat element = getStructuringElement(MORPH_RECT,Size(3,3));
		dilate(result,result,element,Point(-1,-1),1 );
		//filter too small rect
		RotatedRect rect;
		vector< vector<Point> > tmpContours;
		findContours(result, tmpContours, subHierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
		int tmpsize = tmpContours.size();
		for (size_t i = 0; i < tmpsize; i++)
		{
			if ( fabs(contourArea(Mat(tmpContours[i]))) < limit ) //问题
			{
				continue;
			}
			contours.push_back(tmpContours[i]); 
		}	
	}	
    //save current image
	grad_img.copyTo(rv->bg_frame[index]);	
	
    return;
}



void getEdge(Mat& src, Mat& dst)
{
	int scale = 1;
	int delta = 0;
	int ddepth = CV_16S;

	GaussianBlur( src, src, Size(3,3), 0, 0, BORDER_DEFAULT );

	/// 转换为灰度图
	//cvtColor( src, src_gray, CV_RGB2GRAY );

	/// 创建 grad_x 和 grad_y 矩阵
	Mat grad_x, grad_y;
	Mat abs_grad_x, abs_grad_y;

	/// 求 X方向梯度
	Sobel( src, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT );
	convertScaleAbs( grad_x, abs_grad_x );

	/// 求Y方向梯度
	Sobel( src, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT );
	convertScaleAbs( grad_y, abs_grad_y );

	/// 合并梯度(近似)
	addWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0, dst );
}


//-------------------------------------------------------------------------------
// get edge



void mMotionAction(rvResource* rv,algorithm_result *result)
{
	//check is enable motion action
    SLS_CommonSettingParams setting_params = rv->pAlgoParams->motion_setting_params;
	
	int zone_count = rv->pAlgoParams->motion_params.zone_count;
	zone_count = zone_count > 3 ? 3:zone_count;
	
	map<int,cv::Rect> motion_zones;
	for ( int i = 0; i < zone_count; i++ )
	{
		SLS_RoiRect roirect = rv->pAlgoParams->motion_params.zone_pos[i];
		cv::Rect cvrect(roirect.x,roirect.y,roirect.width,roirect.height);
		motion_zones[rv->pAlgoParams->motion_params.zone_id[i]] = cvrect;		
	}	

    Mat grayimg(rv->frame_height,rv->frame_width,CV_8U, rv->srcFrame.data );	
	Mat gradframe,dstmat;
	if ( grayimg.cols != MINMUM_FRAME_WIDTH && grayimg.rows != MINMUM_FRAME_HEIGHT )
	{
		resize(grayimg,dstmat,Size(MINMUM_FRAME_WIDTH,MINMUM_FRAME_HEIGHT));
		getEdge(dstmat, gradframe);
	}
	else
	{
		getEdge(grayimg, gradframe);
	}
    
    //filter picture
	//Mat gradframe;
	//getEdge(rv->srcFrame, gradframe);
    //if zone update we will free the background model
    
    if ( zone_count > 0 )
    {
        //准备一个队列来放置需要上报的zone_id
       // vector<int> zoneArray;        
#if DEBUG_ENABLE_GUI
        Mat frame;
        rv->srcFrame.copyTo(frame);

		//gradframe.copyTo(frame);
#endif              
	            
        //按照zone的坐标 将原图切开
        map<int, Rect>::iterator it;
		vector< vector<Point> > contours;
		vector<int> zoneArray;
		int zoneid = -1;
        bool bg_update = false; 
        for (it = motion_zones.begin(); it != motion_zones.end(); ++it)
        {
            Rect roiRect = it->second;
			if ( roiRect.width<=0 || roiRect.height<=0 || 
                    roiRect.x+roiRect.width > 100 || roiRect.y+roiRect.height > 100 )
			{
                rv->plog->log_print(SLS_LOG_ERROR,"%s--zone paramers error!!!",rv->token);
                continue;
			}
            Rect rect = Rect(gradframe.cols * roiRect.x / 100,
                             gradframe.rows * roiRect.y / 100,
                             gradframe.cols * roiRect.width / 100,
                             gradframe.rows * roiRect.height / 100);
            Mat roiImg;
			
			try
			{
				gradframe(rect).copyTo( roiImg );
			}
			catch (cv::Exception& e)
			{
				gradframe.copyTo(roiImg);
				rv->plog->log_print(SLS_LOG_ERROR,"cv exception x=%d,y=%d,width=%d,height=%d -----\n",rect.x,rect.y,rect.width,rect.height);
			}

            Mat fg_mask( roiImg.rows,roiImg.cols,CV_8U );

            int ret = getForegroundMask(rv, roiImg, fg_mask, distance(motion_zones.begin(), it));
            if (ret == -1)
            {
				rv->plog->log_print(SLS_LOG_ERROR,"%s--fatal error",rv->token );
                continue;
            }
            else if(ret == 0)
            {
				rv->plog->log_print(SLS_LOG_DEBUG,"%s--roi create background",rv->token );
                bg_update=true;
                continue;                
            }
            
			contours.clear();
			getValidContours( roiImg,fg_mask, contours, (int)(roiImg.cols * roiImg.rows / setting_params.sensitivity),
		    	rv,distance(motion_zones.begin(), it) );
			
			if( contours.size() > 0 )
			{
				zoneArray.push_back(it->first);
			}			
		}
        if ( rv->isModelUpdate && bg_update)
        {
			rv->isModelUpdate=false;
        }
		if (zoneArray.size() > 0)
		{
			//new version modify
			reportMotionEvent(rv, true, zoneArray[0],result);				
		}
		else
		{
			reportMotionEvent(rv, false,0,result);				
			rv->plog->log_print(SLS_LOG_DEBUG,"%s -----This frame has not motion-----",rv->token);
		}
		
#if DEBUG_ENABLE_GUI
       imshow("motion img", frame);
	   waitKey(1);
#endif
    }
    else
    {
		Mat fg_mask( gradframe.rows,gradframe.cols,CV_8U );
        
        //create model and get foreground mask
        int ret = getForegroundMask(rv, gradframe, fg_mask, 0);

        if( ret == 0 )
		{
			rv->plog->log_print(SLS_LOG_DEBUG,"%s-- create background",rv->token );
			if ( rv->isModelUpdate )
			{
				rv->isModelUpdate = false;
			}
			reportMotionEvent(rv, false, 0 ,result );
			return;
		}
        else if(ret == -1)
        {
			rv->plog->log_print(SLS_LOG_ERROR,"%s-- fatal error",rv->token );
			reportMotionEvent(rv, false, 0 ,result );
            return;
        }
        // get all valid contours
        vector< vector<Point> > contours;
        getValidContours(gradframe,fg_mask, contours, (int)(gradframe.cols * gradframe.rows / setting_params.sensitivity),rv,0);
       // rv->plog->log_print( "contour size=%d\n",contours.size() );
        //do report
        if (contours.size() > 0)
        {
			rv->plog->log_print( SLS_LOG_DEBUG,"This picture has motion");
			reportMotionEvent(rv, true, -1,result );
        }
		else
		{
			reportMotionEvent(rv, false, 0 ,result );
			rv->plog->log_print(SLS_LOG_DEBUG,"%s no Zone-----This frame has not motion-----",rv->token);
		}
       
#if DEBUG_ENABLE_GUI
        Mat frame;
        rv->srcFrame.copyTo(frame);
		
        drawContours(frame, contours, -1, Scalar(0, 0, 255));
        imshow("motion img", frame);
		waitKey(1);
#endif
    }  	
    
}

















