package part1.negotiation;

public class Offer {
    private final String buyerName;
    private final double price;
    private final int round;

    public Offer(String buyerName, double price, int round) {
        this.buyerName = buyerName;
        this.price = price;
        this.round = round;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public double getPrice() {
        return price;
    }

    public int getRound() {
        return round;
    }
}
