# EAC_API_JAVA
A simple block explorer &amp; API node for EarthCoin (EAC) written in java

Requirements:
- A fully sync EarthCoin v.2.0 node with <code>txindex=1</code> in the config file and enabled rpc calls
- Java runtime environment v.8.0 or higher
- Approx. 10 GB disk space for blockchain data

Advantages:
- Platform independent code. Can be operated at any operating system supporting Java.
- No external database is required. All data are obtained from the coin node through the rpc calls.
- Multithread implementation to avoid a response stuck.

Instalation:
1. Get and compile the EarthCoin v.2.0 wallet. Follow the instructions at https://github.com/Sandokaaan/EarthCoin2019/releases
2. Edit the config file of EarthCoin, make sure to add lines:
<code> txindex=1</code><br>
<code>rpcuser=YOUR_RPC_USERNAME</code><br>
<code>rpcpassword=YOUR_RPC_PASSWORD</code><br>
<code>rpcport=YOUR_RPC_PORT_NUMBER</code><br>
3. Run the EarthCoin v.2.0 wallet and let it sync.
4. Install/update the Java runtime environment, see https://www.java.com/ 
5. Get the code from this repository: git clone https://github.com/Sandokaaan/EAC_API_JAVA.git
6. Optionnaly, you can build the code with your java compiller (Netbeans, Eclipse, javac).
7. Open the folder "EAC_API_JAVA/dist" and run the code: <code>java -jar ApiExtension.jar</code>
8. Stop the program started in the stage 7, locate the file "api.conf" and modify the lines <code>rpcuser, rpcpassword and rpcport</code> to be the same as in wallet config file - see stage 2. Also se the port number for the API server.
9. Start code <code>java -jar ApiExtension.jar</code> again and let it run on a background.

HTTPS/SSL configuration (optional):<br>
10. Obtain a valid SSL certificate for the domain name of your server (for a free certificate see e.g. https://github.com/diafygi/acme-tiny).<br>
11. Use <code> keytool </code> to import your SSL certificate to a <code> *.jks </code> keystore file:
<code>
keytool -importkeystore -deststorepass PASSWORD -destkeypass PASSWORD -destkeystore ./KEYSTOREFILE.jks -deststoretype pkcs12 -srckeystore SERVERCERTIFICATE.p12 -srcstoretype pkcs12 -srcstorepass PASSWORD -alias CERTIFICATENAME
</code><br>
Do not forget to change capitalized words to your password and appropriate file names.<br>
12. Move the keystore file to a secure PATH<br>
13. Open the API config file <code> api.conf </code>. Set variables:<br>
<code> usessl=1 </code><br>
<code> sslcertpath=PATH  </code><br>
<code> sslcertpassword=PASSWORD  </code><br>
Where change PATH with the full or relative path to your keystore file from the step 12, and PASSWORD with the password used in the step 11.<br>
14. Restart the API.<br>

A demo server with SSL-protocol enabled:
https://blocks.deveac.com:3000/
