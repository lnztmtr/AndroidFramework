#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "RS_fec.h"
#include "common.h"

const BYTE RS_MODULUS = 0x1d;				 //00011101: x^8+x^4+x^3+x^2+1=0
const XWORD RS_BOUND = 0x100;                //maximum matrix size


const static XWORD RS_SIZE = 0xFF;
const static BYTE ALPHA = 0x2;
static BYTE rs_exp[255 + 1];
static XWORD rs_log[255 + 1];

static int g_malloc_times =0;

#ifdef MEM_LEAK_DETECT
static int g_malloc_size =0;
#endif

static int RS_fec_initialized = 0 ;

static void *rs_malloc(int sz)
{
#ifdef MEM_LEAK_DETECT
	void *a = malloc(sz + sizeof(int));
	
	if(a)
	{
		int *b = (int *)a;
		b[0] = sz;
		g_malloc_times++;
		g_malloc_size += sz;
		return ((char *)a + sizeof(int));
	}
	return NULL;
#else
	void *a = malloc(sz);
	return a;
#endif

}

static void rs_free(void *p)
{
	if(p == NULL)
		return;
#ifdef MEM_LEAK_DETECT	
	int *b = (int *)((char *)p - sizeof(int));
	if(g_malloc_size >= b[0])
		g_malloc_size -= b[0];
	else
		printf("free size more than malloc size!\n");
	if(g_malloc_times >= 1)
	{
		g_malloc_times--;
	}
	else
		printf("free times more than malloc!\n");
	
	char *a = ((char *)p - sizeof(int));
	free(a);
#else
	free(p);
#endif
    	return ;
}

static void generate_G_table256(void)
{
	BYTE x = 0x1;
	XWORD y = 0x1; 
	unsigned int i;
			
	rs_exp[RS_SIZE] = 0;
			
	for (i = 0; i < RS_SIZE; ++i) 
	{
		rs_exp[i] = x;
		y <<= 1;
		if (y & 0x100)
			y ^= RS_MODULUS;
		y %= 0x100;
		x = y;
	}
	
	for (i = 0; i <= RS_SIZE; ++i) 
	{
		rs_log[rs_exp[i]] = i;
	}
	
	return;
}

void init_RS_fec(void)
{
    generate_G_table256();
    RS_fec_initialized = 1 ;
    return;
}

static int matrix_invGF256(PBYTE* matrix, const int n) 
{
	int i, j, k, l, ll;
	int irow = 0, icol = 0;
	BYTE dum, big;
	XWORD pivinv;

	int indxc[RS_BOUND], indxr[RS_BOUND], ipiv[RS_BOUND];
	if(n >= (int)RS_BOUND || n <=0) //noted zhouj 0610
	{
		//printf("matrix_invGF256: n = %d, n >= BOUND, just return!");
		return -1; 
	}

	for (j = 0; j < n; ++j) 
	{
	    indxc[j] = 0;
	    indxr[j] = 0;
	    ipiv[j] = 0;
	}
    	for (i = 0; i < n; ++i) 
	{
		big = 0;
		for (j = 0; j < n; ++j)
		{
			if (ipiv[j] != 1)
			{
				for (k = 0; k < n; ++k) 
				{
					if (ipiv[k] == 0) 
					{
						if (matrix[j][k] >= big) //noted zhouj 0610 assign the max value in matri[j] to big/irow/icol
						{ 
						    big = matrix[j][k];
						    irow = j;
						    icol = k;    
						}
					}
					else if (ipiv[k] > 1) 
					{
						//printf("matrix_invGF256: ipiv[k] > 1, k= %d, just return!", k);
						return -1;  
					}
				}
			}
		}
        ++(ipiv[icol]);
        
        
        if (irow != icol)
            for (l = 0; l < n; ++l) 
            		SWAP(matrix[irow][l],matrix[icol][l])  
        
        indxr[i] = irow;
        indxc[i] = icol;
        if (matrix[icol][icol] == 0) 
        {
        	//printf("matrix_invGF256: matrix[icol][icol] = 0, icol= %d, just return!", icol);
		return -1;
        }
        pivinv = RS_SIZE - rs_log[matrix[icol][icol]];
        matrix[icol][icol] = 0x1;
        for (l = 0; l < n; ++l) 
			if ( matrix[icol][l] )
				matrix[icol][l] = rs_exp[(rs_log[matrix[icol][l]]+pivinv)%RS_SIZE]; 
        for (ll = 0; ll < n; ++ll)
            if (ll != icol) 
		{
                dum = matrix[ll][icol];
                matrix[ll][icol] = 0;
                for (l = 0; l < n; ++l) 
                    if (matrix[icol][l] && dum)
                        matrix[ll][l] ^= rs_exp[(rs_log[matrix[icol][l]]+rs_log[dum]) % RS_SIZE];    
            }
	}
    for (l = n-1; l >= 0; --l) 
	{
		if (indxr[l] != indxc[l]) 
		    for (k = 0; k < n; ++k)
		        SWAP(matrix[k][indxr[l]], matrix[k][indxc[l]])
	}  

    return 0;
}

