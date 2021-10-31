package SenderReceiver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


// Class that can send and/or receive udp packets
public class SenderReceiver{
    
	static final int MTU = 1500;

	private DatagramSocket receiverSocket;
	private DatagramSocket senderSocket;

	private String owner;

	
	public SenderReceiver(int receiverPort, String owner) throws IOException{

		InetAddress address = InetAddress.getLocalHost();

		this.owner = owner;

		senderSocket = new DatagramSocket();
		receiverSocket= new DatagramSocket(receiverPort, address);
		
	}
    public String receive() throws IOException{
		
		// create buffer for data, packet and receiverSocket
		byte[] buffer= new byte[MTU];
		DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
	
		receiverSocket.receive(packet);

		// extract data from packet
		buffer= packet.getData();
		ByteArrayInputStream bstream= new ByteArrayInputStream(buffer);
		ObjectInputStream  ostream= new ObjectInputStream(bstream);

		// print data and end of program
        String data =  ostream.readUTF();
		System.out.println(owner+"received: " + data);
		return data;
	}

    public void send(String payload, int dest) throws IOException{
		
		InetAddress address= InetAddress.getLocalHost();   
		int port= dest;                       
	
		ByteArrayOutputStream bstream= new ByteArrayOutputStream();
		ObjectOutputStream ostream= new ObjectOutputStream(bstream);

		ostream.writeUTF(payload);
		ostream.flush();
		
		byte[] buffer = bstream.toByteArray();
		// create packet addressed to destination
		DatagramPacket packet= new DatagramPacket(buffer, buffer.length, address, port);
		senderSocket.send(packet);
		
	}

	public String buildPayload(String topic, String data){
        String load = "2"+ topic + data;
        //System.out.println(load);
		return  load;
	}
}
