import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/*
  Class performs functionality of an echo server using udp
  @author: Andrew Olesak
  CIS 457
 */

class Udpserver{
    public static void main(String args[]){
		Console cons = System.console();
		try{
		    // open a channel
		    DatagramChannel c = DatagramChannel.open();
		    // container that can hold multiple channels
		    Selector s = Selector.open();
		    // makes the channel so that it will not lock on read calls
		    // meaning it will always return immediatly
		    c.configureBlocking(false);
		    c.register(s,SelectionKey.OP_READ);
		    // ask user for input
		    String portNum = cons.readLine("Please enter a port number: ");
		    int port = Integer.parseInt(portNum);
		    // connect
		    c.bind(new InetSocketAddress(Integer.parseInt(portNum)));
		    // continually search for any clients sending
		    while(true){
				// check all channels in selector
				int num = s.select(5000);
				if(num==0){
				    System.out.println("nobody sent anything");
				}else{
				    Iterator i = s.selectedKeys().iterator();
				    while(i.hasNext()){
						SelectionKey k = (SelectionKey)i.next();
						DatagramChannel mychannel = (DatagramChannel)k.channel();
						ByteBuffer buffer = ByteBuffer.allocate(4096);
						SocketAddress clientaddr = mychannel.receive(buffer);
						String message = new String(buffer.array());
						message = message.trim();
						System.out.println(message);
						c.send(buffer,clientaddr);
						i.remove();
				    }
				}
		    }
		}catch(IOException e){
		    System.out.println("Got an IO Exception");
		}
    }
}
