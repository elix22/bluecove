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
 
 
 #include "blueCovejnilib.h"

void initializeToDoList(todoListRoot *rootPtr){
	pthread_mutexattr_t			attributes;
	
	pthread_mutexattr_init(&attributes);
	pthread_mutexattr_settype(&attributes, PTHREAD_MUTEX_RECURSIVE);
	pthread_mutex_init(&(rootPtr->listLock), &attributes);
	pthread_mutexattr_destroy(&attributes);
	rootPtr->listHead = NULL;

}
void addToDoItem(todoListRoot *rootPtr, threadPassType aRec){
		
	todoListItem			*anItem;
	todoListItem			*newItem;
	
	/* initialize a new item*/
	newItem = (todoListItem*) malloc(sizeof(todoListItem));
	newItem->next = NULL;
	newItem->type = aRec;

	pthread_mutex_lock(&(rootPtr->listLock));
	/* special case if first on list */
	if(rootPtr->listHead == NULL) {
		rootPtr->listHead = newItem;
	} else {
		/* find the end of the list */
		anItem = rootPtr->listHead;
		while(anItem->next != NULL) anItem = anItem->next;
		anItem->next = newItem;
	}
	pthread_mutex_unlock(&(rootPtr->listLock));
}

void deleteToDoItem(todoListRoot *rootPtr, threadPassType aRec){
	
	todoListItem			*anItem;
	todoListItem			*previousItem;
	todoListItem*			toFree;
	
	pthread_mutex_lock(&(rootPtr->listLock));
	/* check if its the first item */
	if(rootPtr->listHead) if(rootPtr->listHead->type.voidPtr == aRec.voidPtr) {
		toFree = rootPtr->listHead;
		rootPtr->listHead = rootPtr->listHead->next;
		free(toFree);
	} else {
	
		/* find the item*/
		previousItem = rootPtr->listHead;
		while( (previousItem->next !=NULL) && (previousItem->next->type.voidPtr != aRec.voidPtr))
					previousItem = previousItem->next;
		if(previousItem->next == NULL) {
			printMessage("deleteToDoItem asked to delete non existant item!", DEBUG_ERROR_LEVEL);
		} else {
			toFree = previousItem->next;
			previousItem->next = toFree->next;
			free(toFree);
		}
	}
			
	pthread_mutex_unlock(&(rootPtr->listLock));


}
threadPassType getNextToDoItem(todoListRoot *rootPtr){
	threadPassType		aType ={0};
	todoListItem		*toFree;
	
	pthread_mutex_lock(&(rootPtr->listLock));
	
	toFree = rootPtr->listHead;
	if(toFree) {
		rootPtr->listHead = toFree->next;
		aType = toFree->type;
		free(toFree);
	}

	pthread_mutex_unlock(&(rootPtr->listLock));
	return aType;
}