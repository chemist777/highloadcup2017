#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdbool.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <errno.h>
#include <netinet/tcp.h>
#include <fcntl.h>
#include <sys/epoll.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include "ru_chemist_highloadcup_jni_NativeNet.h"

JNIEXPORT void JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_test
  (JNIEnv *env, jobject obj) {
    printf ("Hello, world!\n");
    fflush(stdout);
}

int optval = 1;

void setnonblocking(int sock)
{
    int opts;
    if ((opts = fcntl(sock, F_GETFL)) < 0) {
        printf("GETFL failed %s\n", strerror(errno));
        fflush(stdout);
        }
    opts = opts | O_NONBLOCK;
    if (fcntl(sock, F_SETFL, opts) < 0) {
        printf("SETFL failed %s\n", strerror(errno));
        fflush(stdout);
        }
}

JNIEXPORT jlong JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_bind
  (JNIEnv *env, jobject obj, jint port) {

    int fd = socket(AF_INET, SOCK_STREAM, 0);
//    int timeout = 1;
//    int ret = setsockopt(fd, IPPROTO_TCP, TCP_DEFER_ACCEPT, &timeout, sizeof(int));
//    if (ret < 0) {
//        printf("set TCP_DEFER_ACCEPT error: %s\n", strerror(errno));
//        fflush(stdout);
//    }

    int ret = setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &optval, sizeof(int));
    if (ret < 0) {
        printf("set TCP_NODELAY error: %s\n", strerror(errno));
        fflush(stdout);
    }

    ret = setsockopt(fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &optval, sizeof(int));
    if (ret < 0) {
        printf("set SO_REUSEADDR, SO_REUSEPORT error: %s\n", strerror(errno));
        fflush(stdout);
    }

    setnonblocking(fd);

    struct sockaddr_in serv_addr;
    memset(&serv_addr, 0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(port);

    ret = bind(fd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
    if (ret < -0)
    {
        printf("bind error: %s\n", strerror(errno));
        fflush(stdout);
        return -1;
    }

    ret = listen(fd, 256 * 1024);
    if (ret < 0) {
        printf("listen error: %s\n", strerror(errno));
        fflush(stdout);
        return -2;
    }

    return fd;
}

JNIEXPORT jint JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_epollListen
  (JNIEnv *env, jclass cl, jlong epollId, jlong fd) {
        struct epoll_event ev;
      // event related descriptor
      ev.data.fd = fd;
      // monitor in message, edge trigger
      ev.events = EPOLLIN | EPOLLET;
      // register epoll event
      return epoll_ctl(epollId, EPOLL_CTL_ADD, fd, &ev);
  }


struct linger linger = { 0 };

JNIEXPORT void JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_close
  (JNIEnv *env, jobject obj, jlong fd) {

    int r = syscall(SYS_setsockopt, fd, SOL_SOCKET, SO_LINGER, (const char *) &linger, sizeof(linger));
    if (r < 0) {
        printf("set linger error: %s\n", strerror(errno));
        fflush(stdout);
    }
    syscall(SYS_close, fd);
  }

JNIEXPORT void JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_unbind
  (JNIEnv *env, jobject obj, jlong fd) {
  int r = setsockopt(fd, SOL_SOCKET, SO_LINGER, (const char *) &linger, sizeof(linger));
      if (r < 0) {
          printf("set linger error: %s\n", strerror(errno));
          fflush(stdout);
      }
    shutdown(fd, SHUT_RDWR);
  }

static void intToBytes(int i, char* b){
    b[0] = (i >> 24) & 0xff;
    b[1] = (i >> 16) & 0xff;
    b[2] = (i >> 8) & 0xff;
    b[3] = (i) & 0xff;
}

struct sockaddr_in cli_addr;
int addr_length;

JNIEXPORT jint JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_read
  (JNIEnv *env, jobject obj, jlong fd, jlong bbAddress, jint size, jint offset) {

    jbyte *iBuf = (jbyte*) bbAddress;

    int total = 0;

    for(;;) {
        int len = syscall(SYS_read, fd, iBuf + offset, size - offset);
        if (len < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) break;
            return len;
        } else if (len == 0) {
            return 0;
        }
        offset += len;
        total += len;
    }

    return total == 0 ? -100 : total;
}

