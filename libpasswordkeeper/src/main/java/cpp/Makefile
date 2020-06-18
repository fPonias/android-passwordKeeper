all: test libaes256.so

test: libaes256.so test.o
	gcc -L. -laes256 test.o -o test

test.o: test.c
	gcc -L. -laes256 -c test.c

libaes256.so: aes256.o MD5.o com_munger_passwordkeeper_struct_AES256.o
	gcc aes256.o MD5.o com_munger_passwordkeeper_struct_AES256.o -shared -g -o libaes256.so

aes256.o: aes256.c aes256.h
	gcc -g -c -lc -fPIC aes256.c
	
MD5.o: MD5.c MD5.h
	gcc -g -c -fPIC MD5.c

sha256.o: sha256.c sha256.h
	gcc -g -c -fPIC sha256.c

com_munger_passwordkeeper_struct_AES256.o: com_munger_passwordkeeper_struct_AES256.c com_munger_passwordkeeper_struct_AES256.h
	gcc -g -I"/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home/include" -I"/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home/include/darwin" -c -fPIC com_munger_passwordkeeper_struct_AES256.c	