static void matrix_mulGF256(PBYTE *a, PBYTE *b, PBYTE *c, int left, int mid, int right) 
{
    int i, j, k;
    for (i = 0; i < left; ++i)
    	 for (j = 0; j < right; ++j)
            for (k = 0; k < mid; ++k) 
			{
				if (a[i][k] && b[k][j] ) 
				{
					c[i][j] ^= rs_exp[(rs_log[a[i][k]]+rs_log[b[k][j]]) % RS_SIZE];
				}
            }
	return;
}


T_RS_FEC_MONDE * RS_fec_new(int data_pkt_num, int fec_pkt_num) //noted zhouj 0610 data_pkt_num->max num of media packet,max parity num + media num
{
	T_RS_FEC_MONDE * rsMonde = NULL;
	PBYTE *en_left = NULL;
	PBYTE *en_right = NULL;
	BYTE *en_left_tmp = NULL;
	BYTE *en_right_tmp = NULL;
	BYTE *en_GM_tmp = NULL;
	if (RS_fec_initialized == 0)
    	{
        	init_RS_fec();
    	}
	int i, _i, j,rs = -1;

	rsMonde = (T_RS_FEC_MONDE *) rs_malloc(sizeof(T_RS_FEC_MONDE));
	if(rsMonde == NULL)
		return NULL;
	
	rsMonde->m = fec_pkt_num;
	rsMonde->k = data_pkt_num;
	
	en_left = (PBYTE *)rs_malloc(sizeof(PBYTE) * rsMonde->m);
	en_right = (PBYTE *)rs_malloc(sizeof(PBYTE) * rsMonde->k);
	en_left_tmp = (BYTE *)rs_malloc(sizeof(BYTE) * rsMonde->m * rsMonde->k);
	en_right_tmp = (BYTE *)rs_malloc(sizeof(BYTE) * rsMonde->k * rsMonde->k);
	rsMonde->en_GM = (PBYTE*)rs_malloc(sizeof(PBYTE) * rsMonde->m);
	en_GM_tmp = (BYTE *)rs_malloc(sizeof(BYTE) * rsMonde->m * rsMonde->k);

	if(en_left == NULL || NULL == en_right || NULL == en_left_tmp || NULL == en_right_tmp
		||NULL == rsMonde->en_GM || NULL == en_GM_tmp
		)
	{
		//printf("RS_fec_new:Allocate mem for decoding process fail\n");
		goto bailout;
	}
	
	for (i = 0, _i = rsMonde->k; i < rsMonde->m; ++i, ++_i) 
	{
		en_left[i] = en_left_tmp + i * rsMonde->k;
		rsMonde->en_GM[i] =  en_GM_tmp + i * rsMonde->k;
		for (j = 0; j < rsMonde->k; ++j) 
		{
			en_left[i][j] = rs_exp[(_i*j)%RS_SIZE]; 
			rsMonde->en_GM[i][j] = 0;
		}
	}

	for (i = 0; i < rsMonde->k; ++i) 
	{
		en_right[i] = en_right_tmp + i * rsMonde->k;
		for (j = 0; j < rsMonde->k; ++j)
			en_right[i][j] = rs_exp[(i*j)%RS_SIZE];
	}	

	rs = matrix_invGF256(en_right, rsMonde->k);
	if(rs != 0)
	{
		//printf("RS_fec_new:matrix_invGF256 exec fail\n");
		goto bailout;
	}
	
	matrix_mulGF256(en_left,en_right,rsMonde->en_GM, rsMonde->m,rsMonde->k,rsMonde->k);

  //rs_free(en_GM_tmp);//add by xzd
	rs_free(en_left_tmp);
	rs_free(en_right_tmp);

	 rs_free(en_left);
	 rs_free(en_right);
	 
	LOGI("[%s:%d]data=%d, fec=%d\n",__FUNCTION__,__LINE__,  data_pkt_num,fec_pkt_num);
	return rsMonde;
	
	bailout:
	if(NULL != en_GM_tmp)
		rs_free(en_GM_tmp);
	if(NULL != rsMonde->en_GM)
		rs_free(rsMonde->en_GM);
	if(NULL != en_left_tmp)
		rs_free(en_left_tmp);
	if(NULL != en_right_tmp)
		rs_free(en_right_tmp);
	if(NULL != en_left)
		rs_free(en_left);
	if(NULL != en_right)
		rs_free(en_right);
	if(NULL != rsMonde)
		rs_free(rsMonde);

		return NULL;
}

