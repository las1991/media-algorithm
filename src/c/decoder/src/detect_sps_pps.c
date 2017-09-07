// Ue find the num of zeros and get (num+1) bits from the first 1, and
// change it to decimal
// e.g. 00110 -> return 6(110)
#include<libavformat/avformat.h>
#include<stdbool.h>
#include"detect_sps_pps.h"

// sps
int g_frame_mbs_only_flag = -1;
int g_pic_order_cnt_type = -1;
int g_delta_pic_order_always_zero_flag = -1;
int g_log2_max_frame_num_minus4 = -1;

// pps
int g_pic_order_present_flag = -1;
int g_redundant_pic_cnt_present_flag = -1;

static unsigned int Ue(char *pBuff, unsigned int len, unsigned int *nStartBit)
{
	//计算0bit的个数
	unsigned int nZeroNum = 0;
	int i;
	while (*nStartBit <len * 8)
	{
		if (pBuff[*nStartBit / 8] & (0x80 >> (*nStartBit % 8))) //&:按位与，%取余
			break;
		nZeroNum++;
		(*nStartBit)++;
	}
	(*nStartBit)++;

	//计算结果
	unsigned int dwRet = 0;
	for (i = 0; i < nZeroNum; i++)
	{
		dwRet <<= 1;
		if (pBuff[*nStartBit / 8] & (0x80 >> (*nStartBit % 8)))
		{
			dwRet += 1;
		}
		(*nStartBit)++;
	}
	return (1 << nZeroNum) - 1 + dwRet;
}

static int Se(char *pBuff, unsigned int len, unsigned int *nStartBit)
{
	int UeVal = Ue(pBuff, len, nStartBit);
	double k = UeVal;
	int nValue = ceil(k/2);
	if (UeVal % 2 == 0)
		nValue = -nValue;
	return nValue;
}

// u Just returns the BitCount bits of buf and change it to decimal.
// e.g. BitCount = 4, buf = 01011100, then return 5(0101)
static int u(unsigned int BitCount, char* buf, unsigned int* nStartBit)
{
	int dwRet = 0;
	unsigned int i;
	for (i = 0; i < BitCount; i++)
	{
		dwRet <<= 1;
		if (buf[*nStartBit / 8] & (0x80 >> (*nStartBit % 8)))
		{
			dwRet += 1;
		}
		(*nStartBit)++;
	}
	return dwRet;
}

typedef enum Slice_type
{
    P   = 0,
    B   = 1,
    I   = 2,
    SP  = 3,
    SI  = 4,

    P1  = 5,
    B1  = 6,
    I1  = 7,
    SP1 = 8,
    SI1 = 9
}Slice_type;

static SPS_TEMP_FLAG fast_analyze_sps(char* sps_buf, int sps_len)
{
    SPS_TEMP_FLAG flag;
    memset(&flag, 0, sizeof(SPS_TEMP_FLAG));
        
    // Analyze SPS 
	unsigned int StartBit = 0;
    char* buf = sps_buf;
	buf = buf + 4;
    int i = 0;
    /*
	int forbidden_zero_bit = u(1, buf, &StartBit);
	int nal_ref_idc = u(2, buf, &StartBit);
	int nal_unit_type = u(5, buf, &StartBit);
	av_log(NULL, AV_LOG_DEBUG, "nal_unit_type = %d\n", nal_unit_type);
    */
    buf = buf + 1;//skip header
    
    int profile_idc = u(8, buf, &StartBit);
    /*av_log(NULL, AV_LOG_DEBUG, "profile_idc = %d\n", profile_idc);
	int constraint_set0_flag = u(1, buf, &StartBit);  //(buf[1] & 0x80)>>7;
	int constraint_set1_flag = u(1, buf, &StartBit);  //(buf[1] & 0x40)>>6;
	int constraint_set2_flag = u(1, buf, &StartBit);  //(buf[1] & 0x20)>>5;
	int constraint_set3_flag = u(1, buf, &StartBit);  //(buf[1] & 0x10)>>4;
	int reserved_zero_4bits = u(4, buf, &StartBit);
	int level_idc = u(8, buf, &StartBit);
    */
    buf = buf + 2;// skip constraint_set0_flag,...level_idc
    
    int seq_parameter_set_id = Ue(buf, sps_len, &StartBit);
    int chroma_format_idc;
    if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144)
    {
	    chroma_format_idc = Ue(buf, sps_len, &StartBit);
        int residual_colour_transform_flag;
		if (chroma_format_idc == 3)
        {
		    residual_colour_transform_flag = u(1, buf, &StartBit);
        }
        int bit_depth_luma_minus8 = Ue(buf, sps_len, &StartBit);
		int bit_depth_chroma_minus8 = Ue(buf, sps_len, &StartBit);
	    int qpprime_y_zero_transform_bypass_flag = u(1, buf, &StartBit);
		int seq_scaling_matrix_present_flag = u(1, buf, &StartBit);

		int seq_scaling_list_present_flag[8];
		if (seq_scaling_matrix_present_flag)
		/*    for (i = 0; i < 8; i++)
			{
			    seq_scaling_list_present_flag[i] = u(1, buf, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "seq_scaling_list_present_flag[%d] = %d\n",
                                               i, seq_scaling_list_present_flag[i]);
			}
        */
            buf = buf + 1;//skip seq_scaling_list_present_flag[i] 
    }
	int log2_max_frame_num_minus4 = Ue(buf, sps_len, &StartBit);//...
	int pic_order_cnt_type = Ue(buf, sps_len, &StartBit);//...
    //av_log(NULL, AV_LOG_DEBUG, "log2_max_frame_num_minus4----------%d\n", log2_max_frame_num_minus4); 
    flag.log2_max_frame_num_minus4 = log2_max_frame_num_minus4;
    flag.pic_order_cnt_type = pic_order_cnt_type;
		
    int log2_max_pic_order_cnt_lsb_minus4;
	if (pic_order_cnt_type == 0)
    {
	    log2_max_pic_order_cnt_lsb_minus4 = Ue(buf, sps_len, &StartBit);
    }
    else if (pic_order_cnt_type == 1)
	{
		int delta_pic_order_always_zero_flag = u(1, buf, &StartBit);//...
        flag.delta_pic_order_always_zero_flag = delta_pic_order_always_zero_flag;
        int offset_for_non_ref_pic = Se(buf, sps_len, &StartBit);
	    int offset_for_top_to_bottom_field = Se(buf, sps_len, &StartBit);
		int num_ref_frames_in_pic_order_cnt_cycle = Ue(buf, sps_len, &StartBit);
			
        int *offset_for_ref_frame = av_malloc(sizeof(int)*num_ref_frames_in_pic_order_cnt_cycle);
		for (i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++)
        {
			offset_for_ref_frame[i] = Se(buf, sps_len, &StartBit);
		}
        av_free(offset_for_ref_frame);
    }
        
	int num_ref_frames = Ue(buf, sps_len, &StartBit);
	int gaps_in_frame_num_value_allowed_flag = u(1, buf, &StartBit);
	int pic_width_in_mbs_minus1 = Ue(buf, sps_len, &StartBit);
	int pic_height_in_map_units_minus1 = Ue(buf, sps_len, &StartBit);
	int frame_mbs_only_flag = u(1, buf, &StartBit);//...
    flag.frame_mbs_only_flag = frame_mbs_only_flag;
    
    //av_log(NULL, AV_LOG_INFO, "flag.frame_mbs_only_flag = %d\n", flag.frame_mbs_only_flag);
    //av_log(NULL, AV_LOG_INFO, "delta_pic_order_always_zero_flag = %d\n", flag.delta_pic_order_always_zero_flag);
    //av_log(NULL, AV_LOG_INFO, "log2_max_frame_num_minus4 = %d\n", flag.log2_max_frame_num_minus4);
    //av_log(NULL, AV_LOG_INFO, "pic_order_cnt_type = %d\n", flag.pic_order_cnt_type);

    return flag;
}

