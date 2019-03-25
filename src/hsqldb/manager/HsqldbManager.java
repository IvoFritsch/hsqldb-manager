/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hsqldb.cli.CliUtility;
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
    private static String jarRoot = "";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        org.eclipse.jetty.util.log.Log.setLog(new NoLogging());
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(MANAGER_PORT);
        // Inidica que somente aceita conexões vindas da máquina local (localhost)
        ((ServerConnector)server.getConnectors()[0]).setPort(1111);
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
        
        
        try{
            startHsqlServer();
            if(!deployedDbs.isEmpty() && hsqldbServer.getState() != ServerConstants.SERVER_STATE_ONLINE){
                return;
            }
            server.setHandler(new HsqldbManager());
            server.start();
            accepting = true;
            server.join();
        } catch (Exception e){
            e.printStackTrace();
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
                shutdownServer();
                while(!hsqldbServer.isNotRunning());
                System.exit(0);
                break;
            case "backup":
                try {
                    DatabaseDescriptor dd2 = deployedDbs.get(c.getName());
                    if(dd2 == null){
                        sendResponse("none");
                        return;
                    }
                    String dbPath = dd2.path;
                    
                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh_mm_ss"); 
                    String arqname = c.getName() + " " + dt.format(new Date());
                    File managedFiles = new File(dbPath+"/managedFiles");
                    if(managedFiles.isDirectory()){
                        FileUtils.copyDirectory(managedFiles, new File(jarRoot+"temp_bkp/managedFiles"));
                    }
                    SqlTool.objectMain(new String[]{"--inlineRc=url=jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+",user=SA,password=,transiso=TRANSACTION_READ_COMMITTED", "--sql="
                            + "BACKUP DATABASE TO '"+jarRoot+"temp_bkp/"+c.getName()+".tgz' NOT BLOCKING;"
                            + ""});
                    
                    pack(jarRoot+"temp_bkp", c.getPath()+"/"+arqname+".zip");
                    FileUtils.deleteDirectory(new File(jarRoot+"temp_bkp"));
                    sendResponse("Backup created in -> "+c.getPath()+"/"+arqname+".zip");
                } catch (Exception e) {
                    e.printStackTrace();
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
            ex.printStackTrace();
            sendResponse("Unable to set the HSQLDB properties, the server will not start.");
            return;
        }
        hsqldbServer.setLogWriter(null); // can use custom writer
        hsqldbServer.setErrWriter(null); // can use custom writer
        hsqldbServer.setNoSystemExit(false);
        hsqldbServer.start();
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
        sendResponse("Database "+c.getName()+" succesfully deployed at port "+DBS_PORT+"...\n"
                + "    Connect to it via the URL 'jdbc:hsqldb:hsql://localhost:"+DBS_PORT+"/"+c.getName()+"'");
        updateDeployedDbsFile();
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
                    System.err.println(e);
                }
              });
        }
    }
    
}
