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

#define INNER_SENSITIVITY   (MINMUM_FRAME_WIDTH * MINMUM_FRAME_HEIGHT * 0.08 * 0.08)

//--------------------------------------------------------------------------------

//report motion event
void reportMotionEvent(rvResource* rv, bool is_motion, map< int,vector<Rect> > &detect_results,algorithm_result *res_describe)
{
    cJSON* root = cJSON_CreateObject();
    cJSON* motion_array = cJSON_CreateArray();
	
    if(is_motion)
    {
        map< int,vector<Rect> >::iterator it;
        cJSON* zone_array;
        cJSON* external_array;
        cJSON* sub_object;
        int zone_id;
        vector<Rect> rects;
        Rect rect;

        int rect_area = 0;
        for( it = detect_results.begin(); it != detect_results.end(); it++ )
        {
            rects = it->second;
            int rsize = rects.size();
            for(int i = 0; i < rsize; i++)
            {
                rect = rects[i];
                rect_area += rect.width * rect.height;
            }
        }
        if(rect_area < INNER_SENSITIVITY)
        {
             res_describe->bresult = false;
             cJSON_Delete(root);
             return;
        }

        for( it = detect_results.begin(); it != detect_results.end(); it++ )
        {
            zone_id = it->first;
            sub_object = cJSON_CreateObject();
            cJSON_AddItemToObject(sub_object,"zone_id",cJSON_CreateNumber(zone_id));

            external_array = cJSON_CreateArray();
            rects = it->second;
            int rsize = rects.size();
            for( int i=0; i < rsize; i++ )
            {
                zone_array = cJSON_CreateArray();
                rect = rects[i];
                int x = rect.x*100/MINMUM_FRAME_WIDTH;
                int y = rect.y*100/MINMUM_FRAME_HEIGHT;
                int dx = (rect.x+rect.width)*100/MINMUM_FRAME_WIDTH;
                int dy = (rect.y+rect.height)*100/MINMUM_FRAME_HEIGHT;
                cJSON_AddItemToArray(zone_array,cJSON_CreateNumber(x));
                cJSON_AddItemToArray(zone_array,cJSON_CreateNumber(y));
                cJSON_AddItemToArray(zone_array,cJSON_CreateNumber(dx));
                cJSON_AddItemToArray(zone_array,cJSON_CreateNumber(dy));
                cJSON_AddItemToArray(external_array,zone_array);
            }
            cJSON_AddItemToObject(sub_object,"boxs",external_array);
            cJSON_AddItemToArray(motion_array,sub_object);
        }		
    }
    cJSON_AddItemToObject(root,"motion",motion_array);
    
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
    if( index > 2 || srcImg.data == NULL || srcImg.cols <= 0 || srcImg.rows <= 0 )
    {
        rv->plog->log_print(SLS_LOG_ERROR,"fatal error  width=%d,height=%d\n",srcImg.cols,srcImg.rows);
        return -1;
    }
	
    if ( rv->isModelUpdate )
    {
        rv->model[index] = (vibeModel_Sequential_t*)libvibeModel_Sequential_New();
        libvibeModel_Sequential_AllocInit_8u_C1R(rv->model[index], srcImg.data, srcImg.cols, srcImg.rows);
        srcImg.copyTo(rv->bg_frame[index]);
        return 0;
    }

    if( rv->model[index] == NULL || fg_mask.data == NULL) 
    {
        rv->plog->log_print(SLS_LOG_ERROR,"fatal error model is null");
        return -1;
    }
    libvibeModel_Sequential_Segmentation_8u_C1R(rv->model[index], srcImg.data, fg_mask.data);
    libvibeModel_Sequential_Update_8u_C1R(rv->model[index], srcImg.data, fg_mask.data);    
   
    return 1;
}

