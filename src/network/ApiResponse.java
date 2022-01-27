/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import database.DbReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import system.Config;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import system.Base58;
import system.Task;
import system.Utils;

/**
 *
 * @author Vratislav Bednařík
 */
public class ApiResponse extends Task {
    private final Config config;
    private static final String BASE_URL = ((Config.getConfig().usessl)?"https://":"http://") + Config.getConfig().apiUrl + ":" + Config.getConfig().apiPortNumber + "/";
    private static final String DEFAULT_THREAD_NAME = "API response task";
    private static final String UNKNOWN_API_METHOD = "{\"Error\": \"Unknown API method\"}\n";
    private static final String UNKNOWN_ERROR = "{\"Error\": \"API request failed\"}\n";
    private static final String MISSING_PARAMETER = "{\"Error\": \"API method requires an parameter\"}\n";
    private static final String RESPONSE_HEADER = 
        "HTTP/1.1 200 OK\n" +
        "Access-Control-Allow-Origin: *\n" + 
        "Access-Control-Request-Method: *\n" +
        "Access-Control-Allow-Methods: GET\n" + 
        "Access-Control-Allow-Headers: *\n" +
        "Content-Type: application/json; charset=UTF-8\n" +
        "X-Powered-By: sando\n" +
        "Connection: close\n";
    private static final String HTTP_HEADER = 
        "HTTP/1.1 200 OK\n" +
        "Content-Type: text/html; charset=UTF-8\n" +
        "Connection: close\n\n";    
    private static final String EXPLORER_HEADER = 
        "<!DOCTYPE html>\n<html>\n<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
        + "<title> " + Config.getConfig().coinName + "(" + Config.getConfig().coinTicker + ") API & Block Explorer</title>\n"
        + "<style>a:link, a:visited {text-decoration: none; color: blue}</style>\n</head>\n"
        + "<body><h1><p align=\"center\"><a href=\"/explorer\">API & Block Explorer for " + Config.getConfig().coinName + "</a></p></h1>"
        + "<table width=\"100%\"><tr><td width=\"20%\">&nbsp;&nbsp;"
        + "</td><td width=\"60%\">"
        + "<div align=\"center\"> "
        + "<form action=\"/search/\" method=\"get\">"
        + "<input name=\"q\" type=\"text\" size=\"80\" placeholder=\"block hash, index, transaction or address\" />"
        + "<input type=\"submit\" value=\"Search\" /></form></div>"
        + "</td><td width=\"20%\">"
        + "<a href=\"/doc\">API documentation</a>" 
        + "</td></tr></table><hr>";
    private static final String EXPLORER_FOOTER = 
        "<hr><p align=\"center\">Powered by <A href=\"https://github.com/Sandokaaan/EAC_API_JAVA.git\">" 
        + "Sando API v.2.9</A> &nbsp; &#9400; 2022</P></body></html>";
    private final static JSONObject jsonDoc = loadJsonDoc();
    private final RawSocket rawSocket;
    private final RpcClient client;
    private String command;
    private String response;
    private boolean explorerMode = false;
    private static final DecimalFormat DF = new DecimalFormat("#.#");
    private static final DecimalFormat DF8 = new DecimalFormat("#.########");
    private boolean sslError = false;

    private static void setJsonOrdered(JSONObject jsonObject){
        try {
            Field changeMap = jsonObject.getClass().getDeclaredField("map");
            changeMap.setAccessible(true);
            changeMap.set(jsonObject, new LinkedHashMap<>());
            changeMap.setAccessible(false);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            System.err.println("Error - creation of an ordered json failed");
        }
    }
    
    private static JSONObject loadJsonDoc(){
        JSONObject fileJson;
        JSONObject rtsJson = new JSONObject();
        setJsonOrdered(rtsJson);
        try {
            InputStream is = new FileInputStream(new File(Config.getConfig().docPath)); 
            JSONTokener tokener = new JSONTokener(is);
            fileJson = new JSONObject(tokener);
            fileJson.keySet().stream().sorted().forEach((String key) -> {
                JSONObject method = fileJson.getJSONObject(key);
                JSONObject outputMethod = new JSONObject();
                setJsonOrdered(outputMethod);
                Stream
                    .of("method", "alias", "parameters", "return", "notes", "warning", "recommendation")
                    .filter(item -> { return method.has(item); })    
                    .forEach(item -> { outputMethod.put(item, method.get(item)); });
                // add base url to links in examples
                if (method.has("examples")) {
                    JSONArray examples = new JSONArray();
                    JSONArray src = method.getJSONArray("examples");
                    src.forEach((Object example) -> {
                        examples.put(BASE_URL + example.toString());
                    });
                    outputMethod.put("examples", examples);
                }
                rtsJson.put(key, outputMethod);
            });
            return rtsJson;
        } catch (FileNotFoundException | JSONException ex) {
            System.err.println("Error - API documentation file is missing or corrupted");
            return new JSONObject();
        }        
    }
    
