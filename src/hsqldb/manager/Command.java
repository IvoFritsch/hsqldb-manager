/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.manager;

/**
 *
 * @author 0186779
 */
public class Command {
    private String command;
    private String name;
    private String path;

    public Command(String command, String name, String path) {
        this.command = command;
        this.name = name;
        this.path = path;
    }

    public String getCommand() {
        return command;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
    
    
    
    
}
