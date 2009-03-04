/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2009 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 * @version $Id$
 */
#define CPP__FILE "BlueCoveLocalSocket.c"

#include "BlueCoveLocalSocket.h"

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeCreate
  (JNIEnv *env, jobject peer, jboolean stream) {
    int rc;
    rc = socket(PF_UNIX, stream ? SOCK_STREAM : SOCK_DGRAM, 0);
    if (rc < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }
    return rc;
}

bool validateSocket(JNIEnv *env, jint handle) {
    if (handle < 0) {
        throwIOException(env, "invalid socket [%d]", handle);
        return false;
    } else {
        return true;
    }
}

struct sockaddr_un* populateSocketAddress(JNIEnv *env, int* paddress_len, jstring name, jboolean abstractNamespace) {
    const char* path;
    int name_len;
    struct sockaddr_un* paddr;

    path = (*env)->GetStringUTFChars(env, name, NULL);
    if (path == NULL) {
        throwRuntimeException(env, "JNI error");
        return NULL;
    }

    name_len = (*env)->GetStringUTFLength(env, name);
    paddr = (struct sockaddr_un*)malloc(sizeof(sa_family_t) + name_len + 1);
    if (paddr == NULL) {
        throwRuntimeException(env, "no memory available");
        return NULL;
    }
    if (abstractNamespace) {
        strncpy(paddr->sun_path + 1, path, name_len);
        paddr->sun_path[0] = '\0';
    } else {
        strncpy(paddr->sun_path, path, name_len + 1);
    }
    paddr->sun_family = AF_UNIX;

    (*env)->ReleaseStringUTFChars(env, name, path);

    (*paddress_len) = offsetof(struct sockaddr_un, sun_path) + name_len + 1;

