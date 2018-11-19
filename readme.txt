How to run

Compile both the client and server
javac -cp ".:stellar-sdk.jar" *.java FlipApp/*.java

Run the orbd for Corba
orbd -ORBInitialPort 1050&

Run the server in the background
java -cp ".:stellar-sdk.jar" FlipServer -ORBInitialPort 1050 -ORBInitialHost localhost&

Run the client
java -cp ".:stellar-sdk.jar" FlipClient -ORBInitialPort 1050 -ORBInitialHost localhost