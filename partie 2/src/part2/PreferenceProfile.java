package part2;

import java.io.Serializable;

public class PreferenceProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double priceWeight;
    private final double qualityWeight;
    private final double deliveryCostWeight;

    public PreferenceProfile(double priceWeight, double qualityWeight, double deliveryCostWeight) {
        this.priceWeight = priceWeight;
        this.qualityWeight = qualityWeight;
        this.deliveryCostWeight = deliveryCostWeight;
    }

    public double getPriceWeight() {
        return priceWeight;
    }

    public double getQualityWeight() {
        return qualityWeight;
    }

    public double getDeliveryCostWeight() {
        return deliveryCostWeight;
    }
}
