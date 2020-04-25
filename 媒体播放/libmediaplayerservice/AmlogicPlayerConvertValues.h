/**
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

#ifndef ANDROID_AMLPLAYERCONVERTVALUES_H
#define ANDROID_AMLPLAYERCONVERTVALUES_H

#include <media/mediascanner.h>

#include "autodetect.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"

extern "C" {
#include "libavutil/avstring.h"
#include "libavformat/avformat.h"
}


namespace android
{

class AmlPlayerConvertValues
{
public:
    AmlPlayerConvertValues();
    virtual ~AmlPlayerConvertValues();
    status_t InitConv(const char *SrcCoding, const char *DstCoding);
    status_t DestroyConv(void);
    status_t ConvertValues(const char *value, char* buffer);
    uint32_t PossibleEncodings(const char *value);
    const char* GetEncodeValue(uint32_t encoding);
    bool nonAscii(const char *value);
    
private:
    UConverter *mSourceConv;
    UConverter *mTargetConv;

};

}

#endif
