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

#ifndef STREAM_SNIFFER_H_
#define STREAM_SNIFFER_H_

#include <sys/types.h>
#include <utils/Errors.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/foundation/ABitReader.h>

namespace android {

class StreamSniffer
{
public:
    StreamSniffer(const char * url);
    ~StreamSniffer();

    enum STREAM_TYPE {
        STREAM_UNKNOWN,
        STREAM_HLS,
        // other types, add here
    };

    size_t sniffStreamType(size_t sniffsize = 0);

private:

    status_t tryHLSParser(ABitReader * br);
    int32_t isBOMHeader(ABitReader * br);

    AString mURL;
};

}

#endif