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

    static final int DB_PORT = 1;
    static final int BROKER_PORT = 2;

    private static HashMap<Integer, Product> products;
   
    private static Transreceiver transreceiver;
    public static void main(String[] args) throws IOException {
        
        System.out.println("DataBase turned on");
        products = new HashMap<>();

        // get receiver port and setup transreceiver
        transreceiver = new Transreceiver(DB_PORT); // hardcoded address cause only one database

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
        
        String[] info = data.split(":"); // // data = <request>:<info>:<requestorPort>
        String request = info[0];
        String[] productDetails = info[1].split("/");
        int requestorPort = Integer.parseInt(info[2]);
        
        switch(request){
            case "addprod": // <info> = <id / name / section / price>
                addProduct(Integer.parseInt(productDetails[0]), productDetails[1], productDetails[2], Double.parseDouble(productDetails[3]), requestorPort);
                break;
            case "ediprod": // <info> = <id / name / section / price>
                updateProduct(Integer.parseInt(productDetails[0]), productDetails[1], productDetails[2], Double.parseDouble(productDetails[3]),requestorPort);
                break;
            case "remprod": // <info> = <id>
                removeProduct(Integer.parseInt(productDetails[0])); // doesn't need requestor port as there isn't a way to error here
                                                                    // thus, no need to send error message
                break;
            case "reqprod": // <info> = <id>
                serve(Integer.parseInt(productDetails[0]), requestorPort);
                break;
        }
    }

    private static synchronized void addProduct( int idCode, String name, String section,  double price, int requestorPort) throws IOException{
        
        if(!products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(idCode, product);
            String update = "NEW PRODUCT ADDED TO YOUR SECTION; "+product.toString();
            transreceiver.send("pub:"+product.section+"*"+update, BROKER_PORT); // ask broker to update subs to this section
        
        }
        else{
            transreceiver.send("serve:Error; requested product (id code = "+idCode+") already exists:"+requestorPort, BROKER_PORT);
        }
    }

    private static synchronized void updateProduct(int idCode, String name, String section, double price, int requestorPort) throws IOException{
        
        if(products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(product.idCode, product);
            String update = "PRODUCT ON YOUR SECTION HAS BEEN MODIFIED;"+product.toString();
            transreceiver.send("pub:"+product.section+"*"+update, BROKER_PORT); // ask broker to update subs to this section
        }
        else{
            transreceiver.send("serve:Error; requested product (id code = "+idCode+") doesn't exist:"+requestorPort, BROKER_PORT);
        }
    }

    private static synchronized void removeProduct( int idCode) throws IOException{
        if(products.containsKey(idCode)){
            String section = products.get(idCode).section;

            String update = "PRODUCT ON YOUR SECTION HAS BEEN REMOVED;"+products.get(idCode).toString();
            products.remove(idCode);
            
            
            transreceiver.send("pub:"+section+"*"+update, BROKER_PORT); // ask broker to update subs to this section
        }

    }

    private static synchronized void serve(int idCode, int destPort) throws IOException{
        if(products.containsKey(idCode)){
            Product requestedProduct = products.get(idCode);
            transreceiver.send("serve:"+"prod;"+requestedProduct.toString()+":"+destPort, BROKER_PORT);
        }
        else{
            transreceiver.send("serve:Error; requested product (id code = "+idCode+") doesn't exist:"+destPort, BROKER_PORT);
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
            // System.out.println("Received: \""+data+",\" from port:"+packet.getPort());
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