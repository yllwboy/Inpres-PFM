#include "libnet.h"
#include <signal.h>

#define PORT 70000

#define MAXSTRING 100
#define LONG_MSG_SERV 5000
#define LONG_MSG_CLI 5000
#define EOC "INPUT-DONE  KO"
#define DOC "DENY_OF_CONNEXION"
#define SEPARATEUR "  "
#define TERMINATEUR "\r\n"

int hSocket;
struct hostent *infosHost;
struct in_addr adresseIP;
struct sockaddr_in adresseSocket;
int choix;

char msgClient[LONG_MSG_CLI], msgServeur[LONG_MSG_SERV], buf[MAXSTRING];
char *commande;

void handlerSigint(int sig);

int main()
{
	puts("Process pere client demarre");
	printf("identite = %d\n", getpid());
	
	char hostname[1024];
	hostname[1023] = '\0';
	gethostname(hostname, 1023);
	printf("Hostname: %s\n", hostname);
	
	infosHost = getInfosHost(hostname);
	memcpy(&adresseIP, infosHost->h_addr, infosHost->h_length);
	printf("Adresse IP = %s\n", inet_ntoa(adresseIP));
	
	memset(&adresseSocket, 0, sizeof(struct sockaddr_in));
	adresseSocket.sin_family = AF_INET;
	adresseSocket.sin_port = htons(PORT);
	memcpy(&adresseSocket.sin_addr, infosHost->h_addr, infosHost->h_length);
	
	hSocket = connectSocket(&adresseSocket);
	
	struct sigaction act;
	act.sa_handler = handlerSigint;
	sigemptyset(&act.sa_mask);
	act.sa_flags = 0;
	sigaction(SIGINT, &act, 0);
	
	do
	{
		printf("== COMMANDES ==\n\n1. LOGIN\n");
		printf("2. INPUT-TRUCK\n3. INPUT-DONE\n");
		printf("4. OUTPUT-READY\n5. OUTPUT-ONE\n6. OUTPUT-DONE\n");
		printf("7. LOGOUT\n8. Quitter\n\n[1-8]? ");
		scanf("%d", &choix);
		
		switch(choix)
		{
			case 1:
				sprintf(msgClient, "LOGIN");
				strcat(msgClient, SEPARATEUR);
				printf("\nNom: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Mot de passe: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 2:
				sprintf(msgClient, "INPUT-TRUCK");
				strcat(msgClient, SEPARATEUR);
				printf("\nNuméro d'immatriculation du camion: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Identifiant du container: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 3:
				sprintf(msgClient, "INPUT-DONE");
				strcat(msgClient, SEPARATEUR);
				strcat(msgClient, "OK");
				strcat(msgClient, SEPARATEUR);
				printf("\nPoids du container: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 4:
				sprintf(msgClient, "OUTPUT-READY");
				strcat(msgClient, SEPARATEUR);
				printf("\nIdentifiant train ou bateau: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Destination: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Capacité maximale: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 5:
				sprintf(msgClient, "OUTPUT-ONE");
				strcat(msgClient, SEPARATEUR);
				printf("\nIdentifiant du container: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 6:
				sprintf(msgClient, "OUTPUT-DONE");
				strcat(msgClient, SEPARATEUR);
				printf("\nIdentifiant train ou bateau: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Nombre de containers chargé: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 7:
				sprintf(msgClient, "LOGOUT");
				strcat(msgClient, SEPARATEUR);
				printf("\nNom: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				strcat(msgClient, SEPARATEUR);
				printf("Mot de passe: ");
				scanf("%s", buf);
				strcat(msgClient, buf);
				break;
			case 8:
				sprintf(msgClient, EOC);
				break;
			default:
				choix = -1;
		}
		if(choix == -1)
			continue;
		
		strcat(msgClient, TERMINATEUR);
		
		if(send(hSocket, msgClient, strlen(msgClient), 0) == -1)
		{
			printf("Erreur sur le send de la socket %d\n", errno);
			close(socket);
			exit(1);
		}
		else printf("Send socket OK\n");
		
		msgClient[strlen(msgClient) - strlen(TERMINATEUR)] = 0;
		
		recvGrosMsg(hSocket, msgServeur, MAXSTRING);
		if(strcmp(msgClient, EOC) && strcmp(msgServeur, DOC))
			recvGrosMsg(hSocket, msgServeur, MAXSTRING);
	}
	while(strcmp(msgClient, EOC) && strcmp(msgServeur, DOC));
	
	close(hSocket);
	printf("Socket connectee au serveur fermee\n");
	
	return 0;
}

void handlerSigint(int sig)
{
	close(hSocket);
	printf("Socket connectee au serveur fermee\n");
	exit(0);
}
