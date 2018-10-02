/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

/**
 *
 * @author ivoaf
 */
public class DatabaseDescriptor {
    
    public String name;
    public String path;

    public DatabaseDescriptor(String name, String path) {
        this.name = name;
        this.path = path;
    }
    
    
}
