package DataBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

/*
This class is a pseudo databse, that uses a hashmap to store info on a shops products

Database can:
- Store item info
- Serve item info
- Add/Edit/Remove item info
- Notify subs on any item changes
- Send error messages to broker
*/
public class DataBase {

    static final int BROKER_PORT = 2;

    private static HashMap<Integer, Product> products;
   
    private static SenderReceiver transreceiver;
    public static void main(String[] args) throws IOException {
        
        System.out.println("DataBase turned on");
        products = new HashMap<>();

        // get receiver port and setup transreceiver
        transreceiver = new SenderReceiver(1, "Database"); // hardcoded address cause only one database

        // start listening for packets
        String request = transreceiver.receive(); // execution is blocked here until a packet is received

        DataBaseRecThread backup = new DataBaseRecThread(); // create new "back up thread" to receive while we execute the request
        backup.start();

        executeRequest(request);
    }
    private static class DataBaseRecThread extends Thread{
        @Override
        public void run(){
            try {
                String request = transreceiver.receive();
                DataBaseRecThread backup = new DataBaseRecThread();
                backup.start();
                executeRequest(request);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void executeRequest(String data) throws NumberFormatException, IOException{
        
        String[] splitData = data.split(":"); // request:idCode/name/section/price:requestorPort
        String request = splitData[0];
        String[] productDetails = splitData[1].split("/");
        int requestorPort = Integer.parseInt(splitData[2]);
        
        switch(request){
            case "addprod":
                addProduct(Integer.parseInt(productDetails[0]), productDetails[1], productDetails[2], Double.parseDouble(productDetails[3]));
                break;
            case "ediprod":
                updateProduct(Integer.parseInt(productDetails[0]), productDetails[1], productDetails[2], Double.parseDouble(productDetails[3]));
                break;
            case "remprod":
                removeProduct(Integer.parseInt(productDetails[0]));
                break;
            case "reqprod":
                serve(Integer.parseInt(productDetails[0]), requestorPort);
                break;
            case "showall":
                showAll();
                break;
        }
    }

    private static void addProduct( int idCode, String name, String section,  double price) throws IOException{
        
        if(!products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(idCode, product);
            
            transreceiver.send("updatesubs:"+product.section, BROKER_PORT); // ask broker to update subs to this section
        
        }
        else{
            transreceiver.send("101: Product exists", BROKER_PORT); // send error message to broker "Product exists"
        }
    }

    private static void updateProduct(int idCode, String name, String section, double price) throws IOException{
        
        if(products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(product.idCode, product);
            
            transreceiver.send("updatesubs:"+product.section, BROKER_PORT); // ask broker to update subs to this section
        }
        else{
            transreceiver.send("102: Product doesn't exist", BROKER_PORT); // send error message to broker "Product doesn't exist"
        }
    }

    private static void removeProduct( int idCode) throws IOException{
        if(products.containsKey(idCode)){
            String section = products.get(idCode).section;

            products.remove(idCode);
            
            transreceiver.send("updatesubs:"+section, 2); // ask broker to update subs to this section
        }

    }

    private static void serve(int idCode, int destPort) throws IOException{
        if(products.containsKey(idCode)){
            Product requestedProduct = products.get(idCode);
            transreceiver.send("serve:"+requestedProduct.toString()+":"+destPort, BROKER_PORT);
        }
        else{
            transreceiver.send("102: Product doesn't exist", BROKER_PORT); // send error message to broker "Product doesn't exist
        }

    }

    private static void showAll(){
        for(Integer key : products.keySet()){
            System.out.println(products.get(key).toString());
        }
    }

    
    /*
    A class to store information on a product
    */
    private static class Product {
        public String name;
        public String section;
        public int idCode;
        public double price;

        public Product(String name, String section, int idCode, double price){
            this.name = name;
            this.section = section;
            this.idCode = idCode;
            this.price = price;
        }

        public String toString(){
            return idCode+"/"+name+"/"+section+"/"+price;
        }
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
    }
}