static PPS_TEMP_FLAG fast_analyze_pps(char* pps_buf, int pps_len)
{
    PPS_TEMP_FLAG flag;
    memset(&flag, 0, sizeof(PPS_TEMP_FLAG));
        
    // Analyze SPS 
	unsigned int StartBit = 0;
    char* buf = pps_buf;
	buf = buf + 4;
    int i = 0;
    buf = buf + 1;//skip header
    
    int pic_parameter_set_id = Ue(buf, pps_len, &StartBit);
    int seq_parameter_set_id = Ue(buf, pps_len, &StartBit);
    int entropy_coding_mode_flag = u(1, buf, &StartBit);
    int pic_order_present_flag = u(1, buf, &StartBit);//....
    flag.pic_order_present_flag = pic_order_present_flag;
    int num_slice_groups_minus1 = Ue(buf, pps_len, &StartBit);
    
    if(num_slice_groups_minus1 > 0)
    {
        int slice_group_map_type = Ue(buf, pps_len, &StartBit);

        if(slice_group_map_type == 0)
        {
            int* run_length_minus1 = av_malloc(num_slice_groups_minus1 + 1);
            for(i = 0; i <= num_slice_groups_minus1; i++)
            {
                run_length_minus1[i] = Ue(buf, pps_len, &StartBit);
                //av_log(NULL, AV_LOG_DEBUG, "run_length_minus1[%d] = %d\n", i, run_length_minus1[i]);
            }
            av_free(run_length_minus1);
        }
        else if(slice_group_map_type == 2)
        {
            int* top_left = av_malloc(num_slice_groups_minus1);
            int* bottom_right = av_malloc(num_slice_groups_minus1);
            for(i = 0; i < num_slice_groups_minus1; i++)
            {
                top_left[i] = Ue(buf, pps_len, &StartBit);
                bottom_right[i] = Ue(buf, pps_len, &StartBit);

                //av_log(NULL, AV_LOG_DEBUG, "top_left[%d] = %d\n", i, top_left[i]);
                //av_log(NULL, AV_LOG_DEBUG, "bottom_right[%d] = %d\n", i, bottom_right[i]);
            }
            av_free(top_left);
            av_free(bottom_right);
        }
        else if(slice_group_map_type == 3 || slice_group_map_type == 4 || slice_group_map_type == 5)
        {
            int slice_group_change_direction_flag = u(1, buf, &StartBit);
            int slice_group_change_rate_minus1 = Ue(buf, pps_len, &StartBit);

            //av_log(NULL, AV_LOG_DEBUG, "slice_group_change_direction_flag = %d\n", slice_group_change_direction_flag);
            //av_log(NULL, AV_LOG_DEBUG, "slice_group_change_rate_minus1 = %d\n", slice_group_change_rate_minus1);
        }
        else if(slice_group_map_type == 6)
        {
            int pic_size_in_map_units_minus1 = Ue(buf, pps_len, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "pic_size_in_map_units_minus1 = %d\n", pic_size_in_map_units_minus1);
            int* slice_group_id = av_malloc(pic_size_in_map_units_minus1 + 1);
            for(i = 0; i < pic_size_in_map_units_minus1; i++)
            {
                slice_group_id[i] = u(1, buf, &StartBit);// need fix
                //av_log(NULL, AV_LOG_DEBUG, "slice_group_id[%d]= %d\n", i, slice_group_id[i]);
            }
            av_free(slice_group_id);
        }
    }
    int num_ref_idx_l0_active_minus1 = Ue(buf, pps_len, &StartBit);
    int num_ref_idx_l1_active_minus1 = Ue(buf, pps_len, &StartBit);
    int weighted_pred_flag = u(1, buf, &StartBit);
    int weighted_bipred_idc = u(2, buf, &StartBit);
    int pic_init_qp_minus26 = Se(buf, pps_len, &StartBit);
    int pic_init_qs_minus26 = Se(buf, pps_len, &StartBit);
    int chroma_qp_index_offset = Se(buf, pps_len, &StartBit);
    int deblocking_filter_control_present_flag = u(1., buf, &StartBit);
    int constrained_intra_pred_flag = u(1, buf, &StartBit);
    int redundant_pic_cnt_present_flag = u(1, buf, &StartBit);//...
    flag.redundant_pic_cnt_present_flag = redundant_pic_cnt_present_flag;

    return flag;
}

