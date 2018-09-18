/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private static int DBS_PORT = 7030;
    private static int MANAGER_PORT = 1111;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.println("HSQL Databases Manager...");
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(MANAGER_PORT);
        // Inidica que somente aceita conexões vinda da máquina local (localhost)
        ((ServerConnector)server.getConnectors()[0]).setPort(1111);
        ((ServerConnector)server.getConnectors()[0]).setHost("localhost");
        File deployed_dbs_File = new File("deployed_dbs.db");
        if(!deployed_dbs_File.exists())
            deployed_dbs_File.createNewFile();
        else {
            
        }
        
        
        try{
            startHsqlServer();
            server.setHandler(new HsqldbManager());
            server.start();
            server.join();
        } catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse response) throws IOException, ServletException {
        System.out.println(hsr.getMethod());
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
            System.err.println("Unable to set the HSQLDB properties, the server will not start.");
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
            System.out.println("There's already an deployed database with the name '"+c.getName()+"'.");
            return;
        }
        if(deployedDbs.size() == 10){
            System.out.println("HSQLDB server supports the maximum of 10 deployed databases at the same time.");
            return;
        }
        System.out.println("Deploying database "+c.getName()+"...");
        deployedDbs.put(c.getName(), new DatabaseDescriptor(c.getName(),c.getPath(), deployedDbs.size()));
        shutdownServer();
        startHsqlServer();
        System.out.println("Database "+c.getName()+" succesfully deployed at port "+DBS_PORT+"...\n"
                + "    Connect to it via the URL 'jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+"'");
        System.out.println(new Gson().toJson(deployedDbs));
    }
    
    private static void undeployHsqlDatabase(String name) throws Exception{
        if(!deployedDbs.containsKey(name)){
            System.out.println("There's no deployed database with the name '"+name+"'.");
            return;
        }
        System.out.println("Undeploying database "+name+"...");
        deployedDbs.remove(name);
        shutdownServer();
        startHsqlServer();
        System.out.println("Database "+name+" was succesfully removed from de deployed databases...");
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
}
