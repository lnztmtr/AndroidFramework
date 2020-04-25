#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdint.h>
#include <zlib.h>

#include "init.h"
#include "property_service.h"
#include "bootenv.h"
#include "log.h"
#include "util.h"

#ifdef MTD_OLD
# include <stdint.h>
# include <linux/mtd/mtd.h>
#else
# define  __user	/* nothing */
# include <mtd/mtd-user.h>
#endif

char BootenvPartitionName[32]={0};
char PROFIX_UBOOTENV_VAR[32]={0};


static unsigned int ENV_PARTITIONS_SIZE = 0;
static unsigned int ENV_EASER_SIZE = 0 ;
static unsigned int ENV_SIZE = 0;


#define MAX_UBOOT_RWRETRY 5


typedef	struct env_image {
	uint32_t  crc;			/* CRC32 over data bytes	*/
	char data[]; 	            /* Environment data		*/
} env_image_t;


typedef struct environment {
	void			*image;
	uint32_t		*crc;
	char			*data;
}environment_t;



typedef struct env_attribute {
        struct env_attribute *next;
        char key[256];
        char value[1024];
}env_attribute;

static struct environment env_data;
static struct env_attribute env_attribute_header;
//static char env_arg_buf[ENV_PARTITIONS_SIZE+sizeof(uint32_t)];


/*************************for demo uboot arg areas write uboot args read and write*********************/

/* Parse a session attribute */
static env_attribute *
env_parse_attribute(void)
{
    char *value,*key; 
	char *line;
	char *proc = env_data.data;
	char *next_proc;
	env_attribute *attr = &env_attribute_header;
    int n_char,n_char_end;

	memset(attr, 0, sizeof(env_attribute));
	
	do{	        
		next_proc = proc+strlen(proc)+sizeof(char);
		//NOTICE("process %s\n",proc);
        key = strchr(proc, (int)'=');
		if(key != NULL){
			*key=0;
			strcpy(attr->key, proc);
			strcpy(attr->value, key+sizeof(char));
		}else{
			ERROR("error need '=' skip this value\n");
		}
		if(!(*next_proc)){
			//NOTICE("process end \n");
			break;
		}
		proc = next_proc;			
		/****************************************************/
		attr->next =(env_attribute *)malloc(sizeof(env_attribute));
		if(attr->next == NULL){
			ERROR("exit malloc error \n");
			break;
		}
		memset(attr->next, 0, sizeof(env_attribute));
		attr = 	attr->next;
	}while(1);

	//NOTICE("*********key: [%s]\n",env_attribute_header.next->key);
    return &env_attribute_header;
}


/*  attribute revert to sava data*/
static int
env_revert_attribute(void)
{
	int len;
	env_attribute *attr = &env_attribute_header;
	char *data = env_data.data;
	memset(env_data.data,0,ENV_SIZE);
	do{
		len=sprintf(data,"%s=%s",attr->key,attr->value);		
		if(len < (int)(sizeof(char)*3)){
			NOTICE("----Invalid data---- \n");
		}
		else
			data += len+sizeof(char);
		attr=attr->next;
	}while(attr);
	return 0; 
}


void  bootenv_print(void)
{
	env_attribute *attr=&env_attribute_header;
	while(attr != NULL){		  	
		NOTICE("key:   [%s]\n",attr->key);
		NOTICE("value: [%s]\n\n",attr->value);
		attr = attr->next;
	}
}
int
read_bootenv()
{
	int fd;
	int ret;
	uint32_t crc_calc;
	env_attribute *attr;
	struct mtd_info_user info;
	struct env_image *image;
	char *addr;

	if ((fd = open(BootenvPartitionName,O_RDONLY)) < 0)
	{
		ERROR("----open devices error: %s\n" ,strerror(errno));
		return -1;
	}
	
	addr = malloc(ENV_PARTITIONS_SIZE);
	if (addr == NULL) {
		ERROR("Not enough memory for environment (%u bytes)\n",ENV_PARTITIONS_SIZE);
		close(fd);
		return -2;
	}

	memset(addr,0,ENV_PARTITIONS_SIZE);
	env_data.image = addr;
	image = (struct env_image *)addr;
	env_data.crc	= &(image->crc);
	env_data.data	= image->data;
		
 	ret = read(fd ,env_data.image, ENV_PARTITIONS_SIZE);
 	if(ret == (int)ENV_PARTITIONS_SIZE)
 	{
 		crc_calc = crc32(0,(uint8_t *)env_data.data, ENV_SIZE); 
		if(crc_calc != *(env_data.crc)){	
			  ERROR("----CRC Check ERROR save_crc=%08x,calc_crc = %08x \n"
													,*env_data.crc,crc_calc);
			  close(fd);
			  return -3;
		}
		attr = env_parse_attribute();
		if(attr == NULL){
			close(fd);
			return -4;
		}
		//bootenv_print();
 	}else{
 		  NOTICE("----read error 0x%x \n",ret);
		  close(fd);
		  return -5;
 	}
	close(fd);
	return 0;	
}