SPS_PPS_FLAG fast_find_pps_sps_from_pkt(char* pkt_buf, int pkt_size)
{
	char* sps_buf = av_malloc(1024);
    char* pps_buf = av_malloc(1024);

	char* p;
	char last_nal_type = 0;
	int  last_nal_pos = 0;
	
    int sps_len;
    int pps_len;

	int bSpsComplete = 0;
	int bPpsComplete = 0;
	int i;

    SPS_PPS_FLAG sps_pps_flag;
    memset(&sps_pps_flag, 0, sizeof(SPS_PPS_FLAG));
    PPS_TEMP_FLAG pps_flag;
    SPS_TEMP_FLAG sps_flag;

    p = pkt_buf;
    for (i = 0; i < pkt_size - 5; i++) {
	    p = pkt_buf + i;
		if (p[0] == 0x00 && p[1] == 0x00 && p[2] == 0x00 && p[3] == 0x01) {
			if (last_nal_type == 0x67) {
				sps_len = i - last_nal_pos;
				memcpy(sps_buf, pkt_buf + last_nal_pos, sps_len);
				bSpsComplete = 1;
			}
            if(last_nal_type == 0x68) {
                pps_len = i - last_nal_pos;
                memcpy(pps_buf, pkt_buf + last_nal_pos, pps_len);
                bPpsComplete = 1;
            } 
		    last_nal_type = p[4];
			last_nal_pos = i;
			if (bSpsComplete && bPpsComplete) {
				break;
			}
		}
	}
	if (last_nal_type == 0x67 && bSpsComplete == 0) {
		sps_len = pkt_size - last_nal_pos;
		memcpy(sps_buf, pkt_buf + last_nal_pos, sps_len);
		bSpsComplete = 1;
	}
    if (last_nal_type == 0x68 && bPpsComplete == 0) {
        pps_len = pkt_size - last_nal_pos;
        memcpy(pps_buf, pkt_buf + last_nal_pos, pps_len);
        bPpsComplete = 1;
    }

    if(bSpsComplete)
        //av_log(NULL, AV_LOG_DEBUG, "Find sps, len = %d\n", sps_len);
        printf("find sps len = %d\n", sps_len);
    if(bPpsComplete)
        //av_log(NULL, AV_LOG_DEBUG, "Find pps, len = %d\n", pps_len);
        printf("find pps len = %d\n", pps_len);
	
    if(bSpsComplete)
        sps_flag = fast_analyze_sps(sps_buf, sps_len);
    if(bPpsComplete)
        pps_flag = fast_analyze_pps(pps_buf, pps_len);
    
    sps_pps_flag.sps_flag = sps_flag;
    sps_pps_flag.pps_flag = pps_flag;

    av_free(sps_buf);
    av_free(pps_buf);
     
    return sps_pps_flag;
}