    return paddr;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeConnect
  (JNIEnv *env, jobject peer, jint handle, jstring name, jboolean abstractNamespace, jint timeout) {
    struct sockaddr_un* paddr;
    int address_len;
    int rc;

    if (!validateSocket(env, handle)) {
        return;
    }
    paddr = populateSocketAddress(env, &address_len, name, abstractNamespace);
    if (paddr == NULL) {
        return;
    }
    rc = connect((int)handle, (struct sockaddr *) paddr, address_len);
    free(paddr);
    if (rc < 0) {
        throwIOException(env, "Failed to connect socket. [%d] %s", errno, strerror(errno));
        return;
    }
    return;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeBind
  (JNIEnv *env, jobject peer, jint handle, jstring name, jboolean abstractNamespace) {
    struct sockaddr_un* paddr;
    int address_len;
    int rc;

    if (!validateSocket(env, handle)) {
        return;
    }
    paddr = populateSocketAddress(env, &address_len, name, abstractNamespace);
    if (paddr == NULL) {
        return;
    }
    rc = bind((int)handle, (struct sockaddr *) paddr, address_len);
    free(paddr);
    if (rc < 0) {
        throwIOException(env, "Failed to bind socket. [%d] %s", errno, strerror(errno));
        return;
    }
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeListen
  (JNIEnv *env, jobject peer, jint handle, jint backlog) {
    int rc;
    int flags;

    if (!validateSocket(env, handle)) {
        return;
    }
    // use non-blocking mode
    flags = fcntl(handle, F_GETFL, 0);
    if (SOCKET_ERROR == flags) {
        throwIOException(env, "Failed to read server descriptor flags. [%d] %s", errno, strerror(errno));
        return;
    }
    if (SOCKET_ERROR == fcntl(handle, F_SETFL, flags | O_NONBLOCK)) {
        throwIOException(env, "Failed to set server non-blocking flags. [%d] %s", errno, strerror(errno));
        return;
    }

    rc = listen(handle, backlog);
    if (rc < 0) {
        throwIOException(env, "Failed to bind socket. [%d] %s", errno, strerror(errno));
        return;
    }
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAccept
  (JNIEnv *env, jobject peer, jint handle) {
    if (!validateSocket(env, handle)) {
        return -1;
    }

    int client_socket = SOCKET_ERROR;
    do {
        client_socket = accept(handle, NULL, NULL);
        if (SOCKET_ERROR == client_socket) {
            if (errno == EWOULDBLOCK) {
                if (isCurrentThreadInterrupted(env, peer)) {
                    return 0;
                }
                if (!threadSleep(env, 100)) {
                    return 0;
                }
                continue;
            } else {
                throwIOException(env, "Failed to accept client connection. [%d] %s", errno, strerror(errno));
                return -1;
            }
        }
    } while (SOCKET_ERROR == client_socket);
    debug("client accepted, handle %li", client_socket);
    return client_socket;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeClose
  (JNIEnv *env, jobject peer, jint handle) {
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        throwIOException(env, "Failed to close socket. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAvailable
  (JNIEnv *env, jobject peer, jint handle) {
    struct pollfd fds;
    int timeout = 10; // milliseconds

    if (!validateSocket(env, handle)) {
        return -1;
    }

    fds.fd = handle;
    fds.events = POLLIN | POLLHUP | POLLERR; // | POLLRDHUP;
    fds.revents = 0;
    int poll_rc = poll(&fds, 1, timeout);
    if (poll_rc > 0) {
        if (fds.revents & (POLLHUP | POLLERR/* | POLLRDHUP */)) {
            throwIOException(env, "Stream socket peer closed connection");
        } else if (fds.revents & POLLIN) {
            return 1;
        }
        // POLLNVAL - this method may choose to throw an IOException if this input stream has been closed by invoking the close() method.
        // We do not
    } else if (poll_rc == -1) {
        throwIOException(env, "Failed to read available. [%d] %s", errno, strerror(errno));
    }
    return 0;
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeRead
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
    jbyte *bytes;
    int done;

    if (!validateSocket(env, handle)) {
        return -1;
    }

    bytes = (*env)->GetByteArrayElements(env, b, 0);
    done = 0;
    while (done == 0) {
        int flags = MSG_DONTWAIT;
        int count = recv(handle, (char *)(bytes + off + done), len - done, flags);
        if (count < 0) {
            if (errno == EAGAIN) { // Try again for non-blocking operation
                count = 0;
                debug("no data available for read");
            } else if (errno == ECONNRESET) { //104 Connection reset by peer
                debug("Connection closed, Connection reset by peer");
                // See InputStream.read();
                done = -1;
                goto rfReadEnd;
            } else {
                throwIOException(env, "Failed to read. [%d] %s", errno, strerror(errno));
                done = 0;
                goto rfReadEnd;
            }
        } else if (count == 0) {
            debug("Connection closed");
            if (done == 0) {
                // See InputStream.read();
                done = -1;
            }
            goto rfReadEnd;
        }
        done += count;
        if (isCurrentThreadInterrupted(env, peer)) {
            done = 0;
            goto rfReadEnd;
        }
        if (done == 0) {
            // Sleep while not avalable
            bool available = false;
            do {
                struct pollfd fds;
                int timeout = 500; // milliseconds
                fds.fd = handle;
                fds.events = POLLIN;
                fds.revents = 0;
                //debug("poll: wait");
                int poll_rc = poll(&fds, 1, timeout);
                if (poll_rc > 0) {
                    if (fds.revents & (POLLHUP | POLLERR)) {
                        debug("Stream socket peer closed connection");
                        done = -1;
                        goto rfReadEnd;
                    } else if (fds.revents & POLLNVAL) {
                        // socket closed...
                         done = -1;
                         goto rfReadEnd;
                    } else if (fds.revents & POLLIN) {
                        debug("poll: data to read available");
                        available = true;
                    } else {
                        debug("poll: revents %i", fds.revents);
                    }
                } else if (poll_rc == -1) {
                    //Edebug("poll: call error %i", errno);
                    throwIOException(env, "Failed to poll. [%d] %s", errno, strerror(errno));
                    done = 0;
                    goto rfReadEnd;
                } else {
                    //debug("poll: call timed out");
                }
                if (isCurrentThreadInterrupted(env, peer)) {
                    done = -1;
                    goto rfReadEnd;
                }
            } while (!available);
        }
    }
rfReadEnd:
    (*env)->ReleaseByteArrayElements(env, b, bytes, 0);
    return done;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeWrite
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
    jbyte *bytes;
    int done;

    if (!validateSocket(env, handle)) {
        return;
    }

    bytes = (*env)->GetByteArrayElements(env, b, 0);
    done = 0;
    while(done < len) {
        int count = send(handle, (char *)(bytes + off + done), len - done, 0);
        if (count < 0) {
            throwIOException(env, "Failed to write. [%d] %s", errno, strerror(errno));
            break;
        }
        if (isCurrentThreadInterrupted(env, peer)) {
            break;
        }
        done += count;
    }
    (*env)->ReleaseByteArrayElements(env, b, bytes, 0);
}