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
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.hsqldb.cmdline.SqlTool;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerConstants;

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
    private static File log_output_File;
    private static String jarRoot = "";
    private static final String NEW_LINE = System.getProperty("line.separator");
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(MANAGER_PORT);
        // Inidica que somente aceita conexões vindas da máquina local (localhost)
        ((ServerConnector)server.getConnectors()[0]).setPort(MANAGER_PORT);
        ((ServerConnector)server.getConnectors()[0]).setHost("localhost");
        try {
            jarRoot = new File(CliUtility.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            if(!jarRoot.endsWith("/")) jarRoot += "/";
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        deployed_dbs_File = new File(jarRoot+"deployed_dbs.db");
        if(!deployed_dbs_File.exists()){
            deployed_dbs_File.createNewFile();
            FileUtils.write(deployed_dbs_File, "{}", "UTF-8");
        } else {
            Type type = new TypeToken<HashMap<String, DatabaseDescriptor>>(){}.getType();
            deployedDbs = new Gson().fromJson(FileUtils.readFileToString(deployed_dbs_File, "UTF-8"), type);
        }
        log_output_File = new File(jarRoot+"logs.txt");
        createTrayIcon();
        try{
            startHsqlServer();
            if(!deployedDbs.isEmpty() && hsqldbServer.getState() != ServerConstants.SERVER_STATE_ONLINE){
                logException(new RuntimeException("HSQLDB Server couldn't start, verify if the port "+DBS_PORT+" is free."));
                return;
            }
            server.setHandler(new HsqldbManager());
            server.start();
            accepting = true;
            logInfo("HSQLDB Manager started.");
            server.join();
        } catch (Exception e){
            e.printStackTrace();
            logException(e);
            System.err.println("The manager is already running or the port "+MANAGER_PORT+" is occupied.");
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
        if(!accepting && !c.getCommand().equals("stop")) {
            return;
        }
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
                sendResponse("Here's the list of names, paths and URLs for the currently deployed databases:");
                deployedDbs.forEach((n,dd) -> {
                    sendResponse("   - "+dd.name+" -> "+dd.path+ " : "+"jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+dd.name);
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
            case "query_location":
                DatabaseDescriptor dd1 = deployedDbs.get(c.getName());
                if(dd1 == null){
                    sendResponse("none");
                    return;
                }
                String pathToSend = dd1.path;
                pathToSend = pathToSend.replace("\\", "/");
                if(!pathToSend.endsWith("/")) pathToSend = pathToSend.concat("/");
                sendResponse(pathToSend);
                break;
            case "stop":
                accepting = false;
                logInfo("HSQLDB Manager stopped.");
                shutdownServer();
                while(!hsqldbServer.isNotRunning());
                System.exit(0);
                break;
            case "backup":
                try {
                    logInfo("Backing up database "+c.getName()+"...");
                    DatabaseDescriptor dd2 = deployedDbs.get(c.getName());
                    if(dd2 == null){
                        sendResponse("none");
                        return;
                    }
                    String dbPath = dd2.path;
                    
                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss"); 
                    String arqname = c.getName() + "_" + dt.format(new Date());
                    File managedFiles = new File(dbPath+"/managedFiles");
                    if(managedFiles.isDirectory()){
                        FileUtils.copyDirectory(managedFiles, new File(jarRoot+"temp_bkp/managedFiles"));
                    }
                    SqlTool.objectMain(new String[]{"--inlineRc=url=jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+",user=SA,password=,transiso=TRANSACTION_READ_COMMITTED", "--sql="
                            + "BACKUP DATABASE TO '"+jarRoot+"temp_bkp/"+c.getName()+".tgz' NOT BLOCKING;"
                            + ""});
                    String finalPath;
                    if(new File(c.getPath()).isDirectory()){
                        finalPath = c.getPath()+ "/"+arqname+".zip";
                    }else{
                        finalPath = c.getPath();
                    }
                    
                    
                    pack(jarRoot+"temp_bkp", finalPath);
                    FileUtils.deleteDirectory(new File(jarRoot+"temp_bkp"));
                    sendResponse("Backup created in -> "+finalPath.replace("\\", "/"));
                    logInfo("Backed up database "+c.getName()+" in -> "+finalPath.replace("\\", "/"));
                } catch (Exception e) {
                    sendResponse("Exception during backup of the database, run the 'logs' command to see the Exception.");
                    logException(e);
                }
                break;
        }
        
        
    }
    
    private static void startHsqlServer(){
        if(hsqldbServer == null)
            hsqldbServer = new Server();
        if(deployedDbs.isEmpty()) return;
        HsqlProperties p = new HsqlProperties();
        p.setProperty("server.port",DBS_PORT);
        Contador i = new Contador();
        // set up the rest of properties
        deployedDbs.forEach((n,dd) -> {
            p.setProperty("server.database."+i.i,"file:"+dd.path+"/"+n);
            p.setProperty("server.dbname."+i.i,n);
            p.setProperty("server.acl",jarRoot+"acl.txt");
            i.i++;
        });
        try {
            hsqldbServer.setProperties(p);
        } catch (Exception ex) {
            logException(ex);
            System.exit(0);
            return;
        }
            System.err.println(2);
        hsqldbServer.setLogWriter(null); // can use custom writer
        hsqldbServer.setErrWriter(null); // can use custom writer
        hsqldbServer.setNoSystemExit(false);
            System.err.println(2);
        hsqldbServer.start();
            System.err.println(4);
    }
    
    private static void shutdownServer(){
        hsqldbServer.shutdownWithCatalogs(org.hsqldb.Database.CLOSEMODE_NORMAL);
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
        deployedDbs.put(c.getName(), new DatabaseDescriptor(c.getName(),c.getPath()));
        hsqldbServer.setNoSystemExit(true);
        shutdownServer();
        startHsqlServer();
        sendResponse("Database "+c.getName()+" successfully deployed at port "+DBS_PORT+"...\n"
                + "    Connect to it via the URL 'jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+"'");
        logInfo("Deployed database "+c.getName()+".");
        updateDeployedDbsFile();
        updateTrayMenus();
    }
    
    private static void undeployHsqlDatabase(Command c){
        if(!deployedDbs.containsKey(c.getName())){
            sendResponse("There's no deployed database with the name '"+c.getName()+"'.");
            return;
        }
        deployedDbs.remove(c.getName());
        hsqldbServer.setNoSystemExit(true);
        shutdownServer();
        startHsqlServer();
        sendResponse("Database "+c.getName()+" was successfully removed from de deployed databases...");
        logInfo("Undeployed database "+c.getName()+".");
        updateDeployedDbsFile();
        updateTrayMenus();
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
    
    public static void pack(String sourceDirPath, String zipFilePath) throws IOException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            Files.walk(pp)
              .filter(path -> !Files.isDirectory(path))
              .forEach(path -> {
                  ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                  try {
                      zs.putNextEntry(zipEntry);
                      Files.copy(path, zs);
                      zs.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
              });
        } catch (Exception e){
            logException(e);
        }
    }
    
    private static Menu openSwingMenu = null;
    private static Menu backupDbMenu = null;
    private static void createTrayIcon(){
        if(!SystemTray.isSupported()) return;
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon =
                new TrayIcon(Toolkit.getDefaultToolkit().getImage(jarRoot+"hsqldb-logo.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        final SystemTray tray = SystemTray.getSystemTray();
       
        openSwingMenu = new Menu("Open swing tool at database");
        backupDbMenu = new Menu("Backup database to user home");
        updateTrayMenus();
       
        //Add components to pop-up menu
        popup.add(openSwingMenu);
        popup.add(backupDbMenu);
        popup.addSeparator();
       
        MenuItem stopItem = new MenuItem("Stop HSQLDB Manager");
        stopItem.addActionListener((ev) -> executeCommand(new Command("stop")));
        popup.add(stopItem);
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("HSQLDB Manager is running.");
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            logException(e);
        }
        
    }
    
    private static void updateTrayMenus(){
        if(openSwingMenu == null || backupDbMenu == null) return;
        openSwingMenu.removeAll();
        backupDbMenu.removeAll();
        if(deployedDbs.isEmpty()){
            MenuItem mi = new MenuItem("There's no deployed database.");
            mi.setEnabled(false);
            MenuItem mib = new MenuItem("There's no deployed database.");
            mib.setEnabled(false);
            openSwingMenu.add(mi);
            backupDbMenu.add(mib);
        }
        String userHome = System.getProperty("user.home");
        deployedDbs.forEach((n, dd) -> {
            MenuItem osm = new MenuItem(n);
            osm.addActionListener((ev) -> CliUtility.openHsqldbSwing(new String[]{"swing", n}));
            openSwingMenu.add(osm);
            MenuItem bdm = new MenuItem(n);
            bdm.addActionListener((ev) -> executeCommand(new Command("backup", n, userHome)));
            backupDbMenu.add(bdm);
        });
    }
    
    private static void logInfo(String log){
        
        try {
            FileUtils.write(log_output_File, "INFO      ["+new Date()+" ("+System.currentTimeMillis()+")]: "+log+NEW_LINE, "UTF-8", true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void logException(Exception ex){
        StringBuilder sb = new StringBuilder("EXCEPTION ["+new Date()+" ("+System.currentTimeMillis()+")]: "+ex.getMessage()+NEW_LINE);
        
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (StackTraceElement s : stackTrace) {
            sb.append("\t\t\t\t\t\t\t\t\t\t").append(s.toString()).append(NEW_LINE);
        }
        try {
            FileUtils.write(log_output_File, sb.toString(), "UTF-8", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
