#include "aes256.h"

void callback(float progress)
{}

int main(int argc, char** argv)
{
	aes256_context context;
	uint8_t* password = (uint8_t*) "pass";
	aes256_init(&context, password);

	const char* text = "plaintext";
	char cipherText[16];
	aes256_encryptString(&context, text, &(cipherText[0]));
	char plainText[16];
	aes256_decryptString(&context, &(cipherText[0]), &(plainText[0]), 16, &callback); 

	
	aes256_done(&context);
	return 0;
}
