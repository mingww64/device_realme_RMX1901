#
# Copyright (C) 2021-2024 The AOSP Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit some common Axion stuff
TARGET_BOOT_ANIMATION_RES := 1080
TARGET_HAS_UDFPS := true
TARGET_DISABLE_EPPE := true

# AxionOS-Specific Flags
AXION_MAINTAINER := Shravan_SK
AXION_PROCESSOR := Qualcomm_Snapdragon_710

# Define rear camera specs
AXION_CAMERA_REAR_INFO := 48,5
AXION_CAMERA_FRONT_INFO := 16

# CPU
AXION_CPU_SMALL_CORES := 0,1,2,3,4,5
AXION_CPU_BIG_CORES := 6,7
AXION_CPU_BG := 0-2
AXION_CPU_FG := 0-7
AXION_CPU_LIMIT_BG := 0-1
AXION_CPU_UNLIMIT_UI := 0-7
AXION_CPU_LIMIT_UI := 0-5
AXION_CPU_DISPLAY := 6-7
AXION_CPU_AUDIO := 0-3

# Blur
TARGET_ENABLE_BLUR := true

$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit from RMX1901 device
$(call inherit-product, $(LOCAL_PATH)/device.mk)

PRODUCT_BRAND := realme
PRODUCT_DEVICE := RMX1901
PRODUCT_MANUFACTURER := realme
PRODUCT_NAME := lineage_RMX1901
PRODUCT_MODEL := RMX1901

PRODUCT_GMS_CLIENTID_BASE := android-oppo

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="RMX1901-user 11 RKQ1.201217.002 1626947099367 release-keys" \
    BuildFingerprint=Realme/RMX1901/RMX1901:11/RKQ1.201217.002/1626947099367:user/release-keys \
    DeviceName=RMX1901 \
    DeviceProduct=RMX1901 \
    SystemDevice=RMX1901 \
    SystemName=RMX1901
