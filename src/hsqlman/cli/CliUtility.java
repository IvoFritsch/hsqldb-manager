/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqlman.cli;

import com.google.gson.Gson;
import hsqlman.manager.Command;
import hsqlman.manager.HsqldbManager;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.hsqldb.cmdline.SqlTool;

/**
 *
 * @author 0186779
 */
public class CliUtility {
    
    private final static int COMMAND = 0;
    private final static int FIRST_ARG = 1;
    private final static int SECOND_ARG = 2;
    
    public static void main(String[] args){
        //args = new String[]{"sqltool", "bestbit-platform"};
        System.out.println("HSQLMAN - HSQL Databases Manager - Haftware SI 2019");
        if(args.length == 0){
            printHelp();
            return;
        }
        if(args.length > 0){
            switch(args[COMMAND]){
                case "deploy":
                    sendDeploy(args);
                    return;
                case "undeploy":
                    sendUndeploy(args);
                    return;
                case "start":
                    startManager();
                    return;
                case "stop":
                    sendStop();
                    return;
                case "list":
                    sendList();
                    return;
                case "status":
                    if(managerAvailabilityCheck())
                        System.out.println("The manager IS RUNNING.");
                    else
                        System.out.println("The manager IS NOT RUNNING.");
                    return;
                case "sqltool":
                    openSqlTool(args);
                    return;
                case "backup":
                    sendBackup(args);
                    return;
                case "swing":
                    openHsqldbSwing(args);
                    return;
                case "logs":
                    InputStream input;
                    try {
                        String jarRoot = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
                        if(!jarRoot.endsWith("/")) jarRoot += "/";
                        File logsFile = new File(jarRoot+"logs.txt");
                        if(!logsFile.exists()) {
                            System.out.println("'logs.txt' file doesn't exist.");
                            return;
                        }
                        System.out.println("'logs.txt' file content:\n");
                        input = new BufferedInputStream(new FileInputStream(logsFile));
                        byte[] buffer = new byte[8192];

                        for (int length = 0; (length = input.read(buffer)) != -1;) {
                            System.out.write(buffer, 0, length);
                        }
                        input.close();
                    } catch(Exception e){
                        e.printStackTrace();
                    } finally {
                    }
                    return;
                case "clearlogs":
                    try {
                        String jarRoot = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
                        if(!jarRoot.endsWith("/")) jarRoot += "/";
                        File logsFile = new File(jarRoot+"logs.txt");
                        if(!logsFile.exists()) {
                            System.out.println("'logs.txt' file doesn't exist.");
                            return;
                        }
                        FileUtils.write(logsFile, "", "UTF-8");
                    } catch(Exception e){
                        e.printStackTrace();
                    } finally {
                    }
                    System.out.println("The 'logs.txt' file was cleared.");
                    return;
                case "webtool":
                    sendWebtoolCommand(args);
                    return;
            }
        }
        printHelp();
    }

    private static void printHelp(){
        System.out.println("Send commands to the HSQL Databases Manager.\n"
                + "    Usage:\n"
                + "    start                     -  Start the HSQLDB Manager, running all the deployed databases.\n"
                + "    stop                      -  Stop all the running HSQLDB instances.\n"
                + "    status                    -  Display if the manager is currently running.\n"
                + "    deploy <db_name>          -  Deploy an database with the provided name, creating it if doesn't exist, and storing its files in the current CLI location.\n"
                + "    undeploy <db_name>        -  Undeploy the database with the provided name, keeping its files as it is.\n"
                + "    list                      -  List all the currently deployed and running databases.\n"
                + "    sqltool <db_name>         -  Open the SQL access tool in the provided database.\n"
                + "    swing [<db_name>]         -  Open HSQLDB swing access tool in the provided database(optional).\n"
                + "    webtool start             -  Start the web access tool server.\n"
                + "    webtool pemit             -  Permits the next client to open an session in the access tool.\n"
                + "    webtool stop              -  Stop the web access tool server, invalidating all the sessions.\n"
                + "    backup <db_name> [<file>] -  Makes an hot backup of the database to the current CLI location or provided file/directory(optional).\n"
                + "    logs                      -  Print the 'logs.txt' file.\n"
                + "    clearlogs                 -  Clear the 'logs.txt' file.");
    }
    
