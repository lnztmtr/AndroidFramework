#ifndef _IRISTRUECUT_H
#define _IRISTRUECUT_H

enum PWTCCodecID {
    PWTC_CODEC_ID_NONE,
    PWTC_CODEC_ID_H264,
    PWTC_CODEC_ID_HEVC,
};

enum {
    PWTC_NONE,
    PWTC_SUCCESS,
    PWTC_FAILED,
};

typedef struct PWTcPacket {
    int     codec_id;   /* PWTC_CODEC_ID_H264 or PWTC_CODEC_ID_HEVC */
    uint8_t *pBuffer;   /* data buffer */
    int32_t nSize;      /* size of data in bytes */
    int64_t nTimeStamp; /* time stamp */
} PWTcPacket;

typedef void (*iristclog_callback) (const char *fmt, va_list vl);

int irisTcProcess(PWTcPacket *pkt);
int irisTcSdkVersion(void);
void irisTcLogCallbackSet(iristclog_callback log_callback);
int isIrisTcStream(PWTcPacket *pkt);  /* 0: no PW-TC stream, 1: PW-TC stream */

#endif // _IRISTRUECUT_H