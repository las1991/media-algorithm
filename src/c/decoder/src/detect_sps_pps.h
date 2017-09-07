#ifndef __DETECT_SPS_PPS_H
#define __DETECT_SPS_PPS_H

typedef struct SPS_TEMP_FLAG
{
    int frame_mbs_only_flag;
    int pic_order_cnt_type;
    int delta_pic_order_always_zero_flag;
    int log2_max_frame_num_minus4;
}SPS_TEMP_FLAG;

typedef struct PPS_TEMP_FLAG
{
    int pic_order_present_flag;
    int redundant_pic_cnt_present_flag;   
}PPS_TEMP_FLAG;

typedef struct SPS_PPS_FLAG
{
    SPS_TEMP_FLAG sps_flag;
    PPS_TEMP_FLAG pps_flag;
}SPS_PPS_FLAG;

int analyse_from_pkt(char* token, char* pkt_buf, int pkt_size);
int analyse_from_buf(char* buf, int buf_size);
int find_base_pframe(char* token, char* buf, int buf_size);

SPS_PPS_FLAG fast_find_pps_sps_from_pkt(char* pkt_buf, int pkt_size);
int fast_find_base_pframe(char* buf, int buf_size, SPS_PPS_FLAG sps_pps_flag);

#endif
