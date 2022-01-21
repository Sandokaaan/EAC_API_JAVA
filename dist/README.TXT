Requirements:
- A fully sync EarthCoin v.2.1.1 node (or higher) with <code>txindex=1</code> in the config file and enabled rpc calls
- Java runtime environment v.8.0 or higher
- Approx. 5 GB disk space for blockchain data
- A properly configure MySQL server

To run this binary distribution use steps as follows:
- Set-up parameters in the api.conf file, especially userbame and password for your RPC node.
- Run the API using command
    java -cp API.jar:lib/*:. main.Api
- Wait for a few hours to get sync

To safelly stop API use command "exit" in the API log console.
