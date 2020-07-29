This is a basic banking system for credit cards, just console based at the moment:
	It generate random credit cards numbers for the 6 first digits they're set to 400000 as they're for Industry ID and Issuer ID so for the 9 digits Account numbers they're randomly generated then we get the Checksum using Luhn Algorithm.
	And for the pin it's a 4 digit randomly generated combination.
	The code is linked to a SQLite3 db to store the data, about credit cards and pin linked to them.
	
	When disconnected :	
		1. creates a new bank account.
		2. log in.
		
	When connected :
		1. Checking account balance
		2. Add money to your account
		3. Transfer money
		4. Close account
		5. Log out
	
	0. Exit