/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hsqldb.cli.CliUtility;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl;

/**
 *
 * @author ivoaf
 */
public class HsqldbManager extends AbstractHandler{

    private static Server hsqldbServer;
    private static Map<String,DatabaseDescriptor> deployedDbs = new HashMap<>();
    public static final int DBS_PORT = 7030;
    public static final int MANAGER_PORT = 1111;
    private static volatile boolean accepting = false;
    private static volatile HttpServletResponse currentResponse = null;
    private static File deployed_dbs_File;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        sendResponse("HSQL Databases Manager...");
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(MANAGER_PORT);
        // Inidica que somente aceita conexões vinda da máquina local (localhost)
        ((ServerConnector)server.getConnectors()[0]).setPort(1111);
        ((ServerConnector)server.getConnectors()[0]).setHost("localhost");
        
        String jarRoot = "";
        try {
            jarRoot = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            if(!jarRoot.endsWith("/")) jarRoot += "/";
        } catch (URISyntaxException ex) {
        }
        deployed_dbs_File = new File(jarRoot+"deployed_dbs.db");
        if(!deployed_dbs_File.exists()){
            deployed_dbs_File.createNewFile();
            FileUtils.write(deployed_dbs_File, "{}", "UTF-8");
        } else {
            Type type = new TypeToken<HashMap<String, DatabaseDescriptor>>(){}.getType();
            deployedDbs = new Gson().fromJson(FileUtils.readFileToString(deployed_dbs_File, "UTF-8"), type);
        }
        
        
        try{
            startHsqlServer();
            server.setHandler(new HsqldbManager());
            server.start();
            accepting = true;
            server.join();
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("The manager is already running.");
            System.exit(0);
        }
    }

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse response) throws IOException, ServletException {
        currentResponse = response;
        if(!accepting) {
            rqst.setHandled(true);
            return;
        }
        if(hsr.getMethod().equals("POST")){
            
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text");
            Command command = new Gson().fromJson(leTodasLinhas(rqst.getReader()), Command.class);
            executeCommand(command);
            rqst.setHandled(true);
        }
        try {
            //deployHsqlDatabase("db2");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static void executeCommand(Command c){
        switch(c.getCommand()){
            case "deploy":
                deployHsqlDatabase(c);
                break;
            case "undeploy":
                undeployHsqlDatabase(c);
                break;
            case "list":
                if(deployedDbs.isEmpty()){
                    sendResponse("There's no deployed database.");
                    return;
                }
                sendResponse("Here's the list of names for currently deployed databases:");
                deployedDbs.forEach((n,dd) -> {
                    sendResponse("   - "+dd.name);
                });
                break;
            case "query_url":
                DatabaseDescriptor dd = deployedDbs.get(c.getName());
                if(dd == null){
                    sendResponse("none");
                    return;
                }
                sendResponse("jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName());
                break;
            case "stop":
                accepting = false;
                ScheduledExecutorService parador = Executors.newScheduledThreadPool(1);
                hsqldbServer.shutdown();
                parador.schedule(() -> {
                    while(!hsqldbServer.isNotRunning());
                    System.exit(0);
                }, 1500, TimeUnit.MILLISECONDS);
                break;
        }
        
        
    }
    
    private static void startHsqlServer(){
        if(hsqldbServer == null)
            hsqldbServer = new Server();
        if(deployedDbs.isEmpty()) return;
        HsqlProperties p = new HsqlProperties();
        p.setProperty("server.port",DBS_PORT);
        // set up the rest of properties
        deployedDbs.forEach((n,dd) -> {
            p.setProperty("server.database."+dd.number,"file:"+dd.path+"/"+n);
            p.setProperty("server.dbname."+dd.number,n);
        });
        try {
            hsqldbServer.setProperties(p);
        } catch (Exception ex) {
            sendResponse("Unable to set the HSQLDB properties, the server will not start.");
            return;
        }
        hsqldbServer.setLogWriter(null); // can use custom writer
        hsqldbServer.setErrWriter(null); // can use custom writer
        hsqldbServer.start();
    }
    
    private static void shutdownServer(){
        hsqldbServer.shutdown();
    }
    
    private static void deployHsqlDatabase(Command c){
        if(deployedDbs.containsKey(c.getName())){
            sendResponse("There's already an deployed database with the name '"+c.getName()+"'.");
            return;
        }
        if(deployedDbs.size() == 10){
            sendResponse("HSQLDB server supports the maximum of 10 deployed databases at the same time.");
            return;
        }
        sendResponse("Deploying database "+c.getName()+"...");
        deployedDbs.put(c.getName(), new DatabaseDescriptor(c.getName(),c.getPath(), deployedDbs.size()));
        shutdownServer();
        startHsqlServer();
        sendResponse("Database "+c.getName()+" succesfully deployed at port "+DBS_PORT+"...\n"
                + "    Connect to it via the URL 'jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+"'");
        //sendResponse(new Gson().toJson(deployedDbs));
        updateDeployedDbsFile();
    }
    
    private static void undeployHsqlDatabase(Command c){
        if(!deployedDbs.containsKey(c.getName())){
            sendResponse("There's no deployed database with the name '"+c.getName()+"'.");
            return;
        }
        deployedDbs.remove(c.getName());
        shutdownServer();
        startHsqlServer();
        sendResponse("Database "+c.getName()+" was succesfully removed from de deployed databases...");
        updateDeployedDbsFile();
    }
    

    public static String leTodasLinhas(BufferedReader in) {
        StringBuilder saida = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                saida.append(line);
            }
        } catch (Exception e) {
        }
        return saida.toString();
    }
    
    public static void sendResponse(String text){
        if(currentResponse == null) return;
        try {
            currentResponse.getWriter().println(text);
        } catch (IOException ex) {
        }
    }
    
    private static void updateDeployedDbsFile(){
        try {
            FileUtils.writeStringToFile(deployed_dbs_File, new Gson().toJson(deployedDbs), "UTF-8", false);
        } catch (IOException ex) {
        }
    }
}