    public ApiResponse(RawSocket rawSocket, RpcClient client) {
        super(DEFAULT_THREAD_NAME);
        this.rawSocket = rawSocket;
        this.client = client;
//        dbManager = null;
        command = null;
        response = null;
        config = Config.getConfig();
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    protected void initialization() {
        try {
            String httprequest = rawSocket.receiveTextLine();
            String[] params = httprequest.split(" ");
            if (params.length == 3) {
                if (params[0].equals("GET") && params[2].startsWith("HTTP")) {
                    command = params[1].trim();
                    return;
                }
            }
            throw (new IOException("Invalid http header."));
        } catch (Exception ex) {
            System.err.println("Receive API command failed. " + ex.getMessage());
            sslError = true;
        }
    }

    @Override
    protected void mainTask() {
        if (!sslError && command != null) {
            System.out.println("Received command: " + command);
            String[] params = command.split("/");
            String method = (params.length>=2) ? params[1].toLowerCase() : "help";
            switch (method) {
                case "favicon.ico":
                    response = "";  // ignore, for now
                    // System.out.println("favicon ignored");
                    break;
                case "getinfo":
                    response = simpleRpcQuery("getblockchaininfo");
                    break;
                case "getheight":
                    response = simpleRpcQuery("getblockcount");
                    break;
                case "getblockchaininfo":
                case "getpeerinfo":
                case "getdifficulty":
                case "getblockcount":
                    response = simpleRpcQuery(method);
                    break;
                case "getbestblockhash":
                case "getnetworkhashps":
                    response = simpleRpcQueryQuoted(method);
                    break;
                case "getblockhash":
                    response = getblockhash(params);
                    break;
                case "getblock":
                case "block":                    
                    response = getblock(params);
                    break;
                case "getrawtransaction":
                case "extx":    
                case "tx":
                    response = getrawtransaction(params);
                    break;
                case "gettxout":
                    response = gettxout(params);
                    break;
                case "sendrawtransaction":
                    response = sendrawtransaction(params);
                    break;
                case "sendcoins":
                    response = sendCoins(params);
                    break;
                case "validateaddress":
                    response = validateaddress(params);
                    break;
                case "utxo":
                case "unspent":
                    response = scantxoutset(params);
                    break;
                case "getbalance":
                    response = getbalance(params);
                    break;
                case "check":    
                case "checktransaction":
                    response = checktransaction(params);
                    break;
                case "explorer":
                    explorerMode = config.withExplorer;
                    response =  explorerMode ? explore(params) : UNKNOWN_API_METHOD;
                    break;
                case "transaction":
                    explorerMode = config.withExplorer;
                    response = explorerMode ? transaction(params) : UNKNOWN_API_METHOD;
                    break;
                case "addressinfo":
                    explorerMode = config.withExplorer;
                    response = explorerMode ? addressinfo(params) : UNKNOWN_API_METHOD;
                    break;
                case "addrstat":
                    response = addressStatistic(params);
                    break;
                case "balance":
                    explorerMode = config.withExplorer;
                    response = explorerMode ? balance(params) : UNKNOWN_API_METHOD;
                    break;
                case "search":
                    explorerMode = config.withExplorer;
                    response = explorerMode ? search(params) : UNKNOWN_API_METHOD;
                    break;   
                case "addresshistory":
                case "txbyaddr":    
                    response = getAddresshistory(params);
                    break;
                case "syncinfo":
                    response = getSyncinfo();
                    break;
                case "doc":
                    response = apidoc(params);
                    break;
                case "getblockheight":
                case "getblockindex":    
                    response = getblockheight(params);
                    break;
                case "addresshistorybyheight":
                    response = getAddresshistoryByHeight(params);
                    break;          
                case "addresshistorybytime":
                    response = getAddresshistoryByTime(params);
                    break;         
                case "gettopblocks":
                    response = getTopBlocks(params);
                    break;
                case "help":
                    explorerMode = config.withExplorer;
                    response = explorerMode ? doc(params) : apidoc(params);
                    break;
                default:
                    explorerMode = config.withExplorer;
                    response = explorerMode ? explore(params) : apidoc(params);
            }
        }
    }

    @Override
    protected void finished() {
        //System.err.println(response); /// debug
        try {
            if (sslError)
                rawSocket.close();
            else if (explorerMode) {
                rawSocket.send(HTTP_HEADER + response);
                rawSocket.close();
            }
            else if (response != null) {
                String formatedTime = 
                DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT")).format(Instant.now());
                String respTime = "Date: " + formatedTime + "\n\n";
                rawSocket.send(RESPONSE_HEADER + respTime + response);
                rawSocket.close();
            }
        } catch (IOException ex) {
            System.err.println("Response socket closing failed. " + ex.getMessage());
        }
    }

    private double round8(double value) {
        return ((double)((long)(value*100000000)))/100000000;
    }
    
    private String addStringQuotes(String s) {
        return (s.startsWith("{")) ? s : "\"" + s + "\"";
    }
    
    private String simpleRpcQuery(String method) {
        try {
            return client.query(method).getAsString();
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
    }
    
    private String simpleRpcQueryQuoted(String method){
        try {
            return addStringQuotes(client.query(method).getAsString());
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
    }
    
    
    @SuppressWarnings("UseSpecificCatch")
    private String getblockhash(String[] params) {
        try {
            if ((params.length<3) || (params[2].length()==0))
                throw (new NumberFormatException("Missing parameter"));
            int index = Integer.parseInt(params[2]);
            return addStringQuotes(client.query("getblockhash", index).getAsString());
        } catch (Exception ex) {
            return MISSING_PARAMETER;
        }
    }

    private String getblock(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        int verbosity = 1;
        if (params.length == 4) {
            try {
                verbosity = Integer.parseInt(params[3]);
                if ( (verbosity < 0) || (verbosity > 2) )
                    verbosity = 1;
            } catch (NumberFormatException ex) {                
                verbosity = 1;
            }
        }
        String hash = params[2];
        try {
            String rts = client.query("getblock", hash, verbosity).getAsString();
            return (verbosity == 0) ? addStringQuotes(rts) : rts;
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
    }

    
    @SuppressWarnings("UseSpecificCatch")
    private String getrawtransaction(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String hash = params[2];
        int decrypt = 0;
        if (params.length>=4)
            if (params[3].equals("true") || params[3].equals("1"))
                decrypt = 1;
        if (params[1].equals("tx") || params[1].equals("extx"))
            decrypt = 1;
        RpcResponse rpcResponse;
        String rts;
        try {
            rpcResponse = client.query("getrawtransaction", hash, decrypt);
            rts = rpcResponse.getAsString();
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
        if (params[1].equals("extx")) try {            
            double txValue = 0.0;
            JSONObject jsonTx = rpcResponse.getAsJson();
            JSONArray jsonOutputs = jsonTx.getJSONArray("vout");
            for (int i=0; i < jsonOutputs.length(); i++) {
                JSONObject jsonOut = jsonOutputs.getJSONObject(i);
                txValue += jsonOut.getDouble("value");
            }
            double txFee = -txValue;
            JSONArray jsonInputs = jsonTx.getJSONArray("vin");
            for (int j=0; j < jsonInputs.length(); j++) {
                JSONObject jsonIn = jsonInputs.getJSONObject(j);
                if (jsonIn.has("txid")) {
                    String srcTx = jsonIn.getString("txid");
                    int i = jsonIn.getInt("vout");
                    JSONObject jsonSrcTx = client.query("getrawtransaction", srcTx, decrypt).getAsJson();
                    JSONObject jsonOut = jsonSrcTx.getJSONArray("vout").getJSONObject(i);
                    String address = jsonOut.getJSONObject("scriptPubKey").getJSONArray("addresses").getString(0);
                    double value = jsonOut.getDouble("value");
                    txFee += value;
                    jsonIn.put("address", address);
                    jsonIn.put("value", value);
                    jsonInputs.put(j, jsonIn);
                }
            }
            jsonTx.put("vin", jsonInputs);
            jsonTx.put("txValue", round8(txValue));
            jsonTx.put("txFee", round8(txFee > 0 ? txFee : 0));
            rts = jsonTx.toString();
        } catch (Exception ex) {
            // do nothing, return original RPC error message
        }
        return (decrypt==0) ? addStringQuotes(rts) : rts;
    }

    @SuppressWarnings("UseSpecificCatch")
    private String gettxout(String[] params) {
        try {
            if ((params.length<4) || (params[2].length()==0) || (params[3].length()==0) )
                throw (new NumberFormatException("Missing parameter"));
            int index = Integer.parseInt(params[3]);
            String txhash = params[2];
            return client.query("gettxout", txhash, index).getAsString();
        } catch (Exception ex) {
            return MISSING_PARAMETER;
        }   
    }

    private String sendrawtransaction(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String txdata = params[2];
        try {
            return addStringQuotes(client.query("sendrawtransaction", txdata).getAsString());
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
    }

    private String validateaddress(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String addr = params[2];
        try {
            return client.query("validateaddress", addr).getAsString();
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
    }

    private String scantxoutset(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        int n = params.length - 2;
        String[] scanobjects = new String[n];
        for (int i=0; i<n; i++)
            scanobjects[i] = "addr(" + params[i+2] + ")";
        try {
            return client.query("scantxoutset", "start", scanobjects).getAsString();
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }        
    }

    private String getbalance(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        JSONObject jsonUnspent = getUnspentBalanceInternal(params[2]);
        double balance = jsonUnspent.optDouble("total_amount", 0);        
        String rts = "{\"balance\": " + balance + "}\n";
        return rts;
    }
    
    private boolean testOutputs(JSONArray jsonOuts, String address, double txValue) {
        for (int i=0; i < jsonOuts.length(); i++) {
            JSONArray arresses = jsonOuts.getJSONObject(i).getJSONObject("scriptPubKey").getJSONArray("addresses");
            double sentValue = jsonOuts.getJSONObject(i).getDouble("value");
            if (sentValue>=txValue)
                for(Object adr: arresses)
                    if (address.equals(adr))
                        return true;
      }       
      return false;
    }
    
    private boolean testTxComment(String txCommemt, JSONObject jsonTx) {
        boolean txCommentPass = true;
            if (txCommemt.length()>0) 
                txCommentPass = (jsonTx.has("txComment")) ? 
                    txCommemt.equals(jsonTx.getString("txComment")) 
                    : false;
        return txCommentPass;
    }

    @SuppressWarnings("UseSpecificCatch")
    private String checktransaction(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String address = params[2];
        double txValue = Utils.passDoubleParam(params, 3, 0);
        String txCommemt = Utils.passStringParam(params,4);
        int orderHeight = Utils.passIntParam(params, 5, 0);
        int expirationTime = Utils.passIntParam(params, 6, 60);
        try {
        if (orderHeight == 0)
            orderHeight = Utils.passIntParam(new String []{
                client.query("getblockcount").getAsString()
            }, 0, 60) - 60;
            // scan the memory pool first
        String rts;
        rts = client.query("getrawmempool").getAsString(); 
        JSONArray jsonMemPool = new JSONArray(rts);
        for(Object o: jsonMemPool){
            JSONObject jsonTx = client.query("getrawtransaction", o, 1).getAsJson();
            JSONArray jsonOuts = jsonTx.getJSONArray("vout");
            if (  testTxComment(txCommemt, jsonTx) 
                   && testOutputs(jsonOuts, address, txValue)) {
                jsonTx.put("confirmations", 0);
                return jsonTx.toString();
            }                
        }
        if (orderHeight > 0) {
            double orderTimeLimit = 0;
            // scan the blockchain
            String blockHash = client.query("getblockhash", orderHeight).getAsString();
            while (true) {
                JSONObject jsonBlock = client.query("getblock", blockHash, 2).getAsJson();
                double blockTime = jsonBlock.getDouble("time");
                if (orderTimeLimit == 0)
                    orderTimeLimit = blockTime + 60*expirationTime;
                JSONArray jsonTxs = jsonBlock.getJSONArray("tx");
                for (int j=1; j < jsonTxs.length(); j++) {
                    JSONObject rtsTx = jsonTxs.getJSONObject(j);
                    JSONArray jsonOuts = rtsTx.getJSONArray("vout");
                    if (  testTxComment(txCommemt, rtsTx)
                            && testOutputs(jsonOuts, address, txValue)) {
                        rtsTx.put("confirmations", jsonBlock.getInt("confirmations"));
                        return rtsTx.toString();
                    }
                }
                if ((!(jsonBlock.has("nextblockhash"))) || (blockTime>orderTimeLimit))
                    break;
                blockHash = jsonBlock.getString("nextblockhash");
            }
        }
        return "{\"Error\": \"Payment not found\"}\n";
        } catch (Exception ex) {
            return UNKNOWN_ERROR;
        }
    }
    
    private String doc(String[] params) {
        String rts = 
            EXPLORER_HEADER
            + "<H3><B>Welcome to the " + Config.getConfig().coinName + " API server</B></H3><BR>"
            + "<B>Avaiable API commands:</B><BR>"
            + "<TABLE border=\"1\"><tr>"
                + "<td width=\"20%\"><B>Command</B></td>"
                + "<td width=\"20%\"><B>Alias</B></td>"
                + "<td width=\"40%\"><B>Description</B></td>"
                + "<td width=\"20%\"><B>Parameters</B></td>"
            + "</tr><tr>"
                + "<td>getblockchaininfo</td>"
                + "<td>getinfo</td>"
                + "<td>Returns an object containing various state info regarding blockchain processing.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getpeerinfo</td>"
                + "<td></td>"
                + "<td>Returns data about each connected network node as a json array of objects.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getdifficulty</td>"
                + "<td></td>"
                + "<td>Returns the proof-of-work difficulty as a multiple of the minimum difficulty.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getblockcount</td>"
                + "<td>getheight</td>"
                + "<td>Returns the number of blocks in the longest blockchain.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getblockhash</td>"
                + "<td></td>"
                + "<td>Returns hash of block in best-block-chain at height provided.</td>"
                + "<td>Block_height</td>"
            + "</tr><tr>"
                + "<td>getbestblockhash</td>"
                + "<td></td>"
                + "<td>Returns the hash of the best (tip) block in the longest blockchain.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getblock</td>"
                + "<td>block</td>"
                + "<td>If verbosity is 0, returns a string that is serialized, hex-encoded data for block 'hash'.\n" 
                + "If verbosity is 1, returns an Object with information about block <hash>.\n" 
                + "If verbosity is 2, returns an Object with information about block <hash> "
                + "and information about each transaction.</td>"
                + "<td>Block_hash, Verbosity</td>"
            + "</tr><tr>"
                + "<td>getrawtransaction</td>"
                + "<td></td>"
                + "<td>Return the raw transaction data.</td>"
                + "<td>Transaction_ID</td>"
            + "</tr><tr>"
                + "<td>tx</td>"
                + "<td></td>"
                + "<td>Return the transaction in a decoded format.</td>"
                + "<td>Transaction_ID</td>"
            + "</tr><tr>"
                + "<td>extx</td>"
                + "<td></td>"
                + "<td>Return the transaction in an extended format. Source addresses and values of transaction inputs are included.</td>"
                + "<td>Transaction_ID</td>"
            + "</tr><tr>"                
                + "<td>gettxout</td>"
                + "<td></td>"
                + "<td>Returns details about an unspent transaction output.</td>"
                + "<td>Transaction_ID, Output_index</td>"
            + "</tr><tr>"
                + "<td>sendrawtransaction</td>"
                + "<td></td>"
                + "<td>Submits raw transaction (serialized, hex-encoded) to local node and network.</td>"
                + "<td>Signed_raw_transaction</td>"
            + "</tr><tr>"
                + "<td>sendcoins</td>"
                + "<td></td>"
                + "<td><FONT color=\"red\">Warning: unsafe method. Your private key is exposed. Use on your own risk. "
                + "Suggested safe way is create and sign a transaction on the client side and send it using sendrawtransaction method.</FONT> "
                + "This function check the balance on specified Sender_address, try to create a transaction, "
                + "sign it with provided Private_key and broadcast it into the network.</td>"
                + "<td>Receiver_address, Value_to_send, Sender_address, Private_key</td>"                
            + "</tr><tr>"
                + "<td>validateaddress</td>"
                + "<td></td>"
                + "<td>Return information about the given " + Config.getConfig().coinName + " address.</td>"
                + "<td>Address</td>"
            + "</tr><tr>"
                + "<td>getnetworkhashps</td>"
                + "<td></td>"
                + "<td>Returns the estimated network hashes per second based on the last n blocks.</td>"
                + "<td>-</td>"
            + "</tr><tr>"
                + "<td>getbalance</td>"
                + "<td></td>"
                + "<td>Returns the total balance hold on an address.</td>"
                + "<td>Address</td>"
            + "</tr><tr>"
                + "<td>unspent</td>"
                + "<td>utxo</td>"
                + "<td>Scans the unspent transaction output set for entries that match certain output descriptors.</td>"
                + "<td>Address1, Address2, ...</td>"
            + "</tr><tr>"
                + "<td>checktransaction</td>"
                + "<td>check</td>"
                + "<td>Scan the blockchain and the memory pool for a transaction matching specified parameters. "
                + "If succeeds, it returns the transaction details. <br><b>Parameters:</b><br>"
                + "<b>Address</b> - mandatory. Transaction must be sent to this address. "
                + "<b>Value</b> - optional. If set, transaction value must be equal or higher. "
                + "<b>Transaction_comment</b> - optional. If set, txComment must match. "
                + "<b>Order_height</b> - optional. If set, transction block height must be equal or higher. If not set, only top 60 block are scanned. "
                + "<b>Expiration_time</b> - optional. If set, transction time must be no later than the specified number of minutes after the order. If not nes, default value of 60 minutes is used.</td>"
                + "<td>Address, Value, Transaction_comment, Order_height, Expiration_time</td>"
            + "</tr><tr>"
                + "<td>addresshistory</td>"
                + "<td></td>"
                + "<td>Returns the tranaction history for the Address. "
                + "Optional parameters LimitX are maximal counts of received (LimitR) and send (LimitS) of transactions shown. "
                + "If only LimitR is specified, the value is used for both limits. "
                + "If no Limit specified, default value of 10 is used. Maximal allowed value for Limit is 100.</td>"
                + "<td>Address, LimitR, LimitS</td>"
            + "</tr><tr>"    
            + "</tr><tr>"
                + "<td>syncinfo</td>"
                + "<td></td>"
                + "<td>Returns count of blocks in the database, percentage of sync and best block height. </td>"
                + "<td></td>"
            + "</tr></TABLE><BR><BR>"
            + "<B>Common syntax:</B><BR>"
                + ((config.usessl)?"https":"http") + "://[SERVER_ADDRESS]:[SERVER_PORT]/[COMMAND]/[PARAM1]/[PARAM2]/[PARAM3]<BR>"
            + "<B>Examples:</B><BR>"
                + BASE_URL + "getinfo<BR>"
                + BASE_URL + "getblockhash/1000<BR>"
                + BASE_URL + "getblock/ea7dd253f3ef1e1353728e92fa637368244ffcf46de9da76f57ed019182fb92f/2<BR>"
                + BASE_URL + "unspent/en6bCHDGejFW2VdMkKZDCfyfzRUAujh6RA<BR>"
                + BASE_URL + "check/ecGUjidXR9vfWZELZzJjWCpu9jbJ6guBAE/2/尝试中文/2887805/10<BR>"
            + EXPLORER_FOOTER;
        return rts;
    }

    private String error(String[] params) {
        String rts = 
            EXPLORER_HEADER
            + "<B><FONT color=\"red\">Error - unknown request</FONT></B>"
            + EXPLORER_FOOTER;
        return rts;
    }
    
    
    @SuppressWarnings("UseSpecificCatch")
    private String explore(String[] params) {
        int blockIndex = -1;
        String blockHash = null;
        if ( (params.length == 3) && (params[2].length() > 0) ) {
            try {
                blockIndex = Integer.parseInt(params[2]);
            } catch (NumberFormatException ex) {
                blockHash = params[2];
            }
        }       
        if (blockHash == null) {
            try {
                blockHash = (blockIndex < 0) 
                    ? client.query("getbestblockhash").getAsString()
                    : client.query("getblockhash", blockIndex).getAsString();
            } catch (Exception ex) {
                return error(params);
            }
        }
        JSONObject json;
        try {
            json = client.query("getblock", blockHash, 2).getAsJson();
        } catch (Exception ex) {
            return error(params);
        }
        String prevHash, nextHash, prevLink, nextLink;
        try {
            prevHash = json.getString("previousblockhash");
            prevLink = "<A href=\"/explorer/" + prevHash + "\"><B>&lt;&lt;</B></A>";
        } catch (JSONException ex) { 
            prevLink = "<FONT color=\"gray\"><B>&lt;&lt;</B></FONT>";
        }
        try {
            nextHash = json.getString("nextblockhash");
            nextLink = "<A href=\"/explorer/" + nextHash + "\"> <B>&gt;&gt;</B></A>";
        } catch (JSONException ex) { 
            nextLink = "<FONT color=\"gray\"><B>&gt;&gt;</B></FONT>";
        }
        try {
            int height = json.getNumber("height").intValue();
            String hash = json.getString("hash");
            int nTx = json.getNumber("nTx").intValue();
            int size = json.getNumber("size").intValue();
            int confirmations = json.getInt("confirmations");
            long time = json.getNumber("time").intValue();
            String formatedTime = 
                DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT")).format(Instant.ofEpochSecond(time));
            double difficulty = json.getDouble("difficulty");
            String sSize = DF.format(((0.0 + size)/1024)) + " kB";
            long now = Instant.now().toEpochMilli()/1000;
            double age = (0.0 + now-time)/60;
            String sAge = DF.format(age);
            JSONArray txArray = json.getJSONArray("tx");
            JSONObject jsonTx[] = new JSONObject[nTx];
            double valuesSent[] = new double[nTx];
            int coutOfSources[] = new int[nTx];
            int coutOfTargets[] = new int[nTx];
            String targetAddresses[] = new String[nTx];
            String targetValues[] = new String[nTx];
            double totalSent = 0.0;
            for (int i=0; i<nTx; i++){ 
                targetValues[i] = "";
                targetAddresses[i] = "";
                valuesSent[i] = 0.0;
                jsonTx[i] = txArray.getJSONObject(i);
                JSONArray coinBaseOutputs = jsonTx[i].getJSONArray("vout");
                coutOfSources[i] = jsonTx[i].getJSONArray("vin").length();
                coutOfTargets[i] = coinBaseOutputs.length();
                for (int j=0; j<coutOfTargets[i]; j++) {
                    JSONObject jsonOut = coinBaseOutputs.getJSONObject(j);
                    String address;
                    try {
                        address = jsonOut.getJSONObject("scriptPubKey").getJSONArray("addresses").getString(0);
                    } catch (JSONException ex) {
                        address = "null";
                    }
                    double value = jsonOut.getDouble("value");
                    targetValues[i] += DF8.format(value) + "<BR>";
                    targetAddresses[i] += "<a href=\"/addressinfo/" + address + "\">" + address + "</a><BR>";
                    totalSent += value;
                    valuesSent[i] += value;
                }
            }
            //System.out.println(json);
            //System.out.println(txArray);
            String rts = EXPLORER_HEADER
                + "<H3>Details of block <code>" + height + "</code> </H1><HR>"
                + "<code><table><tr>"
                    + "<td> Hash </td>"
                    + "<td align=\"right\"> " + prevLink + " </td>"
                    + "<td> " + hash + " </td>"
                    + "<td align=\"left\"> " + nextLink + " </td></tr>"
                + "<tr><td>Height</td><td>&nbsp;</td><td>" + height + "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Confirmations</td><td>&nbsp;</td><td>" + confirmations+ "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Timestamp</td><td>&nbsp;</td><td>" + time + "</td><td>&nbsp;</td></tr>"                    
                + "<tr><td>Date/Time</td><td>&nbsp;</td><td>" + formatedTime + "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Age</td><td>&nbsp;</td><td>" + sAge + " min</td><td>&nbsp;</td></tr>"                    
                + "<tr><td>Count of transactions</td><td>&nbsp;</td><td>" + nTx + "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Block size</td><td>&nbsp;</td><td>" + sSize + "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Difficulty</td><td>&nbsp;</td><td>" + DF8.format(difficulty) + "</td><td>&nbsp;</td></tr>"
                + "<tr><td>Block reward</td><td>&nbsp;</td><td>" + DF8.format(valuesSent[0]) + " " + config.coinTicker + " </td><td>&nbsp;</td></tr>"
                + "<tr><td>Total output</td><td>&nbsp;</td><td>" + DF8.format(totalSent) + " " + config.coinTicker + " </td><td>&nbsp;</td></tr>"
                + "</table></code><br><br>Transactions<br>"
                + "<code><table width=\"100%\" border=\"1\"><tr><td width=\"5%\" align=\"center\">#</td>"
                + "<td width=\"45%\" align=\"center\">TxID</td>"
                + "<td width=\"10%\" align=\"center\">Value Out (" + config.coinTicker + ")</td>"
                + "<td width=\"10%\" align=\"center\">Count of sources</td>"
                + "<td width=\"20%\" align=\"center\">To</td>"
                + "<td width=\"10%\" align=\"center\">Amount (" + config.coinTicker + ")</td</tr><tr>";
            for (int i=0; i<nTx; i++){ 
                String sTx = jsonTx[i].getString("txid");
                rts += "<td align=\"center\">" + i + "</td>"
                + "<td align=\"center\"><a href=\"/transaction/" + sTx + "\">" + sTx + "</a></td>"
                + "<td align=\"center\">" + DF8.format(valuesSent[i]) + "</td>"
                + "<td align=\"center\">" + coutOfSources[i] + "</td>"
                + "<td align=\"center\">" + targetAddresses[i] + "</td>"
                + "<td align=\"center\">" + targetValues[i] + "</td></tr>";
            }
            rts += "</table></code>" + EXPLORER_FOOTER;
            return rts;
        } catch (JSONException e) {
            return error(params);
        }        
    }

    @SuppressWarnings("UseSpecificCatch")
    private String transaction(String tx) {
        String txid, blockhash;
        int confirmations, size, version;
        String inputs, outputs;
        long timestamp;
        JSONArray vin, vout;
        double txFee, txValue;
        try {
            inputs = "<TABLE>";
            outputs = "<TABLE>";
            JSONObject json = new JSONObject(tx);
            txid = json.getString("txid");
            blockhash = json.getString("blockhash");
            confirmations = json.getInt("confirmations");
            version = json.getInt("version");
            size = json.getInt("size");
            timestamp = json.getLong("time");
            vin = json.getJSONArray("vin");
            vout = json.getJSONArray("vout");
            txFee = json.optDouble("txFee");
            txValue = json.optDouble("txValue");
            for (int i=0; i<vin.length(); i++) {
                JSONObject jsonVin = vin.getJSONObject(i);
                if (jsonVin.has("coinbase"))
                    inputs += "<tr><td>"+i+"</td><td>&nbsp;</td><td>coinbase</td><td>&nbsp;</td><td>&nbsp;</td></tr>";
                else {
                    String prevTx = jsonVin.getString("txid");
                    int prevVout = jsonVin.getInt("vout");
                    inputs += "<tr><td>"+i+"</td><td>&nbsp;</td><td>txid: <A href=\"/transaction/"+prevTx+"\">"+prevTx+"</a></td><td>&nbsp;</td><td>vout: " + prevVout + "</td><tr>";
                    try {
                      String srcAddress = jsonVin.getString("address");
                      double value = jsonVin.getDouble("value");
                      inputs += "<tr><td>\t</td><td>&nbsp;</td><td>address: <A href=\"/addressinfo/"+srcAddress+"\">"+srcAddress+"</a></td><td>&nbsp;</td><td>value: " + round8(value) + " " + config.coinTicker + "</td><tr>";
                    } catch (Exception ex) {
                      inputs += "<tr><td>\t</td><td>&nbsp;</td><td>hidden source address</td><td>&nbsp;</td><td>&nbsp;</td><tr>";
                    }
                }
            }
            inputs += "</TABLE>";
            for (int i=0; i<vout.length(); i++) {
                JSONObject jsonVout = vout.getJSONObject(i);
                JSONArray addresses = jsonVout.getJSONObject("scriptPubKey").optJSONArray("addresses");
                String address;
                if (addresses != null)
                    address = addresses.optString(0, "null");
                else
                    address = "null";
                String linkedAddr = "<a href=\"/addressinfo/" + address + "\">" + address + "</a>";
                double value = jsonVout.getDouble("value");
                outputs += "<tr><td>"+i+"</td><td>&nbsp;</td><td>"+linkedAddr+"</td><td>&nbsp;</td><td> value: "+DF8.format(value)+"</td></tr>";
            }                
            outputs += "</TABLE>";
            if (version == 2) {
                String txMessage = json.optString("txComment");
                if (txMessage.length()>0) {
                    outputs += "</br><font color=\"red\"><b>Transaction message: </b></font>" + txMessage + "</br>";
                }
                String IPFS_CID = json.optString("IPFS_CID");               
                if (IPFS_CID.length()>0) {
                    outputs += "<form action=\"https://ipfs.io/ipfs/"+IPFS_CID+"\">"
                            + " <font color=\"red\"><b>IPFS_CID: </b></font>" + IPFS_CID
                            + " <input style=\"border-radius: 12px;\" type=\"submit\" formtarget=\"_blank\" value=\"View on ipfs.io\" />"
                            + " <button style=\"border-radius: 12px;\" type=\"submit\" formtarget=\"_blank\" formaction=\"https://gateway.pinata.cloud/ipfs/"+IPFS_CID+"\">View on pinata.cloud</button>"
                            + " <button style=\"border-radius: 12px;\" type=\"submit\" formtarget=\"_blank\" formaction=\"https://dweb.link/ipfs/"+IPFS_CID+"\">View on dweb.link</button>"
                            + " <button style=\"border-radius: 12px;\" type=\"submit\" formtarget=\"_blank\" formaction=\"https://ipfs.fleek.co/ipfs/"+IPFS_CID+"\">View on fleek.co</button>"
                            + "</form><br>";
                }
            }
        } catch (JSONException ex) {
            return error(null);
        }
        String formatedTime = 
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT")).format(Instant.ofEpochSecond(timestamp));
        String rts = 
            EXPLORER_HEADER
            + "<H3>Details of transaction <code>" + txid + "</code> </H1><HR><code><table>"
            + "<tr><td>txid</td><td>&nbsp;</td><td>" + txid + "</td></tr>"
            + "<tr><td>confirmations</td><td>&nbsp;</td><td>" + confirmations + "</td></tr>"
            + "<tr><td>blockhash</td><td>&nbsp;</td><td><A href=\"/explorer/" + blockhash + "\">" + blockhash + "</A></td></tr>"
            + "<tr><td>size</td><td>&nbsp;</td><td>" + size + "</td></tr>"
            + "<tr><td>version</td><td>&nbsp;</td><td>" + version + "</td></tr>"
            + "<tr><td>timestamp</td><td>&nbsp;</td><td>" + timestamp + "</td></tr>"
            + "<tr><td>date/time</td><td>&nbsp;</td><td>" + formatedTime + "</td></tr>";
        if (!Double.isNaN(txValue))
            rts += "<tr><td>value</td><td>&nbsp;</td><td>" + DF8.format(txValue) + " " + config.coinTicker + " </td></tr>";
        if (!Double.isNaN(txFee))
            rts += "<tr><td>fee</td><td>&nbsp;</td><td>" + DF8.format(txFee) + " " + config.coinTicker + " </td></tr>";
        rts += "</table><br><B>inputs</B><BR>" + inputs + "<br>"
            + "<B>outputs</B><BR>" + outputs + "<br> </code>"
            + EXPLORER_FOOTER;
        return rts;
    }
    
    private String transaction(String[] params) {
        if ( (params.length >= 3) && (params[2].length() > 0) ) {
            try {
                params[1] = "extx";
                return transaction(getrawtransaction(params));
            } catch (Exception ex) {
                return error(params);
            }
        }
        return error(params);
    }        

    @SuppressWarnings("UseSpecificCatch")
    private String addressinfo(String[] params) {
        if ( (params.length >= 3) && (params[2].length() > 0) ) {
            try {
                String addrInfo = client.query("validateaddress", params[2]).getAsString();
                return addressinfo(addrInfo, params[2]);
            } catch (Exception ex) {
                return error(params);
            }
        }
        return error(params);
    }

    private String addressinfo(String addrInfo, String address) {
        String stripedInfo = addrInfo.replace("\"", "").replace("{", "").replace("}", "").replace(":", " ").replace(",", "<br>");
        JSONObject json = new JSONObject(addrInfo);
        String balanceLink;
        try {
            boolean isvalid = json.getBoolean("isvalid");
            if (isvalid)
                balanceLink = "<A href=\"/balance/" + address + "\">Check the balance at this address</A>";
            else
                throw new JSONException("Invalid address");
        } catch (JSONException ex) {
            balanceLink = "<B><FONT color=\"red\">This is not a valid " + config.coinTicker + " address.</FONT></B>";
        }
        String rts = 
            EXPLORER_HEADER 
            + "<H3>Details of address <code>" + address + "</code> </H1><HR><code>"
            + stripedInfo + "<br>" + balanceLink    
            + EXPLORER_FOOTER;   
        return rts; 
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private String balance(String[] params) {
        if ( (params.length >= 3) && (params[2].length() > 0) ) {
            try {
                String[] scanobjects = new String[1];
                scanobjects[0] = "addr(" + params[2] + ")";
                JSONObject json = client.query("scantxoutset", "start", scanobjects).getAsJson();
                double total_amount = json.getDouble("total_amount");
                JSONArray unspents = json.getJSONArray("unspents");
                String utxo = "<TABLE>";
                for (int i=0; i<unspents.length(); i++) {
                    JSONObject jsonUtxo = unspents.getJSONObject(i);
                    double value = jsonUtxo.getDouble("amount");
                    int height = jsonUtxo.getInt("height");
                    String txid = jsonUtxo.getString("txid");
                    int vout = jsonUtxo.getInt("vout");
                    utxo += "<TR><TD>&nbsp</TD><TD>[" + i + "]</TD><TD>&nbsp</TD><TD>" 
                         + DF8.format(value) + " " + config.coinTicker + "</TD><TD>&nbsp</TD>"
                         + "<TD>block: <A href=\"/explorer/" + height + "\">" + height + "</A></TD>"
                         + "<TD>&nbsp</TD><TD>txid: <A href=\"/transaction/" + txid + "\">" + txid + "</A></TD>"
                         + "<TD>&nbsp</TD><TD>tx_output_index: " + vout + "</TD></TR>";
                }
                utxo += "</TABLE>";
                String rts = 
                    EXPLORER_HEADER 
                    + "<H3>Unspent balance at address <code>" + params[2] + "</code> </H1><HR><code>"
                    + "<B>Total amount:</b> " + DF8.format(total_amount) + " " + config.coinTicker + "<br><br>"
                    + "Count of unspent transactions: " + unspents.length()
                    + utxo    
                    + EXPLORER_FOOTER;   
                return rts; 
            } catch (Exception ex) {
                return error(params);
            }
        }
        return error(params);
    }

    @SuppressWarnings("UseSpecificCatch")
    private String search(String[] params) {
        if ( (params.length >= 3) && (params[2].length() > 0) ) {
            params[2] = params[2].substring(3).replace("+", "").trim();  // remove "?q="
            System.out.println(params[2]);
            try {
                RpcResponse rpcResponse = client.query("validateaddress", params[2]);
                JSONObject json = rpcResponse.getAsJson();
                try {
                    boolean isvalid = json.getBoolean("isvalid");
                    if (isvalid)
                        return addressinfo(rpcResponse.getAsString(), params[2]);
                    else
                        throw new JSONException("Invalid address");
                } catch (JSONException ex) {
                    String[] tmpParams= {"", "extx", params[2]};
                    String testParam = getrawtransaction(tmpParams);
                    try {
                        JSONObject json2 = new JSONObject(testParam);
                        String txid = json2.getString("txid");
                        return transaction(testParam);
                    } catch (JSONException ex2) {
                        return explore(params);
                    }                                   
                }
            } catch (Exception ex) {
                return error(params);
            }
        }
        return error(params);
    }

    private boolean addressValidationInternal(String address) {
        try {
            return client.query("validateaddress", address).getAsJson().optBoolean("isvalid");
        } catch (IOException ex) {
            return false;
        }
    }
    
    private JSONObject getUnspentBalanceInternal(String address) {
        try {
            String[] scanobjects = new String[1];
            scanobjects[0] = "addr(" + address + ")";
            return client.query("scantxoutset", "start", scanobjects).getAsJson();
        } catch (IOException ex) {
            return new JSONObject("error");            
        }
    }
    
    private JSONArray selectSourcesInternal(JSONArray arrayUnspents, double amountToSent) {
        JSONArray rts = new JSONArray();
        double selected = 0.0;
        for (int i=0; i<arrayUnspents.length(); i++) {
            JSONObject jsonTxo = arrayUnspents.getJSONObject(i);
            selected += jsonTxo.getDouble("amount");
            rts.put(jsonTxo);
            if (selected>amountToSent)
                break;
        }
        return rts;
    }
    
    private String doubleToFixedDecimalInternal(double value) {
        return String.format("%.8f", value).replace(",", ".");
    }
     
    private String createTransactionInternal(JSONArray selectedSources, double amountToSent, String targetAddress, String sourceAddress) {
        JSONArray inputs = new JSONArray();
        JSONArray outputs = new JSONArray();
        double selected = 0.0;
        double fee = 0.001;
        
        for (int i=0; i<selectedSources.length(); i++) {
            JSONObject jsonTxo = selectedSources.getJSONObject(i);
            selected += jsonTxo.getDouble("amount");
            JSONObject input = new JSONObject();
            input.put("txid", jsonTxo.getString("txid"));
            input.put("vout", jsonTxo.getInt("vout"));
            inputs.put(input);
        }
        JSONObject output1 = new JSONObject("{\"" + targetAddress + "\":" + doubleToFixedDecimalInternal(amountToSent) + "}");
        outputs.put(output1);
        double sendBackAmount = selected - amountToSent;
        if (sendBackAmount>fee) {
            sendBackAmount -= fee;
            JSONObject output2 = new JSONObject("{\"" + sourceAddress + "\":" + doubleToFixedDecimalInternal(sendBackAmount) + "}");
            outputs.put(output2);
        }
        String rts;
        try {
            rts = client.query("createrawtransaction", inputs, outputs).getAsString();
        } catch (IOException ex) {
            return UNKNOWN_ERROR;
        }
        return rts;
    }
    
    private String signTransactionInternal(String rawTx, String privKey) throws Exception {
        JSONArray keys = new JSONArray();
        keys.put(privKey);
        JSONObject json = client.query("signrawtransactionwithkey", rawTx, keys).getAsJson();
        boolean complete = json.optBoolean("complete", false);
        if (!complete)
            throw (new Exception("Transaction signing failed. Probably a bad private key."));
        return json.getString("hex");
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private String sendTransactionInternal(String signTx) {
        try {
            RpcResponse rpcResponse = client.query("sendrawtransaction", signTx);
            JSONObject jsonRts = rpcResponse.getAsJson();
            return (jsonRts.isEmpty())?"{\"txid\": \"empty\"}\n":rpcResponse.getAsString();
        } catch (Exception ex) {
            return "{\"txid\": \"error\"}\n";
        }
    }
        
    
    private String sendCoins(String[] params) {
        try {
            if ( (params.length!=6) || (params[2].length()==0) || (params[3].length()==0) || (params[4].length()==0) || (params[5].length()==0) )
                throw (new Exception ("This API method requires exactly 4 parameters - an address to receive coins, the amount to be send, the source address and the base58-encoded private key for signing the transaction."));
            double amountToSent;
            try {
                amountToSent = Double.parseDouble(params[3]);
            } catch (NumberFormatException ex) {
                throw (new Exception("The 2nd parameter should be a number value."));
            }
            try {
                Base58.decodeChecked(params[5]);
            } catch (IllegalArgumentException ex) {
                throw (new Exception("The 4th parameter should be a valid base58-encoded key."));
            }
            if (!(addressValidationInternal(params[2])))
                throw (new Exception("The 1st parameter should be a valid address to receive transaction."));
            if (!(addressValidationInternal(params[4])))
                throw (new Exception("The 3th parameter should be a valid address with a balance as the transaction source."));
            JSONObject jsonUnspent = getUnspentBalanceInternal(params[4]);
            double balance = jsonUnspent.optDouble("total_amount", 0);
            if (amountToSent>balance)
                throw (new Exception("Unsufficient balance on source address " + params[4]));
            JSONArray arrayUnspents = jsonUnspent.getJSONArray("unspents");
            JSONArray selectedSources = (arrayUnspents.length() > 1) ? selectSourcesInternal(arrayUnspents, amountToSent) : arrayUnspents;
            String rawTx = createTransactionInternal(selectedSources, amountToSent, params[2], params[4]);
            String signTx = signTransactionInternal(rawTx, params[5]);
            String rts = sendTransactionInternal(signTx);            
            return rts;
        } catch (Exception ex) {
            return "{\"Error\": \"" + ex.getMessage() + "\"}\n";
        }        
    }

    private String getAddresshistory(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String address = params[2];
        int limitR = 10;
        int limitS = 10;
        if (params.length == 4) {
            try {
                limitR = Integer.parseInt(params[3]);
                if ( (limitR < 0) || (limitR > 100) )
                    limitR = 10;
            } catch (NumberFormatException ex) {                
                limitR = 10;
            }
        }
        if (params.length == 5) {
            try {
                limitR = Integer.parseInt(params[4]);
                if ( (limitS < 0) || (limitS > 100) )
                    limitS = 10;
            } catch (NumberFormatException ex) {                
                limitS = 10;
            }
        } else
            limitS = limitR;
        try (DbReader dbReader = new DbReader()) {
            return dbReader.getHistorry(address, limitR, limitS);
        } catch (Exception ex) {
            System.err.println("ApiResponse: " + ex.getMessage());
            return UNKNOWN_ERROR;
        }
    }

    private String getSyncinfo() {
        try (DbReader dbReader = new DbReader()) {
            return dbReader.getSyncStat().toString();
        } catch (Exception ex) {
            System.err.println("ApiResponse: " + ex.getMessage());
            return UNKNOWN_ERROR;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private String apidoc(String[] params) {
        try {
            if (params.length<2 || (params.length==2 && params[1].equals("doc")))
                return jsonDoc.toString();  // show documentation of all methods
        } catch (Exception ex) {
            return "{\"Error\": \"Documentation is missing or damaged.\"}\n";
        }
        TreeSet<String> methods = new TreeSet<>();
        for (int i=2; i<params.length; i++)
            if (params[i].length()!=0)
                methods.add(params[i]);
        JSONObject selected = new JSONObject();
        jsonDoc.keySet().forEach(key -> {
            JSONObject subJson = jsonDoc.optJSONObject(key);
            if (subJson != null) {
                Boolean hasAlias = subJson.has("alias");
                String alias = hasAlias?subJson.optString("alias"):null;
                if (hasAlias && alias!=null && alias.length() == 0)
                    hasAlias = false;
                if (methods.contains(key) || (hasAlias && methods.contains(alias)))
                    selected.put(key, jsonDoc.get(key));
            }
        });
        if (selected.isEmpty())
            return "{\"Error\": \"Method not found.\"}\n";
        return selected.toString();
    }

    private String addressStatistic(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String address = params[2];
        try (DbReader dbReader = new DbReader()) {
            return dbReader.addressStatistics(address).toString();
        } catch (Exception ex) {
            System.err.println("ApiResponse: " + ex.getMessage());
            return UNKNOWN_ERROR;
        }          
    }

    private String getblockheight(String[] params) {
        if ( (params.length >= 3) && (params[2].length() > 0) ) {
            try (DbReader dbReader = new DbReader()) {
                int height = dbReader.getBlockHeight(params[2]);
                if (height > 0)
                    return "" + height;
            } catch (Exception ex) {
                System.err.println("ApiResponse: " + ex.getMessage());
                return UNKNOWN_ERROR;
            }                   
        }
        return "{\"Error\": \"Height not known.\"}\n";
    }

    private int validateHeight(int height, int bestHeight) {
        if (height<0)
            return 0;
        if (height>bestHeight)
            return bestHeight;
        return height;
    }

    private int validateTime(int time, int minTime, int maxTime) {
        if (time<minTime)
            return minTime;
        if (time>maxTime)
            return maxTime;
        return time;
    }       
    
    private String getAddresshistoryByHeight(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String address = params[2];
        try (DbReader dbReader = new DbReader()) {
            int bitmask = (params.length>5) ? Integer.parseInt(params[5]) : 3;
            int bestHeight = dbReader.getBestHeight();
            int highBlock = (params.length>4) ? Integer.parseInt(params[4]) 
                : bestHeight;
            int lowBlock = (params.length>3) ? Integer.parseInt(params[3]) 
                : bestHeight - 10080;
            lowBlock = validateHeight(lowBlock, bestHeight);
            highBlock = validateHeight(highBlock, bestHeight);
            return dbReader.getHistorryByHeight(address, bitmask, lowBlock, highBlock);
        } catch (Exception ex) {
            System.err.println("ApiResponse: " + ex.getMessage());
            return UNKNOWN_ERROR;
        }      
    }
    
    private String getAddresshistoryByTime(String[] params) {
        if ((params.length<3) || (params[2].length()==0))
            return MISSING_PARAMETER;
        String address = params[2];
        try (DbReader dbReader = new DbReader()) {
            int minTime = dbReader.getLowestTime();
            int maxTime = dbReader.getHighestTime();
            int bitmask = (params.length>5) ? Integer.parseInt(params[5]) : 3;
            int highTime = (params.length>4) ? Integer.parseInt(params[4]) 
                : maxTime;
            int lowTime = (params.length>3) ? Integer.parseInt(params[3]) 
                : maxTime - 10080*60;
            lowTime = validateTime(lowTime, minTime, maxTime);
            highTime = validateTime(highTime, minTime, maxTime);
            return dbReader.getHistorryByTime(address, bitmask, lowTime, highTime);
        } catch (Exception ex) {
            return UNKNOWN_ERROR;
        }
    }
    
    private String getTopBlocks(String[] params) {
        try (DbReader dbReader = new DbReader()) {
            int n = (params.length>2) ? Integer.parseInt(params[2]) : 20;
            n = (n<=0) ? 1 : (n>100) ? 100 : n;
            return dbReader.getBestBlocks(n).toString();
        } catch (Exception ex) {
            return UNKNOWN_ERROR;
        }
    }
        
}



// IPFS gates into config?
// txbyaddr - do it compatible with API v.1?
