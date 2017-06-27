#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-MacOSX
CND_DLIB_EXT=dylib
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/2e406b84/MD5.o \
	${OBJECTDIR}/_ext/2e406b84/aes256.o \
	${OBJECTDIR}/_ext/2e406b84/com_munger_passwordkeeper_struct_AES256.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ../res/libaes256-${CND_PLATFORM}.${CND_DLIB_EXT}

../res/libaes256-${CND_PLATFORM}.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ../res
	${LINK.c} -o ../res/libaes256-${CND_PLATFORM}.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -dynamiclib -install_name libaes256-${CND_PLATFORM}.${CND_DLIB_EXT} -fPIC

${OBJECTDIR}/_ext/2e406b84/MD5.o: ../../libpasswordkeeper/src/main/java/cpp/MD5.c
	${MKDIR} -p ${OBJECTDIR}/_ext/2e406b84
	${RM} "$@.d"
	$(COMPILE.c) -O2 -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include/darwin -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2e406b84/MD5.o ../../libpasswordkeeper/src/main/java/cpp/MD5.c

${OBJECTDIR}/_ext/2e406b84/aes256.o: ../../libpasswordkeeper/src/main/java/cpp/aes256.c
	${MKDIR} -p ${OBJECTDIR}/_ext/2e406b84
	${RM} "$@.d"
	$(COMPILE.c) -O2 -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include/darwin -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2e406b84/aes256.o ../../libpasswordkeeper/src/main/java/cpp/aes256.c

${OBJECTDIR}/_ext/2e406b84/com_munger_passwordkeeper_struct_AES256.o: ../../libpasswordkeeper/src/main/java/cpp/com_munger_passwordkeeper_struct_AES256.c
	${MKDIR} -p ${OBJECTDIR}/_ext/2e406b84
	${RM} "$@.d"
	$(COMPILE.c) -O2 -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include/darwin -I/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/include -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/2e406b84/com_munger_passwordkeeper_struct_AES256.o ../../libpasswordkeeper/src/main/java/cpp/com_munger_passwordkeeper_struct_AES256.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
