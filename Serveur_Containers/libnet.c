#include "libnet.h"

char marqueurRecu (char *m, int nc)
/* Recherche de la sequence \r\n */
{
	static char demiTrouve = 0;
	int i;
	char trouve = 0;
	
	if(demiTrouve == 1 && m[0] == '\n')
		return 1;
	else
		demiTrouve=0;
	
	for(i = 0; i < nc-1 && !trouve; i++)
		if (m[i] == '\r' && m[i+1] == '\n')
			trouve=1;
	
	if(trouve)
		return 1;
	else
		if(m[nc] == '\r')
		{
			demiTrouve=1;
			return 0;
		}
		else
			return 0;
}

int recvGrosMsg(int socket, char *msg, size_t segSize)
{
	int tailleMsgRecu = 0, nbreBytesRecus = 0, finDetectee = 0;
	char buf[segSize];
	
	/*if((nbreRecv = recv(socket, msg, size, 0)) == -1)
	{
		printf("Erreur sur le recv de la socket %d\n", errno);
		close(socket);
		exit(1);
	}
	else printf("Recv socket OK\n");
	
	msg[nbreRecv] = 0;
	printf("Message reçu = %s\n", msg);
	
	return nbreRecv;
	*/
	do
	{
		puts("Passage boucle de reception");
		if((nbreBytesRecus = recv(socket, buf, segSize, 0)) == -1)
		{
			printf("Erreur sur le recv de la socket %d\n", errno);
			close(socket);
			exit(1);
		}
		else
		{
			finDetectee = marqueurRecu(buf, nbreBytesRecus);
			memcpy((char *)msg + tailleMsgRecu, buf, nbreBytesRecus);
			tailleMsgRecu += nbreBytesRecus;
			printf("finDetecteee = %d\n", finDetectee);
			printf("Nombre de bytes recus = %d\n", nbreBytesRecus);
			printf("Taille totale msg recu = %d\n", tailleMsgRecu);
		}
	}
	while (nbreBytesRecus != 0 && nbreBytesRecus != -1 && !finDetectee);
	
	msg[tailleMsgRecu - 2] = 0;
	printf("Recv socket OK\n");
	printf("Message reçu = %s\n", msg);
	
	return tailleMsgRecu;
}

int sendGrosMsg(int socket, char *msg, size_t size)
{
	if(send(socket, msg, size, 0) == -1)
	{
		printf("Erreur sur le send de la socket %d\n", errno);
		close(socket);
		exit(1);
	}
	else printf("Send socket OK\n");
	
	return 0;
}

int createSocket(struct sockaddr_in *adresseSocket)
{
	int s = socket(AF_INET, SOCK_STREAM, 0);
	if (s == -1)
	{
		printf("Erreur de creation de la socket %d\n", errno);
		return -1;
	}
	else printf("Creation de la socket OK\n");
	
	if(bind(s, (struct sockaddr *)adresseSocket, sizeof(struct sockaddr_in)) == -1)
	{
		printf("Erreur sur le bind de la socket %d\n", errno);
		close(s);
		exit(1);
	}
	else printf("Bind adresse et port socket OK\n");
	
	return s;
}

int listenSocket(int socket, struct sockaddr_in *adresseSocket)
{
	int socketService;
	unsigned int tailleSockaddr_in = sizeof(struct sockaddr_in);
	
	if(listen(socket, SOMAXCONN) == -1)
	{
		printf("Erreur sur le listen de la socket %d\n", errno);
		close(socket);
		exit(1);
	}
	else printf("Listen socket OK\n");
	
	if((socketService = accept(socket, (struct sockaddr *)adresseSocket, &tailleSockaddr_in)) == -1)
	{
		if(errno == EINTR)
			return -1;
		printf("Erreur sur l'accept de la socket %d\n", errno);
		close(socket);
		exit(1);
	}
	else printf("Accept socket OK\n");
	
	return socketService;
}

int connectSocket(struct sockaddr_in *adresseSocket)
{
	int s = socket(AF_INET, SOCK_STREAM, 0);
	if (s == -1)
	{
		printf("Erreur de creation de la socket %d\n", errno);
		return -1;
	}
	else printf("Creation de la socket OK\n");
	
	int ret;
	if((ret = connect(s, (struct sockaddr*)adresseSocket, sizeof(struct sockaddr_in))) == -1)
	{
		printf("Erreur sur le connect de la socket %d\n", errno);
		close(s);
		exit(1);
	}
	else printf("Connect socket OK\n");
	
	return s;
}

struct hostent *getInfosHost(char *hostname)
{
	struct hostent *infosHost;
	if(hostname != NULL && (infosHost = gethostbyname(hostname)) == 0)
	{
		printf("Erreur d'acquisition d'infos sur le host %d\n", errno);
		return NULL;
	}
	else printf("Acquisition infos host OK\n");
	
	return infosHost;
}
