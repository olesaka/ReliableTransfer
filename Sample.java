public class Sample{
	public static void main(String[] args){


		ByteBuffer buffers[] = new ByteBuffer[5];
		for(int i=0; i<5; i++){
			buffers[i].allocate(1027)
		}

		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteBuffer ackbuf = ByteBuffer.allocate(8);

		int times[] = new int[5];

		String ack = "";

		int numOfPackets = 0;
		int packetNum = 0;
		int packetsSent = 0;
		int acksReceived = 0;
		int bufIndex = 0;

		// set num of packets***

		while(packetsSent!=numOfPackets || packetsSent!=packetAcks){
			while(packetsSent-acksReceived<5){
				// set the buffer by starting with the first two bytes
				// add the packet number
				buffers[bufIndex].put(packetNum.getBytes());
				buffers[bufIndex].put(bufIndex.getBytes());
				// now compute the checksum and add the byte here
				// ***checksum().....buffers[bufIndex].put(checkSum);

				// obtain next file contents
				fileChan.read(buffer)
				// add the file data info to the rest of the packet
				buffers[bufIndex].put(buffer);

				// send the packet to the client
				dc.send(buffers[bufIndex], clientAddrs);

				// increment the number of packets sent
				packetsSent++;

				// increment the bufIndex or reset it accordingly
				if(bufIndex==4){
					bufIndex=-1;
				}
				bufIndex+=1;

				// increment the packet number
				packetNum++;

				// record current time
				times[bufIndex-1] = s.select(1000);
			}

			

			int num = s.select(System.currentTimeMillis() - times[bufIndex]);
			if(num==0){
				// no ack received, so resend packet
				dc.send(buffers[bufIndex], clientAddrs);
				break;
			}else{
				// listen for packet acknowledgement
				dc.receive(ackbuf);
				ack = ack.toString(ackbuf.array());
				ack = ack.trim();
				if(ack.equals("posACK")){
					System.out.println("Acknowledgement received");
					break;
				}else{
					// negative acknowledgement received, so resend packet
					int num = ackbuf.getInt();
					dc.send(buffers[num], clientAddrs);
					break;
				}
			}








			// look to receive packet acknowledgements
			while(true){
				if(System.currrentTimeMillis() - times[bufIndex]<1000){
					// listen for packet acknowledgement
					dc.receive(ackbuf);
					ack = ack.toString(ackbuf.array());
					ack = ack.trim();
					if(ack.equals("posACK")){
						System.out.println("Acknowledgement received");
						break;
					}else{
						// negative acknowledgement received, so resend packet
						int num = ackbuf.getInt();
						dc.send(buffers[num], clientAddrs);
						break;
					}
				}else{
					// no ack received, so resend packet
					dc.send(buffers[bufIndex], clientAddrs);
					break;
				}
			}



			

			

			// either a positive acknowledgement is sent
			// a negative acknowledgement is sent
			// timeout before acknowledgement

		}
	}
}