
/*
 * This UDP client program requests a file from a server 
 * and receives the file packet by packet, sending 
 * acknowledgments for each packet
 * 
 * @author Joseph Seder, Andrew Olesak, Keith Rodgers
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

class udpclient {
	public static void main(String args[]) {
		udpclient check = new udpclient();
		try {
			// open Datagram Channel 
			DatagramChannel dc = DatagramChannel.open();
			Console cons = System.console();
			String ipAdd = "";
			int portNum = 0;
			// ask for IP address and port number
			boolean validPortAndIp = false;
			while (!validPortAndIp) {
				/* Check for valid ip and port */
				try {
					ipAdd = cons.readLine("Enter IP address: ");
					portNum = Integer.parseInt(cons.readLine("Enter port number: "));
					dc.connect(new InetSocketAddress(ipAdd, portNum));
					validPortAndIp = true;
				} catch (Exception e) {
					System.out.println("Invalid IP or port");
				}
			}

			// initialize user message string 
			String input = "";

			// initialize acknowledgment string for filename
			String fileAck = "";



			Selector s = Selector.open();
			dc.configureBlocking(false);
			dc.register(s, SelectionKey.OP_READ);


			// send filename packet until ack received
			boolean done = true;
			boolean getFile = false;
			while (done) {
				// get desired file name from user 
				input = cons.readLine("Enter desired file name: ");
				// send the file name to server
				ByteBuffer buf = ByteBuffer.allocate(1037);
				buf.putInt(1);
				buf.putInt(input.length());
				// fill the buffer with chars
				for(int i=0; i<input.length(); i++){
					buf.putChar(input.charAt(i));
				}
				// run the chekcsum
				byte c1 = check.calcCheckSum(buf);
				buf.put(1036, c1);
				// flip the buffer to be sent
				buf.flip();
				System.out.println("Filename packet length: " + buf.remaining());
				dc.send(buf, new InetSocketAddress(ipAdd, portNum));
				System.out.println("File Name Packet Sent!");
				boolean done3 = true;
				while(done3){
					// this runs if the filename needs to be entered again
					if(getFile){
						getFile=false;
						break;
					}
					int n = s.select(1000);
					if(n==0){
						// no ack sent, so resend
						buf.flip();	
						System.out.println("length: " + buf.remaining());
						dc.send(buf, new InetSocketAddress(ipAdd, portNum));
						System.out.println("Filename sent again");
					}else{
						Iterator i = s.selectedKeys().iterator();
						while(i.hasNext()){
							SelectionKey k = (SelectionKey)i.next();
							DatagramChannel mychannel = (DatagramChannel)k.channel();
							ByteBuffer b = ByteBuffer.allocate(1037);
							mychannel.receive(b);
							// flip the buffer to be read
							b.flip();
							// see if buffer passes checksum
							if(check.checkSum1(b)){
								getFile=true;
								done = false;
								i.remove();
								break;
							}
							b.position(0);
							if(check.checkSum2(b)){
								b.position(0);
								int v = b.getInt();
								System.out.println("packet header: " + v);
								// see if the packet is from the filename portion
								if(v==1){
									int v1 = b.getInt();
									// this runs if the packet is a positive ack
									if(v1==1){System.out.println("Positive ACK Received");
										i.remove();
										done3=false;
										done = false;
									// this runs if the packet is file not found ack
									}else if(v==2){
										System.out.println("FileNotFound ACK Received");
										System.out.println("Sorry, " + input + " was not found");
										done3=false;
										i.remove();
									// this runs for a negative ack
									}else{
										// resend
										buf.flip();
										System.out.println("Negative ACK Received");
										System.out.println("first ACK num: " + v);
										dc.send(buf, new InetSocketAddress(ipAdd, portNum));
										System.out.println("Corrupt filename, filename sent again");
										getFile=true;
										i.remove();
									}
								// packet wasn't from section one
								}else{
									done3 = false;
									getFile=true;
									done = false;
									i.remove();
									System.out.println("break out of one");
									break;
								}
									
							}else{
								// corrupt ack, resend
								buf.flip();
								System.out.println("Corrupt ACK Received");
								dc.send(buf, new InetSocketAddress(ipAdd, portNum));
								System.out.println("corrupt ACK, finename sent again");
								i.remove();

							}
						}
					}
				}

			}

			// wait for server to time out before moving on
			try {
			    Thread.sleep(500);               
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			

			System.out.println("Move on to part 2");


			
			// prepare file streams and create counting 
			// variables to keep track of data
			// as the client received data
			File file = new File(input);
			FileOutputStream fos = new FileOutputStream(file);
			FileChannel fileChan = fos.getChannel();
			int packetNum = 0;
			int numPackets = 0;
			int lastPacket = -5;


			

			// create an array to hold the buffers for order
			ArrayList<ByteBuffer> buffs = new ArrayList<ByteBuffer>();
			int currentNum = 0;
			boolean last = false;
			while(true){
				// listen for packets being sent
				int n2 = s.select(1000);
				if(n2==0){
					// if this is true, then all of the packets for the file have been
					// send and it will break from the loop
					if(lastPacket+1==currentNum){
						break;
					}
					continue;
				}else{
					Iterator i = s.selectedKeys().iterator();
					while(i.hasNext()){
						SelectionKey k = (SelectionKey)i.next();
						DatagramChannel mychannel = (DatagramChannel)k.channel();
						ByteBuffer buff = ByteBuffer.allocate(1037);
						// receive single file packet from server 
						// and print some info about it along with
						mychannel.receive(buff);
						System.out.println("Packet Received");
						buff.flip();
						System.out.println("currentPacket " + currentNum);
						System.out.println("length: " + buff.remaining());
						// get the buffer info
						int part = buff.getInt();
						packetNum = buff.getInt();
						System.out.println("Packet #: " + packetNum);
						int bufIndex = buff.getInt();
						byte checkValue = buff.get();
						// create an ACK buffer and set the header info
						ByteBuffer buf = ByteBuffer.allocate(1037);
						buf.putInt(3);
						buf.putInt(packetNum);
						buf.putInt(bufIndex);
						buff.position(0);
						buff.position(0);
						// check to see if the packet had some errors
						// but running the checksum algorithm
						if(check.checkSum1(buff)){
							// if the packet is the next packet of info
							// to be written to the file, then do it and check
							// for any successive values that can be 
							// written from the arraylist
							if(packetNum==currentNum){
								buff.position(0);
								if(buff.getInt()==4){
									lastPacket=buff.getInt();
								}
								// set the position and write the
								// contents to the file
								buff.position(13);
								int bytesWritten = fileChan.write(buff);
								System.out.println("Bytes written: " + bytesWritten);
								// increment the counter for the next packet
								currentNum++;
								while(buffs.size()>0){
									int l = buffs.size();
									// check for any packets that arrived early and need
									// to be written to the file now
									for (int j=0; j<buffs.size();j++){										
										buffs.get(j).position(4);
										if(buffs.get(j).getInt()==currentNum){
											buffs.get(j).position(13);
											bytesWritten = fileChan.write(buffs.get(j));
											System.out.println("Byteswritten: " + bytesWritten);
											buffs.remove(j);
											j--;
											// increment the counter for each packet
											// that gets written
											currentNum++;
										}
										// if the buffer is the same size as it was
										// when it started, then nothing was written 
										//from it so break
									}if(l==buffs.size()){
										break;
									}
								}
							}else if(packetNum>currentNum){
								// check if buffer is already in the arraylist
								// if it isn't, then add it
								System.out.println("add to buffer array");
								for(ByteBuffer f:buffs){
									f.position(0);
									if(f.getInt()==packetNum){
										System.out.println("Buffer was in the arraylist already");
										break;
									}
								}
								buffs.add(buff);
								
							}

							// send acknowledgment to server 
							buf.putInt(1);
							byte sum3 = check.calcCheckSum(buf);
							buf.put(1036, sum3);
							buf.flip();
							dc.send(buf, new InetSocketAddress(ipAdd, portNum));
							System.out.println("Ack Sent!");
							i.remove();
						}else{
							// packet was corrupt so let it timeout and resend
							// on the server side
							System.out.println("Corrupt packet Received");
							i.remove();
						}
					}
				}
			}

			// close connectiona and file streams 
			fos.close();
			fileChan.close();
			dc.close();
			
		} catch (IOException e) {
			System.out.println("Got an IO Exception");
		}
	}


	// method calculates the checkcsum value for a 
	// given bytebuffer and returns the values as a byte
	public byte calcCheckSum(ByteBuffer bu){
		bu.position(0);
		byte b[] = new byte[bu.remaining()];
		bu.get(b);
		int checkSum = 0;
		for(int i=0; i<b.length; i++){
			
			checkSum+=b[i];
			if((checkSum & 0x100)==0x100){
				checkSum&=0xFF;
				checkSum++;
			}
		}
		return (byte)((~checkSum)&0xFF);
	}


	// method calcultes whether or not a given bytebuffer
	// is courrupt by running a checksum algorithm on it
	// returns true if the buffer isn't corrupt and
	// false if it is.
	// this method is specific to the filedata
	public boolean checkSum1(ByteBuffer bu){
		bu.position(0);
		byte b[] = new byte[bu.remaining()];
		bu.get(b);
		if(b.length==0){
			return false;
		}
		int checkSum = 0;
		for(int i=0; i<b.length; i++){
			
			if(i!=12){
				checkSum+=b[i];
				if((checkSum & 0x100)==0x100){
					checkSum&=0xFF;
					checkSum++;
				}
			}
		}
		checkSum+=b[12];
		if(((byte)checkSum)==-1){
			return true;
		}else{
			return false;
		}
	}

	// method is used for finding the checksum
	// for a regular packet
	public boolean checkSum2(ByteBuffer bu){
		bu.position(0);
		byte b[] = new byte[bu.remaining()];
		bu.get(b);
		if(b.length==0){
			return false;
		}
		int checkSum = 0;
		for(int i=0; i<b.length-1; i++){
			checkSum+=b[i];
			if((checkSum & 0x100)==0x100){
				checkSum&=0xFF;
				checkSum++;
			}
		}
		checkSum+=b[b.length-1];
		if(((byte)checkSum)==-1){
			return true;
		}else{
			return false;
		}
	}
}