const char * bootenv_get_value(const char * key)
{
	env_attribute *attr=&env_attribute_header;
	while(attr){
		if(!strcmp(key,attr->key)){
			return attr->value;
		}
		attr = attr->next;
	}
	return NULL;
}
/*
creat_args_flag : if true , if envvalue don't exists Creat it .
					if false , if envvalue don't exists just exit .
*/
int bootenv_set_value(const char * key,  const char * value,int creat_args_flag)
{
	env_attribute *attr=&env_attribute_header;
	env_attribute *last = attr;
	while(attr){
		if(!strcmp(key,attr->key)){
			strcpy(attr->value,value);
			return 2;
		}
		last = attr;
		attr = attr->next;
	}
	
	if(creat_args_flag){
		NOTICE("ubootenv.var.%s not found, create it.\n", key);
		/*******Creat a New args*********************/
		attr =(env_attribute *)malloc(sizeof(env_attribute));
		last->next = attr;
		memset(attr, 0, sizeof(env_attribute));
		strcpy(attr->key,key);
		strcpy(attr->value,value);
		return 1;
	}else
		return 0;
}

int save_bootenv()
{
	int fd;
	int err;
	struct erase_info_user erase;
	struct mtd_info_user info;
	unsigned char *data;
	env_revert_attribute();
	*(env_data.crc) = crc32(0, (uint8_t *)env_data.data, ENV_SIZE);

	
	if ((fd = open (BootenvPartitionName, O_RDWR)) < 0)	{
		ERROR("----open devices error\n");
		return -1;
	}

	if(strstr (BootenvPartitionName, "mtd")){
		memset(&info, 0, sizeof(info));
		err = ioctl(fd, MEMGETINFO, &info);
		if(err < 0)
		{
			ERROR("Get MTD info error\n");
			close(fd);
			return -4;
		}

		erase.start = 0;
		if(info.erasesize > ENV_PARTITIONS_SIZE)
		{
			data = (unsigned char*)malloc(info.erasesize);
			if(data == NULL)
			{
				ERROR("Out of memory!!!\n");
				close(fd);
				return -5;
			}
			memset(data, 0, info.erasesize);
			err = read(fd, (void*)data, info.erasesize);
			if(err != (int)info.erasesize)
			{
				ERROR("Read access failed !!!\n");
				free(data);
				close(fd);
				return -6;
			}
			memcpy(data, env_data.image, ENV_PARTITIONS_SIZE);
			erase.length = info.erasesize;
		}
		else{
			erase.length = ENV_PARTITIONS_SIZE;
		}

		err = ioctl (fd,MEMERASE,&erase);
		if (err < 0)
		{
			ERROR ("MEMERASE ERROR %d\n",err);
		    close(fd);
			return  -2;
		}

		if(info.erasesize > ENV_PARTITIONS_SIZE)
		{
			lseek(fd, 0L, SEEK_SET);
	 		err = write(fd , data, info.erasesize);
			free(data);
		}
		else
	 		err = write(fd ,env_data.image, ENV_PARTITIONS_SIZE);		

	}else{
		//emmc and nand needn't erase
		err = write(fd ,env_data.image, ENV_PARTITIONS_SIZE); 		
	}


	close(fd);
	if (err < 0){ 
		NOTICE ("----ERROR  write, size %d \n",ENV_PARTITIONS_SIZE);
		return -3;
	}
	return 0;
}

int is_bootenv_varible(const char* prop_name) 
{
	if (!prop_name || !(*prop_name))
		return 0;

	if (!(*PROFIX_UBOOTENV_VAR))
		return 0;

	if (strncmp(prop_name, PROFIX_UBOOTENV_VAR, strlen(PROFIX_UBOOTENV_VAR)) == 0
		&& strlen(prop_name) > strlen(PROFIX_UBOOTENV_VAR) )
		return 1;

	return 0;
}

static void init_bootenv_prop(const char *key, const char *value, void *cookie)
{
	if (is_bootenv_varible(key)) {
		const char* varible_name = key + strlen(PROFIX_UBOOTENV_VAR);
		const char *varible_value = bootenv_get_value(varible_name);
		if (!varible_value)
			varible_value = "";
		if (strcmp(varible_value, value)) {
			property_set(key, varible_value);
			(*((int*)cookie))++;
		}		
	}
}
#if BOOT_ARGS_CHECK

