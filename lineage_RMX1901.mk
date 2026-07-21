#
# Copyright (C) 2021-2024 The AOSP Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit some common Evolution stuff
TARGET_BOOT_ANIMATION_RES := 1080
# WITH_GMS := false
TARGET_USES_MINI_GAPPS := true
TARGET_HAS_UDFPS := true

BYPASS_CHARGE_SUPPORTED := true
TARGET_INCLUDE_VIPERFX := true

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