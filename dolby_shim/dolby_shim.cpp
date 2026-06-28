#include <log/log.h>
#include <hardware/audio_effect.h>
#include <string.h>
#include <stdlib.h>

static const effect_uuid_t kUuidDap = {
    0x9d4921da, 0x8225, 0x4f29, 0xaefa,
    {0x39, 0x53, 0x7a, 0x04, 0xbc, 0xaa}
};

static const effect_uuid_t kUuidPatchedType = {
    0xec7178ec, 0xe5e1, 0x4432, 0xa3f4,
    {0x46, 0x57, 0xe6, 0x79, 0x52, 0x10}
};

static int32_t effect_process(effect_handle_t self,
        audio_buffer_t *inBuffer, audio_buffer_t *outBuffer) {
    (void)self; (void)inBuffer; (void)outBuffer;
    return 0;
}

static int32_t effect_command(effect_handle_t self,
        uint32_t cmdCode, uint32_t cmdSize,
        void *pCmdData, uint32_t *replySize, void *pReplyData) {
    (void)self; (void)cmdSize; (void)pCmdData; (void)pReplyData;
    switch (cmdCode) {
    case EFFECT_CMD_INIT:
    case EFFECT_CMD_SET_CONFIG:
    case EFFECT_CMD_RESET:
    case EFFECT_CMD_ENABLE:
    case EFFECT_CMD_DISABLE:
    case EFFECT_CMD_SET_PARAM:
        if (replySize != nullptr) {
            *replySize = sizeof(int32_t);
        }
        if (pReplyData != nullptr) {
            *(int32_t *)pReplyData = 0;
        }
        return 0;
    case EFFECT_CMD_GET_PARAM: {
        if (pCmdData == nullptr || pReplyData == nullptr || replySize == nullptr) {
            return -EINVAL;
        }
        uint32_t copySize = (cmdSize < *replySize) ? cmdSize : *replySize;
        memcpy(pReplyData, pCmdData, copySize);
        if (*replySize >= 12) {
            int32_t *reply = (int32_t *)pReplyData;
            reply[0] = 0; // status
            uint32_t psize = reply[1];
            uint32_t vsize = reply[2];
            uint32_t voffset = 12 + ((psize + 3) & ~3);
            if (voffset + vsize <= *replySize) {
                memset((char *)pReplyData + voffset, 0, vsize);
            }
        }
        return 0;
    }
    case EFFECT_CMD_SET_DEVICE:
    case EFFECT_CMD_SET_VOLUME:
        return 0;
    default:
        return 0;
    }
}

static int32_t shim_get_descriptor(const effect_uuid_t *uuid,
        effect_descriptor_t *pDesc);

static int32_t effect_get_descriptor(effect_handle_t self, effect_descriptor_t *pDescriptor) {
    (void)self;
    return shim_get_descriptor(&kUuidDap, pDescriptor);
}

static struct effect_interface_s gEffectIf = {
    .process = effect_process,
    .command = effect_command,
    .get_descriptor = effect_get_descriptor,
    .process_reverse = nullptr
};
static struct effect_interface_s *gEffectIfPtr = &gEffectIf;

static bool uuid_equal(const effect_uuid_t *a, const effect_uuid_t *b) {
    return memcmp(a, b, sizeof(effect_uuid_t)) == 0;
}

static int32_t shim_create_effect(const effect_uuid_t *uuid,
        int32_t sessionId, int32_t ioId, effect_handle_t *pHandle) {
    (void)sessionId; (void)ioId;
    if (!uuid_equal(uuid, &kUuidDap)) return -ENOENT;
    *pHandle = &gEffectIfPtr;
    ALOGI("dolby_shim: create_effect OK for DAP");
    return 0;
}

static int32_t shim_release_effect(effect_handle_t handle) {
    (void)handle;
    return 0;
}

static int32_t shim_get_descriptor(const effect_uuid_t *uuid,
        effect_descriptor_t *pDesc) {
    if (uuid == nullptr || pDesc == nullptr) {
        return -EINVAL;
    }
    if (!uuid_equal(uuid, &kUuidDap)) return -ENOENT;

    memset(pDesc, 0, sizeof(effect_descriptor_t));
    pDesc->type = kUuidPatchedType;
    pDesc->uuid = kUuidDap;
    pDesc->apiVersion = 0x00020000;
    pDesc->flags = 0x00040288;
    pDesc->cpuLoad = 0;
    pDesc->memoryUsage = 0;
    memcpy(pDesc->name, "DAP", 3);
    memcpy(pDesc->implementor, "Dolby Laboratories", 18);

    ALOGI("dolby_shim: get_descriptor OK for DAP (patched type)");
    return 0;
}

extern "C" {
    __attribute__((visibility("default")))
    audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO = {
        .tag = AUDIO_EFFECT_LIBRARY_TAG,
        .version = EFFECT_LIBRARY_API_VERSION,
        .name = "Effect DAP Library",
        .implementor = "Dolby Laboratories",
        .create_effect = shim_create_effect,
        .release_effect = shim_release_effect,
        .get_descriptor = shim_get_descriptor,
    };

    __attribute__((visibility("default")))
    audio_effect_library_t AELI = {
        .tag = AUDIO_EFFECT_LIBRARY_TAG,
        .version = EFFECT_LIBRARY_API_VERSION,
        .name = "Effect DAP Library",
        .implementor = "Dolby Laboratories",
        .create_effect = shim_create_effect,
        .release_effect = shim_release_effect,
        .get_descriptor = shim_get_descriptor,
    };
}
