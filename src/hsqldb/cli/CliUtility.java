/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.cli;

import com.google.gson.Gson;
import hsqldb.manager.Command;
import hsqldb.manager.HsqldbManager;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hsqldb.cmdline.SqlTool;

/**
 *
 * @author 0186779
 */
public class CliUtility {
    
    private final static int COMMAND = 0;
    private final static int FIRST_ARG = 1;
    
    public static void main(String[] args){
        if(args.length == 0){
            System.out.println("Send commands to the HSQL Databases Manager.\n"
                    + "    Usage:\n"
                    + "    start               -  Start the HSQLDB Manager, running all the deployed databases.\n"
                    + "    stop                -  Stop all the running HSQLDB instances.\n"
                    + "    status              -  Display if the manager is currently running.\n"
                    + "    deploy <db_name>    -  Deploy an database with the provided name, storing its files in the current\n"
                    + "                           CLI location.\n"
                    + "    undeploy <db_name>  -  Undeploy the database with the provided name, keeping its files as it is.\n"
                    + "    list                -  List all the currently deployed and running databases.\n"
                    + "    sqltool <db_name>   -  Open the SQL access tool in the provided database.");
            return;
        }
        
        switch(args[COMMAND]){
            case "deploy":
                sendDeploy(args);
                break;
            case "start":
                startManager();
                break;
            case "stop":
                sendStop();
                break;
            case "list":
                sendList();
                break;
            case "status":
                if(managerAvailabilityCheck())
                    System.out.println("The manager IS RUNNING");
                else
                    System.out.println("The manager IS NOT RUNNING");
                break;
            case "sqltool":
                openSqlTool(args);
                break;
        }
    }

    private static void sendDeploy(String[] args) {
        if(args.length < 2){
            System.out.println("Deploy an database with the provided name, storing its files in the current CLI location.\n"
                    + "    Usage:\n"
                    + "    deploy <db_name>");
            return;
        }
        
        File target = new File(args[FIRST_ARG]);
        target.mkdir();
        if(!target.exists()) {
            System.out.println("Coudn't create the directory to the database files, check if the name is valid.");
        }
        String resp = sendCommand(new Command("deploy", args[FIRST_ARG], target.getAbsolutePath()));
        if(resp == null) return;
        System.out.println(resp);
    }
    
    private static void sendStop() {
        sendCommand(new Command("stop"));
    }

    private static void startManager(){
        if(managerAvailabilityCheck()){
            System.err.println("The manager is already running.");
            return;
        }
        
        String jarPath = null;
        try {
            jarPath = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException ex) {
            System.err.println("Couldn't start the HSQLDB Manager, unable to find the .jar file.");
            return;
        }
        try {
            new ProcessBuilder("java", "-cp", jarPath, "hsqldb.manager.HsqldbManager").start();
            Thread.sleep(1000);
            if(managerAvailabilityCheck())
                System.out.println("Manager started succesfully.");
            else
                System.err.println("Manager couldn't start.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    // HTTP GET request
    private static String sendCommand(Command c){
        if(!managerAvailabilityCheck()){
            System.err.println("The manager is not running, start it with the 'start' command");
            return null;
        }

        String url = "http://localhost:"+HsqldbManager.MANAGER_PORT+"/";

        URL obj;
        HttpURLConnection con;
        try {
            obj = new URL(url);
            con = (HttpURLConnection) obj.openConnection();
        } catch (Exception ex) {
            return null;
        }
        try {
            // optional default is GET
            con.setRequestMethod("POST");
        } catch (ProtocolException ex) {
        }
        con.setDoOutput(true);
        try {
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(new Gson().toJson(c));
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                response.append("\n");
            }
            in.close();
            return response.toString();
        } catch (IOException ex) {
            System.err.println("Could not communicate with the HSQLDB Manager, it's probably not running, start it with the 'start' command");
        }
        return null;
    }
    public static boolean managerAvailabilityCheck() { 
        try (Socket s = new Socket("localhost", HsqldbManager.MANAGER_PORT)) {
            return true;
        } catch (IOException ex) {
            /* ignore */
        }
        return false;
    }

    private static void openSqlTool(String[] args) {
        if(args.length < 2){
            System.out.println("Open the SQL access tool in the provided database.\n"
                    + "    Usage:\n"
                    + "    sqltool <db_name>");
            return;
        }
        
        String url = sendCommand(new Command("query_url", args[FIRST_ARG])).trim();
        if(url.equals("none")){
            System.err.println("There's no deployed database with the name '"+args[FIRST_ARG]+"'.");
            return;
        }
        System.setProperty("sqltool.REMOVE_EMPTY_VARS", "false");
        SqlTool.main(new String[]{"--inlineRc=url="+url+",user=SA"});
    }

    private static void sendList() {
        String list = sendCommand(new Command("list"));
        if(list == null) return;
        System.out.println(list);
        
    }
}