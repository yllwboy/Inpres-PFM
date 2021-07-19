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

#define PORT 70000
#define MAXSTRING 100

int main()
{
	int hSocketEcoute,
		hSocketService;
	struct hostent *infosHost;
	struct in_addr adresseIP;
	struct sockaddr_in adresseSocket;
	int tailleSockaddr_in;
	char msgClient[MAXSTRING], msgServeur[MAXSTRING];
	int nbreRecv;
	
	/* 1. */
	hSocketEcoute = socket(AF_INET, SOCK_STREAM, 0);
	if (hSocketEcoute == -1)
	{
		printf("Erreur de creation de la socket %d\n", errno);
		exit(1);
	}
	else printf("Creation de la socket OK\n");
	
	/* 2. */
	if((infosHost = gethostbyname("localhost")) == 0)
	{
		printf("Erreur d'acquisition d'infos sur le host %d\n", errno);
		exit(1);
	}
	else printf("Acquisition infos host OK\n");
	memcpy(&adresseIP, infosHost->h_addr, infosHost->h_length);
	printf("Adresse IP = %s\n", inet_ntoa(adresseIP));
	
	/* 3. */
	memset(&adresseSocket, 0, sizeof(struct sockaddr_in));
	adresseSocket.sin_family = AF_INET;
	adresseSocket.sin_port = htons(PORT);
	memcpy(&adresseSocket.sin_addr, infosHost->h_addr, infosHost->h_length);
	
	/* 4. */
	if(bind(hSocketEcoute, (struct sockaddr*)&adresseSocket, sizeof(struct sockaddr_in)) == -1)
	{
		printf("Erreur sur le bind de la socket %d\n", errno);
		exit(1);
	}
	else printf("Bind adresse et port socket OK\n");
	
	/* 5. */
	if(listen(hSocketEcoute, SOMAXCONN) == -1)
	{
		printf("Erreur sur le listen de la socket %d\n", errno);
		close(hSocketEcoute);
		exit(1);
	}
	else printf("Listen socket OK\n");
	
	/* 6. */
	tailleSockaddr_in = sizeof(struct sockaddr_in);
	if((hSocketService = accept(hSocketEcoute, (struct sockaddr*)&adresseSocket, &tailleSockaddr_in))==-1)
	{
		printf("Erreur sur l'accept de la socket %d\n", errno);
		close(hSocketEcoute);
		exit(1);
	}
	else printf("Accept socket OK\n");
	
	/* 7. */
	if((nbreRecv = recv(hSocketService, msgClient, MAXSTRING, 0)) == -1)
	{
		printf("Erreur sur le recv de la socket %d\n", errno);
		close(hSocketEcoute);
		close(hSocketService);
		exit(1);
	}
	else printf("Recv socket OK\n");
	msgClient[nbreRecv] = 0;
	printf("Message reçu = %s\n", msgClient);
	
	/* 8. */
	sprintf(msgServeur, "ACK pour votre message : <%s>", msgClient);
	if(send(hSocketService, msgServeur, MAXSTRING, 0) == -1)
	{
		printf("Erreur sur le send de la socket %d\n", errno);
		close(hSocketEcoute);
		close(hSocketService);
		exit(1);
	}
	else printf("Send socket OK\n");
	
	/* 9. */
	
	
	/* 10. */
	
	
	/* 11. */
	close(hSocketService);
	printf("Socket connectee au client fermee\n");
	close(hSocketEcoute);
	printf("Socket serveur fermee\n");
	
	return 0;
}

/*
void fctSousProcess(int hSockServ, int port)
{
	int vr = port;
	char msgServeur[LONG_MSG_SERV];
	container *msgContainer = (container *)malloc(sizeof(container));
	int tailleMsgRecu, nbreBytesRecus;
	char buf[MAXSTRING];
	
	printf("** vr = %d\n", vr);
	affSousProcess(vr, "Debut du sous-processus");
	sprintf(buf, "identite = %d\n", getpid());
	affSousProcess(vr, buf);
	sprintf(buf, "je travaille sur la socket de service %d\n", hSockServ);
	affSousProcess(vr, buf);
	
	// 1. 
	tailleMsgRecu = 0;
	do
	{
		nbreBytesRecus = recvMsg(hSockServ, ((char *)msgContainer + tailleMsgRecu, LONG_STRUCT_CON-tailleMsgRecu);
		if(nbreBytesRecus == -1)
		{
			close(hSockServ);
			exit(1);
		}
		else tailleMsgRecu += nbreBytesRecus;
		printf("Taille msg = %d et nbreBytes = %d\n", tailleMsgRecu, nbreBytesRecus);
	}
	while(nbreBytesRecus != 0 && nbreBytesRecus != -1 && tailleMsgRecu < LONG_STRUCT_CON);
	
	affSousProcess(vr, "Recv socket OK");
	
	sprintf(buf, "Demande recue pour le client = %s\n", msgContainer->a);
	affSousProcess(vr, buf);
	
	// 2. 
	sprintf(msgServeur, "Demande recue du client %s !!!", msgContainer->a);
	affSousProcess(vr, msgServeur);
	afficheRequete(msgContainer);
	
	sendMsg(hSocketServ, msgServeur, LONG_MSG_SERV);
	
	affSousProcess(vr, "--fin du sous-process--");
}
*/
