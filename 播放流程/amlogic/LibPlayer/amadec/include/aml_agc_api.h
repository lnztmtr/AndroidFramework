#ifndef _AMLAGCAPI_H_
#define _AMLAGCAPI_H_

#ifdef __cplusplus
extern "C"  {
#endif
int aml_agc_init(float peak_level, float dynamic_theshold, float noise_threshold, int response_time);
int aml_agc_process(void *buffer, int samples);
void aml_agc_release(void);
void aml_agc_enable(int enable);
#ifdef __cplusplus
}
#endif

#endif