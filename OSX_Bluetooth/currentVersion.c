/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2007 Eric Wagner
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
 
 
#include <stdio.h>
/* simple tool to increment the libraries version count each time its compiled */

int main(int argc, const char * argv[]) {
	FILE		*fp;
	int			lastVersion;
	int			newVersion;
	const char	*indexFile = argv[1];
	const char	*defineFile = argv[2];
	
	if(argc!=3) return -1;
	
	fp = fopen(indexFile, "r");
	fscanf(fp, "%d", &lastVersion);
	
	newVersion = lastVersion + 1;
	fclose(fp);
	
	fp = fopen(indexFile, "w");
	fprintf(fp, "%d", newVersion);
	fclose(fp);
	
	fp = fopen(defineFile, "w");
	fprintf(fp, "/* DO NOT EDIT, this file is machine generated */\n");
	fprintf(fp,"#define BUILD_VERSION \"%d\"\n", newVersion);
	fclose(fp);
}
	
	