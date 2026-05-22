package part1.negotiation;

import java.util.Locale;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class AuctionLauncher {
    private static final String SOLD_SCENARIO = "sold";
    private static final String UNSOLD_SCENARIO = "unsold";

    public static void main(String[] args) {
        String scenarioName = readScenarioName(args);
        Scenario scenario = createScenario(scenarioName);

        System.out.println("[Launcher] Scenario=" + scenarioName);
        System.out.println("[Launcher] Product start=" + scenario.product.getStartPrice()
                + ", reserve=" + scenario.product.getReservePrice()
                + ", buyer1Max=" + scenario.buyer1Max
                + ", buyer2Max=" + scenario.buyer2Max);

        try {
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(false);

            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");

            AgentContainer mainContainer = runtime.createMainContainer(profile);

            AgentController buyer1 = mainContainer.createNewAgent(
                    "buyer1",
                    "part1.negotiation.BuyerAgent",
                    new Object[] { "seller", Double.valueOf(scenario.buyer1Max), Double.valueOf(scenario.increment) });

            AgentController buyer2 = mainContainer.createNewAgent(
                    "buyer2",
                    "part1.negotiation.BuyerAgent",
                    new Object[] { "seller", Double.valueOf(scenario.buyer2Max), Double.valueOf(scenario.increment) });

            buyer1.start();
            buyer2.start();

            AgentController seller = mainContainer.createNewAgent(
                    "seller",
                    "part1.negotiation.SellerAgent",
                    new Object[] {
                            scenario.product,
                            Long.valueOf(scenario.globalTimeoutMs),
                            new String[] { "buyer1", "buyer2" }
                    });

            seller.start();
            System.out.println("[Launcher] JADE GUI conservee ouverte: les agents restent visibles dans la table.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void waitForDemoEnd(AgentController seller, long maxWaitMs)
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + maxWaitMs;

        while (System.currentTimeMillis() < endTime) {
            try {
                String stateName = seller.getState().getName();
                if ("Deleted".equalsIgnoreCase(stateName)) {
                    return;
                }
            } catch (StaleProxyException e) {
                return;
            }

            Thread.sleep(200L);
        }
    }

    private static String readScenarioName(String[] args) {
        if (args == null || args.length == 0) {
            return SOLD_SCENARIO;
        }

        String scenarioName = args[0].trim().toLowerCase(Locale.ROOT);
        if (UNSOLD_SCENARIO.equals(scenarioName)) {
            return UNSOLD_SCENARIO;
        }

        if (!SOLD_SCENARIO.equals(scenarioName)) {
            System.out.println("[Launcher] Scenario inconnu: " + args[0] + ". Utilisation du scenario sold.");
        }

        return SOLD_SCENARIO;
    }

    private static Scenario createScenario(String scenarioName) {
        double startPrice = 100.0;
        double increment = 20.0;
        long globalTimeoutMs = 15000L;

        if (UNSOLD_SCENARIO.equals(scenarioName)) {
            return new Scenario(
                    new Product("Ordinateur portable", startPrice, 300.0),
                    150.0,
                    180.0,
                    increment,
                    globalTimeoutMs);
        }

        return new Scenario(
                new Product("Ordinateur portable", startPrice, 160.0),
                150.0,
                200.0,
                increment,
                globalTimeoutMs);
    }

    private static class Scenario {
        private final Product product;
        private final double buyer1Max;
        private final double buyer2Max;
        private final double increment;
        private final long globalTimeoutMs;

        private Scenario(Product product, double buyer1Max, double buyer2Max, double increment, long globalTimeoutMs) {
            this.product = product;
            this.buyer1Max = buyer1Max;
            this.buyer2Max = buyer2Max;
            this.increment = increment;
            this.globalTimeoutMs = globalTimeoutMs;
        }
    }
}
