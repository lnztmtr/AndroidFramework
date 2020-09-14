
#ifndef __ADEC_OMXDDPDEC_BRIGE_H__
#define __ADEC_OMXDDPDEC_BRIGE_H__

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/ioctl.h>
#include <dlfcn.h>

#include <audio-dec.h>
#include <adec-pts-mgt.h>
#include <adec_write.h>

enum CodecID {
    CODEC_ID_NONE,
    /* AMR */
    CODEC_ID_AMR_NB= 0x12000,
    CODEC_ID_AMR_WB,

    /* RealAudio codecs*/
    CODEC_ID_RA_144= 0x13000,
    CODEC_ID_RA_288,

    /* various DPCM codecs */
    CODEC_ID_ROQ_DPCM= 0x14000,
    CODEC_ID_INTERPLAY_DPCM,
    CODEC_ID_XAN_DPCM,
    CODEC_ID_SOL_DPCM,

    /* audio codecs */
    CODEC_ID_MP2= 0x15000,
    CODEC_ID_MP3, ///< preferred ID for decoding MPEG audio layer 1, 2 or 3
    CODEC_ID_AAC,
    CODEC_ID_AC3,
    CODEC_ID_DTS,
    CODEC_ID_VORBIS,
    CODEC_ID_DVAUDIO,
    CODEC_ID_WMAV1,
    CODEC_ID_WMAV2,
    CODEC_ID_MACE3,
    CODEC_ID_MACE6,
    CODEC_ID_VMDAUDIO,
    CODEC_ID_FLAC,
    CODEC_ID_MP3ADU,
    CODEC_ID_MP3ON4,
    CODEC_ID_SHORTEN,
    CODEC_ID_ALAC,
    CODEC_ID_WESTWOOD_SND1,
    CODEC_ID_GSM, ///< as in Berlin toast format
    CODEC_ID_QDM2,
    CODEC_ID_COOK,
    CODEC_ID_TRUESPEECH,
    CODEC_ID_TTA,
    CODEC_ID_SMACKAUDIO,
    CODEC_ID_QCELP,
    CODEC_ID_WAVPACK,
    CODEC_ID_DSICINAUDIO,
    CODEC_ID_IMC,
    CODEC_ID_MUSEPACK7,
    CODEC_ID_MLP,
    CODEC_ID_GSM_MS, /* as found in WAV */
    CODEC_ID_ATRAC3,
    CODEC_ID_VOXWARE,
    CODEC_ID_APE,
    CODEC_ID_NELLYMOSER,
    CODEC_ID_MUSEPACK8,
    CODEC_ID_SPEEX,
    CODEC_ID_WMAVOICE,
    CODEC_ID_WMAPRO,
    CODEC_ID_WMALOSSLESS,
    CODEC_ID_ATRAC3P,
    CODEC_ID_EAC3,
    CODEC_ID_SIPR,
    CODEC_ID_MP1,
    CODEC_ID_TWINVQ,
    CODEC_ID_TRUEHD,
    CODEC_ID_MP4ALS,
    CODEC_ID_ATRAC1,
    CODEC_ID_BINKAUDIO_RDFT,
    CODEC_ID_BINKAUDIO_DCT,
    CODEC_ID_AAC_LATM,
    CODEC_ID_QDMC,
    CODEC_ID_CELT,


    /* other specific kind of codecs (generally used for attachments) */
    CODEC_ID_TTF= 0x18000,

    CODEC_ID_PROBE= 0x19000, ///< codec_id is not known (like CODEC_ID_NONE) but lavf should attempt to identify it

    CODEC_ID_MPEG2TS= 0x20000, /**< _FAKE_ codec to indicate a raw MPEG-2 TS
                                * stream (only used by libavformat) */
    CODEC_ID_FFMETADATA=0x21000,   ///< Dummy codec for streams containing only metadata information.
};


enum OMX_CodecID {
    OMX_ENABLE_CODEC_NULL = 0,
    OMX_ENABLE_CODEC_AC3,
    OMX_ENABLE_CODEC_EAC3,
    OMX_ENABLE_CODEC_AMR_NB,
    OMX_ENABLE_CODEC_AMR_WB,
    OMX_ENABLE_CODEC_MPEG,
    OMX_ENABLE_CODEC_MPEG_LAYER_I,
    OMX_ENABLE_CODEC_MPEG_LAYER_II,
    OMX_ENABLE_CODEC_AAC,
    OMX_ENABLE_CODEC_QCELP,
    OMX_ENABLE_CODEC_VORBIS,
    OMX_ENABLE_CODEC_G711_ALAW,
    OMX_ENABLE_CODEC_G711_MLAW,
    OMX_ENABLE_CODEC_RAW,
    OMX_ENABLE_CODEC_ADPCM_IMA,
    OMX_ENABLE_CODEC_ADPCM_MS,
    OMX_ENABLE_CODEC_FLAC,
    OMX_ENABLE_CODEC_AAC_ADTS,
    OMX_ENABLE_CODEC_ALAC,
    OMX_ENABLE_CODEC_AAC_ADIF,
    OMX_ENABLE_CODEC_AAC_LATM,
    OMX_ENABLE_CODEC_ADTS_PROFILE,
    OMX_ENABLE_CODEC_WMA,
    OMX_ENABLE_CODEC_WMAPRO,
    OMX_ENABLE_CONTAINER_WAV,
    OMX_ENABLE_CONTAINER_AIFF,
    OMX_ENABLE_CODEC_DTSHD,
    OMX_ENABLE_CODEC_TRUEHD,
    OMX_ENABLE_CODEC_WMAVOI,
};


void stop_decode_thread_omx(aml_audio_dec_t *audec);

#endif

