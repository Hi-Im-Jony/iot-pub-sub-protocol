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
        
        String[] splitData = data.split("/"); // idCode+"/"+name+"/"+section+"/"+price
        int request = Integer.parseInt(splitData[0]);
        System.out.println("All good 1");
        switch(request){
            case 0:
                addProduct(Integer.parseInt(splitData[1]), splitData[2], splitData[3], Double.parseDouble(splitData[4]));
                break;
            case 1:
                updateProduct(Integer.parseInt(splitData[1]), splitData[2], splitData[3], Double.parseDouble(splitData[4]));
                break;
            case 2:
                removeProduct(Integer.parseInt(splitData[1]));
                break;
            case 3:
                serve(Integer.parseInt(splitData[1]));
                break;
            case 4:
                printAll();
                break;
        }
    }

    private static void addProduct( int idCode, String name, String section,  double price) throws IOException{
        System.out.println("All good 2");
        if(!products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(idCode, product);
            
            transreceiver.send("updatesubs:"+product.section, 2); // ask broker to update subs to this section
        
        }
        else{
            transreceiver.send("Error 101: Product exists", 2); // send error message to broker "Product exists"
        }
    }

    private static void updateProduct(int idCode, String name, String section, double price) throws IOException{
        
        if(products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(product.idCode, product);
            
            transreceiver.send("updatesubs:"+product.section, 2); // ask broker to update subs to this section
        }
        else{
            transreceiver.send("Error 102: Product doesn't exist", 2); // send error message to broker "Product doesn't exist"
        }
    }

    private static void removeProduct( int idCode) throws IOException{
        if(products.containsKey(idCode)){
            String section = products.get(idCode).section;

            products.remove(idCode);
            
            transreceiver.send("updatesubs:"+section, 2); // ask broker to update subs to this section
        }

    }

    private static void serve(int idCode) throws IOException{
        if(products.containsKey(idCode)){
            Product requestedProduct = products.get(idCode);
            transreceiver.send(requestedProduct.toString(), 1);
        }
        else{
            transreceiver.send("Error 102: Product doesn't exist", 1); // send error message to broker "Product doesn't exist
        }

    }

    private static void printAll(){
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