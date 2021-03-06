package Printer;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.Random;

/*
A pseudo printer, will print item SEL's 
Printer can:
- Connect to Broker
- Subscribe topic (ie, tell broker which section it will print for)
- Print item SEL
*/
public class Printer{
    
    static final int BROKER_PORT = 2;

    private static Transreceiver transreceiver;
    public static void main(String[] args) throws IOException, InterruptedException {
        
        
        transreceiver = new Transreceiver();
        Random random = new Random();
        String sectionSubbingTo = args[0];
        
        if(sectionSubbingTo.equals("Games"))
            TimeUnit.SECONDS.sleep(random.nextInt(20)+10); // for demo purposes, printers in the gaming section wont turn on for 10sec

        connect(sectionSubbingTo);
        System.out.println("Printer turned on");

        String data = transreceiver.receive(); // receive data
        PrinterReceiverThread backup = new PrinterReceiverThread(); // create new "back up thread" to receive while we print
        backup.start();
        printSEL(data);
    }

    private static void connect(String section) throws IOException{
        transreceiver.send("connect:"+section, BROKER_PORT);
    }

    private static synchronized void printSEL(String data){
        System.out.println("\n*********************************"
                        +"\n          "+data+"               "
                        +"\n*********************************\n");
    }
    private static class PrinterReceiverThread extends Thread{
        @Override
        public void run(){
            try {
                String data = transreceiver.receive();
                PrinterReceiverThread backup = new PrinterReceiverThread();
                backup.start();
                printSEL(data);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static class Transreceiver{
        
        static final int MTU = 1500;

        private DatagramSocket transreceiver;
        
        public Transreceiver() throws IOException{
            transreceiver= new DatagramSocket();
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

            // print data and end of program
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
            transreceiver.send(packet);
        }
    }
}


