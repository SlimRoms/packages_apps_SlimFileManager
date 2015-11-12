LOCAL_PATH :=$(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, SlimFileManager/src/main/java)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/SlimFileManager/src/main/res
LOCAL_MANIFEST_FILE := SlimFileManager/src/main/AndroidManifest.xml

ifeq ($(TARGET_BUILD_APPS),)
    LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += frameworks/support/v7/recyclerview/res
else
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
    LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/recyclerview/res
endif

LOCAL_STATIC_JAVA_LIBRARIES := android-common
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-recyclerview
LOCAL_STATIC_JAVA_LIBRARIES += juniversalchardet
LOCAL_STATIC_JAVA_LIBRARIES += apache-commons-compress
LOCAL_STATIC_JAVA_LIBRARIES += apache-commons-io
LOCAL_STATIC_JAVA_LIBRARIES += apache-commons-lang3

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.recyclerview
LOCAL_AAPT_FLAGS += --rename-manifest-package com.slim.filemanager

LOCAL_PACKAGE_NAME := SlimFileManager
LOCAL_CERTIFICATE := platform
LOCAL_SDK_VERSION := current
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    juniversalchardet:SlimFileManager/libs/juniversalchardet-1.0.3.jar \
    apache-commons-compress:SlimFileManager/libs/commons-compress-1.10/commons-compress-1.10.jar \
    apache-commons-io:SlimFileManager/libs/commons-io-2.4/commons-io-2.4.jar \
    apache-commons-lang3:SlimFileManager/libs/commons-lang3-3.4/commons-lang3-3.4.jar

include $(BUILD_MULTI_PREBUILT)
