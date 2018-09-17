/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

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
import org.eclipse.jetty.server.Request;
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
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        int PORTA = 1111;
        System.out.println("HSQL Databases Manager...\n");
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(PORTA);
        try{
            startHsqlServer();
            //deployHsqlDatabase("db1");
            server.setHandler(new HsqldbManager());
            server.start();
            server.join();
        } catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, HttpServletResponse hsr1) throws IOException, ServletException {
        try {
            System.out.println("Deploying...");
            deployHsqlDatabase("db2");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static void startHsqlServer() throws Exception{
    
        HsqlProperties p = new HsqlProperties();
        //p.setProperty("server.database.0","file:db/"+name);
        //p.setProperty("server.dbname.0","hehe");
        p.setProperty("server.port","7030");
        p.setProperty("server.remote_open", "true");
        // set up the rest of properties

        // alternative to the above is
        hsqldbServer = new Server();
        hsqldbServer.setProperties(p);
        //hsqldbServer.setLogWriter(null); // can use custom writer
        //hsqldbServer.setErrWriter(null); // can use custom writer
        hsqldbServer.start();
        System.out.println("vai");
    }
    
    
    private static void deployHsqlDatabase(String name) throws Exception{
        if(deployedDbs.containsKey(name)){
            System.out.println("Database "+name+" already deployed");
            return;
        }
        System.out.println("jdbc:hsqldb:hsql://localhost:7030/"+name+";filepath=db/"+name+"/"+name);
        Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:7030/"+name+";filepath=db/"+name+"/"+name, "SA", "");
        c.close();
    }
    
}