int fast_find_base_pframe(char* nal_buf, int len, SPS_PPS_FLAG sps_pps_flag)
{
	unsigned int StartBit = 0;
	char* buf = nal_buf;
    buf = buf + 4;
    int i = 0;
	int forbidden_zero_bit = u(1, buf, &StartBit);
	int nal_ref_idc = u(2, buf, &StartBit);
	int nal_unit_type = u(5, buf, &StartBit);
    //av_log(NULL, AV_LOG_DEBUG, "nal_unit_type = %d\n", nal_unit_type);
	if (nal_unit_type == 1 || nal_unit_type == 5 || nal_unit_type == 2 || nal_unit_type == 19)
    {
        int first_mb_in_slice = Ue(buf, len, &StartBit);
        int slice_type = Ue(buf, len, &StartBit);
        int pic_parameter_set_id = Ue(buf, len, &StartBit);
        int frame_num = u(sps_pps_flag.sps_flag.log2_max_frame_num_minus4 + 4, buf, &StartBit);
        printf("frame num = %d\n", frame_num); 
        //av_log(NULL, AV_LOG_DEBUG, "first_mb_in_slice = %d\n", first_mb_in_slice);
        //av_log(NULL, AV_LOG_DEBUG, "slice_type = %d\n", slice_type);
        //av_log(NULL, AV_LOG_DEBUG, "pic_parameter_set_id = %d\n", pic_parameter_set_id);
        //av_log(NULL, AV_LOG_DEBUG, "frame_num = %d\n", frame_num);
        
        int field_pic_flag = 0;
        if(sps_pps_flag.sps_flag.frame_mbs_only_flag == 0)
        {
            field_pic_flag = u(1, buf, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "field_pic_flag = %d\n", field_pic_flag);
            if(field_pic_flag)
            {
                int bottom_field_flag = u(1, buf, &StartBit);
                //av_log(NULL, AV_LOG_DEBUG, "bottom_field_flag = %d\n", bottom_field_flag);
            }
        }
        if(nal_unit_type == 5)
        {
            int idr_pic_id = Ue(buf, len, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "idr_pic_id = %d\n", idr_pic_id);
        }
        if(sps_pps_flag.sps_flag.pic_order_cnt_type == 0)
        {
            int pic_order_cnt_lsb = u(1, buf, &StartBit);// need fix u(v)
            //av_log(NULL, AV_LOG_DEBUG, "pic_order_cnt_lsb = %d\n", pic_order_cnt_lsb);
            if(sps_pps_flag.pps_flag.pic_order_present_flag >= 1 && !field_pic_flag)
            {
                int delta_pic_order_cnt_bottom = Se(buf, len, &StartBit);
                //av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt_bottom = %d\n", delta_pic_order_cnt_bottom);
            }
        }
        if(sps_pps_flag.sps_flag.pic_order_cnt_type == 1 && sps_pps_flag.sps_flag.delta_pic_order_always_zero_flag == 0)
        {
            int delta_pic_order_cnt0 = Se(buf, len, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt0 = %d\n", delta_pic_order_cnt0);
            if(sps_pps_flag.pps_flag.pic_order_present_flag >= 1 && !field_pic_flag)
            {
                int delta_pic_order_cnt1 = Se(buf, len, &StartBit);
                //av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt1 = %d\n", delta_pic_order_cnt1);
            }
        }
        if(sps_pps_flag.pps_flag.redundant_pic_cnt_present_flag >= 1)
        {
            int redundant_pic_cnt = Ue(buf, len, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "redundant_pic_cnt = %d\n", redundant_pic_cnt);
        }
        if(slice_type == B || slice_type == B1)
        {
            int direct_spatial_mv_pred_flag = u(1, buf, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "direct_spatial_mv_pred_flag = %d\n", direct_spatial_mv_pred_flag);
        }
        if(slice_type == P || slice_type == P1 || slice_type == SP || slice_type == SP1
                       || slice_type == B || slice_type == B1)
        {
            int num_ref_idx_active_override_flag = u(1, buf, &StartBit);
            //av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_active_override_flag = %d\n", num_ref_idx_active_override_flag);
            if(num_ref_idx_active_override_flag)
            {
                int num_ref_idx_l0_active_minus1 = Ue(buf, len, &StartBit);
                //av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l0_active_minus1 = %d\n", num_ref_idx_l0_active_minus1);
                if(slice_type == B || slice_type == B1)
                {
                    int num_ref_idx_l1_active_minus1 = Ue(buf, len, &StartBit);
                    //av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l1_active_minus1 = %d\n", num_ref_idx_l1_active_minus1);
                }
            }
        }

        //ref_pic_list_reordering()
        if(slice_type != I && slice_type != I1 && slice_type != SI && slice_type != SI1)
        {
            int ref_pic_list_reordering_flag_l0 = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "ref_pic_list_reordering_flag = %d...........\n", 
                                       ref_pic_list_reordering_flag_l0);
            return ref_pic_list_reordering_flag_l0;
            // need some later
        }
    }
    
    return 0;
}