void RS_fec_free(T_RS_FEC_MONDE *p)
{
	if(NULL != p)
	{
		if(NULL != p->en_GM[0])
	    {
	    	rs_free(p->en_GM[0]);
	    	p->en_GM[0] = NULL;
	    }
	
		if(NULL != p->en_GM)
		{
		 	rs_free(p->en_GM);
		 	p->en_GM = NULL;
		}
	
	    rs_free(p);
        p = NULL;	
	    
	}
	return;
}


int fec_encode(T_RS_FEC_MONDE *code,PBYTE *src, PBYTE *fec_data, int data_len) 
{
	matrix_mulGF256(code->en_GM, src, fec_data, code->m, code->k, data_len);

	return 0;
}

int fec_decode(T_RS_FEC_MONDE *code, PBYTE *data, PBYTE *fec_data, int lost_map[], int data_len)
{
	int N = code->k + code->m;
	int S = data_len;

	int recv_count = 0;
	int tmp_count = 0;
	int i, j, r, l;
	int rs = -1;
	int lost_pkt_cnt = 0;

	int lost_pkt_id[RS_SIZE+1];  //noted zhouj 0610 set to global?
	BYTE *de_subGM_tmp =NULL;
	
	//LOGI("[%s:%d]i0=%d, i1=%d, lostaddr=%x\n",__FUNCTION__,__LINE__, lost_map[0], lost_map[1],lost_map);
	for(i = 0;i<(int)(RS_SIZE+1);i++)
	{
		lost_pkt_id[i] = 0;
	}

	PBYTE *de_subGM = (PBYTE *)rs_malloc(sizeof(PBYTE) * code->k);
	if(de_subGM == NULL)
	{
		return -1;
	}
	for(i = 0; i < code->k; ++i)
		de_subGM[i] = NULL;

	de_subGM_tmp = (BYTE *)rs_malloc(sizeof(BYTE) * code->k * code->k);
	if(de_subGM_tmp == NULL)
	{
		rs_free(de_subGM);
		return -1;
	}
	
	for (i = 0; i < code->k; ++i) 
	{
		de_subGM[i] = de_subGM_tmp + code->k * i;
		for (j = 0; j < code->k; ++j)
			de_subGM[i][j] = 0;
	}

	
	for (i = 0; i < code->k; ++i) 
	{ 
		if (lost_map[i] == 1) 
		{
			de_subGM[recv_count][i] = 1;
			++recv_count;
		}
		else if(lost_pkt_cnt < code->m)  //noted zhouj 0610 The dianxin algrithm have not protect lost_pkt_id
		{
			lost_pkt_id[lost_pkt_cnt++] = i;
			LOGI("[%s:%d]i=%d, lost_pkt_cnt\n",__FUNCTION__,__LINE__, i, lost_pkt_cnt);
		}
		else
		{
			if(de_subGM_tmp)
			{
				rs_free(de_subGM_tmp);
				de_subGM_tmp = NULL;
			}
			if(de_subGM)
			{
				rs_free(de_subGM);
				de_subGM = NULL;
			}

			LOGI("[%s:%d]decode:k = %d, M = %d, lost_pkt_id= %d, lost_pkt_cnt >= M, just return!\n",__FUNCTION__,__LINE__, code->k , code->m, lost_pkt_cnt);

			//printf("decode:k = %d, M = %d, lost_pkt_id= %d, lost_pkt_cnt >= M, just return!\n", code->k , code->m, lost_pkt_cnt);
			return -1;
		}
	}

	for (i = code->k; i < N; ++i) 
	{ 
		if (lost_map[i] == 1) 
		{
			if (recv_count < code->k) 
			{  
				for (j = 0; j < code->k; ++j)
					de_subGM[recv_count][j] = code->en_GM[i-code->k][j];
				++recv_count;
			}
			else 
				break;
		}
	}
	LOGI("[%s:%d]decode:k = %d, M = %d, lost_pkt_cnt= %d\n",__FUNCTION__,__LINE__, code->k , code->m, lost_pkt_cnt);

	rs = matrix_invGF256(de_subGM,code->k);
	if(rs == -1)
	{
		if(de_subGM)
		{
			rs_free(de_subGM);
			de_subGM = NULL;
		}
		if(de_subGM_tmp)
		{
			rs_free(de_subGM_tmp);
			de_subGM_tmp = NULL;
		}
		LOGI("[%s:%d]fec_decode:matrix_invGF256 exec failed\n",__FUNCTION__,__LINE__);
		return -1;
	}

	PBYTE *recv_data = (PBYTE *)rs_malloc(sizeof(PBYTE) * code->k);
	for (i = 0; i < N; ++i) 
	{
		if (lost_map[i] && (recv_data != NULL) )
		{
			if(i <  code->k) 
			        recv_data[tmp_count] = data[i];
			else
				recv_data[tmp_count] = fec_data[i-code->k];
			++tmp_count;
		}
		if (tmp_count == code->k)
		{
			break;
		}
	}
	

	for(i = 0; i < lost_pkt_cnt; ++i) 
	{
		int cur_lost_pkt = lost_pkt_id[i];
		memset(data[cur_lost_pkt],0,S);
		for(r = 0; r < S; ++r) 
		{
			for(l = 0; l < code->k; ++l) 
			{
				if (de_subGM[cur_lost_pkt][l] && recv_data[l][r])
					data[cur_lost_pkt][r] ^= rs_exp[(rs_log[de_subGM[cur_lost_pkt][l]]+rs_log[recv_data[l][r]]) % (RS_SIZE)];
			}
		}
	}

	if(recv_data)
	{
		rs_free(recv_data);
		recv_data = NULL;
    }
	if(de_subGM)
    {
    	rs_free(de_subGM);
    	de_subGM = NULL;
    }
	if(de_subGM_tmp)
	{
		rs_free(de_subGM_tmp);
		de_subGM_tmp = NULL;
	}
	LOGI("[%s:%d]decode:k = %d, M = %d, lost_pkt_cnt= %d, malloc times = %d----0811\n",__FUNCTION__,__LINE__, code->k , code->m, lost_pkt_cnt, g_malloc_times);
	return 0;
}