    private static void sendDeploy(String[] args) {
        if(args.length < 2){
            System.out.println("Deploy an database with the provided name, creating it if doesn't exist, and storing its files in the current CLI location.\n"
                    + "    Usage:\n"
                    + "    deploy <db_name>");
            return;
        }
        if(!managerAvailabilityCheck()){
            System.err.println("The manager is not running, start it with the 'start' command");
            return;
        }
        if(args[FIRST_ARG].contains("/") || args[FIRST_ARG].contains("\\") || args[FIRST_ARG].contains(":") || 
           args[FIRST_ARG].contains("*") || args[FIRST_ARG].contains("?") || args[FIRST_ARG].contains("\"") || 
           args[FIRST_ARG].contains("<") || args[FIRST_ARG].contains(">") || args[FIRST_ARG].contains("|")){
            System.err.println("Invalid database name, the name can't contain any of these characters:\n"
                    + "    / \\ : * ? \" < > |");
            return;
        }
        
        String url = sendCommand(new Command("query_url", args[FIRST_ARG]));
        if(!url.equals("none")){
            System.err.println("There's already an deployed database with the name '"+args[FIRST_ARG]+"'.");
            return;
        }
        File target = new File(args[FIRST_ARG]);
        target.mkdir();
        if(!target.exists()) {
            System.err.println("Coudn't create the directory to the database files, check if the name is valid.");
        }
        String resp = sendCommand(new Command("deploy", args[FIRST_ARG], target.getAbsolutePath()));
        if(resp == null) return;
        System.out.println(resp);
    }

    private static void sendUndeploy(String[] args) {
        if(args.length < 2){
            System.out.println("Undeploy the database with the provided name, keeping its files as it is.\n"
                    + "    Usage:\n"
                    + "    undeploy <db_name>");
            return;
        }
        String resp = sendCommand(new Command("undeploy", args[FIRST_ARG]));
        if(resp == null) return;
        System.out.println(resp);
    }

    private static void sendWebtoolCommand(String[] args) {
        if(args.length < 2){
            printWebtoolHelp();
            return;
        }
        switch(args[FIRST_ARG]){
            case "start": case "permit": case "stop":
                break;
            default:
                printWebtoolHelp();
                return;
        }
        String resp = sendCommand(new Command("webtool", args[FIRST_ARG]));
        if(resp == null) return;
        System.out.println(resp);
    }
    
    private static void printWebtoolHelp(){
        System.out.println("Control the web access tool.\n"
                + "    Usage:\n"
                + "    webtool start    -  Start the web access tool server.\n"
                + "    webtool permit   -  Permits the next client to open an session in the access tool.\n"
                + "    webtool stop     -  Stop the web access tool server, invalidating all the sessions.");
    }
    
    private static void sendStop() {
        sendCommand(new Command("stop"));
    }

