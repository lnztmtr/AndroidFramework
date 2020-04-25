/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_NDEBUG 0
#define LOG_TAG "SubSource"
#include "utils/Log.h"
#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <utils/String8.h>

#include <SubSource.h>
extern "C"
{
	#include "stdio.h"
};

//#include <ui/Overlay.h>
//#define  TRACE()	LOGV("[%s::%d]\n",__FUNCTION__,__LINE__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

//#define  TRACE()

#include <cutils/properties.h>

// ----------------------------------------------------------------------------

int open_sub_device()
{
	int handle;
	do
   {
	    handle=open("/dev/amstream_sub_read",O_RDONLY);
		if(handle < 0){
			handle=-1;
			usleep(1000);
			continue;
		}
		break;
	}while(1);
	return handle;
}

void close_sub_device(int handle)
{
	close(handle);
}
namespace android {
#ifndef MIN
#define MIN(x,y) ((x)<(y)?(x):(y))
#endif
SubSource::SubSource()
    : mDataSource(NULL),
      mFirstFramePos(-1),
      mFixedHeader(0),
      mCurrentPos(0),
      mCurrentTimeUs(0),
      mStarted(false),
      mBasisTimeUs(0),
      mSamplesRead(0) {
      sub_cur_id=-1;
      sub_num=0;
      sub_handle=-1;
      //mMeta=new MetaData;
	  //mMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_TEXT_3GPP);
}

SubSource::~SubSource() {
    if (mStarted) {
        stop();
    }
}

status_t SubSource::start(MetaData *) {
    //CHECK(!mStarted);
	//open amstreamer fd
	if(sub_handle>=0)
		close(sub_handle);
	sub_handle=open_sub_device();
	LOGE("sub_handle:%d \n",sub_handle);
	mStarted = true;
    return OK;
}

status_t SubSource::stop() {
    //CHECK(mStarted);
	 if(sub_handle>=0)
	 	close_sub_device(sub_handle);
	 sub_handle=-1;
	 mStarted = false;
    return OK;
}

sp<MetaData> SubSource::getFormat() {
	if(!sub_num)
		return NULL;
	if(sub_cur_id==-1)
		return NULL;
    return mMeta[sub_cur_id];
}

int get_subtitle_index()
{
    int fd;
	int subtitle_cur = -1;
    char *path = "/sys/class/subtitle/index";    
	char  bcmd[16];
	fd=open(path, O_RDONLY);
	if(fd>=0)	{    	
    	read(fd,bcmd,sizeof(bcmd)); 
		sscanf(bcmd, "%d", &subtitle_cur);
    	close(fd);    	
	}
	return subtitle_cur;   
}

/*
type 1: 3gpp
type 2 :---
*/
int SubSource::addType(int index,int type)
{
	if(index>8)
	{
		LOGE("too much sub\n");
		return -1;
	}
	if(sub_num>0)
	{
		if(index<sub_num)
		{
			LOGE("sub has been added before\n");
			return -1;
		}
		if(index!=sub_num)
		{
			LOGE("wrong sub index\n");
			return -1;
		}
	}
	if(type!=1)
	{
		LOGE("wrong sub type\n");
		return -1;
	}	
	sub_num++;
	mMeta[sub_num-1]=new MetaData;
	mMeta[sub_num-1]->setCString(kKeyMIMEType, MEDIA_MIMETYPE_TEXT_3GPP);
	if(sub_cur_id==-1)
		sub_cur_id=0;
	return 0;
}
status_t SubSource:: find_sub_header(char *header)
{
	int ret;
	int index=0;
	int need_read_count=20;
	char tmp_buf[20];
	ret =read_sub_data(sub_handle,header,need_read_count);
	if(ret==-1)
	{
		LOGE("amstream buf have no sub data now\n");
		return WOULD_BLOCK;
	}
	need_read_count=0;
	do{
		//start code
		if ((header[index]!=0x41)||(header[index+1]!=0x4d)||
				(header[index+2]!=0x4c)||(header[index+3]!=0x55)|| (header[index+4]!=0xaa))
		{
				//LOGE("\n wrong subtitle header :%x %x %x %x    %x %x %x %x    %x %x %x %x \n",header[0],header[1],header[2],header[3],header[4],header[5],
				//						header[6],header[7],header[8],header[9],header[10],header[11]);

				LOGE("\n wrong subtitle header :%x %x %x %x %x  \n",header[index],header[index+1],header[index+2],header[index+3],header[index+4],header[index+5]
										);
				//LOGE("\n\n ******* find wrong subtitle header!! ******\n\n");
				if(index==15)
				{
					need_read_count=16;
					memcpy(tmp_buf,header+16,4);
					ret=read_sub_data(sub_handle,tmp_buf+4,need_read_count);
					if(ret==-1)
					{
						LOGE("amstream buf have no sub data now\n");
						return WOULD_BLOCK;
					}
					memcpy(header,tmp_buf,20);
					need_read_count=0;
					index=0;
					continue;
				}
				index++;
				continue;
		}
		// we found header
		if(index==0)
			break;
		need_read_count=index;
		memcpy(tmp_buf,header+index,20-index);
	   ret=read_sub_data(sub_handle,tmp_buf+20-index,need_read_count);
		if(ret==-1)
		{
			LOGE("amstream buf have no sub data now\n");
			return WOULD_BLOCK;
		}
		memcpy(header,tmp_buf,20);
		break;
	}while(1);
	return OK;
}
status_t SubSource::read(
        MediaBuffer **out, const ReadOptions *options) {
    *out = NULL;
	
	int ret=0;
	unsigned sync_bytes=0x414d4c55;
	int rd_oft = 0;
	if(sub_handle < 0)
	{
		LOGE("sub_handle invalid\n");
		//use WOULD_BLOCK, Since other weill crash
		return WOULD_BLOCK;
	}
	//get current sub index.
	int actual_id=get_subtitle_index();
	if(actual_id==-1||sub_cur_id==-1)
	{
		LOGE("acturl sub get error \n");
		return WOULD_BLOCK;
	}
	if(sub_cur_id!=actual_id)
	{
		LOGE("id not equal sub_cur_id:%d acturl:%d \n",sub_cur_id,actual_id);
		return WOULD_BLOCK;
	}
	//LOGE("=sjw==ok,start read header.sub_cur_id:%d actural:%d \n",sub_cur_id,actual_id);
	/*read sub header*/
	char *header=(char *)malloc(20);
	ret=find_sub_header(header);
	if(ret !=OK)
		return WOULD_BLOCK;//find no header
	rd_oft=5;
	//sub type-- data size-- sub pts
	unsigned int current_type,current_length,current_pts;
	current_type = header[rd_oft++]<<16;
	current_type |= header[rd_oft++]<<8;
	current_type |= header[rd_oft++];
	
	current_length = header[rd_oft++]<<24;
	current_length |= header[rd_oft++]<<16;
	current_length |= header[rd_oft++]<<8;
	current_length |= header[rd_oft++];	
		
	current_pts = header[rd_oft++]<<24;
	current_pts |= header[rd_oft++]<<16;
	current_pts |= header[rd_oft++]<<8;
	current_pts |= header[rd_oft++];
	//last 4bytes 0xffffffff  --skip
	//LOGE("current_pts is %d\n",current_pts);
   //LOGE("current_length is %d\n",current_length);
	/*data*/
	//get next frame size
	MediaBuffer *buffer=new MediaBuffer(current_length);
	//read data to buffer->data
	ret =read_sub_data(sub_handle,(char *)(buffer->data()),current_length);
	if(ret==-1)
		return NOT_ENOUGH_DATA;
	//set metadata
	buffer->meta_data()->setInt64(kKeyTime,current_pts*100/9);
	*out=buffer;
	//LOGE("Read Data OK\n");
    return OK;
}
int SubSource::read_sub_data(int sub_fd, char *buf, unsigned int length)
{
    int data_size=length, r, read_done=0;
	int fail_cnt=0;
    while (data_size)
    {
        r = ::read(sub_fd,buf+read_done,data_size);
        if (r<=0)
        {
			 usleep(1000);
			 fail_cnt++;
			 if(fail_cnt==50)
			 	return -1;
			 continue;
            //return 0;
        }
        else
        {
			  //LOGE("have  data r:%d \n",r);
            data_size -= r;
            read_done += r;
			  fail_cnt=0;
        }
    }
    return 0;
}

}
