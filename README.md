# Coin API coded in Java
A simple block explorer &amp; API node for EarthCoin (EAC) written in java

Requirements:
- A fully sync EarthCoin v.2.0 node with <code>txindex=1</code> in the config file and enabled rpc calls
- Java runtime environment v.8.0 or higher
- Approx. 50 GB disk space for blockchain data and database operations

Advantages:
- Platform independent code. Can be operated at any operating system supporting Java.
- Blockchain data are obtained from the coin node through the rpc calls.
- Multithread implementation to avoid a response stuck.
- External database of transactions sync from the top for a more complex queries.

... under construction
...