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
//#include "MD5.h"
#include "sha256.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "com_munger_passwordkeeper_struct_AES256.h"


float decodeProgress = 0.0f;

void doDecodeCallback(float percent)
{
    decodeProgress = percent;
}

JNIEXPORT jfloat JNICALL Java_com_munger_passwordkeeper_struct_AES256_getDecodeProgress (JNIEnv * env, jobject jthis)
{
    return (jfloat) decodeProgress;
}

JNIEXPORT jbyteArray JNICALL Java_com_munger_passwordkeeper_struct_AES256_init(JNIEnv * env, jobject jthis, jstring jpass, jint hashType)
{
	const char* password = (*env)->GetStringUTFChars(env, jpass, 0);
	aes256_context ret;
	aes256_initFromPassword(&ret, password, hashType);

    int sz = sizeof(aes256_context);
    printf("context size: %i", sz);
    jbyteArray retArr = (*env)->NewByteArray(env, sz);
    (*env)->SetByteArrayRegion(env, retArr, 0, sz, (uint8_t *) (&ret));
	return retArr;
}

JNIEXPORT void JNICALL Java_com_munger_passwordkeeper_struct_AES256_destroy(JNIEnv * env, jobject jthis, jbyteArray jcontext)
{
    aes256_context ctx;
    int size = sizeof(aes256_context);
    (*env)->ReleaseByteArrayElements(env, jcontext, (uint8_t *) (&ctx), 0);
}

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_encode(JNIEnv * env, jobject jthis, jbyteArray jcontext, jstring jtarget)
{
	aes256_context ctx;
    int size = sizeof(aes256_context);
    (*env)->GetByteArrayRegion(env, jcontext, 0, size, (uint8_t *) (&ctx));
	const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    unsigned int sz = strlen(targetPtr);

    unsigned int retSz = sz / 16;
    if (sz % 16 != 0)
        retSz += 1;
    retSz *= 16;

    char retPtr[retSz];
    char* retPtr2 = &(retPtr[0]);
    aes256_encryptString(&ctx, targetPtr, retPtr2);
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

JNIEXPORT jbyteArray JNICALL Java_com_munger_passwordkeeper_struct_AES256_encodeToBytes (JNIEnv * env, jobject jthis, jbyteArray jcontext, jstring jtarget)
{
    aes256_context ctx;
    int size = sizeof(aes256_context);
    (*env)->GetByteArrayRegion(env, jcontext, 0, size, (uint8_t *) (&ctx));
    const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    unsigned int sz = strlen(targetPtr);

    unsigned int retSz = sz / 16;
    if (sz % 16 != 0)
        retSz += 1;
    retSz *= 16;

    char retPtr[retSz];
    char* retPtr2 = &(retPtr[0]);
    aes256_encryptString(&ctx, targetPtr, retPtr2);

    jbyteArray ret = (*env)->NewByteArray(env, retSz);
    (*env)->SetByteArrayRegion(env, ret, 0, retSz, retPtr2);
    return ret;
}

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_decode(JNIEnv * env, jobject jthis, jbyteArray jcontext, jstring jtarget)
{
    aes256_context ctx;
    int size = sizeof(aes256_context);
    (*env)->GetByteArrayRegion(env, jcontext, 0, size, (uint8_t *) (&ctx));
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

    aes256_decryptString(&ctx, decPtr, retPtr, decSz - 1, &doDecodeCallback);

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

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_struct_AES256_decodeFromBytes (JNIEnv *env, jobject jthis, jbyteArray jcontext, jbyteArray jtarget)
{
    aes256_context ctx;
    int size = sizeof(aes256_context);
    (*env)->GetByteArrayRegion(env, jcontext, 0, size, (uint8_t *) (&ctx));
    int sz = (*env)->GetArrayLength(env, jtarget);
    char targetArr[sz];
    char* targetPtr = &(targetArr[0]);
    (*env)->GetByteArrayRegion(env, jtarget, 0, sz, targetPtr);

    char retArr[sz + 1];
    retArr[sz] = '\0';
    char* retPtr = &(retArr[0]);
    aes256_decryptString(&ctx, targetPtr, retPtr, sz, &doDecodeCallback);

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

JNIEXPORT jbyteArray JNICALL Java_com_munger_passwordkeeper_struct_AES256_md5Hash (JNIEnv* env, jobject jthis, jstring jtarget)
{
    const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    char hash[33];
    hash[32] = '\0';
    char* hashPtr = &(hash[0]);
    MD5Hash(targetPtr, hashPtr);
    jstring ret = (*env)->NewStringUTF(env, hashPtr);

    return ret;
}

JNIEXPORT jbyteArray JNICALL Java_com_munger_passwordkeeper_struct_AES256_shaHash (JNIEnv* env, jobject jthis, jstring jtarget)
{
    const char* targetPtr = (*env)->GetStringUTFChars(env, jtarget, 0);

    char hash[65];
    hash[64] = '\0';
    char* hashPtr = &(hash[0]);
    shaHash(targetPtr, hashPtr);
    jstring ret = (*env)->NewStringUTF(env, hashPtr);

    return ret;
}