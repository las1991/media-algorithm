#include<stdio.h>
#include"cJSON.h"

/*const char *json1 = "{\
                     \n\"sensitivity\": 1000,\
                     \n\"dataList\":[\
                     \n{\
                     \n\"id\": 297,\
                     \n\"pos\": \"17,27,62,72\"\
                     \n},\
                     \n{\
                     \n\"id\": 298,\
                     \n\"pos\": \"17,27,62,72\"
                     \n}\
                     \n]\
                     \n}";*/
                   //\n\"pos\":\"17,23,34,45\"

const char *json1="{\
                   \n\"sensitivity\":1000,\
                   \n\"dataList\":[{\
                   \n\"id\":297,\
                   \n\"pos\":\"17,23,34,45\" \
                   \n}] \
                    \n}";

int main()
{
    cJSON *pjson = cJSON_Parse(json1);
    if( !pjson ) 
    {
        return -1;
    }
    cJSON *psen = cJSON_GetObjectItem(pjson,"sensitivity");
    if( !psen ) return -1;
    int sensitivity_value = psen->valueint;
    printf("sensitivity=%d\n",sensitivity_value);
    cJSON *pparam_array= cJSON_GetObjectItem(pjson,"dataList");
    if(!pparam_array) return -1;
    if(cJSON_Array != pparam_array->type)
    {
        printf("parase error array\n");
        return -1;
    }
    int size = cJSON_GetArraySize(pparam_array);
    printf("zone_count=%d\n",size);
    int i = 0;
    for( i = 0; i < size; i++ )
    {
        cJSON *item = cJSON_GetArrayItem(pparam_array,i);
        if( !item ) continue;
        cJSON *subitem = item->child;
        printf("%s:%d\n",subitem->string,subitem->valueint);
        printf("%s:%s\n",subitem->next->string,subitem->next->valuestring);
        char *str = subitem->next->valuestring;
        char *token = strtok(str,",");
        while(token)
        {
            int a = atoi(token);
            printf("pos=%d\n",a);
            token = strtok(NULL,",");
        }
        cJSON *cid = cJSON_GetObjectItem(subitem,"id");
        if(!cid) return -1;
        printf("%d\n",cid->valueint);
        //printf("id=%d\n",cJSON_GetObjectItem(subitem,"id")->valueint);
        //char *pos = cJSON_GetObjectItem(subitem->next,"pos")->valuestring;
        //printf("pos=%s\n",pos);
    }
    cJSON_Delete(pjson);
    return 0;
}


