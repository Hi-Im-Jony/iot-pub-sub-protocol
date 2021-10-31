package Printer;

import java.io.IOException;

import Product.Product;
import SenderReceiver.SenderReceiver;

/*
A pseudo printer, will print item SEL's 
Printer can:
- Print item SEL
*/
public class Printer{

    private static class PrinterReceiver extends Thread{
        SenderReceiver receiver;
        int receiverPort;

        public PrinterReceiver(int receiverPort) throws IOException{
            this.receiverPort = receiverPort;
            receiver = new SenderReceiver(receiverPort, "Printer");
        }

        public String receive() throws IOException{
            return receiver.receive();
        }

        @Override
        public void run(){
            try {
                String data = receiver.receive();
                PrinterReceiver backup = new PrinterReceiver(receiverPort);
                backup.start();
                printSEL(data, 0.0);
                
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    
    public static void main(String[] args) throws IOException {
        
        int receiverPort =  Integer.parseInt(args[0]);
        PrinterReceiver receiver = new PrinterReceiver(receiverPort);

        String data = receiver.receive(); // receive data
        PrinterReceiver backup = new PrinterReceiver(receiverPort); // create new "back up thread" to receive while we print
        backup.start();
        printSEL(data, 0.0);
    }

    private static void printSEL(String name, double price){
        System.out.println("*********************************"
                        +"\n          Name:"+name+"               "
                        +"\n          Price: "+price+"               "
                        +"\n*********************************");
    }
}