char mac_address[20] = {0};
char licence[36] = {0};
char device_id[42] = {0};
#define BURN_ARGS_SIZE  128
#define BURN_ARGS_FILENAME "/param/burn_args.arg"
#define ARGS_ETHADDR "ethaddr"
#define ARGS_ETHADDR_WHOLE "ubootenv.var.ethaddr"
#define ARGS_LICENCE "igrsid"
#define ARGS_LICENCE_WHOLE "ubootenv.var.igrsid"
#define ARGS_DEVICE_ID "huanid"
#define ARGS_DEVICE_ID_HOLE "ubootenv.var.huanid"


typedef	struct burn_args {
	uint32_t	  crc;			/* CRC32 over data bytes	*/
	unsigned char data[BURN_ARGS_SIZE]; 	/* Environment data		*/
} burn_t;

static struct burn_args  burn_args_data ;

int read_args_from_file()
{
    char *data ;
    uint32_t crc_calc;
    char * write_buff = mac_address ;
    int file_size = 0 , read_pos = 0 , write_pos = 0 ;
    data = read_file( BURN_ARGS_FILENAME , &file_size);
    
    if (!data)
    {
    	NOTICE( " E03LOG %s failed!\n" , BURN_ARGS_FILENAME );
    	return -1;
    }
    else
    {
		NOTICE( " E03LOG read_default_boot_args success size: [%d]  burn_args_data size : [%d] ;  %s \n" , file_size , sizeof(burn_args_data) , data );
		if( file_size > sizeof(burn_args_data) )
		{
	    	NOTICE( " E03LOG file size :[%d] > burn_args_data size: [%d] \n" , file_size , sizeof(burn_args_data) );
			//return -3 ;
		}
		memset((void*)&burn_args_data,0,sizeof(burn_args_data));
    	memcpy( (void *)&burn_args_data , data , sizeof(burn_args_data) );
		crc_calc = crc32 (0,burn_args_data.data, BURN_ARGS_SIZE); 
		if(crc_calc != burn_args_data.crc)
		{
			NOTICE( " E03LOG check args failed! file crs32:[%d]  ,  real crs32:[%d]\n" , burn_args_data.crc , crc_calc );
			return -2 ;
		}
		else
		{
			NOTICE( " E03LOG check args accordant ! file crs32:[%d]  ,  real crs32:[%d]\n" , burn_args_data.crc , crc_calc );
		}
    }
    for( read_pos = 0 ; read_pos < file_size ; read_pos++ )
    {
    	if( 0x0A == burn_args_data.data[read_pos] )
    	{
    		if( write_buff == mac_address )
			{
				NOTICE( " E03LOG mac of file %s \n" , write_buff  );
				write_buff = licence ;
			}
			else if( write_buff == licence  )
			{
				NOTICE( " E03LOG licence of file %s \n" , write_buff  );
				write_buff = device_id ;
			}
			else 
			{
				NOTICE( " E03LOG device_id of file %s \n" , write_buff  );
				break;
			}
			write_pos = 0 ;
    		continue ;
    	}
        write_buff[write_pos] = burn_args_data.data[read_pos] ;
		write_pos++ ;
    }
    return 0;
}

int write_args_to_file()
{
	int fd;
	int err;
	if ((fd = open (BURN_ARGS_FILENAME, O_RDWR| O_CREAT )) < 0)
	{
		NOTICE( " E03LOG ----open devices error\n");
		return -1;
	}

	if(lseek(fd, 0, SEEK_SET) != 0) 
	{
		NOTICE( " E03LOG lseek ERROR %d\n",err);
	}
    memset((void*)&burn_args_data,0,sizeof(burn_args_data));
	sprintf( burn_args_data.data , "%s\n%s\n%s\n", bootenv_get_value(ARGS_ETHADDR),bootenv_get_value(ARGS_LICENCE),bootenv_get_value(ARGS_DEVICE_ID)) ;
	burn_args_data.crc = crc32 (0,burn_args_data.data, BURN_ARGS_SIZE); 

	NOTICE( " E03LOG  write args crc: [%d]   \n %s \n " , burn_args_data.crc , burn_args_data.data ) ;
	err = write(fd ,(void *)&burn_args_data, sizeof(burn_args_data));	
	close(fd);
	if (err < 0)
	{ 
		NOTICE( " E03LOG write args----ERROR  write, size %d \n",err);
		return -3;
	}
	else
	{
		NOTICE( " E03LOG write args----success , size %d \n",err);
	}
	return 0 ;
}

