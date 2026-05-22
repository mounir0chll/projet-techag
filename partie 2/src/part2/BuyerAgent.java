package part2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends Agent {
    private String productName;
    private String[] sellerNames;
    private PreferenceProfile preferences;
    private long responseTimeoutMs;
    private String conversationId;
    private String[] sellerContainerNames;
    private String homeContainerName;
    private boolean mobileMode;
    private int mobileSellerIndex;
    private boolean returningHome;
    private List<ProductOffer> mobileOffers;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args == null || args.length < 4) {
            System.out.println("[" + getLocalName() + "] Arguments invalides: productName, sellers, preferences, timeout attendus.");
            doDelete();
            return;
        }

        productName = (String) args[0];
        sellerNames = (String[]) args[1];
        preferences = (PreferenceProfile) args[2];
        responseTimeoutMs = ((Number) args[3]).longValue();
        conversationId = "part2-selection-" + getLocalName() + "-" + System.currentTimeMillis();
        mobileMode = args.length >= 6;
        if (mobileMode) {
            sellerContainerNames = (String[]) args[4];
            homeContainerName = (String) args[5];
            mobileOffers = new ArrayList<ProductOffer>();
        }

        System.out.println("[" + getLocalName() + "] Demarrage buyer."
                + " product=" + productName
                + ", priceWeight=" + preferences.getPriceWeight()
                + ", qualityWeight=" + preferences.getQualityWeight()
                + ", deliveryCostWeight=" + preferences.getDeliveryCostWeight());
        System.out.println("[" + getLocalName() + "] Sellers connus=" + joinSellers());
        System.out.println("[" + getLocalName() + "] conversationId=" + conversationId);

        if (mobileMode) {
            System.out.println("[" + getLocalName() + "] Mode mobile active. Home=" + homeContainerName
                    + ", itinerary=" + joinContainers());
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    moveToNextSellerContainer();
                }
            });
        } else {
            addBehaviour(new BuyerSelectionBehaviour());
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + getLocalName() + "] Arret buyer.");
    }

    @Override
    protected void beforeMove() {
        Location here = here();
        System.out.println("[" + getLocalName() + "] beforeMove depuis " + here.getName());
    }

    @Override
    protected void afterMove() {
        Location here = here();
        System.out.println("[" + getLocalName() + "] afterMove vers " + here.getName());

        if (!mobileMode) {
            return;
        }

        if (returningHome) {
            returningHome = false;
            addBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    evaluateMobileOffers();
                }
            });
        } else {
            addBehaviour(new MobileVisitBehaviour());
        }
    }

    private String joinSellers() {
        String result = "";
        for (int i = 0; i < sellerNames.length; i++) {
            if (i > 0) {
                result += ", ";
            }
            result += sellerNames[i];
        }
        return result;
    }

    private String joinContainers() {
        String result = "";
        for (int i = 0; i < sellerContainerNames.length; i++) {
            if (i > 0) {
                result += " -> ";
            }
            result += sellerContainerNames[i];
        }
        return result;
    }

    private void moveToNextSellerContainer() {
        if (mobileSellerIndex >= sellerContainerNames.length) {
            returningHome = true;
            moveToContainer(homeContainerName);
            return;
        }

        moveToContainer(sellerContainerNames[mobileSellerIndex]);
    }

    private void moveToContainer(String containerName) {
        ContainerID destination = new ContainerID();
        destination.setName(containerName);
        System.out.println("[" + getLocalName() + "] Migration demandee vers " + containerName);
        doMove(destination);
    }

    private void evaluateMobileOffers() {
        System.out.println("[" + getLocalName() + "] Retour au conteneur home. Offres collectees="
                + mobileOffers.size());

        BuyerSelectionBehaviour evaluator = new BuyerSelectionBehaviour();
        evaluator.evaluateOffersFromMobile(mobileOffers);
        System.out.println("[" + getLocalName() + "] Agent conserve dans la table JADE.");
    }

    private class MobileVisitBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            String requestId = conversationId + "-mobile-request-" + here().getName();
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setConversationId(conversationId);
            cfp.setReplyWith(requestId);
            cfp.setContent("product=" + productName);

            Set<String> waitingSellers = new HashSet<String>();
            for (int i = 0; i < sellerNames.length; i++) {
                cfp.addReceiver(new AID(sellerNames[i], AID.ISLOCALNAME));
                waitingSellers.add(sellerNames[i]);
            }

            send(cfp);

            System.out.println("[" + getLocalName() + "] CFP local envoye aux vendeurs "
                    + joinSellers()
                    + " depuis " + here().getName());

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(conversationId),
                    MessageTemplate.MatchInReplyTo(requestId));

            long endTime = System.currentTimeMillis() + responseTimeoutMs;
            while (!waitingSellers.isEmpty() && System.currentTimeMillis() < endTime) {
                ACLMessage answer = blockingReceive(template, Math.max(1L, endTime - System.currentTimeMillis()));
                if (answer == null) {
                    break;
                }

                String sellerName = answer.getSender().getLocalName();
                if (!waitingSellers.remove(sellerName)) {
                    continue;
                }

                if (answer.getPerformative() == ACLMessage.PROPOSE) {
                    ProductOffer offer = parseOffer(answer.getContent(), sellerName);
                    mobileOffers.add(offer);
                    System.out.println("[" + getLocalName() + "] Offre mobile collectee: seller="
                            + offer.getSellerName()
                            + ", price=" + offer.getPrice()
                            + ", quality=" + offer.getQuality()
                            + ", deliveryCost=" + offer.getDeliveryCost());
                } else {
                    System.out.println("[" + getLocalName() + "] Reponse mobile non retenue depuis "
                            + sellerName + ": " + answer.getContent());
                }
            }

            for (String sellerName : waitingSellers) {
                System.out.println("[" + getLocalName() + "] Timeout chez " + sellerName);
            }

            mobileSellerIndex = sellerContainerNames.length;
            moveToNextSellerContainer();
        }
    }

    private class BuyerSelectionBehaviour extends Behaviour {
        private static final int SEND_CFP = 0;
        private static final int COLLECT = 1;
        private static final int EVALUATE = 2;
        private static final int DONE = 3;

        private int state = SEND_CFP;
        private String requestId;
        private long collectionEndTime;
        private Set<String> waitingSellers = new HashSet<String>();
        private List<ProductOffer> offers = new ArrayList<ProductOffer>();
        private ProductOffer bestOffer;

        @Override
        public void action() {
            if (state == SEND_CFP) {
                sendCfp();
            } else if (state == COLLECT) {
                collectAnswers();
            } else if (state == EVALUATE) {
                evaluateOffers();
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return state == DONE;
        }

        @Override
        public int onEnd() {
            System.out.println("[" + getLocalName() + "] Selection terminee. Agent conserve dans la table JADE.");
            return 0;
        }

        private void sendCfp() {
            requestId = conversationId + "-request-1";
            collectionEndTime = System.currentTimeMillis() + responseTimeoutMs;

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setConversationId(conversationId);
            cfp.setReplyWith(requestId);
            cfp.setContent("product=" + productName);

            for (int i = 0; i < sellerNames.length; i++) {
                cfp.addReceiver(new AID(sellerNames[i], AID.ISLOCALNAME));
                waitingSellers.add(sellerNames[i]);
            }

            send(cfp);
            System.out.println("[" + getLocalName() + "] CFP envoye pour product=" + productName
                    + ", replyWith=" + requestId);

            state = COLLECT;
        }

        private void collectAnswers() {
            if (waitingSellers.isEmpty() || System.currentTimeMillis() >= collectionEndTime) {
                state = EVALUATE;
                return;
            }

            MessageTemplate template = MessageTemplate.MatchConversationId(conversationId);
            ACLMessage message = receive(template);

            if (message == null) {
                block(Math.max(1L, collectionEndTime - System.currentTimeMillis()));
                return;
            }

            handleAnswer(message);
        }

        private void handleAnswer(ACLMessage message) {
            String sellerName = message.getSender().getLocalName();

            if (!requestId.equals(message.getInReplyTo())) {
                System.out.println("[" + getLocalName() + "] Message hors requete ignore depuis " + sellerName);
                return;
            }

            if (!waitingSellers.contains(sellerName)) {
                System.out.println("[" + getLocalName() + "] Reponse dupliquee ou inattendue ignoree depuis " + sellerName);
                return;
            }

            waitingSellers.remove(sellerName);

            if (message.getPerformative() == ACLMessage.PROPOSE) {
                ProductOffer offer = parseOffer(message.getContent(), sellerName);
                offers.add(offer);
                System.out.println("[" + getLocalName() + "] Offre recue: seller=" + offer.getSellerName()
                        + ", price=" + offer.getPrice()
                        + ", quality=" + offer.getQuality()
                        + ", deliveryCost=" + offer.getDeliveryCost());
            } else if (message.getPerformative() == ACLMessage.REFUSE) {
                System.out.println("[" + getLocalName() + "] REFUSE recu depuis " + sellerName
                        + ": " + message.getContent());
            } else {
                System.out.println("[" + getLocalName() + "] Performative ignoree depuis " + sellerName);
            }
        }

        private void evaluateOffers() {
            if (offers.isEmpty()) {
                System.out.println("[" + getLocalName() + "] Aucune offre recue. Aucun achat.");
                sendFinalMessages(null);
                state = DONE;
                return;
            }

            bestOffer = chooseBestOffer();
            System.out.println("[" + getLocalName() + "] Meilleure offre retenue: seller="
                    + bestOffer.getSellerName()
                    + ", price=" + bestOffer.getPrice()
                    + ", quality=" + bestOffer.getQuality()
                    + ", deliveryCost=" + bestOffer.getDeliveryCost());

            sendFinalMessages(bestOffer);
            state = DONE;
        }

        private void evaluateOffersFromMobile(List<ProductOffer> collectedOffers) {
            offers.clear();
            offers.addAll(collectedOffers);
            evaluateOffers();
        }

        private ProductOffer chooseBestOffer() {
            double minPrice = offers.get(0).getPrice();
            double maxPrice = offers.get(0).getPrice();
            double minQuality = offers.get(0).getQuality();
            double maxQuality = offers.get(0).getQuality();
            double minDeliveryCost = offers.get(0).getDeliveryCost();
            double maxDeliveryCost = offers.get(0).getDeliveryCost();

            for (int i = 1; i < offers.size(); i++) {
                ProductOffer offer = offers.get(i);
                minPrice = Math.min(minPrice, offer.getPrice());
                maxPrice = Math.max(maxPrice, offer.getPrice());
                minQuality = Math.min(minQuality, offer.getQuality());
                maxQuality = Math.max(maxQuality, offer.getQuality());
                minDeliveryCost = Math.min(minDeliveryCost, offer.getDeliveryCost());
                maxDeliveryCost = Math.max(maxDeliveryCost, offer.getDeliveryCost());
            }

            ProductOffer best = offers.get(0);
            double bestScore = -1.0;

            for (int i = 0; i < offers.size(); i++) {
                ProductOffer offer = offers.get(i);
                double priceScore = normalizeMin(offer.getPrice(), minPrice, maxPrice);
                double qualityScore = normalizeMax(offer.getQuality(), minQuality, maxQuality);
                double deliveryScore = normalizeMin(offer.getDeliveryCost(), minDeliveryCost, maxDeliveryCost);

                double score = preferences.getPriceWeight() * priceScore
                        + preferences.getQualityWeight() * qualityScore
                        + preferences.getDeliveryCostWeight() * deliveryScore;

                System.out.println("[" + getLocalName() + "] Score " + offer.getSellerName()
                        + " = " + score
                        + " (price=" + priceScore
                        + ", quality=" + qualityScore
                        + ", deliveryCost=" + deliveryScore + ")");

                if (score > bestScore) {
                    bestScore = score;
                    best = offer;
                }
            }

            return best;
        }

        private double normalizeMax(double value, double min, double max) {
            if (max == min) {
                return 1.0;
            }
            return (value - min) / (max - min);
        }

        private double normalizeMin(double value, double min, double max) {
            if (max == min) {
                return 1.0;
            }
            return (max - value) / (max - min);
        }

        private void sendFinalMessages(ProductOffer winner) {
            for (int i = 0; i < sellerNames.length; i++) {
                String sellerName = sellerNames[i];
                boolean accepted = winner != null && winner.getSellerName().equals(sellerName);

                ACLMessage message = new ACLMessage(accepted ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
                message.addReceiver(new AID(sellerName, AID.ISLOCALNAME));
                message.setConversationId(conversationId);

                if (accepted) {
                    message.setContent("result=ACCEPTED;product=" + productName);
                } else {
                    message.setContent("result=REJECTED;product=" + productName);
                }

                send(message);
            }
        }

        private ProductOffer parseOffer(String content, String senderName) {
            String seller = readValue(content, "seller");
            if (seller.length() == 0) {
                seller = senderName;
            }

            return new ProductOffer(
                    seller,
                    readValue(content, "product"),
                    readDoubleValue(content, "price"),
                    readDoubleValue(content, "quality"),
                    readDoubleValue(content, "deliveryCost"));
        }
    }

    private ProductOffer parseOffer(String content, String senderName) {
        String seller = readValue(content, "seller");
        if (seller.length() == 0) {
            seller = senderName;
        }

        return new ProductOffer(
                seller,
                readValue(content, "product"),
                readDoubleValue(content, "price"),
                readDoubleValue(content, "quality"),
                readDoubleValue(content, "deliveryCost"));
    }

    private double readDoubleValue(String content, String key) {
        return Double.parseDouble(readValue(content, key));
    }

    private String readValue(String content, String key) {
        if (content == null) {
            throw new IllegalArgumentException("Contenu vide");
        }

        String[] parts = content.split(";");
        for (int i = 0; i < parts.length; i++) {
            String[] pair = parts[i].split("=", 2);
            if (pair.length == 2 && key.equals(pair[0].trim())) {
                return pair[1].trim();
            }
        }

        throw new IllegalArgumentException("Cle manquante: " + key);
    }
}
