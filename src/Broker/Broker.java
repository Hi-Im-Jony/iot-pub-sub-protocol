package Broker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.HashMap;

import java.util.ArrayList;
/* 
The broker is the middle man in this network. 
It receives packets with requests from various clients, and tries to fulfill those requests.

Broker can:
- Send/Receive packets
- Store list of subs to topics
- Publish data to subs of topic
*/
public class Broker {
    
	static final int MTU = 1500;

	static DatagramSocket receiverSocket;
	static DatagramSocket senderSocket;

    static HashMap<Integer, ArrayList<Integer>> topicSubscribers;
    public static void main(String[] args) throws IOException {

        System.out.println("ReceiverProcess - Program start");

		InetAddress address= InetAddress.getLocalHost(); 

		int receiverPort =  Integer.parseInt(args[0]);
        int destinationPort = Integer.parseInt(args[1]);

        String topic = args[2];
		String data = args[2];
		
		senderSocket = new DatagramSocket();
			
		receiverSocket= new DatagramSocket(receiverPort, address);
		receiverSocket.setSoTimeout(500); // 0.5 seconds

        topicSubscribers = new HashMap<Integer, ArrayList<Integer>>();

        subscibe(1, 3);
        subscibe(1, 2);
        subscibe(1, 1);
		while(true){
            
			try {
                if(receiverPort>0)
                    receive();
            } catch (IOException e) {
                System.out.println("Nothing received" );
            }
            if(destinationPort>0)
				send(buildPayload(topic, data), destinationPort);	
            		
		}
    }

    private static void receive() throws IOException{
		
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

		//System.out.println("Data: " + data);

        executeRequest(packet.getPort(), data);
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
		senderSocket.send(packet);
		
	}

	private static String buildPayload(String topic, String data){
		return topic + data;
	}

    private static void executeRequest(int requestor_port, String data) throws IOException{
        int request = data.charAt(0) - 48; // turn ASCII value into int value
        int topic = data.charAt(1) - 48;
        String info = data.substring(2, data.length()-1);
        //System.out.println("Request is: "+ request);
        switch(request){
            case 0:
                // subscribe to a topic
                // System.out.print("Subbing");
                subscibe(requestor_port, topic);
                break;
            case 1:
                // unsubscribe from a topic
               // System.out.print("Un-subbing");
                unsubscribe(requestor_port, topic);
                break;
            case 2:
                // publish data about a topic
                // System.out.print("Publishing");
                publish(topic, info);
                break;
        }
    }

    private static void subscibe(int requestor_port, int topic){
        //System.out.println("Executing 'subscribe'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);
        if(subs != null)
            subs.add(requestor_port);
        else{
            subs = new ArrayList<Integer>();
            subs.add(requestor_port);
            topicSubscribers.put(topic, subs);
        }
    }

    private static void unsubscribe(int requestor_port, int topic){
        //System.out.println("Executing 'unsub'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);

        if(subs != null)
            for(int i = 0; i<subs.size(); i++){
                if(subs.get(i) == requestor_port){
                    subs.remove(i);
                    break;
                }
            }
    }

    private static void publish(int topic, String data) throws IOException{
       // System.out.println("Executing 'publish'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);
        if(subs != null){
            String payload = buildPayload((Integer.toString(topic)), data);
            for(int i = 0; i< subs.size(); i ++){
                send(payload, subs.get(i));
            }
        }
    }
}