//get contours from foreground mask
void getValidContours( Mat &grad_img,Mat& fg_mask, vector<Rect>& rectangles, int limit,rvResource* rv, 
        int index, Rect &zone_rect )
{
    if ( fg_mask.empty() || index < 0 || index > 2 )
    {
        return;
    }
    
    Mat fillContoursMat;
    fg_mask.copyTo(fillContoursMat);
    
    vector< Vec4i > hierarchy,subHierarchy;
	
    //two frame and operator
    Mat diff,diff_mask;			
    absdiff(grad_img,rv->bg_frame[index],diff);
    threshold(diff,diff_mask,20,255,CV_THRESH_BINARY );
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
    Rect tmprect;
    for (size_t i = 0; i < tmpsize; i++){
        if ( fabs(contourArea(Mat(tmpContours[i]))) < limit ) {
            continue;
        }
        //contours.push_back(tmpContours[i]); 
        int index_size = tmpContours[i].size();
        for( int j = 0; j < index_size; j++ ){
            tmpContours[i][j].x += zone_rect.x;
            tmpContours[i][j].y += zone_rect.y;
        }
        tmprect = boundingRect(tmpContours[i]);
        rectangles.push_back(tmprect);
    }

    Rect vec_temp; 
    for(unsigned int i = 1; i < rectangles.size(); i++){
        for(unsigned int m = rectangles.size() - 1; m >= i; m--){
            if(rectangles[m].x < rectangles[m - 1].x){
                vec_temp = rectangles[m - 1]; 
                rectangles[m - 1] = rectangles[m];
                rectangles[m] = vec_temp;
            }
        }
    }
    Rect temp;
    for(unsigned int i = 0; i < rectangles.size(); i++){
        for(unsigned int m = i + 1; m < rectangles.size(); m++){
            if(rectangles[i].x + rectangles[i].width >= rectangles[m].x && rectangles[m].y <= rectangles[i].y + rectangles[i].height 
                && rectangles[m].y + rectangles[m].height >= rectangles[i].y)
            {
                if(rectangles[i].x > rectangles[m].x)
                    temp.x = rectangles[m].x;
                else
                    temp.x = rectangles[i].x;

                if(rectangles[i].y > rectangles[m].y)
                    temp.y = rectangles[m].y;
                else
                    temp.y = rectangles[i].y;
    
                if(rectangles[i].x + rectangles[i].width > rectangles[m].x + rectangles[m].width)
                    temp.width = rectangles[i].x + rectangles[i].width - temp.x;
                else
                    temp.width = rectangles[m].x + rectangles[m].width - temp.x;

                if(rectangles[i].y + rectangles[i].height > rectangles[m].y + rectangles[m].height)
                    temp.height = rectangles[i].y + rectangles[i].height - temp.y;
                else
                    temp.height = rectangles[m].y + rectangles[m].height - temp.y;
                rectangles[i] = temp;
 
                rectangles.erase((vector<Rect>::iterator)(&rectangles[m]));
                i--;
                break;
            }
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
    int zone_id0, zone_id1, zone_id2;
    cv::Rect zrect0, zrect1, zrect2;
    int delete_second = 0;
    int delete_last = 0;
    switch(zone_count){
    case 1:
        break;
    case 2:
        zone_id0 = rv->pAlgoParams->motion_params.zone_id[0];
        zone_id1 = rv->pAlgoParams->motion_params.zone_id[1];
        zrect0 = motion_zones[zone_id0];
        zrect1 = motion_zones[zone_id1];

        if(zrect0.x < zrect1.x && zrect0.y < zrect1.y && zrect0.x + zrect0.width > zrect1.x + zrect1.width 
           && zrect0.y + zrect0.height > zrect1.y + zrect1.height){
            motion_zones.erase(zone_id0);
        }
        
        if(zrect1.x < zrect0.x && zrect1.y < zrect0.y && zrect1.x + zrect1.width > zrect0.x + zrect0.width 
           && zrect1.y + zrect1.height > zrect0.y + zrect0.height){
            motion_zones.erase(zone_id1);
        }

        break;
    case 3:
        zone_id0 = rv->pAlgoParams->motion_params.zone_id[0];
        zone_id1 = rv->pAlgoParams->motion_params.zone_id[1];
        zone_id2 = rv->pAlgoParams->motion_params.zone_id[2];
        
        zrect0 = motion_zones[zone_id0];
        zrect1 = motion_zones[zone_id1];
        zrect2 = motion_zones[zone_id2];

        int first, second, last;
        if(zrect0.x <= zrect1.x){
            if(zrect0.x <= zrect2.x){
                first = zone_id0;
                if(zrect1.x <= zrect2.x){
                    second = zone_id1;
                    last = zone_id2;
                }else{
                    second = zone_id2;
                    last = zone_id1;
                }
            }else{
                first = zone_id2;
                second = zone_id0;
                last = zone_id1;
            }
        }else{
            if(zrect1.x <= zrect2.x){
               first = zone_id1;
               if(zrect0.x <= zrect2.x){
                   second = zone_id0;
                   last = zone_id2;
               }else{
                   second = zone_id2;
                   last = zone_id0;
               }
            }else{
               first = zone_id2;
               second = zone_id1;
               last = zone_id0;
            }
        }
        if(motion_zones[first].y < motion_zones[second].y 
             && motion_zones[first].x + motion_zones[first].width > motion_zones[second].x + motion_zones[second].width
             && motion_zones[first].y + motion_zones[first].height > motion_zones[second].y + motion_zones[second].height)
        {
            motion_zones.erase(second);
            delete_second = 1;
        }
        if(motion_zones[first].y < motion_zones[last].y 
           && motion_zones[first].x + motion_zones[first].width > motion_zones[last].x + motion_zones[last].width
           && motion_zones[first].y + motion_zones[first].height > motion_zones[last].y + motion_zones[last].height)
        {
            motion_zones.erase(last);
            delete_last = 1;
        }
        if(!delete_second && !delete_last){
            if(motion_zones[second].y < motion_zones[last].y 
              && motion_zones[second].x + motion_zones[second].width > motion_zones[last].x + motion_zones[last].width
              && motion_zones[second].y + motion_zones[second].height > motion_zones[last].y + motion_zones[last].height)
            {
               motion_zones.erase(last);
               delete_last = 1;
            }
        }
        break;
    default:
        break;
    }

    Mat grayimg(rv->frame_height,rv->frame_width,CV_8U, rv->srcFrame.data);	
    Mat gradframe,dstmat;
    if ( grayimg.cols != MINMUM_FRAME_WIDTH && grayimg.rows != MINMUM_FRAME_HEIGHT )
    {
        resize(grayimg,dstmat,Size(MINMUM_FRAME_WIDTH,MINMUM_FRAME_HEIGHT));
        dstmat.copyTo(gradframe);
        //getEdge(dstmat, gradframe);
    }
    else
    {
        grayimg.copyTo(gradframe);
        //getEdge(grayimg, gradframe);
    }
    
    vector<Rect> rectangles;
    vector<int> zoneArray;
    map< int,vector<Rect> > detect_results;   
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
		
        int zoneid = -1;
        bool bg_update = false; 
        for (it = motion_zones.begin(); it != motion_zones.end(); ++it)
            rv->plog->log_print(SLS_LOG_DEBUG,"--zone %d %d %d %d!!!\n", it->second.x, it->second.y, it->second.width, it->second.height);

        for (it = motion_zones.begin(); it != motion_zones.end(); ++it)
        {
            Rect roiRect = it->second;
            if ( roiRect.width<=0 || roiRect.height<=0 )
            {
                rv->plog->log_print(SLS_LOG_ERROR,"%s--zone paramers error!!!",rv->token);
                continue;
            }
            if(roiRect.x+roiRect.width > 100)
            {
                roiRect.width = 100-roiRect.x;
            }
            if(roiRect.y+roiRect.height > 100)
            {
                roiRect.height = 100-roiRect.y;
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
            
            rectangles.clear();
            getValidContours( roiImg,fg_mask, rectangles, (int)(MINMUM_FRAME_WIDTH * MINMUM_FRAME_HEIGHT / setting_params.sensitivity),
		    	rv,distance(motion_zones.begin(), it) ,rect);
			
            if( rectangles.size() > 0 )
            {
                //zoneArray.push_back(it->first);
                detect_results[it->first] = rectangles;
            }			
        }
        if ( rv->isModelUpdate && bg_update)
        {
            rv->isModelUpdate=false;
        }
        if (detect_results.size() > 0)
        {
            //new version modify
            reportMotionEvent(rv, true, detect_results,result);				
        }
        else
        {
            reportMotionEvent(rv, false,detect_results,result);				
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
            reportMotionEvent(rv, false, detect_results ,result );
            return;
        }
        else if(ret == -1)
        {
            rv->plog->log_print(SLS_LOG_ERROR,"%s-- fatal error",rv->token );
            reportMotionEvent(rv, false, detect_results ,result );
            return;
        }
        // get all valid contours
        vector< vector<Point> > contours;
        Rect zone_rect(0,0,100,100);
        getValidContours(gradframe,fg_mask, rectangles, 
                (int)(gradframe.cols * gradframe.rows / setting_params.sensitivity),rv,0,zone_rect);
        // rv->plog->log_print( "contour size=%d\n",contours.size() );
        //do report
        if (rectangles.size() > 0)
        {
            rv->plog->log_print( SLS_LOG_DEBUG,"This picture has motion");
            detect_results[-1]=rectangles;
            reportMotionEvent(rv, true, detect_results,result );
        }
        else
        {
            reportMotionEvent(rv, false, detect_results ,result );
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

