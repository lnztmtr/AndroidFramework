#ifndef _AMLAGC_H_
#define _AMLAGC_H_

#ifdef __cplusplus
extern "C"  {
#endif

typedef struct {
    int         agc_enable;
    int         response_time;
    int         sample_max[2];
    int         counter[2];
    float       gain[2];
    float       CompressionRatio;
    int         silence_counter[2];
    long        peak;
    long        silence_threshold;
    long        active_threshold;
    float       sample_sum[2];
    long        last_sample[2];
    float       average_level[2];
    float       cross_zero_num[2];
}AmlAGC;

AmlAGC* NewAmlAGC(float peak_level, float dynamic_theshold, float noise_threshold, int response_time);
void DoAmlAGC(AmlAGC *agc, void *buffer, int len);
void DeleteAmlAGC(AmlAGC *agc);

#ifdef __cplusplus
}
#endif

#endif
