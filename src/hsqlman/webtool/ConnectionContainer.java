/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqlman.webtool;

import hsqlman.manager.HsqldbManager;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import javax.servlet.http.HttpSession;
import static hsqlman.webtool.Webtool.CONNECTION_TIMEOUT;

/**
 *
 * @author Ivo
 */
public class ConnectionContainer {
    public String connectionId;
    public Statement stmt;
    public Connection conn;
    public DatabaseMetaData meta;
    public long lastKeepAlive;
    public boolean isValid;

    public ConnectionContainer(String connectionId) {
        this.lastKeepAlive = System.currentTimeMillis();
        this.connectionId = connectionId;
        this.isValid = true;
    }

    public boolean timedOut(){
        return (System.currentTimeMillis() - lastKeepAlive) > CONNECTION_TIMEOUT;
    }

    public void expire(){
        try{
            HsqldbManager.logInfo("Connection "+connectionId+" expired due to keepalive inactivity.");
            isValid = false;
            conn.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void keepAlive(){
        this.lastKeepAlive = System.currentTimeMillis();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(!(obj instanceof ConnectionContainer)) return false;
        return this.connectionId.equals(((ConnectionContainer)obj).connectionId);
    }

    @Override
    public int hashCode() {
        return this.connectionId.hashCode();
    }
}
