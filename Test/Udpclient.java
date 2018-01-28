import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

/*
  class performs functionality of a client on a udp network
  @author: Andrew Olesak
  CIS 457
 */
class Udpclient{
    public static void main(String args[]){
	boolean exit = false;
	try{
	    // open a channel
	    DatagramChannel c = DatagramChannel.open();
	    Console cons = System.console();
	    // read in an IP address and port number
	    String IPAddr = cons.readLine("Please enter an IP Address: ");
	    String port = cons.readLine("Please enter a port number: ");
	    int portNum = Integer.parseInt(port);
	    // if the user enters exit, this value will be 
	    // changed true and the loop will exit
	    while(!exit){
		String m = cons.readLine("Enter your message or exit: " );
		// if user enters exit, close the connection and
		// reset loop value
		if(m.toLowerCase().equals("exit")){
		    c.close();
		    exit = true;
		    break;
		}
		// create a buffer to format string
		ByteBuffer buf = ByteBuffer.wrap(m.getBytes());
		// send the buffer using the given IP address and port number
		c.send(buf,new InetSocketAddress(IPAddr,portNum));
		c.receive(buf);
		String message  = new String(buf.array());
		System.out.println(message);
	    }
	}catch(IOException e){
	    System.out.println("Got an Exception");
	}
    }
}
