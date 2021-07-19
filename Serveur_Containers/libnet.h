#ifndef LIBNET
#define LIBNET

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <errno.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <time.h>

#define h_addr h_addr_list[0]

int recvGrosMsg(int socket, char *msg, size_t segSize);

int sendGrosMsg(int socket, char *msg, size_t size);

int createSocket(struct sockaddr_in *adresseSocket);

int listenSocket(int socket, struct sockaddr_in *adresseSocket);

int connectSocket(struct sockaddr_in *adresseSocket);

struct hostent *getInfosHost(char *hostname);

#endif
