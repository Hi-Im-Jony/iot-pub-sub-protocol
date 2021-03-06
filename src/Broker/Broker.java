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
        
        String[] info = data.split(":"); // data = <request>:<info>:<requestorPort>
        String request = info[0];
        int port = Integer.parseInt(info[2]);
        switch(request){
            
            // used by Printer to connect to Broker, 
            // by technically "subscribing" to the section (topic) it will be listening to print requests for
            case "connect": // <info> = <section>
            

                String section  = info[1];
                
                connect(port, section);
                checkPrintStack(section);
                break;

            // used by ShopTool and Computer to ask printer to print SEL with the provided info
            case "print": // <info> = <id / name / section / price>
            
                String details = info[1];
                String[] productInfo = details.split("/");
                if(productInfo.length!=4)
                    break;
                String productSection = productInfo[2];
                Stack<String> stack = printerStacks.get(productSection);
                
                if(stack != null)
                    stack.push(details);
                else{
                    stack = new Stack<>();
                    stack.push(details);
                }
                printerStacks.put(productSection, stack);

                checkPrintStack(productSection);
                break;

            // used by ShopTool to sub to section (topic) the tool will be used in, in the "store"
            case "sub": // <info> = <section>
                subscribe(port, info[1]);
                break;

            // used by ShopTool to unsub from section (topic)
            case "unsub": // <info> = <section>
                unsubscribe(port, info[1]);
                break;

            // used by DataBase to notify (publish) relevant ShopTools about
            // a change to products in their section
            case "pub": // <info> = <topic * message ; id / name / section / price>
                String[] params = info[1].split("\\*");
                String topic = params[0];
                String message = params[1];
                message = message.replaceAll(";", ": ");
                publish(topic, message);
                break;

            // used by Computer to make changes to DataBase
            case "addprod": // <info> = <id / name / section / price>
            case "ediprod": // <info> = <id / name / section / price>
            case "remprod": // <info> = <id>
            // used by Computer and ShopTool to request details of a product
            case "reqprod": // <info> = <id>
                transreceiver.send(data, DB_PORT);
                break;

            // used by DataBase to serve a response to a client
            case "serve": // <info> = <prod ; id / name / section / price> OR <info> = <Error ; message>
                int destPort = Integer.parseInt(info[2]);
                info[1] = info[1].replaceAll(";", ":");
                transreceiver.send(info[1], destPort);
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

    private static void publish(String topic, String data) throws IOException{
        ArrayList<Integer> subs = topicSubscribers.get(topic);

        if(subs == null)
            return;

        for(Integer port:subs){
            transreceiver.send(data, port);
        }
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


