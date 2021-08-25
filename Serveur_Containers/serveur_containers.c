#include "libnet.h"
#include <pthread.h>

#define NB_MAX_SOCKETS 5
#define NB_MAX_CLIENTS 10
#define NB_DESC_DEF 4

#define PORT 70000
#define PMOUV 53000

#define MAXSTRING 100
#define LONG_MSG_SERV 5000
#define LONG_MSG_CLI 5000
#define EOC "INPUT-DONE  KO"
#define DOC "DENY_OF_CONNEXION"
#define SEPARATEUR "  "
#define TERMINATEUR "\r\n"

#define affThread(num, msg) printf("th_%s> %s\n", num, msg)

typedef struct sContainer
{
	unsigned int x;
	unsigned int y;
	char identifiant[8];
	unsigned short etat;
	char dateReservation[11];
	char dateArrivee[11];
	unsigned int poids;
	char destination[MAXSTRING];
	char typeTransport;
}
container;

pthread_mutex_t mutexLogin;
pthread_mutex_t mutexFichParc;
pthread_mutex_t mutexIndiceCourant;
pthread_cond_t condIndiceCourant;
pthread_t threadHandle[NB_MAX_CLIENTS]; /* Threads pour clients*/
void *fctThread(void *param);
char *getThreadIdentity();
int indiceCourant = -1;
int hSocketConnectee[NB_MAX_CLIENTS]; /* Sockets pour clients*/
int hSocketMouv;

int main()
{
	int hSocketEcoute,
		hSocketService;
	int i, j;
	struct hostent *infosHost;
	struct in_addr adresseIP;
	struct sockaddr_in adresseSocket;
	struct in_addr adresseIPMouv;
	struct sockaddr_in adresseSocketMouv;
	int ret;
	char msgServeur[LONG_MSG_SERV];
	
	puts("* Thread principal serveur demarre *");
	printf("identite = %s\n", getThreadIdentity());
	
	FILE *f = fopen("FICH_PARC", "wb");
	if(f == NULL)
		return 1;
	
	container *c = (container *)malloc(sizeof(container));
	
	c->x = 1;
	c->y = 1;
	c->etat = 0;
	
	fwrite(c, sizeof(container), 1, f);
	
	c->x = 2;
	c->y = 1;
	c->etat = 0;
	
	fwrite(c, sizeof(container), 1, f);
	
	c->x = 1;
	c->y = 2;
	c->etat = 0;
	
	fwrite(c, sizeof(container), 1, f);
	
	c->x = 2;
	c->y = 2;
	c->etat = 0;
	
	fwrite(c, sizeof(container), 1, f);
	
	fclose(f);
	
	pthread_mutex_init(&mutexLogin, NULL);
	pthread_mutex_init(&mutexFichParc, NULL);
	pthread_mutex_init(&mutexIndiceCourant, NULL);
	pthread_cond_init(&condIndiceCourant, NULL);
	
	/* Si la socket n'est pas utilisee, le descripteur est a -1 */
	for (i=0; i<NB_MAX_CLIENTS; i++)
		hSocketConnectee[i] = -1;
	
	char hostname[1024];
	hostname[1023] = '\0';
	gethostname(hostname, 1023);
	printf("Hostname: %s\n", hostname);
	
	infosHost = getInfosHost(hostname);
	memcpy(&adresseIP, infosHost->h_addr, infosHost->h_length);
	printf("Adresse IP = %s\n", inet_ntoa(adresseIP));
	
	/* 3. */
	memset(&adresseSocket, 0, sizeof(struct sockaddr_in));
	adresseSocket.sin_family = AF_INET;
	adresseSocket.sin_port = htons(PORT);
	memcpy(&adresseSocket.sin_addr, infosHost->h_addr, infosHost->h_length);
	
	/* 4. */
	hSocketEcoute = createSocket(&adresseSocket);
	
	/* 3. */
	memset(&adresseSocketMouv, 0, sizeof(struct sockaddr_in));
	adresseSocketMouv.sin_family = AF_INET;
	adresseSocketMouv.sin_port = htons(PMOUV);
	memcpy(&adresseSocketMouv.sin_addr, infosHost->h_addr, infosHost->h_length);
	
	/* 4. */
	hSocketMouv = connectSocket(&adresseSocketMouv);

	for (i = 0; i < NB_MAX_CLIENTS; i++)
	{
		ret = pthread_create(&threadHandle[i], NULL, fctThread, (void*)i);
		printf("Thread secondaire %d lance !\n", i);
		ret = pthread_detach(threadHandle[i]);
	}
	
	do
	{
		/* 5. */
		puts("Process principal : en attente d'une connexion");
		hSocketService = listenSocket(hSocketEcoute, &adresseSocket);
		if(hSocketService == -1)
			continue;
		
		printf("Socket d'ecoute = %d\n", hSocketEcoute);
		printf("Socket de service attribuee = %d\n", hSocketService);
		
		printf("Recherche d'une socket connectee libre ...\n");
		for(j = 0; j < NB_MAX_CLIENTS && hSocketConnectee[j] != -1; j++);
		
		if(j == NB_MAX_CLIENTS)
		{
			printf("Plus de connexion disponible\n");
			sprintf(msgServeur, DOC);
			sendGrosMsg(hSocketService, msgServeur, LONG_MSG_SERV);
			close(hSocketService);
		}
		else
		{
			printf("socket = %d\n", hSocketConnectee[j]);
			printf("Connexion sur la socket num. %d\n", j);
			pthread_mutex_lock(&mutexIndiceCourant);
			hSocketConnectee[j] = hSocketService;
			indiceCourant = j;
			pthread_mutex_unlock(&mutexIndiceCourant);
			pthread_cond_signal(&condIndiceCourant);
		}
	}
	while (1);
	
	close(hSocketEcoute);
	printf("Socket serveur fermee\n");
	
	puts("Fin du thread principal");
	return 0;
}

