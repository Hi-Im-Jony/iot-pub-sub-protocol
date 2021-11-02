package Broker;

import java.util.HashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

    static SenderReceiver transreceiver;

    static HashMap<Integer, ArrayList<Integer>> topicSubscribers;

   public static void main(String[] args) throws IOException {
        
        System.out.println("Broker turned on");
        topicSubscribers = new HashMap<>();

        transreceiver = new SenderReceiver(2, "Broker"); // hardcoded address cause only one broker

        // start listening for packets
        String request = transreceiver.receive(); // execution is blocked here until a packet is received

        BrokerReceiverThread backup = new BrokerReceiverThread(); // create new "back up thread" to receive while we execute the request
        backup.start();

        executeRequest(request);
    }
    private static class BrokerReceiverThread extends Thread{
        @Override
        public void run(){
            try {
                String request = transreceiver.receive();

                BrokerReceiverThread backup = new BrokerReceiverThread();
                backup.start();

                executeRequest(request);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void executeRequest(String data) throws NumberFormatException, IOException{
        
        String[] splitData = data.split(":"); // request:idCode/name/section/price
        String request = splitData[0];
        switch(request){
            // cases to send to DataBase
            case "addprod":
            case "ediprod":
            case "remprod":
            case "reqprod":
            case "showall":
                transreceiver.send(data, 1);
                break;
        }
    }
    private static void subscribe(int requestorPort, int topic){
        //System.out.println("Executing 'subscribe'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);
        if(subs != null)
            subs.add(requestorPort);
        else{
            subs = new ArrayList<Integer>();
            subs.add(requestorPort);
            topicSubscribers.put(topic, subs);
        }
    }

    private static void unsubscribe(int requestorPort, int topic){
        //System.out.println("Executing 'unsub'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);

        if(subs != null)
            for(int i = 0; i<subs.size(); i++){
                if(subs.get(i) == requestorPort){
                    subs.remove(i);
                    break;
                }
            }
    }

    private static void publish(int topic, String data) throws IOException{
       // TODO publish data to subs of topic
    }

    
    // Class that can send and/or receive udp packets
    private static class SenderReceiver{
        
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
            
            String data =  ostream.readUTF();
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
    }
}
