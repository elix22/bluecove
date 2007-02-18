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
 
#import "ThreadCleanups.h"
#include "Foundation/NSAutoreleasePool.h"
#include "Foundation/NSThread.h"

void* OSXThreadWrapper( void *data )
{ 
	ThreadCleanups *tData= (ThreadCleanups*) data;
	void* r = 0;
	
	if( tData && tData->function ){

		NSAutoreleasePool *autoreleasepool = [[NSAutoreleasePool alloc] init];
		
		/* QUESTION: At this point Cocoa isn't in multithread mode. But we're going to 
			pains to keep all the Cocoa code in a single thread. It seems to work in 
			singlethreaded mode for now. If it requires multithread mode this is the place
			to activate it */
			
		r= (*tData->function)( tData->data );
		[autoreleasepool release];
	}
	return r;
}
