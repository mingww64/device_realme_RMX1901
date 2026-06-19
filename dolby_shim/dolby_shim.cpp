#include <dlfcn.h>
#include <string.h>
#include <log/log.h>
#include <hardware/audio_effect.h>

#define LIBSWDAP_PATH "libswdap.so"
#define LIBSWDAP_SYM  "AELI"

static const effect_uuid_t kUuidDap = {
    0x9d4921da, 0x8225, 0x4f29, 0xaefa,
    {0x39, 0x53, 0x7a, 0x04, 0xbc, 0xaa}
};

static const effect_uuid_t kUuidType = {
    0xec7178ec, 0xe5e1, 0x4432, 0xa3f4,
    {0x46, 0x57, 0xe6, 0x79, 0x52, 0x10}
};

static void *gLibSwDap = NULL;
static audio_effect_library_t *gSwDapLib = NULL;

static bool uuid_equal(const effect_uuid_t *a, const effect_uuid_t *b) {
    return memcmp(a, b, sizeof(effect_uuid_t)) == 0;
}

static int ensure_lib_loaded() {
    if (gSwDapLib != NULL) return 0;

    gLibSwDap = dlopen(LIBSWDAP_PATH, RTLD_NOW);
    if (gLibSwDap == NULL) {
        ALOGE("dolby_shim: failed to dlopen %s: %s", LIBSWDAP_PATH, dlerror());
        return -ENOENT;
    }

    gSwDapLib = (audio_effect_library_t *)dlsym(gLibSwDap, LIBSWDAP_SYM);
    if (gSwDapLib == NULL) {
        ALOGE("dolby_shim: no %s in %s", LIBSWDAP_SYM, LIBSWDAP_PATH);
        dlclose(gLibSwDap);
        gLibSwDap = NULL;
        return -ENOENT;
    }

    ALOGI("dolby_shim: loaded %s", LIBSWDAP_PATH);
    return 0;
}

static int shim_create_effect(const effect_uuid_t *uuid, int32_t sessionId,
        int32_t ioId, effect_handle_t *pHandle) {
    if (uuid_equal(uuid, &kUuidDap)) {
        int ret = ensure_lib_loaded();
        if (ret != 0) return ret;
        ALOGV("dolby_shim: create_effect delegating to swdap");
        return gSwDapLib->create_effect(uuid, sessionId, ioId, pHandle);
    }
    ALOGW("dolby_shim: create_effect unsupported UUID");
    return -ENOENT;
}

static int shim_release_effect(effect_handle_t handle) {
    return gSwDapLib ? gSwDapLib->release_effect(handle) : -ENOENT;
}

static int shim_get_descriptor(const effect_uuid_t *uuid,
        effect_descriptor_t *pDescriptor) {
    if (uuid_equal(uuid, &kUuidDap)) {
        int ret = ensure_lib_loaded();
        if (ret != 0) return ret;

        ret = gSwDapLib->get_descriptor(uuid, pDescriptor);
        if (ret != 0) {
            ALOGE("dolby_shim: get_descriptor failed: %d", ret);
            return ret;
        }

        pDescriptor->type = kUuidType;
        pDescriptor->uuid = kUuidDap;

        ALOGI("dolby_shim: get_descriptor patched type+uuid for DAP");
        return 0;
    }
    ALOGW("dolby_shim: get_descriptor unsupported UUID");
    return -ENOENT;
}

audio_effect_library_t AELI = {
    .create_effect = shim_create_effect,
    .release_effect = shim_release_effect,
    .get_descriptor = shim_get_descriptor,
};