JNIEXPORT jint JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_write
  (JNIEnv *env, jobject obj, jlong fd, jobject buf, jint size) {
   jbyte *iBuf = (*env)->GetDirectBufferAddress(env, buf);

   int ret = syscall(SYS_write, fd, iBuf, size);
    if (ret < 0) {

        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return -100;
        }
       printf("write error: %s\n", strerror(errno));
       fflush(stdout);
   }
       return ret;
}

JNIEXPORT void JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_init
  (JNIEnv *env, jclass cl) {

    linger.l_onoff = 1;
    linger.l_linger = 0;

    memset(&cli_addr, 0, sizeof(cli_addr));
    addr_length = sizeof(cli_addr);

  }

JNIEXPORT jlong JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_epollCreate
  (JNIEnv *env, jclass cl) {
    return epoll_create1(0);
}



int epollMaxEvents = 4096;
int epollTimeoutMs = 1000;

JNIEXPORT jlong JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_eventsBuffer
  (JNIEnv *env, jclass cl) {
  struct epoll_event *t = malloc(epollMaxEvents * sizeof(struct epoll_event));
  return (long) t;
}

JNIEXPORT jint JNICALL Java_ru_chemist_highloadcup_jni_NativeNet_getEvents
  (JNIEnv *env, jclass cl, jlong epollId, jlong serverSocketId, jlong bbAddress, jlong eventsPointer) {

  struct epoll_event *events = (struct epoll_event *) eventsPointer;
  jbyte *iBuf = (jbyte*) bbAddress;

  int nfds = syscall(SYS_epoll_wait, epollId, events, epollMaxEvents, epollTimeoutMs);

  int i, j = 0;
  int total = 0;
  int cli_sock, ret;

  for (i = 0; i < nfds; ++i) {
    int fd = events[i].data.fd;

    if (fd == serverSocketId) {
        for(;;) {
            cli_sock = syscall(SYS_accept4, fd, (struct sockaddr *) &cli_addr, (socklen_t *) &addr_length, SOCK_NONBLOCK);
//            printf("accept: %i\n", cli_sock);
//            fflush(stdout);
            if (cli_sock < 0) {
                if (errno == EAGAIN || errno == EWOULDBLOCK) {
                    break;
                }
              printf("accept error: %s\n", strerror(errno));
              fflush(stdout);
              break;
            } else {
                ret = syscall(SYS_setsockopt, cli_sock, IPPROTO_TCP, TCP_NODELAY, &optval, sizeof(int));
                if (ret < 0) {
                    printf("set TCP_NODELAY error: %s\n", strerror(errno));
                    fflush(stdout);
                }
//                int val = IP_PMTUDISC_DONT;
//                ret = syscall(SYS_setsockopt, cli_sock, IPPROTO_IP, IP_MTU_DISCOVER, &val, sizeof(val));
//                if (ret < 0) {
//                    printf("set IP_MTU_DISCOVER error: %s\n", strerror(errno));
//                    fflush(stdout);
//                }

                events[i].data.fd = cli_sock;
                events[i].events = EPOLLIN | EPOLLET;

                syscall(SYS_epoll_ctl, epollId, EPOLL_CTL_ADD, cli_sock, &events[i]);

                iBuf[j++] = 0x00;
                intToBytes(cli_sock, &iBuf[j]);
                j += 4;
                total++;
//                if (j>=4000) {
//                                    printf("out of mem1");
//                                    fflush(stdout);
//                                }
            }
        }
    } else {
        iBuf[j++] = 0x01;
        intToBytes(fd, &iBuf[j]);
        j += 4;
        total++;
//        if (j>=4000) {
//                            printf("out of mem2");
//                            fflush(stdout);
//                        }
    }
  }

//   printf("last: %i\n", i);
//       fflush(stdout);

  return total;
}
