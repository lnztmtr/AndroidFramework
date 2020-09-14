/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef LIBPLAYER_UDRM_H
#define LIBPLAYER_UDRM_H


#ifdef __cplusplus
extern "C" {
#endif

typedef int (*udrm_notify)(int error_num, void *cb_data);


int udrm_init();
void udrm_deinit();
void udrm_set_msg_func(udrm_notify notify_cb, void *cb_data);
int udrm_decrypt_start(unsigned int program_num);
int udrm_decrypt_stop(int handle);
int udrm_ts_decrypt(int handle, char *ts_in, unsigned int len_in, char *ts_out, unsigned int len_out);
int udrm_mp4_set_pssh(int handle, unsigned char *pssh, unsigned int len);
int udrm_mp4_decrypt(int handle, char *iv_value, char *enc_data, unsigned int len, char *out_data);


#ifdef __cplusplus
}
#endif


#endif

