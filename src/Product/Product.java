package Product;

/*
A class to store information on a product
*/
public class Product {
    public String name;
    public String section;
    public int idCode;
    public double price;

    Product(String name, String section, int idCode, double price){
        this.name = name;
        this.section = section;
        this.idCode = idCode;
        this.price = price;
    }
}
