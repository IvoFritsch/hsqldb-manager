/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsqldb.cli;

import com.google.gson.Gson;
import hsqldb.manager.Command;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 0186779
 */
public class CliUtility {
    
    private final static int COMMAND = 0;
    private final static int FIRST_ARG = 1;
    
    public static void main(String[] args) {
        
        if(args.length == 0){
            System.out.println("Send commands to the HSQL Databases Manager.\n"
                    + "    Usage:\n"
                    + "    deploy <db_name>    -  Deploy an database with the provided name, storing its files in the current\n"
                    + "                           CLI location.\n"
                    + "    undeploy <db_name>  -  Undeploy the database with the provided name, keeping its files as it is.\n"
                    + "    list_deployed       -  List all the currently deployed and running databases.");
            return;
        }
        
        switch(args[COMMAND]){
            case "deploy":
                sendDeploy(args);
                break;
        }
    }

    private static void sendDeploy(String[] args) {
        if(args.length < 2){
            System.out.println("Deploy an database with the provided name, storing its files in the current CLI location.\n"
                    + "    Usage:\n"
                    + "    deploy <db_name>");
        }
        
        File target = new File(args[FIRST_ARG]);
        target.mkdir();
        if(!target.exists()) {
            System.out.println("Coudn't create the directory to the database files, check if the name is valid.");
        }
        
        try {
            sendCommand(new Command("deploy", args[FIRST_ARG], target.getAbsolutePath()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    
    // HTTP GET request
    private static void sendCommand(Command c) throws Exception {

            String url = "http://localhost:1111/";

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("POST");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(new Gson().toJson(c));
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

    }
}
