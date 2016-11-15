/**
 * Copyright 2014 Cody Munger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "jni.h"
#include "aes256.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "com_munger_passwordkeeper_struct_AES256.h"

JNIEXPORT jint JNICALL Java_com_munger_passwordkeeper_struct_AES256_init(JNIEnv * env, jobject jthis, jstring jpass)
{
	const char* password = (*env)->GetStringUTFChars(env, jpass, 0);
	aes256_context* ret = malloc(sizeof(aes256_context));
	aes256_initFromPassword(ret, password);

    setupDecodeCallback(env, &jthis);

	return (long) ret;
}

JNIEXPORT void JNICALL Java_com_munger_passwordkeeper_struct_AES256_destroy(JNIEnv * env, jobject jthis, jint jcontext)
{
	aes256_context* ctx = (aes256_context*) jcontext;
	free(ctx);
    cleanupDecodeCallback(env);
}

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_encode(JNIEnv * env, jobject jthis, jint jcontext, jstring jtarget)
{
	aes256_context* ctx = (aes256_context*) jcontext;
	const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    unsigned int sz = strlen(targetPtr);

    unsigned int retSz = sz / 16;
    if (sz % 16 != 0)
        retSz += 1;
    retSz *= 16;

    char retPtr[retSz];
    char* retPtr2 = &(retPtr[0]);
    aes256_encryptString(ctx, targetPtr, retPtr2);
    char retPtrEnc[retSz * 2 + 1];

    int i;
    for (i = 0; i < retSz; i++)
    {
        unsigned char ch = retPtr2[i];
        sprintf(&(retPtrEnc[i * 2]), "%02x", ch);
    }

    retPtrEnc[retSz * 2] = '\0';
    jstring ret = (*env)->NewStringUTF(env, retPtrEnc);

	return ret;
}

JNIEXPORT jbyteArray JNICALL Java_com_munger_passwordkeeper_struct_AES256_encodeToBytes (JNIEnv * env, jobject jthis, jint jcontext, jstring jtarget)
{
    aes256_context* ctx = (aes256_context*) jcontext;
    const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    unsigned int sz = strlen(targetPtr);

    unsigned int retSz = sz / 16;
    if (sz % 16 != 0)
        retSz += 1;
    retSz *= 16;

    char retPtr[retSz];
    char* retPtr2 = &(retPtr[0]);
    aes256_encryptString(ctx, targetPtr, retPtr2);

    jbyteArray ret = (*env)->NewByteArray(env, retSz);
    (*env)->SetByteArrayRegion(env, ret, 0, retSz, retPtr2);
    return ret;
}

JNIEnv* decodeCallbackEnv = 0;
jobject* decodeCallbackObject = 0;
jmethodID decodeCallbackMethodID = 0;

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_decode(JNIEnv * env, jobject jthis, jint jcontext, jstring jtarget)
{
	aes256_context* ctx = (aes256_context*) jcontext;
	const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);
	unsigned int sz = strlen(targetPtr);

	unsigned int decSz = sz / 2 + 1;
    char decPtr[decSz];

    char xlate[] = "0123456789abcdef";
    int idx = 0;
    int i;
    for (i = 0; i < sz; i += 2)
    {
        decPtr[idx] = ((strchr(xlate, targetPtr[i]) - xlate) * 16) + ((strchr(xlate, targetPtr[i + 1]) - xlate));
        idx++;
    }

    decPtr[decSz - 1] = '\0';
    char retPtr[decSz];
    retPtr[decSz - 1] = '\0';

    decodeCallbackEnv = env;
    aes256_decryptString(ctx, decPtr, retPtr, decSz - 1, &doDecodeCallback);

    jstring ret;
    int found = 1;

    //check the validity of the string
    for (i = 0; i < decSz; i++)
    {
    	//null tab return alphanumeric and symbols
    	if (!(retPtr[i] == 0 || retPtr[i] == 9 || retPtr[i] == 10 || (retPtr[i] > 31 && retPtr[i] < 127)))
    	{
    		ret = (*env)->NewStringUTF(env, "");
    		found = 0;
    		break;
    	}
    }

    if (found > 0)
    	ret = (*env)->NewStringUTF(env, retPtr);

    return ret;
}

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_decodeFromBytes (JNIEnv *env, jobject jthis, jint jcontext, jbyteArray jtarget)
{
    aes256_context* ctx = (aes256_context*) jcontext;
    int sz = (*env)->GetArrayLength(env, jtarget);
    char targetArr[sz];
    char* targetPtr = &(targetArr[0]);
    (*env)->GetByteArrayRegion(env, jtarget, 0, sz, targetPtr);

    char retArr[sz + 1];
    retArr[sz] = '\0';
    char* retPtr = &(retArr[0]);
    decodeCallbackEnv = env;
    aes256_decryptString(ctx, targetPtr, retPtr, sz, &doDecodeCallback);

    jstring ret;
    int found = 1;

    //check the validity of the string
    for (int i = 0; i < sz; i++)
    {
        //null tab return alphanumeric and symbols
        if (!(retPtr[i] == 0 || retPtr[i] == 9 || retPtr[i] == 10 || (retPtr[i] > 31 && retPtr[i] < 127)))
        {
            ret = (*env)->NewStringUTF(env, "");
            found = 0;
            break;
        }
    }

    if (found > 0)
        ret = (*env)->NewStringUTF(env, retPtr);

    return ret;
}

void setupDecodeCallback(JNIEnv * env, jobject* jthis)
{
    decodeCallbackEnv = env;

    if (decodeCallbackObject != 0)
        return;

    decodeCallbackObject = (*env)->NewGlobalRef(env, *jthis);
    if (decodeCallbackObject == 0)
        return;

    jclass clazz = (*env)->GetObjectClass(env, decodeCallbackObject);
    decodeCallbackMethodID = (*env)->GetMethodID(env, clazz, "doCallback", "(F)V");
}

void cleanupDecodeCallback(JNIEnv* env)
{
    (*env)->DeleteGlobalRef(env, decodeCallbackObject);
}

void doDecodeCallback(float percent)
{
    if (decodeCallbackMethodID == 0)
        return;

    (*decodeCallbackEnv)->CallVoidMethod(decodeCallbackEnv, decodeCallbackObject, decodeCallbackMethodID, percent);
}

JNIEXPORT void JNICALL Java_com_munger_passwordkeeper_struct_AES256_clearDecodeCallback (JNIEnv *env)
{
    decodeCallbackEnv = 0;
    decodeCallbackObject = 0;
    decodeCallbackMethodID = 0;
}