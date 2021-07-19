#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main()
{
	FILE *f;
	
	f = fopen("login.csv","a");
	
	fputs("john;doe\n", f);
	
	fclose(f);
	
	return 0;
}
