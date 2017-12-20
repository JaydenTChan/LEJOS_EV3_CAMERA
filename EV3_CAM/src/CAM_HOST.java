import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.Button;

public class CAM_HOST {
	
	public static DataInputStream in;
	public static DataOutputStream out;
	
	public static int port = 1112;
    
    public static void main(String[] args) {

		try {
			ServerSocket serv = new ServerSocket(port);
			System.out.println("Waiting for connection...");
			Socket s = serv.accept(); //Wait for Lap top to connect
			System.out.println("Connected!");
			in = new DataInputStream(s.getInputStream());
			out = new DataOutputStream(s.getOutputStream());
				
			String action = "";
			
			while(Button.getButtons()==0) {
				
				//Reads action from CAMERA application
				action = in.readUTF();
                
			}

			serv.close();
			out.close();
		}catch(IOException ioe){
			System.out.println("Error in connection");
		}
	}	
}
