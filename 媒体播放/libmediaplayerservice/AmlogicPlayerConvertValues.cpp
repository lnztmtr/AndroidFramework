/*
 * Copyright (C) 2009 The Android Open Source Project
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
 
#include "AmlogicPlayerConvertValues.h"
#include <cutils/properties.h>


namespace android {

AmlPlayerConvertValues::AmlPlayerConvertValues()
{
    mSourceConv = NULL;
    mTargetConv = NULL;
}
    
AmlPlayerConvertValues:: ~AmlPlayerConvertValues()
{
    DestroyConv();
}
    
status_t AmlPlayerConvertValues::InitConv(const char *SrcCoding, const char *DstCoding)
{
    UErrorCode status = U_ZERO_ERROR;
    if (SrcCoding != NULL){
        mSourceConv = ucnv_open(SrcCoding, &status);
        if (U_FAILURE(status)) {
            ALOGD("could not create UConverter for %s\n", SrcCoding);
            return -1;
        }
    }
        
    if (DstCoding != NULL){
        mTargetConv = ucnv_open(DstCoding, &status);
        if (U_FAILURE(status)) {
            ALOGD("could not create UConverter for UTF-8\n");
            ucnv_close(mSourceConv);
            return -1;
        }
    }
    return 0;
}
    
status_t AmlPlayerConvertValues::DestroyConv()
{
    if (mSourceConv != NULL)
        ucnv_close(mSourceConv);

    if (mTargetConv != NULL)
        ucnv_close(mTargetConv);
    
    return 0;
}

bool AmlPlayerConvertValues::nonAscii(const char *value)
{
    //skip 0xFF byte
    if (value == NULL){
        return false;
    }
    if (*value == 0xFF) {
        value++;
    }
    bool nAscii = false;
    const char* chp = value;
    char ch;
    while ((ch = *chp++)) {
        if (ch & 0x80) {
            nAscii = true;
            break;
        }
    }
    return nAscii;
}

status_t AmlPlayerConvertValues::ConvertValues(const char *value, char* buffer)
{
    UErrorCode status = U_ZERO_ERROR;
    if ((mTargetConv == NULL) 
        || (mSourceConv== NULL) 
        || value == NULL 
        || buffer == NULL) {
        return -1;
    }
    // first we need to untangle the utf8 and convert it back valueto the original bytes
    // since we are reducing the length of the string, we can do this in place
    uint8_t* src = (uint8_t *)value;
    int len = strlen((char *)src);
    uint8_t* dest = src;

    uint8_t uch;
    while ((uch = *src++)) {
        if (uch & 0x80)
            *dest++ = ((uch << 6) & 0xC0) | (*src++ & 0x3F);
        else
            *dest++ = uch;
    }
    *dest = 0;

    // now convert from native encoding to UTF-8
    const char* source = value;
    int targetLength = len * 3 + 1;
    // don't normally check for NULL, but in this case targetLength may be large
    char* target = buffer;
     
    ucnv_convertEx(mTargetConv, mSourceConv, &target, target + targetLength,
                    &source, (const char *)dest, NULL, NULL, NULL, NULL, TRUE, TRUE, &status);

    if (U_FAILURE(status)) {
        ALOGE("ucnv_convertEx failed: %d", status);
        return -1;
    }
    
    ALOGD("ucnv_convertEx success: %s\n", buffer);
    return 0;
}
    
uint32_t AmlPlayerConvertValues::PossibleEncodings(const char *value)
{
    uint32_t result = kEncodingAll;
    if (value == NULL) {
        return kEncodingAll;
    }
    // if s contains a native encoding, then it was mistakenly encoded in utf8 as if it were latin-1
    // so we need to reverse the latin-1 -> utf8 conversion to get the native chars back
    uint8_t ch1, ch2;
    uint8_t* chp = (uint8_t *)value;

    while ((ch1 = *chp++)) {
        if (ch1 & 0x80) {
            ch2 = *chp++;
            ch1 = ((ch1 << 6) & 0xC0) | (ch2 & 0x3F);
            // ch1 is now the first byte of the potential native char

            ch2 = *chp++;
            if (ch2 & 0x80)
                ch2 = ((ch2 << 6) & 0xC0) | (*chp++ & 0x3F);
            // ch2 is now the second byte of the potential native char
            int ch = (int)ch1 << 8 | (int)ch2;
            result &= findPossibleEncodings(ch);
        }

    }
    return result;
}

static uint32_t getLocalEncoding()
{
    uint32_t localeEncoding = kEncodingNone;

    char propLang[PROPERTY_VALUE_MAX], propRegn[PROPERTY_VALUE_MAX];

    property_get("persist.sys.language", propLang, "na");
    property_get("persist.sys.country", propRegn, "na");

    ALOGV("propLang: %s, propRegn: %s", propLang, propRegn);

    if (!strncmp(propLang, "ja", 2)) {
        localeEncoding = kEncodingShiftJIS;
    } else if (!strncmp(propLang, "ko", 2)) {
        localeEncoding = kEncodingEUCKR;
    } else if (!strncmp(propLang, "zh", 2)) {
        if (!strncmp(propRegn, "CN", 2)) {
            localeEncoding = kEncodingGBK;
        } else {
            localeEncoding = kEncodingBig5;
        }
    }
    return localeEncoding;
}

const char* AmlPlayerConvertValues::GetEncodeValue(uint32_t encoding)
{
    const char* enc = NULL;
    switch (encoding) {
        case kEncodingShiftJIS:
            enc = "shift-jis";
            break;
        case kEncodingGBK:
            enc = "gbk";
            break;
        case kEncodingBig5:
            enc = "Big5";
            break;
        case kEncodingEUCKR:
            enc = "EUC-KR";
            break;
        default:
            // need enhance, mLocaleEncoding should be set acrroding country and language
            uint32_t mLocaleEncoding = getLocalEncoding();
            if (encoding > 0) {
                if (mLocaleEncoding == kEncodingNone) {
                    if (encoding & 0x2) {
                        enc = "gbk";
                    }
                    else if (encoding & 0x4) {
                        enc = "Big5";
                    }
                    else if(encoding & 0x8) {
                        enc = "EUC-KR";
                    }
                    else if(encoding & 0x1) {
                        enc = "shift-jis";
                    }
                } else if (mLocaleEncoding == kEncodingGBK) {
                    if (encoding & 0x2) {
                        enc = "gbk";
                    }
                    else if (encoding & 0x4) {
                        enc = "Big5";
                    }
                    else if(encoding & 0x8) {
                        enc = "EUC-KR";
                    }
                    else if(encoding & 0x1) {
                        enc = "shift-jis";
                    }
                } else if (mLocaleEncoding == kEncodingBig5) {
                    if(encoding & 0x4){
                        enc = "Big5";
                    }
                    else if(encoding & 0x2){
                        enc = "gbk";
                    }
                    else if(encoding & 0x8){
                        enc = "EUC-KR";
                    }
                    else if(encoding & 0x1){
                        enc = "shift-jis";
                    }
                } else if (mLocaleEncoding == kEncodingEUCKR) {
                    if(encoding & 0x8){
                        enc = "EUC-KR";
                    }
                    else if(encoding & 0x2){
                       enc = "gbk";
                    }
                   else if(encoding & 0x4){
                       enc = "Big5";
                    }
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }
                } else if(mLocaleEncoding == kEncodingShiftJIS) {
                    if(encoding & 0x1){
                        enc = "shift-jis";
                    }
                    else if(encoding & 0x8){
                        enc = "EUC-KR";
                    }
                    else if(encoding & 0x2){
                        enc = "gbk";
                    }
                    else if(encoding & 0x4){
                         enc = "Big5";
                    }
                }
            } 
        }
        if (enc == NULL) {
            enc = "UTF-8";
        }
        return enc;
    }

}  // namespace android
