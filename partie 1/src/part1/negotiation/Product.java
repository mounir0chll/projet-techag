package part1.negotiation;

public class Product {
    private final String name;
    private final double startPrice;
    private final double reservePrice;

    public Product(String name, double startPrice, double reservePrice) {
        this.name = name;
        this.startPrice = startPrice;
        this.reservePrice = reservePrice;
    }

    public String getName() {
        return name;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getReservePrice() {
        return reservePrice;
    }
}