void check_boot_args()
{
	int ret = read_args_from_file();
	char * p_ethaddr = bootenv_get_value(ARGS_ETHADDR) ;
	char * p_licence = bootenv_get_value(ARGS_LICENCE);
	char * p_device_id = bootenv_get_value(ARGS_DEVICE_ID) ;
	
	if( 0 != ret )
	{
		if( NULL != p_ethaddr && NULL != p_licence && NULL != p_device_id )
			write_args_to_file();
		NOTICE( " E03LOG read args from file failed . write boot args to file \n" ) ;
		return ;
	}
	 
	if( NULL == p_ethaddr || 0 != strcmp( mac_address ,  p_ethaddr ))
	{
		NOTICE( " E03LOG check mac address failed . file: %s  boot: %s \n" , mac_address ,  bootenv_get_value(ARGS_ETHADDR)  ) ;
		update_bootenv_varible( ARGS_ETHADDR_WHOLE, mac_address );
	}
	else
	{
		NOTICE( " E03LOG check mac address accordant . file: %s  boot: %s \n" , mac_address ,  bootenv_get_value(ARGS_ETHADDR)  ) ;
	}
	
	if( NULL == p_licence || 0 != strcmp( licence,  p_licence ))
	{
		NOTICE( " E03LOG check licence failed . file: %s  boot: %s \n" , licence ,  bootenv_get_value(ARGS_LICENCE)  ) ;
		update_bootenv_varible( ARGS_LICENCE_WHOLE, licence );
	}
	else
	{
		NOTICE( " E03LOG check licence accordant . file: %s  boot: %s \n" , licence ,  bootenv_get_value(ARGS_LICENCE)  ) ;
	}
	
	if( NULL == p_device_id || 0 != strcmp( device_id,  p_device_id  ) )
	{
		NOTICE( " E03LOG check device_id failed . file: %s  boot: %s \n" , device_id ,  bootenv_get_value(ARGS_DEVICE_ID)  ) ;
		update_bootenv_varible( ARGS_DEVICE_ID_HOLE, device_id );
	}
	else
	{
		NOTICE( " E03LOG check device_id accordant . file: %s  boot: %s \n" , device_id ,  bootenv_get_value(ARGS_DEVICE_ID)  ) ;
	}

}

#endif

