/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqlman.webtool;

import hsqlman.manager.HsqldbManager;
import hsqlman.manager.NoLogging;
import java.awt.TrayIcon;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author Ivo
 */
public class Webtool implements Runnable {

    
    public static final int WEBTOOL_PORT = 35888;
    
    public static final long CONNECTION_TIMEOUT = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    
    private static boolean status = false;
    private static org.eclipse.jetty.server.Server server;
    
    private static volatile boolean permitNextSession = false;
    private static ScheduledExecutorService activityVerifier;
    private static List<ConnectionContainer> openConnections;
    
    private static final Map<Integer, String> TYPES_DICTIONARY = new HashMap<>();
    static{
        Field[] fields = Types.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                TYPES_DICTIONARY.put(field.getInt(null), field.getName());
            } catch (Exception ex) {
            }
        }
    }
    
    public static void startWebTool(){
        if(status) {
            HsqldbManager.sendResponse("Webtool is already running.");
            return;
        }
        openConnections = Collections.synchronizedList(new ArrayList<>());
        permitNextSession = true;
        new Thread(new Webtool()).start();
        
        activityVerifier = Executors.newSingleThreadScheduledExecutor();
        activityVerifier.scheduleAtFixedRate(() -> {
            List<ConnectionContainer> checkList = new ArrayList<>(openConnections);
            checkList.forEach(cc -> {
                if(cc.timedOut()){
                    cc.expire();
                    openConnections.remove(cc);
                }
            });
            if(openConnections.isEmpty()){
                HsqldbManager.logInfo("WebTool will stop due to no more active connections.");
                Webtool.stopWebTool(true, true);
            }
        }, 3, 1, TimeUnit.MINUTES);
        HsqldbManager.sendResponse("Webtool server started succesfully...\n"
                + "    Access it through the port "+WEBTOOL_PORT+".");
        HsqldbManager.newTrayNotification("Webtool server is now running", "Access it through the port "+WEBTOOL_PORT+".", TrayIcon.MessageType.INFO);
        HsqldbManager.logInfo("Web access tool started at port "+WEBTOOL_PORT+".");
        
    }
    
    public static void stopWebTool() {
        stopWebTool(false, true);
    }
    
    public static void stopWebTool(boolean inactivity, boolean showNotif) {
        if(!status) {
            HsqldbManager.sendResponse("Webtool is not running.");
            return;
        }
        openConnections.forEach(c -> {
            c.expire();
        });
        try {
            activityVerifier.shutdown();
            status = false;
            server.stop();
            server = null;
            openConnections = null;
            activityVerifier = null;
            HsqldbManager.sendResponse("Webtool server stopped succesfully.");
            HsqldbManager.logInfo("Web access tool stopped.");
            if(showNotif) HsqldbManager.newTrayNotification("Webtool server stopped", inactivity ? "It stopped due to inactivity." : "", TrayIcon.MessageType.INFO);
            HsqldbManager.updateTrayMenus();
        } catch (Exception ex) {
        }
    }
    public static boolean permitNextSession() {
        return permitNextSession;
    }

    public static void setPermitNextSession(boolean permitNextSession) {
        if(!status) {
            HsqldbManager.sendResponse("Webtool is not running.");
            return;
        }
        Webtool.permitNextSession = permitNextSession;
        if(permitNextSession){
            HsqldbManager.sendResponse("The next client will be permitted to connect.");
        }
    }
    
    @Override
    public void run() {
        try {
            org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
            server = new org.eclipse.jetty.server.Server(WEBTOOL_PORT);
            
            ServletContextHandler ctxHand = new ServletContextHandler(ServletContextHandler.SESSIONS);
            ctxHand.setContextPath("/");
            ctxHand.setServer(server);
            ctxHand.addServlet(new ServletHolder(new WebtoolServlet()), "/");
            
            server.setHandler(ctxHand);
            server.start();
            status = true;
            HsqldbManager.updateTrayMenus();
            server.join();
        } catch (Exception ex) {
            HsqldbManager.logException(ex);
            throw new RuntimeException(ex);
        }
    }
    
    public static String getTypeName(int type){
        return TYPES_DICTIONARY.get(type);
    }
    
    public static boolean getStatus(){
        return status;
    }
    
    public static void putConnection(ConnectionContainer cc){
        openConnections.add(cc);
    }
    
    
    
}