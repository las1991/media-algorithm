#ifndef __DETECT_SPS_PPS_H
#define __DETECT_SPS_PPS_H

typedef struct AnalyseFrameGlobalParams{
    // sps
    int g_frame_mbs_only_flag;
    int g_pic_order_cnt_type;
    int g_delta_pic_order_always_zero_flag;
    int g_log2_max_frame_num_minus4;

    //pps
    int g_pic_order_present_flag;
    int g_redundant_pic_cnt_present_flag;

}AnalyseFrameGlobalParams;

int AnalysePpsSps (AnalyseFrameGlobalParams* params, const char* buf, int buf_size);
int FindBasePframe(AnalyseFrameGlobalParams* params, const char* buf, int buf_size);


#endif