int init_bootenv_varibles(void) {
	struct stat st;
	struct mtd_info_user info;
	int err;
	int fd;
	int  ret = -1; 
	int i = 0;
	int count = 0;	
	int id = mtd_name_to_number("ubootenv");
	if(id >= 0){
		sprintf(BootenvPartitionName, "/dev/mtd/mtd%d", id);
		if ((fd = open (BootenvPartitionName, O_RDWR)) < 0){
			ERROR("open device(%s) error : %s \n",BootenvPartitionName,strerror(errno) );
			return -2;
		}
		memset(&info, 0, sizeof(info));
		err = ioctl(fd, MEMGETINFO, &info);
		if(err < 0){
			ERROR("get MTD info error\n");
			close(fd);
			return -3;
		}
		ENV_EASER_SIZE = info.erasesize;
		ENV_PARTITIONS_SIZE = info.size;
		ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(long);

	}else if(!stat("/dev/nand_env", &st)){
		INFO("stat /dev/nand_env OK\n");
		sprintf (BootenvPartitionName, "/dev/nand_env");
		/*#ifdef MESON8_ENVSIZE
		ENV_PARTITIONS_SIZE = 0x10000;
		#else
		ENV_PARTITIONS_SIZE = 0x8000;
		#endif
		ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(long);*/
		ENV_PARTITIONS_SIZE = 0x10000;
		ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(uint32_t);
        INFO("[ubootenv] using /dev/nand_env with size(%d)(%d)", ENV_PARTITIONS_SIZE,ENV_SIZE);
		
	}else if(!stat("/dev/block/env", &st)){
		INFO("stat /dev/block/env OK\n");
		sprintf (BootenvPartitionName, "/dev/block/env");
		/*#ifdef MESON8_ENVSIZE
		ENV_PARTITIONS_SIZE = 0x10000;
		#else
		ENV_PARTITIONS_SIZE = 0x8000;
		#endif
		ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(long);*/
        ENV_PARTITIONS_SIZE = 0x10000;
        ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(uint32_t);
        INFO("[ubootenv] using /dev/block/env with size(%d)(%d)", ENV_PARTITIONS_SIZE,ENV_SIZE);
	}else if(!stat("/dev/block/ubootenv", &st)){

		sprintf(BootenvPartitionName, "/dev/block/ubootenv");
		if ((fd = open(BootenvPartitionName, O_RDWR)) < 0){
			ERROR("open device(%s) error\n",BootenvPartitionName );
			return -2;
		}

		memset(&info, 0, sizeof(info));
		err = ioctl(fd, MEMGETINFO, &info);
		if(err < 0){
			fprintf (stderr,"get MTD info error\n");
			close(fd);
			return -3;
		}

		ENV_EASER_SIZE  = info.erasesize;//0x20000;//128K
		ENV_PARTITIONS_SIZE = info.size;//0x8000;
		ENV_SIZE = ENV_PARTITIONS_SIZE - sizeof(long);
	}


	while (i < MAX_UBOOT_RWRETRY && ret < 0) {
		i ++;
		ret = read_bootenv();
		if (ret < 0) 
		    ERROR("Cannot read %s: %d.\n", BootenvPartitionName, ret);
		if(ret < -2)
			free(env_data.image);
	}

	if (i >= MAX_UBOOT_RWRETRY) {
		ERROR("read %s failed \n", BootenvPartitionName);
		return  -2;
	}
	char prefix[PROP_VALUE_MAX]={0};
    property_get("ro.ubootenv.varible.prefix", prefix);
    if (prefix[0]==0) {
		strcpy(prefix , "ubootenv.var");
		INFO("property_set ro.ubootenv.varible.prefix is: %s\n", prefix);
		property_set("ro.ubootenv.varible.prefix",  prefix);
    }
    
	
    if (strlen(prefix) > 16) {
		NOTICE("Cannot r/w ubootenv varibles - prefix length > 16.\n");
		return -4;
	}

	sprintf(PROFIX_UBOOTENV_VAR, "%s.", prefix);
	INFO("ubootenv varible prefix is: %s\n", prefix);
	
	property_list(init_bootenv_prop, (void*)&count);
	char bootcmd[32];
    char val[PROP_VALUE_MAX]={0};
	sprintf(bootcmd, "%s.bootcmd", prefix);
	property_get(bootcmd, val);
	if(val[0] == 0) {
		const char* value = bootenv_get_value("bootcmd");
		INFO("value: %s\n", value);
		if (value) {
			property_set(bootcmd, value);
			count ++;
		}
	}
	
	INFO("Get %d varibles from %s succeed!\n", count, BootenvPartitionName);	
	return 0;
}

int update_bootenv_varible(const char* name, const char* value)
{
	INFO("update_bootenv_varible name [%s]  value [%s] \n",name,value);
	const char* varible_name = 0;
	if (strcmp(name, "ubootenv.var.bootcmd") == 0) {
		varible_name = "bootcmd";
	}else {
		if (!is_bootenv_varible(name)) {
			//should assert here. 
			ERROR("%s is not a ubootenv varible.\n", name);
			return -2;
		}
		varible_name = name + strlen(PROFIX_UBOOTENV_VAR);
	}

	const char *varible_value = bootenv_get_value(varible_name);
	if (!varible_value)
		varible_value = "";
		
	if (!strcmp(value, varible_value)) 
		return 0;

	bootenv_set_value(varible_name, value, 1);
	
 	int i = 0;
	int ret = -1;
	while (i < MAX_UBOOT_RWRETRY && ret < 0) {
		i ++;
		ret = save_bootenv();
		if (ret < 0) 
		    ERROR("Cannot write %s: %d.\n", BootenvPartitionName, ret);
	}

	if (i < MAX_UBOOT_RWRETRY) {
		INFO("Save ubootenv to %s succeed!\n",  BootenvPartitionName);
	}

	#if BOOT_ARGS_CHECK
	NOTICE( " E03LOG ----update_bootenv_varible %s = %s \n" , name , value );
	if (strcmp(name, ARGS_ETHADDR_WHOLE) == 0 ||
   	     strcmp(name, ARGS_LICENCE_WHOLE) == 0 ||
   	     strcmp(name, ARGS_DEVICE_ID_HOLE) == 0 )
	{
		char * p_ethaddr = bootenv_get_value(ARGS_ETHADDR) ;
		char * p_licence = bootenv_get_value(ARGS_LICENCE);
		char * p_device_id = bootenv_get_value(ARGS_DEVICE_ID) ;
		NOTICE( " E03LOG ----update_bootenv_varible write_args_to_file   \n");
		if( NULL != p_ethaddr && NULL != p_licence && NULL != p_device_id )
			write_args_to_file();
	}
	#endif
	return ret;
}
