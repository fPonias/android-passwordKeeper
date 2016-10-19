all: test

test: aes256.o MD5.o test.o
	gcc aes256.o MD5.o test.o -g -o test

aes256.o: aes256.c aes256.h
	gcc -g -c -lc aes256.c
	
MD5.o: MD5.c MD5.h
	gcc -g -c MD5.c
	
test.o: test.c
	gcc -g -c -I /System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers test.c