    private static void sendBackup(String[] args) {
        if(args.length < 2){
            System.out.println("Makes an hot backup of the database to the current CLI location or provided file/directory(optional).\n"
                    + "    Usage:\n"
                    + "    backup <db_name> [<file>]");
            return;
        }
        
        String url = sendCommand(new Command("query_url", args[FIRST_ARG]));
        if(url == null) return;
        url = url.trim();
        if(url.equals("none")){
            System.err.println("There's no deployed database with the name '"+args[FIRST_ARG]+"'.");
            return;
        }
        File bkpFile = null;
        if(args.length > 2){
            bkpFile = new File(args[SECOND_ARG]);
            
            if(!bkpFile.isDirectory() && !args[SECOND_ARG].endsWith(".zip")){
               args[SECOND_ARG] = args[SECOND_ARG] + ".zip";
               bkpFile = new File(args[SECOND_ARG]);
            }
            if(bkpFile.exists() && bkpFile.isFile()){
                Scanner in = new Scanner(System.in);
                System.out.print("The file '"+bkpFile.getAbsolutePath()+"' already exists, overwrite? (Y/_): ");
                String overwrite = in.nextLine();
                if(overwrite.trim().isEmpty())
                    overwrite = "N";
                if(!overwrite.toUpperCase().equals("Y")){
                    System.err.println("Backup aborted.");
                    return;
                }
                bkpFile.delete();
            }
        }
        
        bkpFile = bkpFile == null ? new File("") : bkpFile;
        System.out.println("Backing up the database, this can take some time...");
        String resp = sendCommand(new Command("backup", args[FIRST_ARG], bkpFile.getAbsolutePath()));
        if(resp == null) return;
        System.out.println(resp);
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
            System.out.println("Starting the HSQLDB Manager...");
            Process manager = new ProcessBuilder("java", "-cp", jarPath, "hsqlman.manager.HsqldbManager").start();
            int timeout = 0;
            while(timeout < 15){
                Thread.sleep(333);
                if(managerAvailabilityCheck()){
                    System.out.println("Manager started succesfully.");
                    System.out.println(sendCommand(new Command("list")));
                    break;
                }
                timeout++;
            }
            if(timeout >= 15){
                if(manager.isAlive()){
                    System.err.println("Manager doesn't start yet, but it's still initializing.");
                } else {
                    System.err.println("Manager couldn't start, verify if the ports "+HsqldbManager.MANAGER_PORT+" and "+HsqldbManager.DBS_PORT+" are free for the manager and the database. If they are, run the 'logs' command to see the possible cause of the error.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    // HTTP GET request
    public static String sendCommand(Command c){
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
            return response.toString().trim();
        } catch (IOException ex) {
            if(c.getCommand().equals("stop"))
                System.out.println("Manager stopped succesfully.");
            else
                System.err.println("Could not communicate with the HSQLDB Manager, it's probably not running, start it with the 'start' command.");
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
        
        String url = sendCommand(new Command("query_url", args[FIRST_ARG]));
        if(url == null) return;
        url = url.trim();
        if(url.equals("none")){
            System.err.println("There's no deployed database with the name '"+args[FIRST_ARG]+"'.");
            return;
        }
        Scanner in = new Scanner(System.in);
        System.out.print("Do you want to use auto-commit for this session? (Y/_): ");
        String autocommit_string = in.nextLine();
        boolean autocommit = false;
        if(autocommit_string.trim().isEmpty())
            autocommit_string = "N";
        if(autocommit_string.toUpperCase().equals("Y")){
            autocommit = true;
        }
        System.setProperty("sqltool.REMOVE_EMPTY_VARS", "false");
        SqlTool.main(new String[]{"--inlineRc=url="+url+",user=SA,password=,transiso=TRANSACTION_READ_COMMITTED", autocommit ? "--autoCommit" : "--continueOnErr=true"});
    }

    private static void sendList() {
        String list = sendCommand(new Command("list"));
        if(list == null) return;
        System.out.println(list);
    }
    
    public static void openHsqldbSwing(String[] args){
        String jarPath = null;
        try {
            jarPath = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException ex) {
            System.err.println("Couldn't start the HSQLDB Swing, unable to find the .jar file.");
            return;
        }
        
        if(GraphicsEnvironment.isHeadless()){
            System.err.println("This environment doesn't support the java swing.\n"
                    + "    Use the 'sqltool <db_name>' command to access the database via command line.");
            return;
        }
        
        ProcessBuilder processo;
        String[] argumentos;
        if (args.length < 2){
            System.out.println("Starting HSQLDB swing utility...");
            argumentos = new String[] {"java",
                                       "-cp", 
                                       jarPath,
                                       "org.hsqldb.util.DatabaseManagerSwing"};
            
        } else {
            String url = sendCommand(new Command("query_url", args[FIRST_ARG]));
            if(url == null) return;
            url = url.trim();
            if(url.equals("none")){
                System.err.println("There's no database with the name '"+args[FIRST_ARG]+"'.");
                return;
            }
            System.out.println("Starting HSQLDB swing utility connected to the '"+args[FIRST_ARG]+"' database...");
            argumentos = new String[] {"java",
                                       "-cp", 
                                       jarPath,
                                       "org.hsqldb.util.DatabaseManagerSwing",
                                       "--driver",
                                       "org.hsqldb.jdbcDriver",
                                       "--url",
                                       "jdbc:hsqldb:hsql://localhost:7030/" + args[FIRST_ARG],
                                       "--user",
                                       "SA"};
        }
        processo = new ProcessBuilder(argumentos);
        try {
            processo.start();
        } catch (IOException ex) {
            System.err.println("Couldn't start the HSQLDB swing utility.");
        }
    } 
}