void *fctThread(void *param)
{
	char *buf = (char*)malloc(MAXSTRING), *numThr = getThreadIdentity();
	char msgServeur[LONG_MSG_SERV];
	char msgClient[LONG_MSG_CLI];
	char msgMouv[LONG_MSG_SERV];
	int vr = (int)(param), iCliTraite, retRecv, etat = 0, hSocketServ;
	char idTransport[MAXSTRING], idContainer[MAXSTRING], username[MAXSTRING], password[MAXSTRING];
	char *commande, *destination, *capMax, *nbContainers;
	
	while(1)
	{
		/* 1. Attente d'un client à traiter */
		pthread_mutex_lock(&mutexIndiceCourant);
		while (indiceCourant == -1)
			pthread_cond_wait(&condIndiceCourant, &mutexIndiceCourant);
		iCliTraite = indiceCourant;
		indiceCourant = -1;
		hSocketServ = hSocketConnectee[iCliTraite];
		pthread_mutex_unlock(&mutexIndiceCourant);
		sprintf(buf,"Je m'occupe du numero %d ...", iCliTraite);
		affThread(numThr, buf);
		/* 2. */
		while(etat != -1)
		{
			retRecv = recvGrosMsg(hSocketServ, msgClient, MAXSTRING);
			if(!retRecv)
			{
				sprintf(buf,"Le client est parti !!!");
				affThread(numThr, buf);
				etat = -1;
				break;
			}
			
			sprintf(msgServeur, "ACK pour votre message : <%s>", msgClient);
			strcat(msgServeur, TERMINATEUR);
			
			if(send(hSocketServ, msgServeur, strlen(msgServeur), 0) == -1)
			{
				printf("Erreur sur le send de la socket %d\n", errno);
				close(socket);
				exit(1);
			}
			else printf("Send socket OK\n");
			
			if (strcmp(msgClient, EOC)==0)
			{
				etat = -1;
				break;
			}
			
			commande = strtok(msgClient, SEPARATEUR);
			
			sprintf(msgServeur, "non: commande inconnue");
			
			if(!strcmp(commande, "LOGIN"))
			{
				if(etat == 0)
				{
					strncpy(username, strtok(NULL, SEPARATEUR), (MAXSTRING - 1) * sizeof(char));
					username[MAXSTRING - 1] = 0;
					strncpy(password, strtok(NULL, SEPARATEUR), (MAXSTRING - 1) * sizeof(char));
					password[MAXSTRING - 1] = 0;
					
					if(password != NULL && username != NULL)
					{
						printf("Login = %s ; %s\n", username, password);
					
						pthread_mutex_lock(&mutexLogin);
						FILE *f = fopen("login.csv", "r");
						if(f == NULL)
							close(hSocketServ);
						
						char *u, *p;
						int loginCheck = 1;
						
						while(fgets(buf, MAXSTRING, f) != NULL && loginCheck)
						{
							u = strtok(buf, ";");
							if(u == NULL)
								close(hSocketServ);
							
							p = strtok(NULL, ";");
							if(p == NULL)
								close(hSocketServ);
							
							p[strlen(p) - 1] = 0; 
							
							loginCheck = (strcmp(username, u) || strcmp(password, p));
						}
						
						if(!loginCheck)
						{
							sprintf(msgServeur, "oui");
							etat = 1;
						}
						else
							sprintf(msgServeur, "non: nom d'utilisateur ou mot de passe incorrect");
						
						fclose(f);

						sprintf(msgMouv, "LOGIN_CONT::john  doe");
						strcat(msgMouv, TERMINATEUR);

						if(send(hSocketMouv, msgMouv, strlen(msgMouv), 0) == -1)
						{
							printf("Erreur sur le send de la socket %d\n", errno);
							close(hSocketMouv);
							exit(1);
						}
						else printf("Send socket OK\n");

						retRecv = recvGrosMsg(hSocketMouv, msgServeur, MAXSTRING);
						if(!retRecv)
						{
							sprintf(buf,"Le client est parti !!!");
							affThread(numThr, buf);
							etat = -1;
							break;
						}

						pthread_mutex_unlock(&mutexLogin);
					}
					else
						sprintf(msgServeur, "non: commande malformee");
				}
				else
					sprintf(msgServeur, "non: vous devez vous deconnecter d'abord");
			}
			else
				sprintf(msgServeur, "non: vous devez vous connecter d'abord");
			
			if(etat)
			{
				if(!strcmp(commande, "INPUT-TRUCK"))
				{
					if(etat == 1)
					{
						char *idT = strtok(NULL, SEPARATEUR);
						char *idC = strtok(NULL, SEPARATEUR);
						
						if(idT != NULL && idC != NULL)
						{
							strncpy(idTransport, idT, (MAXSTRING - 1) * sizeof(char));
							idTransport[MAXSTRING - 1] = 0;
							strncpy(idContainer, idC, (MAXSTRING - 1) * sizeof(char));
							idContainer[MAXSTRING - 1] = 0;
							
							pthread_mutex_lock(&mutexFichParc);
							
							sprintf(msgMouv, "GET_XY::");
							strcat(msgMouv, idTransport);
							strcat(msgMouv, SEPARATEUR);
							strcat(msgMouv, idContainer);
							strcat(msgMouv, TERMINATEUR);
							
							if(send(hSocketMouv, msgMouv, strlen(msgMouv), 0) == -1)
							{
								printf("Erreur sur le send de la socket %d\n", errno);
								close(hSocketMouv);
								exit(1);
							}
							else printf("Send socket OK\n");

							retRecv = recvGrosMsg(hSocketMouv, msgServeur, MAXSTRING);
							if(!retRecv)
							{
								sprintf(buf,"Le client est parti !!!");
								affThread(numThr, buf);
								etat = -1;
								break;
							}

							// FILE *f = fopen("FICH_PARC", "r+b");
							// if(f == NULL)
							// 	close(hSocketServ);
							
							// container *c = (container *)malloc(sizeof(container));
							
							// c->etat = 2;
							
							// while(!feof(f) && c->etat)
							// 	fread(c, sizeof(container), 1, f);
							
							// if(!c->etat)
							// {
							// 	strncpy(c->identifiant, idContainer, (MAXSTRING - 1) * sizeof(char));
							// 	c->identifiant[MAXSTRING - 1] = 0;
							// 	c->etat = 1;
							// 	strcpy(c->dateReservation, "");
							// 	strcpy(c->dateArrivee, "");
							// 	c->poids = 0;
							// 	strcpy(c->destination, "Test");
							// 	c->typeTransport = 'b';
								
							// 	fseek(f, -sizeof(container), SEEK_CUR);
							// 	fwrite(c, sizeof(container), 1, f);
								
							// 	puts(idContainer);
							//	sprintf(msgServeur, "oui: %u, %u", c->x, c->y);
							etat = 2;
							// }
							// else
							// 	sprintf(msgServeur, "non: pas d'emplacements libres");
							
							// fclose(f);
							pthread_mutex_unlock(&mutexFichParc);
						}
						else
							sprintf(msgServeur, "non: commande malformee");
					}
					else
						sprintf(msgServeur, "non: vous devez finir l'operation en cours d'abord");
				}
				
				if(!strcmp(commande, "INPUT-DONE"))
				{
					if(etat == 2)
					{
						char *param = strtok(NULL, SEPARATEUR);
						
						if(!strcmp(param, "OK"))
						{
							char *poids = strtok(NULL, SEPARATEUR);
							
							if(poids != NULL)
							{
								pthread_mutex_lock(&mutexFichParc);
								
								sprintf(msgMouv, "SEND_WEIGHT::");
								strcat(msgMouv, idContainer);
								strcat(msgMouv, SEPARATEUR);
								strcat(msgMouv, poids);
								strcat(msgMouv, TERMINATEUR);
								
								if(send(hSocketMouv, msgMouv, strlen(msgMouv), 0) == -1)
								{
									printf("Erreur sur le send de la socket %d\n", errno);
									close(hSocketMouv);
									exit(1);
								}
								else printf("Send socket OK\n");

								retRecv = recvGrosMsg(hSocketMouv, msgServeur, MAXSTRING);
								if(!retRecv)
								{
									sprintf(buf,"Le client est parti !!!");
									affThread(numThr, buf);
									etat = -1;
									break;
								}
								// FILE *f = fopen("FICH_PARC", "r+b");
								// if(f == NULL)
								// 	close(hSocketServ);
								
								// container *c = (container *)malloc(sizeof(container));
								
								// c->identifiant[0] = 0;
								
								// while(!feof(f) && strcmp(c->identifiant, idContainer))
								// 	fread(c, sizeof(container), 1, f);
								
								// if(!strcmp(c->identifiant, idContainer))
								// {
								// 	sscanf(poids, "%u", &(c->poids));
									
								// 	fseek(f, -sizeof(container), SEEK_CUR);
								// 	fwrite(c, sizeof(container), 1, f);
									
								//  sprintf(msgServeur, "oui");
								etat = 1;
								// }
								// else
								// 	sprintf(msgServeur, "non: erreur dans la recherche du container");
								
								// fclose(f);
								pthread_mutex_unlock(&mutexFichParc);
							}
							else
								sprintf(msgServeur, "non: commande malformee");
						}
						else
							sprintf(msgServeur, "non: commande malformee");
					}
					else
						sprintf(msgServeur, "non: vous devez executer INPUT-TRUCK d'abord");
				}
				
				if(!strcmp(commande, "OUTPUT-READY"))
				{
					if(etat == 1)
					{
						char *idT = strtok(NULL, SEPARATEUR);
						destination = strtok(NULL, SEPARATEUR);
						
						if(idT != NULL && destination != NULL)
						{
							strncpy(idTransport, idT, (MAXSTRING - 1) * sizeof(char));
							idTransport[MAXSTRING - 1] = 0;
							
							pthread_mutex_lock(&mutexFichParc);
							
							sprintf(msgMouv, "GET_LIST::");
							strcat(msgMouv, destination);
							strcat(msgMouv, TERMINATEUR);
							
							if(send(hSocketMouv, msgMouv, strlen(msgMouv), 0) == -1)
							{
								printf("Erreur sur le send de la socket %d\n", errno);
								close(hSocketMouv);
								exit(1);
							}
							else printf("Send socket OK\n");

							retRecv = recvGrosMsg(hSocketMouv, msgServeur, MAXSTRING);
							if(!retRecv)
							{
								sprintf(buf,"Le client est parti !!!");
								affThread(numThr, buf);
								etat = -1;
								break;
							}

							sprintf(msgMouv, "SIGNAL_DEP::");
							strcat(msgMouv, idTransport);


							// FILE *f = fopen("FICH_PARC", "rb");
							// if(f == NULL)
							// 	close(hSocketServ);
							
							// container *c = (container *)malloc(sizeof(container));
							
							// sprintf(msgServeur, "oui: ");
							
							// int containerTrouve = 0;
							
							// while(!feof(f))
							// {
							// 	fread(c, sizeof(container), 1, f);
								
							// 	if(c->etat && !strcmp(c->destination, destination))
							// 	{
							// 		containerTrouve = 1;
							// 		strcat(msgServeur, c->identifiant);
							// 		strcat(msgServeur, SEPARATEUR);
							// 	}
							// }
							// fclose(f);
							pthread_mutex_unlock(&mutexFichParc);
							
							// if(containerTrouve)
							// {
							// 	msgServeur[strlen(msgServeur) - 1] = 0;
							etat = 3;
							// }
							// else
							// 	sprintf(msgServeur, "non: aucun container trouve");
						}
						else
						{
							sprintf(msgServeur, "non: commande malformee");
							affThread(numThr, msgServeur);
							sendGrosMsg(hSocketServ, msgServeur, LONG_MSG_SERV);
							continue;
						}
						
						
					}
					else
						sprintf(msgServeur, "non: vous devez finir l'operation en cours d'abord");
				}
				
				if(!strcmp(commande, "OUTPUT-ONE"))
				{
					if(etat == 3 || etat == 4)
					{
						char *idC = strtok(NULL, SEPARATEUR);
						
						if(idC != NULL)
						{
							strncpy(idContainer, idC, (MAXSTRING - 1) * sizeof(char));
							idContainer[MAXSTRING - 1] = 0;
							
							pthread_mutex_lock(&mutexFichParc);
							strcat(msgMouv, SEPARATEUR);
							strcat(msgMouv, idContainer);
							
							// FILE *f = fopen("FICH_PARC", "r+b");
							// if(f == NULL)
							// 	close(hSocketServ);
							
							// container *c = (container *)malloc(sizeof(container));
							
							// c->identifiant[0] = 0;
							
							// while(!c->etat || (!feof(f) && strcmp(c->identifiant, idC)))
							// 	fread(c, sizeof(container), 1, f);
							
							// if(c->etat && !strcmp(c->identifiant, idC))
							// {
							// 	c->etat = 0;
								
							// 	fseek(f, -sizeof(container), SEEK_CUR);
							// 	fwrite(c, sizeof(container), 1, f);
								
							sprintf(msgServeur, "oui");
							etat = 4;
							// }
							// else
							// 	sprintf(msgServeur, "non: container inconnu");
							
							// fclose(f);
							pthread_mutex_unlock(&mutexFichParc);
						}
						else
							sprintf(msgServeur, "non: commande malformee");
					}
					else
						sprintf(msgServeur, "non: vous devez executer OUTPUT-READY d'abord");
				}
				
				if(!strcmp(commande, "OUTPUT-DONE"))
				{
					if(etat == 4)
					{
						strcat(msgMouv, TERMINATEUR);

						if(send(hSocketMouv, msgMouv, strlen(msgMouv), 0) == -1)
						{
							printf("Erreur sur le send de la socket %d\n", errno);
							close(hSocketMouv);
							exit(1);
						}
						else printf("Send socket OK\n");

						retRecv = recvGrosMsg(hSocketMouv, msgServeur, MAXSTRING);
						if(!retRecv)
						{
							sprintf(buf,"Le client est parti !!!");
							affThread(numThr, buf);
							etat = -1;
							break;
						}

						// char *idT = strtok(NULL, SEPARATEUR);
						// nbContainers = strtok(NULL, SEPARATEUR);
						
						// if(idT != NULL && nbContainers != NULL)
						// {
						// 	strncpy(idTransport, idT, (MAXSTRING - 1) * sizeof(char));
						// 	idTransport[MAXSTRING - 1] = 0;
							
						// 	unsigned int n, max;
							
						// 	sscanf(nbContainers, "%u", &n);
						// 	sscanf(capMax, "%u", &max);
							
						// 	if(n == max)
						// 	{
						// 		sprintf(msgServeur, "oui");
						// 		etat = 1;
						// 	}
						// 	else
						// 		sprintf(msgServeur, "non: incoherence detectee");
						// }
						// else
						// 	sprintf(msgServeur, "non: commande malformee");
					}
					else
						sprintf(msgServeur, "non: vous devez executer OUTPUT-ONE d'abord");
				}
				
				if(!strcmp(commande, "LOGOUT"))
				{
					if(etat == 1)
					{
						char *u = strtok(NULL, SEPARATEUR);
						char *p = strtok(NULL, SEPARATEUR);
						
						if(p != NULL && u != NULL)
						{
							printf("Logout = %s ; %s\n", username, password);
						
							if(!strcmp(username, u) && !strcmp(password, p))
							{
								sprintf(msgServeur, "oui: session fermee");
								etat = 0;
							}
							else
								sprintf(msgServeur, "non: nom d'utilisateur ou mot de passe incorrect");
						}
						else
							sprintf(msgServeur, "non: commande malformee");
					}
					else
						sprintf(msgServeur, "non: vous devez finir l'operation en cours d'abord");
				}
			}
			
			affThread(numThr, msgServeur);
			
			strcat(msgServeur, TERMINATEUR);
			
			if(send(hSocketServ, msgServeur, strlen(msgServeur), 0) == -1)
			{
				printf("Erreur sur le send de la socket %d\n", errno);
				close(hSocketServ);
				exit(1);
			}
			else printf("Send socket OK\n");
		}
		pthread_mutex_lock(&mutexIndiceCourant);
		hSocketConnectee[iCliTraite] = -1;
		pthread_mutex_unlock(&mutexIndiceCourant);
	}
	affThread(numThr, "--fin du sous-process--");
	close(hSocketServ);
	return (void *)vr;
}

char *getThreadIdentity()
{
	char *buf = (char*)malloc(MAXSTRING);
	sprintf(buf, "%d.%lu", getpid(), pthread_self());
	return buf;
}