int find_base_pframe(char* token, char* nal_buf, int len)
{
	unsigned int StartBit = 0;
    unsigned int frame_numStartBit = 0;
	char* buf = nal_buf;
    buf = buf + 4;
    int i = 0;
	int forbidden_zero_bit = u(1, buf, &StartBit);
	int nal_ref_idc = u(2, buf, &StartBit);
	int nal_unit_type = u(5, buf, &StartBit);
    av_log(NULL, AV_LOG_DEBUG, "nal_ref_idc = %d\n", nal_ref_idc);
    av_log(NULL, AV_LOG_DEBUG, "nal_unit_type = %d\n", nal_unit_type);
	if (nal_unit_type == 1 || nal_unit_type == 5 || nal_unit_type == 2 || nal_unit_type == 19)
    {
        int first_mb_in_slice = Ue(buf, len, &StartBit);
        int slice_type = Ue(buf, len, &StartBit);
        int pic_parameter_set_id = Ue(buf, len, &StartBit);
        //int frame_num = u(1, buf, &StartBit);// need fix u(v)
	   
        frame_numStartBit = StartBit;
        int frame_num = u(g_log2_max_frame_num_minus4 + 4, buf, &StartBit);
        av_log(NULL, AV_LOG_DEBUG, "first_mb_in_slice = %d\n", first_mb_in_slice);
        av_log(NULL, AV_LOG_DEBUG, "slice_type = %d\n", slice_type);
        av_log(NULL, AV_LOG_DEBUG, "pic_parameter_set_id = %d\n", pic_parameter_set_id);
        av_log(NULL, AV_LOG_DEBUG, "frame_num = %d\n", frame_num);
        
        int field_pic_flag = 0;
        if(g_frame_mbs_only_flag == 0)
        {
            field_pic_flag = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "field_pic_flag = %d\n", field_pic_flag);
            if(field_pic_flag)
            {
                int bottom_field_flag = u(1, buf, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "bottom_field_flag = %d\n", bottom_field_flag);
            }
        }
        if(nal_unit_type == 5)
        {
            int idr_pic_id = Ue(buf, len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "idr_pic_id = %d\n", idr_pic_id);
        }
        if(g_pic_order_cnt_type == 0)
        {
            int pic_order_cnt_lsb = u(1, buf, &StartBit);// need fix u(v)
            av_log(NULL, AV_LOG_DEBUG, "pic_order_cnt_lsb = %d\n", pic_order_cnt_lsb);
            if(g_pic_order_present_flag >= 1 && !field_pic_flag)
            {
                int delta_pic_order_cnt_bottom = Se(buf, len, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt_bottom = %d\n", delta_pic_order_cnt_bottom);
            }
        }
        if(g_pic_order_cnt_type == 1 && g_delta_pic_order_always_zero_flag == 0)
        {
            int delta_pic_order_cnt0 = Se(buf, len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt0 = %d\n", delta_pic_order_cnt0);
            if(g_pic_order_present_flag >= 1 && !field_pic_flag)
            {
                int delta_pic_order_cnt1 = Se(buf, len, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_cnt1 = %d\n", delta_pic_order_cnt1);
            }
        }
        if(g_redundant_pic_cnt_present_flag >= 1)
        {
            int redundant_pic_cnt = Ue(buf, len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "redundant_pic_cnt = %d\n", redundant_pic_cnt);
        }
        if(slice_type == B || slice_type == B1)
        {
            int direct_spatial_mv_pred_flag = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "direct_spatial_mv_pred_flag = %d\n", direct_spatial_mv_pred_flag);
        }
        if(slice_type == P || slice_type == P1 || slice_type == SP || slice_type == SP1
                       || slice_type == B || slice_type == B1)
        {
            int num_ref_idx_active_override_flag = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_active_override_flag = %d\n", num_ref_idx_active_override_flag);
            if(num_ref_idx_active_override_flag)
            {
                int num_ref_idx_l0_active_minus1 = Ue(buf, len, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l0_active_minus1 = %d\n", num_ref_idx_l0_active_minus1);
                if(slice_type == B || slice_type == B1)
                {
                    int num_ref_idx_l1_active_minus1 = Ue(buf, len, &StartBit);
                    av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l1_active_minus1 = %d\n", num_ref_idx_l1_active_minus1);
                }
            }
        }

        //ref_pic_list_reordering()
        if(slice_type != I && slice_type != I1 && slice_type != SI && slice_type != SI1)
        {
            int ref_pic_list_reordering_flag_l0 = u(1, buf, &StartBit);
            if(ref_pic_list_reordering_flag_l0 == 1){
                int first_half = g_log2_max_frame_num_minus4 + 4 - (8 - frame_numStartBit % 8);
                if(first_half > 0){
		            buf[frame_numStartBit / 8] &= 0xFF << (8 - frame_numStartBit % 8);
                    buf[frame_numStartBit / 8 + 1] &= 0xFF >> (first_half - 1);
                }
                else{
                //nothing to do      
                }
            }
            av_log(NULL, AV_LOG_DEBUG, "ref_pic_list_reordering_flag = %d...........\n", 
                                       ref_pic_list_reordering_flag_l0);
            return ref_pic_list_reordering_flag_l0;
            // need some later
        }
    }
    
    return 0;
}

static int analyze_pps(char* token, char* pps_buf, int pps_len)
{
    // Analyze PPS 
	unsigned int StartBit = 0;
	char* buf = pps_buf;
    buf = buf + 4;
    int i = 0;
	int forbidden_zero_bit = u(1, buf, &StartBit);
	int nal_ref_idc = u(2, buf, &StartBit);
	int nal_unit_type = u(5, buf, &StartBit);
    av_log(NULL, AV_LOG_DEBUG, "nal_ref_idc = %d\n", nal_ref_idc);
    av_log(NULL, AV_LOG_DEBUG, "nal_unit_type = %d\n", nal_unit_type);
	if (nal_unit_type == 8)
	{
        int pic_parameter_set_id = Ue(buf, pps_len, &StartBit);
        int seq_parameter_set_id = Ue(buf, pps_len, &StartBit);
        int entropy_coding_mode_flag = u(1, buf, &StartBit);
        int pic_order_present_flag = u(1, buf, &StartBit);
        g_pic_order_present_flag = pic_order_present_flag;
        int num_slice_groups_minus1 = Ue(buf, pps_len, &StartBit);
        av_log(NULL, AV_LOG_DEBUG, "pic_parameter_set_id = %d\n", pic_parameter_set_id);
        av_log(NULL, AV_LOG_DEBUG, "seq_parameter_set_id = %d\n", seq_parameter_set_id);
        av_log(NULL, AV_LOG_DEBUG, "entropy_coding_mode_flag = %d\n", entropy_coding_mode_flag);
        av_log(NULL, AV_LOG_DEBUG, "pic_order_present_flag = %d\n", pic_order_present_flag);
        av_log(NULL, AV_LOG_DEBUG, "num_slice_groups_minus1 = %d\n", num_slice_groups_minus1);

        if(num_slice_groups_minus1 > 0)
        {
            int slice_group_map_type = Ue(buf, pps_len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "slice_group_map_type = %d\n", slice_group_map_type);

            if(slice_group_map_type == 0)
            {
                int* run_length_minus1 = av_malloc(num_slice_groups_minus1 + 1);
                for(i = 0; i <= num_slice_groups_minus1; i++)
                {
                    run_length_minus1[i] = Ue(buf, pps_len, &StartBit);
                    av_log(NULL, AV_LOG_DEBUG, "run_length_minus1[%d] = %d\n", i, run_length_minus1[i]);
                }
                av_free(run_length_minus1);
            }
            else if(slice_group_map_type == 2)
            {
                int* top_left = av_malloc(num_slice_groups_minus1);
                int* bottom_right = av_malloc(num_slice_groups_minus1);
                for(i = 0; i < num_slice_groups_minus1; i++)
                {
                    top_left[i] = Ue(buf, pps_len, &StartBit);
                    bottom_right[i] = Ue(buf, pps_len, &StartBit);

                    av_log(NULL, AV_LOG_DEBUG, "top_left[%d] = %d\n", i, top_left[i]);
                    av_log(NULL, AV_LOG_DEBUG, "bottom_right[%d] = %d\n", i, bottom_right[i]);
                }
                av_free(top_left);
                av_free(bottom_right);
            }
            else if(slice_group_map_type == 3 || slice_group_map_type == 4 || slice_group_map_type == 5)
            {
                int slice_group_change_direction_flag = u(1, buf, &StartBit);
                int slice_group_change_rate_minus1 = Ue(buf, pps_len, &StartBit);

                av_log(NULL, AV_LOG_DEBUG, "slice_group_change_direction_flag = %d\n", slice_group_change_direction_flag);
                av_log(NULL, AV_LOG_DEBUG, "slice_group_change_rate_minus1 = %d\n", slice_group_change_rate_minus1);
            }
            else if(slice_group_map_type == 6)
            {
                int pic_size_in_map_units_minus1 = Ue(buf, pps_len, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "pic_size_in_map_units_minus1 = %d\n", pic_size_in_map_units_minus1);
                int* slice_group_id = av_malloc(pic_size_in_map_units_minus1 + 1);
                for(i = 0; i < pic_size_in_map_units_minus1; i++)
                {
                    slice_group_id[i] = u(1, buf, &StartBit);// need fix

                    av_log(NULL, AV_LOG_DEBUG, "slice_group_id[%d]= %d\n", i, slice_group_id[i]);
                }
                av_free(slice_group_id);
            }
        }
        int num_ref_idx_l0_active_minus1 = Ue(buf, pps_len, &StartBit);
        int num_ref_idx_l1_active_minus1 = Ue(buf, pps_len, &StartBit);
        int weighted_pred_flag = u(1, buf, &StartBit);
        int weighted_bipred_idc = u(2, buf, &StartBit);
        int pic_init_qp_minus26 = Se(buf, pps_len, &StartBit);
        int pic_init_qs_minus26 = Se(buf, pps_len, &StartBit);
        int chroma_qp_index_offset = Se(buf, pps_len, &StartBit);
        int deblocking_filter_control_present_flag = u(1., buf, &StartBit);
        int constrained_intra_pred_flag = u(1, buf, &StartBit);
        int redundant_pic_cnt_present_flag = u(1, buf, &StartBit);  
        g_redundant_pic_cnt_present_flag = redundant_pic_cnt_present_flag;

        av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l0_active_minus1 = %d\n", num_ref_idx_l0_active_minus1);
        av_log(NULL, AV_LOG_DEBUG, "num_ref_idx_l1_active_minus1 = %d\n", num_ref_idx_l1_active_minus1);
        av_log(NULL, AV_LOG_DEBUG, "weighted_pred_flag = %d\n", weighted_pred_flag);
        av_log(NULL, AV_LOG_DEBUG, "weighted_bipred_idc = %d\n", weighted_bipred_idc);
        av_log(NULL, AV_LOG_DEBUG, "pic_init_qp_minus26 = %d\n", pic_init_qp_minus26);
        av_log(NULL, AV_LOG_DEBUG, "pic_init_qs_minus26 = %d\n", pic_init_qs_minus26);
        av_log(NULL, AV_LOG_DEBUG, "chroma_qp_index_offset = %d\n", chroma_qp_index_offset);
        av_log(NULL, AV_LOG_DEBUG, "deblocking_filter_control_present_flag = %d\n", deblocking_filter_control_present_flag);
        av_log(NULL, AV_LOG_DEBUG, "constrained_intra_pred_flag = %d\n", constrained_intra_pred_flag);
        av_log(NULL, AV_LOG_DEBUG, "redundant_pic_cnt_present_flag = %d\n", redundant_pic_cnt_present_flag);
          
        // need more deal for later
    }
    return 0;
}

static int analyze_sps(char* token, char* sps_buf, int sps_len)
{
    // Analyze SPS 
	unsigned int StartBit = 0;
    char* buf = sps_buf;
	buf = buf + 4;
    int i = 0;
	int forbidden_zero_bit = u(1, buf, &StartBit);
	int nal_ref_idc = u(2, buf, &StartBit);
	int nal_unit_type = u(5, buf, &StartBit);
	av_log(NULL, AV_LOG_DEBUG, "nal_ref_idc = %d\n", nal_ref_idc);
	av_log(NULL, AV_LOG_DEBUG, "nal_unit_type = %d\n", nal_unit_type);
    if (nal_unit_type == 7)
	{
		int profile_idc = u(8, buf, &StartBit);
		int constraint_set0_flag = u(1, buf, &StartBit);  //(buf[1] & 0x80)>>7;
		int constraint_set1_flag = u(1, buf, &StartBit);  //(buf[1] & 0x40)>>6;
		int constraint_set2_flag = u(1, buf, &StartBit);  //(buf[1] & 0x20)>>5;
		int constraint_set3_flag = u(1, buf, &StartBit);  //(buf[1] & 0x10)>>4;
		int reserved_zero_4bits = u(4, buf, &StartBit);
		int level_idc = u(8, buf, &StartBit);
        
        av_log(NULL, AV_LOG_DEBUG, "profile_idc = %d\n", profile_idc);
        av_log(NULL, AV_LOG_DEBUG, "constraint_set0_flag = %d\n", constraint_set0_flag);
        av_log(NULL, AV_LOG_DEBUG, "constraint_set1_flag = %d\n", constraint_set1_flag);
        av_log(NULL, AV_LOG_DEBUG, "constraint_set2_flag = %d\n", constraint_set2_flag);
        av_log(NULL, AV_LOG_DEBUG, "constraint_set3_flag = %d\n", constraint_set3_flag);
        av_log(NULL, AV_LOG_DEBUG, "level_idc = %d\n", level_idc);

		int seq_parameter_set_id = Ue(buf, sps_len, &StartBit);
        av_log(NULL, AV_LOG_DEBUG, "seq_parameter_set_id = %d\n", seq_parameter_set_id);

		int chroma_format_idc;
		if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144)
		{
			chroma_format_idc = Ue(buf, sps_len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "chroma_format_idc = %d\n", chroma_format_idc);
			
            int residual_colour_transform_flag;
			if (chroma_format_idc == 3)
            {
				residual_colour_transform_flag = u(1, buf, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "residual_colour_transform_flag = %d\n", residual_colour_transform_flag);
			}
            int bit_depth_luma_minus8 = Ue(buf, sps_len, &StartBit);
			int bit_depth_chroma_minus8 = Ue(buf, sps_len, &StartBit);
			int qpprime_y_zero_transform_bypass_flag = u(1, buf, &StartBit);
			int seq_scaling_matrix_present_flag = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "bit_depth_luma_minus8 = %d\n", bit_depth_luma_minus8);
            av_log(NULL, AV_LOG_DEBUG, "bit_depth_chroma_minus8 = %d\n", bit_depth_chroma_minus8);
            av_log(NULL, AV_LOG_DEBUG, "qpprime_y_zero_transform_bypass_flag = %d\n",
                                       qpprime_y_zero_transform_bypass_flag);
            av_log(NULL, AV_LOG_DEBUG, "seq_scaling_matrix_present_flag = %d\n",
                                       seq_scaling_matrix_present_flag);

			int seq_scaling_list_present_flag[8];
			if (seq_scaling_matrix_present_flag)
			{
				for (i = 0; i < 8; i++)
				{
					seq_scaling_list_present_flag[i] = u(1, buf, &StartBit);
                    av_log(NULL, AV_LOG_DEBUG, "seq_scaling_list_present_flag[%d] = %d\n",
                                               i, seq_scaling_list_present_flag[i]);
				}
			}
		}
		else
			chroma_format_idc = 1;
		int log2_max_frame_num_minus4 = Ue(buf, sps_len, &StartBit);
        g_log2_max_frame_num_minus4 = log2_max_frame_num_minus4;
		int pic_order_cnt_type = Ue(buf, sps_len, &StartBit);
        g_pic_order_cnt_type = pic_order_cnt_type;
        av_log(NULL, AV_LOG_DEBUG, "log2_max_frame_num_minus4 = %d\n", log2_max_frame_num_minus4);
        av_log(NULL, AV_LOG_DEBUG, "pic_order_cnt_type = %d\n", pic_order_cnt_type);

		int log2_max_pic_order_cnt_lsb_minus4;
		if (pic_order_cnt_type == 0)
        {
			log2_max_pic_order_cnt_lsb_minus4 = Ue(buf, sps_len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "log2_max_pic_order_cnt_lsb_minus4 = %d\n", log2_max_pic_order_cnt_lsb_minus4);
		}
        else if (pic_order_cnt_type == 1)
		{
			int delta_pic_order_always_zero_flag = u(1, buf, &StartBit);
			g_delta_pic_order_always_zero_flag = delta_pic_order_always_zero_flag;
            int offset_for_non_ref_pic = Se(buf, sps_len, &StartBit);
			int offset_for_top_to_bottom_field = Se(buf, sps_len, &StartBit);
			int num_ref_frames_in_pic_order_cnt_cycle = Ue(buf, sps_len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "delta_pic_order_always_zero_flag = %d\n", delta_pic_order_always_zero_flag);
            av_log(NULL, AV_LOG_DEBUG, "offset_for_non_ref_pic = %d\n", offset_for_non_ref_pic);
            av_log(NULL, AV_LOG_DEBUG, "offset_for_top_to_bottom_field = %d\n", offset_for_top_to_bottom_field);
            av_log(NULL, AV_LOG_DEBUG, "num_ref_frames_in_pic_order_cnt_cycle = %d\n", num_ref_frames_in_pic_order_cnt_cycle);
            
			int *offset_for_ref_frame = av_malloc(sizeof(int)*num_ref_frames_in_pic_order_cnt_cycle);
			for (i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; i++)
            {
				offset_for_ref_frame[i] = Se(buf, sps_len, &StartBit);
                av_log(NULL, AV_LOG_DEBUG, "offset_for_ref_frame[%d] = %d\n", i, offset_for_ref_frame[i]);
			}
            av_free(offset_for_ref_frame);
		}
		int num_ref_frames = Ue(buf, sps_len, &StartBit);
		int gaps_in_frame_num_value_allowed_flag = u(1, buf, &StartBit);
		int pic_width_in_mbs_minus1 = Ue(buf, sps_len, &StartBit);
		int pic_height_in_map_units_minus1 = Ue(buf, sps_len, &StartBit);
        av_log(NULL, AV_LOG_DEBUG, "num_ref_frames = %d\n", num_ref_frames);
        av_log(NULL, AV_LOG_DEBUG, "gaps_in_frame_num_value_allowed_flag = %d\n", gaps_in_frame_num_value_allowed_flag);
        av_log(NULL, AV_LOG_DEBUG, "pic_width_in_mbs_minus1 = %d\n", pic_width_in_mbs_minus1);
        av_log(NULL, AV_LOG_DEBUG, "pic_height_in_map_units_minus1 = %d\n", pic_height_in_map_units_minus1);

		int frame_mbs_only_flag = u(1, buf, &StartBit);
		g_frame_mbs_only_flag = frame_mbs_only_flag;
        av_log(NULL, AV_LOG_DEBUG, "frame_mbs_only_flag = %d\n", frame_mbs_only_flag);
	    int mb_adaptive_frame_field_flag;
		if (!frame_mbs_only_flag)
        {
	        mb_adaptive_frame_field_flag = u(1, buf, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "mb_adaptive_frame_field_flag = %d\n", mb_adaptive_frame_field_flag);
        }
	    else
	        mb_adaptive_frame_field_flag = 0;

	    int direct_8x8_inference_flag = u(1, buf, &StartBit);
	    int frame_cropping_flag = u(1, buf, &StartBit);
        av_log(NULL, AV_LOG_DEBUG, "direct_8x8_inference_flag = %d\n", direct_8x8_inference_flag);
        av_log(NULL, AV_LOG_DEBUG, "frame_cropping_flag = %d\n", frame_cropping_flag);

	    int crop_left = 0, crop_right = 0, crop_top = 0, crop_bottom = 0;
	    if (frame_cropping_flag)
	    {
			crop_left = Ue(buf, sps_len, &StartBit);
			crop_right = Ue(buf, sps_len, &StartBit);
			crop_top = Ue(buf, sps_len, &StartBit);
			crop_bottom = Ue(buf, sps_len, &StartBit);
            av_log(NULL, AV_LOG_DEBUG, "crop_left = %d\n", crop_left);
            av_log(NULL, AV_LOG_DEBUG, "crop_right = %d\n", crop_right);
            av_log(NULL, AV_LOG_DEBUG, "crop_top = %d\n", crop_top);
            av_log(NULL, AV_LOG_DEBUG, "crop_bottom = %d\n", crop_bottom);
	    }
        int vui_parameters_present_flag = u(1, buf, &StartBit);
        if(vui_parameters_present_flag)
            av_log(NULL, AV_LOG_DEBUG, "vui parameters will go on...\n");
        else
            av_log(NULL, AV_LOG_DEBUG, "no vui parameters\n");

		return 0;
	} else {
		return -1;
	}
}

