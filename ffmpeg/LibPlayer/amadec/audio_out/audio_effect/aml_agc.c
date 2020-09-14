/*
 * Copyright (C) 2010 Amlogic Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "effect_agc"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <dlfcn.h>

#include <cutils/log.h>
#include "aml_agc.h"

//-----------------------------AGC----------------------------------------------
AmlAGC *aml_AGC = NULL;

int aml_agc_init(float peak_level, float dynamic_theshold, float noise_threshold, int response_time) {
    aml_agc_release();
    if (aml_AGC == NULL) {
        aml_AGC = NewAmlAGC(peak_level, dynamic_theshold, noise_threshold, response_time);
        ALOGI("aml_agc_init! peak_level = %fdB, dynamic_theshold = %fdB, noise_threshold = %fdB\n",
            peak_level,dynamic_theshold,noise_threshold);
    } else
        ALOGE("%s, AGC is exist\n", __FUNCTION__);
    return 0;
}

/*only support int16_t data format*/
int aml_agc_process(void *buffer, int samples) {
    DoAmlAGC(aml_AGC, buffer, samples);
    return 0;
}

void aml_agc_release(void) {
    aml_agc_enable(0);
    DeleteAmlAGC(aml_AGC);
    aml_AGC = NULL;
    ALOGI("aml_agc_release!\n");
    return;
}

void aml_agc_enable(int enable) {
    if (aml_AGC == NULL)
        return;
    aml_AGC->agc_enable = enable;
    ALOGI("agc enable: %d\n", enable);
    return;
}