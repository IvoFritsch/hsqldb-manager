/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.webtool;

import hsqldb.manager.HsqldbManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Ivo
 */
public class WebtoolServlet extends HttpServlet{

    private final String UNAUTHORIZED_RESPONSE = new JSONObject().put("status", "UNAUTHORIZED").toString();
    private final String EXPIRED_RESPONSE = new JSONObject().put("status", "EXPIRED").toString();
    private final String BAD_REQUEST_RESPONSE = new JSONObject().put("status", "BAD_REQUEST").toString();
    private static volatile int nextConnId = 1;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doMethod(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        if(!HsqldbManager.RUNNING_FROM_JAR) resp.addHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type, jsessionid");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    }
    
    private void doMethod(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        if(!HsqldbManager.RUNNING_FROM_JAR) resp.addHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type, jsessionid");
        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        try{
            String fullPath = req.getServletPath();
            if(!fullPath.startsWith("/"))fullPath = "/" + fullPath;
            if(fullPath.equals("/") || fullPath.equals("/index.html")) fullPath = "/static/index.html";
            String masterPath = fullPath.split("/")[1];
            if(!masterPath.equals("static") && !fullPath.endsWith("/"))fullPath += "/";
            switch(masterPath){
                case "api":
                    supplyApi(req.getMethod()+" "+fullPath.split("/", 3)[2], req, resp);
                    break;
                case "static":
                    supplyStatic(fullPath.split("/", 3)[2], req, resp);
                    break;
            }
        } catch(Exception e){
        }
        if(!HsqldbManager.RUNNING_FROM_JAR) addSameSiteCookieAttribute(resp);
    }
    
    private void supplyStatic(String resource, HttpServletRequest request, HttpServletResponse response){
        
        if(!HsqldbManager.RUNNING_FROM_JAR) resource = "webtool_build/" + resource;
        resource = "webtool_build/" + resource;
        
        
        try{
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            OutputStream outStream;
            // if you want to use a relative path to context root:
            try (InputStream fileStream = HsqldbManager.getResource(resource)) {
                if(fileStream == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                // obtains ServletContext
                ServletContext context = getServletContext();
                // gets MIME type of the file
                String mimeType = context.getMimeType("a."+resource.substring(resource.lastIndexOf(".")));
                if (mimeType == null) {
                    // set to binary type if MIME mapping not found
                    mimeType = "application/octet-stream";
                }   // modifies response
                response.setContentType(mimeType);
                response.setHeader("Cache-Control", "public, max-age=31536000");
                // forces download
                String headerKey = "Content-Disposition";
                String headerValue = "inline";
                response.setHeader(headerKey, headerValue);
                // obtains response's output stream
                outStream = response.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalSize = 0;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    totalSize += bytesRead;
                    outStream.write(buffer, 0, bytesRead);
                }
                response.setContentLength(totalSize);
            }
            outStream.close();
        } catch(Exception e){}
    }
    
    
    private void addSameSiteCookieAttribute(HttpServletResponse response) {
        Collection<String> headers = response.getHeaders("Set-Cookie");
        boolean firstHeader = true;
        for (String header : headers) { // there can be multiple Set-Cookie attributes
            if (firstHeader) {
                response.setHeader("Set-Cookie", String.format("%s; %s", header, "SameSite=None"));
                firstHeader = false;
                continue;
            }
            response.addHeader("Set-Cookie", String.format("%s; %s", header, "SameSite=None"));
        }
    }
    
    private void supplyApi(String subPath, HttpServletRequest req, HttpServletResponse resp) throws IOException{
        HttpSession session = req.getSession(true);
        PrintWriter writer = resp.getWriter();
        if(!subPath.endsWith("/"))subPath += "/";
        String resource = subPath.split("/")[0];
        switch(resource){
            case "GET list":
                JSONArray arr = new JSONArray();
                HsqldbManager.getDeployedDbsNames().forEach(db -> arr.put(db));
                writer.println(arr.toString());
                break;
            case "GET init":
                supplyInit(subPath.split("/")[1], writer, req, resp);
                break;
            case "POST query":
                supplyQuery(subPath.split("/")[1], writer, req, resp);
                break;
            case "GET metadata":
                supplyMetadata(subPath.split("/")[1], writer, req, resp);
                break;
            case "GET keepalive":
                supplyKeepAlive(subPath.split("/")[1], writer, req, resp);
                break;
            default:
            writer.println("{\"status\": \"NOT_FOUND\"}");
        }
    }
    
    private void supplyKeepAlive(String connectionId, PrintWriter writer, HttpServletRequest req, HttpServletResponse resp){
        HttpSession session = req.getSession(true);
        // If this session was not permited
        if(session.getAttribute("permitted_"+connectionId) == null){
            writer.println(UNAUTHORIZED_RESPONSE);
            return;
        }
        ConnectionContainer cc = getSessionConnectionContainer(session, connectionId);
        if(!cc.isValid){
            writer.println(EXPIRED_RESPONSE);
            return;
        }
        cc.keepAlive();
        writer.println(new JSONObject().put("status", "OK").toString());
    }
    
    private void supplyMetadata(String connectionId, PrintWriter writer, HttpServletRequest req, HttpServletResponse resp){
        HttpSession session = req.getSession(true);
        // If this session was not permited
        if(session.getAttribute("permitted_"+connectionId) == null){
            writer.println(UNAUTHORIZED_RESPONSE);
            return;
        }
        ConnectionContainer cc = getSessionConnectionContainer(session, connectionId);
        if(!cc.isValid){
            writer.println(EXPIRED_RESPONSE);
            return;
        }
        JSONObject root = new JSONObject().put("status", "OK");
        JSONArray tablesArr = new JSONArray();
        root.put("tables", tablesArr);
        ResultSet tablesRs = null;
        ResultSet colsRs = null;
        try{
            List<SimpleTableInfo> tables = new ArrayList<>();
            tablesRs = cc.meta.getTables(null, null, null, new String[] {
            "TABLE", "GLOBAL TEMPORARY", "VIEW"
            });
            
            while (tablesRs.next()) {
                tables.add(new SimpleTableInfo(tablesRs));
            }

            tablesRs.close();
            tablesRs = null;
            
            for (SimpleTableInfo sti : tables) {
                JSONObject thisTable = new JSONObject().put("name", sti.name);
                JSONArray thisTableCols = new JSONArray();
                tablesArr.put(thisTable);
                thisTable.put("cols", thisTableCols);
                try{
                    colsRs = cc.meta.getColumns(null, sti.schema, sti.name, null);
                    
                    while (colsRs.next()) {
                        String c = colsRs.getString(4);
                        thisTableCols.put(c);
                    }
                }catch(Exception e){
                    
                }finally{
                    
                    if(colsRs != null) {
                        try {
                            colsRs.close();
                        } catch (SQLException se) {}
                    }
                }
            }
        } catch(Exception e) {
            
        } finally {
            if(tablesRs != null) {
                try {
                    colsRs.close();
                } catch (SQLException se) {}
            }
        }
        writer.println(root.toString());
        
    }
    
    private ConnectionContainer getSessionConnectionContainer(HttpSession session, String connId){
        return (ConnectionContainer) session.getAttribute(connId);
    }
    
    class SimpleTableInfo{
        String name;
        String schema;

        public SimpleTableInfo(ResultSet rs) throws SQLException {
            name = rs.getString(3);
            schema = rs.getString(2);
        }
        
        
    }
    
    private void supplyQuery(String connectionId, PrintWriter writer, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        // If this session was not permited
        if(session.getAttribute("permitted_"+connectionId) == null){
            writer.println(UNAUTHORIZED_RESPONSE);
            return;
        }
        ConnectionContainer cc = getSessionConnectionContainer(session, connectionId);
        if(!cc.isValid){
            writer.println(EXPIRED_RESPONSE);
            return;
        }
        String json = readInputStream(req.getInputStream());
        String sql = null;
        try {
            sql = new JSONObject(json).getString("sql");
        } catch (Exception e){
            writer.println(BAD_REQUEST_RESPONSE);
            return;
        }

        try {
            long entryTime = System.nanoTime();
            cc.stmt.execute(sql);

            int r = cc.stmt.getUpdateCount();

            if (r == -1) {

                try(ResultSet rs = cc.stmt.getResultSet()) {
                    writer.println(new JSONObject().put("status", "RESULT_SET").put("rs", mountResultSetJson(rs)).put("time", System.nanoTime() - entryTime).toString());
                } catch (Exception t) {
                    t.printStackTrace();
                }
            } else {
                writer.println(new JSONObject().put("status", "UPDATE").put("time", System.nanoTime() - entryTime).put("message", r + " rows were updated").toString());
            }
            HsqldbManager.logInfo("Connection "+connectionId+ " executed the following query:"+HsqldbManager.NEW_LINE+sql);
        } catch(Exception e){
            try {
                writer.println(new JSONObject().put("status", "SQL_ERROR").put("message", e.getMessage()).toString());
            } catch (Exception t) {
                t.printStackTrace();
            }
        }
    }
    
    public JSONObject mountResultSetJson(ResultSet r){
        JSONObject root = new JSONObject();
        JSONArray headers = new JSONArray();
        JSONArray types = new JSONArray();
        JSONArray data = new JSONArray();
        
        try {
            ResultSetMetaData m         = r.getMetaData();
            int               col       = m.getColumnCount();

            for (int i = 1; i <= col; i++) {
                headers.put(m.getColumnLabel(i));
                types.put(Webtool.getTypeName(m.getColumnType(i)));
            }

            while (r.next()) {
                JSONArray row = new JSONArray();
                
                for (int i = 1; i <= col; i++) {
                    try {
                        Object o = r.getObject(i);
                        if(o == null) o = "[null]";
                        row.put(o.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                data.put(row);
            }
            r.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return root.put("headers", headers).put("types", types).put("data", data);
    }
    
    
    public String readInputStream(InputStream inputStream) throws IOException {
 
	try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
		return br.lines().collect(Collectors.joining(System.lineSeparator()));
	}
    }
    
    private synchronized void supplyInit(String dbName, PrintWriter writer, HttpServletRequest req, HttpServletResponse resp){
        HttpSession session = req.getSession(true);
        session.setMaxInactiveInterval(-1);
        if(!HsqldbManager.getDeployedDbsNames().contains(dbName)){
            writer.println(UNAUTHORIZED_RESPONSE);
            return;
        }
        // If this session was not permited
        if(session.getAttribute("permitted_session") == null){
            if(!Webtool.permitNextSession()){
                writer.println(UNAUTHORIZED_RESPONSE);
                return;
            }
        }
        Webtool.setPermitNextSession(false);
        session.setAttribute("permitted_session", true);
        Connection conn = null;
        try{
           //STEP 2: Register JDBC driver
           Class.forName("org.hsqldb.jdbcDriver");

           //STEP 3: Open a connection
           conn = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:"+HsqldbManager.DBS_PORT+"/"+dbName, "SA", "");
           conn.setAutoCommit(false);
           String connId = dbName+"_"+nextConnId;
           nextConnId++;
           ConnectionContainer cc = new ConnectionContainer(connId);
           cc.conn = conn;
           cc.meta = conn.getMetaData();
           cc.stmt = conn.createStatement();
           cc.stmt.setMaxRows(100);
           session.setAttribute(connId, cc);
           session.setAttribute("permitted_"+connId, true);
           Webtool.putConnection(cc);
           HsqldbManager.logInfo("Webtool session openned to the "+dbName+" database from the IP "+req.getRemoteAddr()+", it received the connection ID '"+connId+"'.");
           writer.println(new JSONObject().put("status", "OK").put("connectionId", connId).toString());
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
