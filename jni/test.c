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

#include "MD5.h"
#include "aes256.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


int main(int argc, const char* argv[])
{
	if (argc < 3)
		return 0;

	const char* password = argv[1];
	printf("password: %s\n", argv[1]);
	aes256_context* ctx = malloc(sizeof(aes256_context));
	aes256_initFromPassword(ctx, password);


	printf("string: %s\n", argv[2]);
	const char* targetPtr = argv[2];
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
	printf("decoded string: %s\n", retPtr);

    int found = 1;
    for (i = 0; i < decSz; i++)
    {
		if (!(retPtr[i] == 0 || retPtr[i] == 10 || (retPtr[i] > 31 && retPtr[i] < 127)))
		{
			found = 0;
			printf("invalid string detected at char %i with value %i\n", i, retPtr[i]);
			break;
		}
    }

    if (found)
    	printf("decoded string: %s\n", retPtr);

    free(ctx);
}
