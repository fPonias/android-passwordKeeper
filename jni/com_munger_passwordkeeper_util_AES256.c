#include "jni.h"
#include "aes256.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

JNIEXPORT jint JNICALL Java_com_munger_passwordkeeper_util_AES256_init(JNIEnv * env, jobject jthis, jstring jpass)
{
	const char* password = (*env)->GetStringUTFChars(env, jpass, 0);
	aes256_context* ret = malloc(sizeof(aes256_context));
	aes256_initFromPassword(ret, password);

	return (long) ret;
}

JNIEXPORT void JNICALL Java_com_munger_passwordkeeper_util_AES256_destroy(JNIEnv * env, jobject jthis, jint jcontext)
{
	aes256_context* ctx = (aes256_context*) jcontext;
	free(ctx);
}

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_util_AES256_encode(JNIEnv * env, jobject jthis, jint jcontext, jstring jtarget)
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

JNIEXPORT jstring JNICALL Java_com_munger_passwordkeeper_util_AES256_decode(JNIEnv * env, jobject jthis, jint jcontext, jstring jtarget)
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

    aes256_decryptString(ctx, decPtr, retPtr, decSz - 1);

    jstring ret;
    int found = 1;

    //check the validity of the string
    for (i = 0; i < decSz; i++)
    {
    	if (!(retPtr[i] == 0 || retPtr[i] == 10 || (retPtr[i] > 31 && retPtr[i] < 127)))
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
