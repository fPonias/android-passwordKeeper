MYPATH := /Users/codymunger/programming/passwordKeeper-android/jni

include $(CLEAR_VARS)

LOCAL_PATH := $(MYPATH)
LOCAL_MODULE := aes256
LOCAL_SRC_FILES := aes256.c MD5.c com_munger_passwordkeeper_util_aes256.c
LOCAL_STATIC_LIBRARIES := 
LOCAL_LDLIBS := -L. -llog -lc
LOCAL_C_INCLUDES := .
LOCAL_CFLAGS := 

include $(BUILD_SHARED_LIBRARY)


LOCAL_PATH := $(call my-dir)

$(call import-module,cpufeatures)

