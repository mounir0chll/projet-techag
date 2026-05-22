package part2;

import java.io.Serializable;

public class ProductOffer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sellerName;
    private final String productName;
    private final double price;
    private final double quality;
    private final double deliveryCost;

    public ProductOffer(String sellerName, String productName, double price, double quality, double deliveryCost) {
        this.sellerName = sellerName;
        this.productName = productName;
        this.price = price;
        this.quality = quality;
        this.deliveryCost = deliveryCost;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getProductName() {
        return productName;
    }

    public double getPrice() {
        return price;
    }

    public double getQuality() {
        return quality;
    }

    public double getDeliveryCost() {
        return deliveryCost;
    }
}
