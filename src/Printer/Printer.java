package Printer;

import Product.Product;

/*
A pseudo printer, will print item SEL's 
Printer can:
- Print item SEL
*/
public class Printer {
    
    private void printSEL(Product product){
        System.out.println("*********************************");
        System.out.println("          Name:"+product.name+"               ");
        System.out.println("          Price: "+product.price+"               ");
        System.out.println("*********************************");
    }
}
