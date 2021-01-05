#ifndef _RS_FEC_H_20100610
#define _RS_FEC_H_20100610

#define SWAP(a,b) {int temp=(a);(a)=(b);(b)=temp;}

#ifndef BYTE
#define BYTE   unsigned char
#endif
#ifndef XWORD
#define XWORD   unsigned int
#endif
#ifndef PBYTE
#define PBYTE   unsigned char*
#endif
#ifndef PPBYTE
#define PPBYTE   unsigned char**
#endif

typedef struct _t_RS_FEC_monde 
{
    int k, m ;      /* parameters of the code */
    PBYTE *en_GM ;
    int sz;
}T_RS_FEC_MONDE;


void init_RS_fec(void);
T_RS_FEC_MONDE * RS_fec_new(int data_pkt_num, int fec_pkt_num);
void RS_fec_free(T_RS_FEC_MONDE *p);
int fec_encode(T_RS_FEC_MONDE *code,PBYTE *src, PBYTE *fec_data, int data_len) ;
int fec_decode(T_RS_FEC_MONDE *code, PBYTE *data, PBYTE *fec_data, int lost_map[], int data_len);

#endif

