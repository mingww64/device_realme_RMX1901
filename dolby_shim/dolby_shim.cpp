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

static int effect_process(effect_handle_t self,
        audio_buffer_t *inBuffer, audio_buffer_t *outBuffer) {
    (void)self; (void)inBuffer; (void)outBuffer;
    return 0;
}

static int effect_command(effect_handle_t self,
        uint32_t cmdCode, uint32_t cmdSize,
        void *pCmdData, uint32_t *replySize, void *pReplyData) {
    (void)self; (void)cmdSize; (void)pCmdData;
    switch (cmdCode) {
    case EFFECT_CMD_INIT:
    case EFFECT_CMD_SET_CONFIG:
    case EFFECT_CMD_RESET:
    case EFFECT_CMD_ENABLE:
    case EFFECT_CMD_DISABLE:
    case EFFECT_CMD_SET_PARAM:
        if (replySize) *replySize = sizeof(int);
        return 0;
    case EFFECT_CMD_GET_PARAM:
        if (replySize) *replySize = sizeof(int);
        return -ENOSYS;
    case EFFECT_CMD_SET_DEVICE:
    case EFFECT_CMD_SET_VOLUME:
        return 0;
    default:
        return 0;
    }
}

struct shim_effect_if {
    int (*process)(effect_handle_t, audio_buffer_t *, audio_buffer_t *);
    int (*command)(effect_handle_t, uint32_t, uint32_t, void *, uint32_t *, void *);
};

static struct shim_effect_if gEffectIf = { effect_process, effect_command };
static struct shim_effect_if *gEffectIfPtr = &gEffectIf;

static bool uuid_equal(const effect_uuid_t *a, const effect_uuid_t *b) {
    return memcmp(a, b, sizeof(effect_uuid_t)) == 0;
}

static int shim_create_effect(const effect_uuid_t *uuid,
        int32_t sessionId, int32_t ioId, effect_handle_t *pHandle) {
    (void)sessionId; (void)ioId;
    if (!uuid_equal(uuid, &kUuidDap)) return -ENOENT;
    *pHandle = &gEffectIfPtr;
    ALOGI("dolby_shim: create_effect OK for DAP");
    return 0;
}

static int shim_release_effect(effect_handle_t handle) {
    (void)handle;
    return 0;
}

static int shim_get_descriptor(const effect_uuid_t *uuid,
        effect_descriptor_t *pDesc) {
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

struct aeli_s {
    uint32_t magic;
    uint32_t version;
    const char *lib_name;
    const char *vendor;
    uint8_t reserved[32];
};

static const char kAeliLibName[] = "Effect DAP Library";
static const char kAeliVendor[] = "Dolby Laboratories";

extern "C" {
    __attribute__((visibility("default")))
    audio_effect_library_t AUDIO_EFFECT_LIBRARY_INFO = {
        shim_create_effect,
        shim_release_effect,
        shim_get_descriptor,
    };

    __attribute__((visibility("default")))
    struct aeli_s AELI = {
        0x41454c54,
        0x00030000,
        kAeliLibName,
        kAeliVendor,
        {0},
    };
}
