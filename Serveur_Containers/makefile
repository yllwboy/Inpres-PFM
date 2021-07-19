CC=gcc
LD=gcc
#CFLAGS=--std=c99 --pedantic -Wall -W -Wmissing-prototypes
CFLAGS=-Wall -pthread
LDFLAGS=-Wall -pthread
EXEC=serveur_containers client_containers

all:$(EXEC)

serveur_containers: serveur_containers.o libnet.o
	$(LD) -o serveur_containers serveur_containers.o libnet.o $(LDFLAGS)

client_containers: client_containers.o libnet.o
	$(LD) -o client_containers client_containers.o libnet.o $(LDFLAGS)

serveur_containers.o: serveur_containers.c libnet.o
	$(CC) -c serveur_containers.c -o serveur_containers.o $(CFLAGS)

client_containers.o: client_containers.c libnet.o
	$(CC) -c client_containers.c -o client_containers.o $(CFLAGS)

libnet.o: libnet.c libnet.h
	$(CC) -c libnet.c -o libnet.o $(CFLAGS)

clean:
	rm -f *.o
	rm -f serveur_containers
	rm -f client_containers
	clear
