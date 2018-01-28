/*
 * This UDP server program sends a file from its directory
 * to a client packet by packet, reading in
 * acknowledgments for each packet sent.
 * 
 * @author Joseph Seder, Andrew Olesak, Keith Rodgers
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

class udpserver {

	public static void main(String args[]) {
		Console cons = System.console();
		udpserver check = new udpserver();
		try {
			// open datagram channel and configure selector
			DatagramChannel dc = DatagramChannel.open();
			
		    // continue to ask user for a port number until
		    // they enter something valid and the loop breaks
		    while(true){
			    try{
				    // ask user for input
				    String portNum = cons.readLine("Please enter a port number: ");
				    // try to connect
			    	dc.bind(new InetSocketAddress(Integer.parseInt(portNum)));
			    	break;
				}catch(Exception e){
					System.out.println("Sorry, that was an invalid port number");
				}
			}


			// create a selector for timing
			Selector s = Selector.open();
			dc.configureBlocking(false);
			dc.register(s, SelectionKey.OP_READ);

			// initialize variables
			File file = null;
			SocketAddress clientAddrs = null;
			String fileName = "";

			// loop until filename is received and positive acknowledgement is sent
			// check for another 50 milliseconds for a resend in case the 
			// acknowledgement was corrupt
			ByteBuffer b2 = ByteBuffer.allocate(1037);
			while(true){
				boolean done1 = false;
				while(true){
					int n1 = s.select(1000);
					// no pakcet was sent
					if(n1==0){
						// this will run when positive ACK is sent for filename
						if(done1){
							done1=false;
							System.out.println("Selector timed out and break");
							break;
						}else{
							continue;
						}
					}else{
						// receive filename packet
						System.out.println("This should run");
						Iterator i = s.selectedKeys().iterator();
						while(i.hasNext()){
							SelectionKey k = (SelectionKey)i.next();
							DatagramChannel mychannel = (DatagramChannel)k.channel();
							ByteBuffer buf = ByteBuffer.allocate(1037);
							// receive a packet
							clientAddrs = mychannel.receive(buf);
							System.out.println("Filename Packet Received");
							// reset the buffer to zeros
							b2.clear();
							b2.put(new byte[1037]);
							b2.clear();
							done1=false;
							buf.flip();		
							// run a checksum algorithm on packet
							if(check.checkSum(buf)){
								buf.position(0);
								if(buf.getInt()==3){
									done1=false;
									break;
								}
								buf.position(4);
								fileName = "";
								int length = buf.getInt();
								// write contents of packet to filename
								for(int j=0; j<length; j++){
									fileName+=buf.getChar();
								}
								System.out.println(fileName);
								// reset the buffer to zeros
								b2.clear();
								b2.put(new byte[1037]);
								b2.clear();
								// create a file to work with
								file = new File(fileName);
								boolean fileFound = file.exists();
								// check to see if the file exists and let the client know
								if (fileFound) {
									System.out.println("File found!" + " " + fileName);
									// fill the packet with header info
									b2.putInt(1);
									b2.putInt(1);
									// get a checksum value and put it at the end
									byte sum2 = check.calcCheckSum(b2);
									b2.put(1036, sum2);
									// flip the packet to send
									b2.flip();
									dc.send(b2, clientAddrs);
									System.out.println("File Found ACK sent");
									// set variables to exit loop
									i.remove();
									done1 = true;
								} else {
									// file not found
									System.out.println("File not found!: " + fileName);
									// fill the packet with header info
									b2.putInt(1);
									b2.putInt(2);
									// run a checksum and put it at the end of the packet
									byte sum2 = check.calcCheckSum(b2);
									b2.put(1036, sum2);
									// flip the buffer to be sent
									b2.flip();
									dc.send(b2, clientAddrs);
									System.out.println("File not found ACK Sent");
									// exit this loop
									i.remove();
								}
								

							}else{
								// packet was corrupt so send a negative acknowledgment
								System.out.println("corrupt filename, send negative ACK");
								// fill the packet header info
								b2.putInt(1);
								b2.putInt(0);
								// run a checksum
								byte sum2 = check.calcCheckSum(b2);
								b2.put(1036, sum2);
								// flip the buffer to be sent
								b2.flip();
								dc.send(buf, clientAddrs);
								i.remove();
							}
						}
					}
				}

				
				// create file stream and channel to interact with
				FileChannel fileChan = FileChannel.open(file.toPath());

				// create an array of times which are the current time
				// when each packet gets sent
				long times[] = new long[5];
				// determine number of packets that will be sent
				int numOfPackets = (int)(file.length()/1024);
				// unless the size is divisible by 1024, account for last byte
				if ((int)file.length() % 1024 != 0) {
					numOfPackets++;
				}
				System.out.println("packets: " + numOfPackets);





				

				// create two array lists to store the acks that have 
				// been sent and the order of the buffers presedence
				ArrayList<Integer> ackNums = new ArrayList<Integer>();
				ArrayList<Integer> order = new ArrayList<Integer>();
				int packetNum = 0;
				int packetsSent = 0;
				int acksReceived = 0;
				int bufIndex = 0;
				int prevPosition = 0;


				// create an array of five byte buffers to store the 
				// five packets of file data that are currently
				// being used by the window
				ByteBuffer buffers[] = new ByteBuffer[5];
				for(int j=0; j<5; j++){
					buffers[j] = ByteBuffer.allocate(1037);
				}


				//start loop that only exits when all packets have been
				//delivered and all acknowledgements for those packets
				//have been received
				while(packetNum!=numOfPackets || acksReceived!=numOfPackets){

					while(packetNum-acksReceived<5 && numOfPackets!=packetNum){
						// set the buffer by starting with the first two bytes
						// add the packet number
						buffers[bufIndex].clear();
						buffers[bufIndex].put(new byte[1037]);
						buffers[bufIndex].clear();
						// if the packet is the last one
						// then flag it
						if(packetNum==numOfPackets-1){
							buffers[bufIndex].putInt(4);
						}else{
							buffers[bufIndex].putInt(3);
						}
						// continue to fill in the buffer header info
						buffers[bufIndex].putInt(packetNum);
						buffers[bufIndex].putInt(bufIndex);
						buffers[bufIndex].put((byte)0);
						fileChan.read(buffers[bufIndex]);
						// get the length of the current position for writing the 
						// data in the client
						int length = buffers[bufIndex].position();
						buffers[bufIndex].position(0);
						// put a checksum value at the end
						byte cSum = check.calcCheckSum(buffers[bufIndex]);
						buffers[bufIndex].put(12, cSum);
						buffers[bufIndex].position(length);
						// flip and send the buffer
						buffers[bufIndex].flip();
						dc.send(buffers[bufIndex], clientAddrs);
						System.out.println("Packet fileData Sent: " + packetNum);
						order.add(bufIndex);
						// record current time
						times[bufIndex] = System.currentTimeMillis();
						// increment the bufIndex or reset it accordingly
						if(bufIndex==4){
							bufIndex=-1;
						}
						bufIndex++;
						// increment the packet number
						packetNum++;
					}
					boolean exit = true;
					while(exit){
						// check to see if earliest packet sent has timed out
						if((order.size()!=0) && (System.currentTimeMillis()-times[order.get(0)]>200)){
							// send packet again and then place
							// it at the end of the order list for
							// least presedence
							bufIndex = order.get(0);
							buffers[bufIndex].flip();
							dc.send(buffers[bufIndex], clientAddrs);
							System.out.println("Send packet longest ago");
							times[bufIndex] = System.currentTimeMillis();
							order.remove((Integer)bufIndex);
							order.add(bufIndex);
							bufIndex = order.get(0);
							continue;

						}
						// listen for acknowledgements with timeout
						int num = 0;
						long t = System.currentTimeMillis()-times[order.get(0)];
						if(200>t){
							num = s.select(200-t);
						}
						if(num==0){
							// packet sent longest ago timed out and no other
							// pakets arrived during the timeout, so resent packet
							// sent longest ago
							// System.out.println("Time Out and resend");
							bufIndex = order.get(0);
							buffers[bufIndex].flip();
							dc.send(buffers[bufIndex], clientAddrs);
							times[bufIndex] = System.currentTimeMillis();
							order.remove((Integer)bufIndex);
							order.add(bufIndex);
							bufIndex = order.get(0);
							continue;
						}else{
							Iterator i = s.selectedKeys().iterator();
							while(i.hasNext()){
								SelectionKey k = (SelectionKey)i.next();
								DatagramChannel mychannel = (DatagramChannel)k.channel();
								ByteBuffer buff = ByteBuffer.allocate(1037);
								mychannel.receive(buff);
								buff.flip();
								// run checksum algorithm to check for data corruption
								if(check.checkSum(buff)){
									buff.position(0);
									System.out.println("Length of filepacket: " + buff.remaining());
									int section = buff.getInt();
									// check to see if a filename packet was received
									// if so then resend the ACK for it
									if(section==1){
										System.out.println("got a filename, so resend ACK for that");
										b2.flip();
										dc.send(b2, clientAddrs);
										break;
									}
									// get the ACK packet info
									int packNum = buff.getInt();
									bufIndex = buff.getInt();
									int ack = buff.getInt();
									if(ack==1){
										System.out.println("Received Packet number: " + packNum);
										if(!ackNums.contains((Integer)packNum)){
											System.out.println("Remove and add: " + bufIndex +"   "+ packNum);
											ackNums.add((Integer)packNum);
											order.remove((Integer)bufIndex);
										}
										System.out.println("Ack received: " + packNum);
										acksReceived++;
										exit = false;
										i.remove();
									}else{
										System.out.println("Negative ACK received");
										System.out.println("packNum: " + packNum);
										System.out.println("bufIndex: " + bufIndex);
										// negative acknowledgement received, so resend packet
										// can't rely on information, so break
										break;
									}
								}else{
									// corrupt ACK, so break and wait for timeout
									break;
								}
							}
						}
					}
				}
				System.out.println("DONE!");
				// close the file channel
				fileChan.close();
			
			}

			
		} catch (IOException e) {
			System.out.println("Got an IO Exception");
		}
	}

	// calculates a checksum value for a given buffer
	// and returns it as a byte
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


	// calculates if a give buffer passes
	// the checksum calcualtion test
	public boolean checkSum(ByteBuffer bu){
		bu.position(0);
		byte b[] = new byte[bu.remaining()];
		bu.get(b);
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
