package SenderReceiver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


// Class that can send or receive, based on inputs

public class SenderReceiver {
    
	static final int MTU = 1500;

	static DatagramSocket receiver_socket;
	static DatagramSocket sender_socket;

    public static void main(String[] args) throws IOException {
        System.out.println("ReceiverProcess - Program start");

		InetAddress address= InetAddress.getLocalHost(); 

		int receiver_port =  Integer.parseInt(args[0]);
        int destination_port = Integer.parseInt(args[1]);

        String topic = args[2];
		String data = args[2];
		
		sender_socket = new DatagramSocket();
			
		receiver_socket= new DatagramSocket(receiver_port, address);
		receiver_socket.setSoTimeout(500); // 0.5 seconds

        int counter = 0;
		while(true){
			try {
                if(receiver_port>0)
                    receive();
            } catch (IOException e) {
                System.out.println("Nothing received" + counter);
                counter ++;
            }
            if(destination_port>0)
				send(buildPayload(topic, data), destination_port);			
            
		}
    }

    private static void receive() throws IOException{
		
		// create buffer for data, packet and receiver_socket
		byte[] buffer= new byte[MTU];
		DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
	
		receiver_socket.receive(packet);

		// extract data from packet
		buffer= packet.getData();
		ByteArrayInputStream bstream= new ByteArrayInputStream(buffer);
		ObjectInputStream  ostream= new ObjectInputStream(bstream);

		// print data and end of program
        String data =  ostream.readUTF();
		System.out.println("Data: " + data);
	}

    private static void send(String payload, int dest) throws IOException{
		
		InetAddress address= InetAddress.getLocalHost();   
		int port= dest;                       
	
		ByteArrayOutputStream bstream= new ByteArrayOutputStream();
		ObjectOutputStream ostream= new ObjectOutputStream(bstream);

		ostream.writeUTF(payload);
		ostream.flush();
		
		byte[] buffer = bstream.toByteArray();
		// create packet addressed to destination
		DatagramPacket packet= new DatagramPacket(buffer, buffer.length, address, port);
		sender_socket.send(packet);
		
	}

	private static String buildPayload(String topic, String data){
        String load = "2"+ topic + data;
        //System.out.println(load);
		return  load;
	}
}
