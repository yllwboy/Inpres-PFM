
//int x = 1, y = 1;
					
					c->etat = 2;
					
					//while(fgets(buf, MAXSTRING, f) != NULL && c->etat)
					do
					{
						fread(c, sizeof(container), 1, f);
						//sscanf(buf, "%u;%u;%s;%hu;%s;%s;%u;%s;%c",
						//			&(c->x), &(c->y), &(c->identifiant), &(c->etat), &(c->dateReservation),
						//			&(c->dateArrivee), &(c->poids), &(c->destination), &(c->typeTransport));
						//x++;
					}
					while(!feof(f) && c->etat);
					
					//c->x = x;
					//c->y = y;



					//sprintf(buf, "%u;%u;%s;%hu;%s;%s;%u;%s;%c\n",
					//			c->x, c->y, c->identifiant, c->etat, c->dateReservation,
					//			c->dateArrivee, c->poids, c->destination, c->typeTransport);
					
					/*c->etat = 2;
					
					while(fgets(buf, MAXSTRING, f) != NULL && c->etat)
						sscanf(buf, "%u;%u;%s;%hu;%s;%s;%u;%s;%c;",
									&(c->x), &(c->y), &(c->identifiant), &(c->etat), &(c->dateReservation),
									&(c->dateArrivee), &(c->poids), &(c->destination), &(c->typeTransport));
					
					if(!c->etat)
					{
						c->
						
						sprintf(buf, "%u;%u;%s;%hu;%s;%s;%u;%s;%c;",
									c->x, c->y, c->identifiant, c->etat, c->dateReservation,
									c->dateArrivee, c->poids, c->destination, c->typeTransport);
						
						size_t prevlen = strlen(buf);
						memset(buf + prevlen, ' ', MAXSTRING-1 - prevlen);
						buf[MAXSTRING-2] = '\n';
						buf[MAXSTRING-1] = '\0';
						fseek(f, -prevlen, SEEK_CUR);
						fputs(buf, f);
					}*/


{
	
	
	char hostname[1024];
	hostname[1023] = '\0';
	gethostname(hostname, 1023);
	printf("Hostname: %s\n", hostname);
	
	nd = setupNetData(hostname);
	
	if(nd == NULL)
		return -1;
	
	
	
	
	/* 4. */
	if(bind(nd->hSocketEcoute, (struct sockaddr*)&(nd->adresseSocket), sizeof(struct sockaddr_in)) == -1)
	{
		printf("Erreur sur le bind de la socket %d\n", errno);
		exit(1);
	}
	else printf("Bind adresse et port socket OK\n");
	
	/* 5. */
	if(listen(nd->hSocketEcoute, SOMAXCONN) == -1)
	{
		printf("Erreur sur le listen de la socket %d\n", errno);
		close(nd->hSocketEcoute);
		exit(1);
	}
	else printf("Listen socket OK\n");
	
	/* 6. */
	nd->tailleSockaddr_in = sizeof(struct sockaddr_in);
	if((nd->hSocketService = accept(nd->hSocketEcoute, (struct sockaddr*)&(nd->adresseSocket), &(nd->tailleSockaddr_in))) == -1)
	{
		printf("Erreur sur l'accept de la socket %d\n", errno);
		close(nd->hSocketEcoute);
		exit(1);
	}
	else printf("Accept socket OK\n");
	
	struct sigaction act;
	act.sa_handler = handlerSigint;
	sigemptyset(&act.sa_mask);
	act.sa_flags = 0;
	sigaction(SIGINT, &act, 0);
	
	/* 9. */
	while(alive)
	{
		if(recvMsg(nd->hSocketService, msgClient) == -1)
		{
			close(nd->hSocketEcoute);
			close(nd->hSocketService);
			exit(1);
		}
		sprintf(msgServeur, "ACK pour votre message : <%s>", msgClient);
		if(sendMsg(nd->hSocketService, msgServeur) == -1)
		{
			close(nd->hSocketEcoute);
			close(nd->hSocketService);
			exit(1);
		}
		
		commande = strtok(msgClient, SEPARATEUR);
		
		sprintf(msgServeur, "non: commande inconnue");
		
		if(!strcmp(commande, "LOGIN"))
		{
			char *username = strtok(NULL, SEPARATEUR);
			char *password = strtok(NULL, SEPARATEUR);
			printf("Login = %s ; %s\n", username, password);
			
			sprintf(msgServeur, "oui");
			loggedin = 1;
		}
		else sprintf(msgServeur, "non: vous devez vous connecter d'abord");
		
		if(loggedin)
		{
			if(!strcmp(commande, "INPUT-TRUCK"))
			{
				sprintf(msgServeur, "oui");
			}
			
			if(!strcmp(commande, "INPUT-DONE"))
			{
				char *param = strtok(NULL, SEPARATEUR);
				
				if(!strcmp(param, "OK"))
				{
					sprintf(msgServeur, "oui");
				}
				else if(!strcmp(param, "KO"))
				{
					sprintf(msgServeur, "oui");
					alive = 0;
				}
			}
			
			if(!strcmp(commande, "OUTPUT-READY"))
			{
				sprintf(msgServeur, "oui");
			}
			
			if(!strcmp(commande, "OUTPUT-ONE"))
			{
				sprintf(msgServeur, "oui");
			}
			
			if(!strcmp(commande, "OUTPUT-DONE"))
			{
				sprintf(msgServeur, "oui");
			}
			
			if(!strcmp(commande, "LOGOUT"))
			{
				char *username = strtok(NULL, SEPARATEUR);
				char *password = strtok(NULL, SEPARATEUR);
				printf("Logout = %s ; %s\n", username, password);
				
				sprintf(msgServeur, "oui: session fermee");
				loggedin = 0;
			}
		}
		
		if(sendMsg(nd->hSocketService, msgServeur) == -1)
		{
			close(nd->hSocketEcoute);
			close(nd->hSocketService);
			exit(1);
		}
	}
	
	/* 11. */
	close(nd->hSocketService);
	printf("Socket connectee au client fermee\n");
	close(nd->hSocketEcoute);
	printf("Socket serveur fermee\n");
	
	return 0;
}

void handlerSigint(int sig)
{
	close(nd->hSocketService);
	printf("Socket connectee au client fermee\n");
	close(nd->hSocketEcoute);
	printf("Socket serveur fermee\n");
	exit(0);
}

