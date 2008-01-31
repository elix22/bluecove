# $Revision$
# $Date$
# $Author$
# $Id$
#
# Makefile for .so
#


SRCDIR=src/main/native
OBJDIR=target
OUTDIR=target/classes

SO_TARGET=libunix-java
SRCS=unix-java.c

CC=gcc
LD=ld

CFLAGS=-fpic -Wall -Os -pedantic -std=c99
INCLUDES=-I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux

LDFLAGS=-fpic -shared -lc

ARCH_EXT := $(shell uname -m | sed -e s/i.86// -e s/x86_64/_x64/)

default: outdir clean so

outdir:
	mkdir -p "$(OBJDIR)"
	mkdir -p "$(OUTDIR)"

clean:
	rm -f "$(OBJDIR)/*.o"
	rm -f "$(OUTDIR)/$(SO_TARGET)*.*"

so: ${SRCDIR}/$(SRCS)
	$(CC) $(CFLAGS) ${INCLUDES} ${SRCDIR}/${SRCS} -o $(OUTDIR)/$(SO_TARGET)${ARCH_EXT}.so $(LDFLAGS)

