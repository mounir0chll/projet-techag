package part2;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Part2Launcher {
    public static void main(String[] args) {
        String productName = "Ordinateur portable";
        long responseTimeoutMs = 3000L;
        PreferenceProfile preferences = new PreferenceProfile(0.35, 0.50, 0.15);

        ProductOffer seller1Offer = new ProductOffer("seller1", productName, 900.0, 8.0, 5.0);
        ProductOffer seller2Offer = new ProductOffer("seller2", productName, 780.0, 6.0, 3.0);
        ProductOffer seller3Offer = new ProductOffer("seller3", productName, 850.0, 9.0, 7.0);

        System.out.println("[Launcher-Part2] Demo: 1 buyer, 3 sellers, selection multicritere.");
        System.out.println("[Launcher-Part2] Mode: acheteur mobile avec migration inter-conteneurs.");
        System.out.println("[Launcher-Part2] Formule: score = wPrice*priceScore(min)"
                + " + wQuality*qualityScore(max)"
                + " + wDelivery*deliveryCostScore(min)");

        try {
            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(false);

            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");
            profile.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
            profile.setParameter(Profile.LOCAL_PORT, "1202");
            profile.setParameter(Profile.MAIN_HOST, "127.0.0.1");
            profile.setParameter(Profile.MAIN_PORT, "1202");

            AgentContainer mainContainer = runtime.createMainContainer(profile);
            AgentContainer buyerContainer = createContainer(runtime, "buyer-site", "1203");

            AgentController seller1 = mainContainer.createNewAgent(
                    "seller1",
                    "part2.SellerAgent",
                    new Object[] { seller1Offer });

            AgentController seller2 = mainContainer.createNewAgent(
                    "seller2",
                    "part2.SellerAgent",
                    new Object[] { seller2Offer });

            AgentController seller3 = mainContainer.createNewAgent(
                    "seller3",
                    "part2.SellerAgent",
                    new Object[] { seller3Offer });

            seller1.start();
            seller2.start();
            seller3.start();

            AgentController buyer = buyerContainer.createNewAgent(
                    "buyerPart2",
                    "part2.BuyerAgent",
                    new Object[] {
                            productName,
                            new String[] { "seller1", "seller2", "seller3" },
                            preferences,
                            Long.valueOf(responseTimeoutMs),
                            new String[] { "Main-Container" },
                            "buyer-site"
                    });

            buyer.start();
            System.out.println("[Launcher-Part2] JADE GUI conservee ouverte: les agents restent visibles dans la table.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AgentContainer createContainer(Runtime runtime, String containerName, String localPort) {
        ProfileImpl profile = new ProfileImpl(false);
        profile.setParameter(Profile.MAIN_HOST, "127.0.0.1");
        profile.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
        profile.setParameter(Profile.LOCAL_PORT, localPort);
        profile.setParameter(Profile.MAIN_PORT, "1202");
        profile.setParameter(Profile.CONTAINER_NAME, containerName);
        return runtime.createAgentContainer(profile);
    }

    private static void waitForDemoEnd(AgentController buyer, long maxWaitMs)
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + maxWaitMs;

        while (System.currentTimeMillis() < endTime) {
            try {
                String stateName = buyer.getState().getName();
                if ("Deleted".equalsIgnoreCase(stateName)) {
                    return;
                }
            } catch (StaleProxyException e) {
                return;
            }

            Thread.sleep(200L);
        }
    }
}