int analyse_from_pkt(char* token, char* pkt_buf, int pkt_size)
{
	char* sps_buf = av_malloc(1024);
    char* pps_buf = av_malloc(1024);

	char* p;
	char last_nal_type = 0;
	int  last_nal_pos = 0;
	
    int sps_len;
    int pps_len;

	int bSpsComplete = 0;
	int bPpsComplete = 0;
	int i;
    
    p = pkt_buf;
    for (i = 0; i < pkt_size - 5; i++) {
	    p = pkt_buf + i;
		if (p[0] == 0x00 && p[1] == 0x00 && p[2] == 0x00 && p[3] == 0x01) {
			if (last_nal_type == 0x67) {
				sps_len = i - last_nal_pos;
				memcpy(sps_buf, pkt_buf + last_nal_pos, sps_len);
				bSpsComplete = 1;
			}
            if(last_nal_type == 0x68) {
                pps_len = i - last_nal_pos;
                memcpy(pps_buf, pkt_buf + last_nal_pos, pps_len);
                bPpsComplete = 1;
            } 
		    last_nal_type = p[4];
			last_nal_pos = i;
			if (bSpsComplete && bPpsComplete) {
				break;
			}
		}
	}
	if (last_nal_type == 0x67 && bSpsComplete == 0) {
		sps_len = pkt_size - last_nal_pos;
		memcpy(sps_buf, pkt_buf + last_nal_pos, sps_len);
		bSpsComplete = 1;
	}
    if (last_nal_type == 0x68 && bPpsComplete == 0) {
        pps_len = pkt_size - last_nal_pos;
        memcpy(pps_buf, pkt_buf + last_nal_pos, pps_len);
        bPpsComplete = 1;
    }

    //if(bSpsComplete)
        //av_log(NULL, AV_LOG_DEBUG, "Find sps, len = %d\n", sps_len);
    
    //if(bPpsComplete)
    //    av_log(NULL, AV_LOG_DEBUG, "Find pps, len = %d\n", pps_len);
	
    if(bSpsComplete)
        analyze_sps(NULL, sps_buf, sps_len);
    if(bPpsComplete)
        analyze_pps(NULL, pps_buf, pps_len);
    
    av_free(sps_buf);
    av_free(pps_buf);
    return 0;
}

int analyse_from_buf(char* pData, int nDataLen)
{
    int bSpsComplete = false;
    int bPpsComplete = false;

    //Find SPS
    unsigned char ucLastNalType = 0;            
    if(pData[0] == 0x00 && pData[1] == 0x00 && pData[2] == 0x00 && pData[3] == 0x01)
    {
        ucLastNalType = pData[4];
        unsigned char uctype = pData[4] & 0x7;
        if(uctype ==  0x7)//SPS后5bit 和为7
            bSpsComplete = true;
        
        if(uctype == 0x8)//PPS
           bPpsComplete = true;
    }

    if (bPpsComplete == false && bSpsComplete == false)
       return false;
    
    if(bSpsComplete)
        analyze_sps(NULL, pData, nDataLen);
    if(bPpsComplete)
        analyze_pps(NULL, pData, nDataLen);
    return true;
}
