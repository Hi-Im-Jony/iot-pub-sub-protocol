package Product;

/*
A class to store information on a product
*/
public class Product {
    public String name;
    public String section;
    public int idCode;
    public float price;

    Product(String name, int idCode, float price){
        this.name = name;
        this.idCode = idCode;
        this.price = price;
    }
}
