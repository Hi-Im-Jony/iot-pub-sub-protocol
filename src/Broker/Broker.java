package Broker;

import java.util.HashMap;
import java.util.Stack;
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
    
    static final int DB_PORT = 1;
    static final int BROKER_PORT = 2;
    static final int PRINTER_PORT = 3;
    
	static final int MTU = 1500;

    static Transreceiver transreceiver;

    static HashMap<String, ArrayList<Integer>> topicSubscribers;
    static HashMap<String, Stack<String>> printerStacks; // key is section (topic) of stack, content is stack of "products".toString()
    static HashMap<String, ArrayList<Integer>> subscribedPrinters; // key is section printer is subbed to, content is port(s) of subbed printer

   public static void main(String[] args) throws IOException {
        
        System.out.println("Broker turned on");
        topicSubscribers = new HashMap<>();
        printerStacks = new HashMap<>();
        subscribedPrinters = new HashMap<>();

        transreceiver = new Transreceiver(BROKER_PORT); // hardcoded address cause only one broker

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
        
        String[] splitData = data.split(":"); // data = request:info:requestorPort
        String request = splitData[0];
        switch(request){
            // relevant to Printer
            case "connect": // connect:section:requestorPort
                String section  = splitData[1];
                int port = Integer.parseInt(splitData[2]);
                connect(port, section);
                checkPrintStack(section);
                break;
            case "print": // info = id/name/section/price
                String info = splitData[1];
                String[] productInfo = info.split("/");
                if(productInfo.length!=4)
                    break;
                String productSection = productInfo[2];
                Stack<String> stack = printerStacks.get(productSection);
                
                if(stack != null)
                    stack.push(info);
                else{
                    stack = new Stack<>();
                    stack.push(info);
                }
                printerStacks.put(productSection, stack);

                checkPrintStack(productSection);
                break;

            // relevant to Scanner
            case "sub":
            case "unsub":
            case "updatesubs":
                break;

            // relevant to DataBase
            case "addprod":
            case "ediprod":
            case "remprod":
            case "reqprod":
            case "showall":
                transreceiver.send(data, DB_PORT);
                break;
            case "serve": // ie, serve only one client based on a request, not same as publishing to subs
                int destPort = Integer.parseInt(splitData[2]);
                transreceiver.send(splitData[1], destPort);
                break;
        }
    }

    private static void connect(int port, String section){
         ArrayList<Integer> subs = subscribedPrinters.get(section);

                if(subs != null)
                    subs.add(port);
                else{
                    subs = new ArrayList<Integer>();
                    subs.add(port);
                }
                
                subscribedPrinters.put(section, subs);
    }

    private static void checkPrintStack(String section) throws IOException{
        Stack<String> printStack = printerStacks.get(section);

        if(printStack==null || printStack.size()==0) // nothing to print
            return;
        
        ArrayList<Integer> printerPorts = subscribedPrinters.get(section);

        if(printerPorts != null)
        while(printStack.size()>0){
            String request = printStack.pop();
            for(Integer printerPort: printerPorts)
                transreceiver.send(request, printerPort); // send to printer
        }
    }

    private static void subscribe(int requestorPort, String topic){
        ArrayList<Integer> subs = topicSubscribers.get(topic);
        if(subs != null)
            subs.add(requestorPort);
        else{
            subs = new ArrayList<Integer>();
            subs.add(requestorPort);
        }
        topicSubscribers.put(topic, subs);
    }

    private static void unsubscribe(int requestorPort, String topic){
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

    private static class Transreceiver{
        
        static final int MTU = 1500;

        private DatagramSocket transreceiver;
        
        public Transreceiver(int port) throws IOException{
            transreceiver= new DatagramSocket(port);
        }

        public String receive() throws IOException{
            
            // create buffer for data, packet and transreceiver
            byte[] buffer= new byte[MTU];
            DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
        
            transreceiver.receive(packet);

            // extract data from packet
            buffer= packet.getData();
            ByteArrayInputStream bstream= new ByteArrayInputStream(buffer);
            ObjectInputStream  ostream= new ObjectInputStream(bstream);

            String data =  ostream.readUTF();
            System.out.println("Received: \""+data+",\" from port:"+packet.getPort());
            data = data+":"+packet.getPort();
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
            transreceiver.send(packet);
        }
    }
}


