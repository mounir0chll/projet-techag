package part1.negotiation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends Agent {
    private static final long ROUND_TIMEOUT_MS = 1500L;

    private Product product;
    private long globalTimeoutMs;
    private List<String> allBuyerNames;
    private List<String> activeBuyerNames;
    private String conversationId;
    private long auctionEndTime;
    private double currentPrice;
    private Offer bestOffer;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args == null || args.length < 3) {
            System.out.println("[" + getLocalName() + "] Arguments invalides: Product, timeout, buyers attendus.");
            doDelete();
            return;
        }

        product = (Product) args[0];
        globalTimeoutMs = ((Number) args[1]).longValue();
        String[] buyerNames = (String[]) args[2];

        allBuyerNames = new ArrayList<String>();
        activeBuyerNames = new ArrayList<String>();
        for (int i = 0; i < buyerNames.length; i++) {
            allBuyerNames.add(buyerNames[i]);
            activeBuyerNames.add(buyerNames[i]);
        }

        currentPrice = product.getStartPrice();
        conversationId = "auction-" + getLocalName() + "-" + System.currentTimeMillis();

        System.out.println("[" + getLocalName() + "] Demarrage seller.");
        System.out.println("[" + getLocalName() + "] Produit=" + product.getName()
                + ", startPrice=" + product.getStartPrice()
                + ", reservePrice secret configure");
        System.out.println("[" + getLocalName() + "] Buyers=" + activeBuyerNames);
        System.out.println("[" + getLocalName() + "] conversationId=" + conversationId);

        addBehaviour(new AuctionBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + getLocalName() + "] Arret seller.");
    }

    private class AuctionBehaviour extends Behaviour {
        private static final int START = 0;
        private static final int SEND_CFP = 1;
        private static final int COLLECT = 2;
        private static final int EVALUATE = 3;
        private static final int FINISH = 4;
        private static final int DONE = 5;

        private int state = START;
        private int round = 1;
        private String roundId;
        private long roundEndTime;
        private Set<String> waitingBuyers = new HashSet<String>();
        private List<Offer> roundOffers = new ArrayList<Offer>();

        @Override
        public void action() {
            if (state == START) {
                startAuction();
            } else if (state == SEND_CFP) {
                sendRoundCfp();
            } else if (state == COLLECT) {
                collectRoundAnswers();
            } else if (state == EVALUATE) {
                evaluateRound();
            } else if (state == FINISH) {
                finishAuction();
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
            System.out.println("[" + getLocalName() + "] Enchere terminee. Agent conserve dans la table JADE.");
            return 0;
        }

        private void startAuction() {
            auctionEndTime = System.currentTimeMillis() + globalTimeoutMs;
            System.out.println("[" + getLocalName() + "] Debut enchere. Timeout global=" + globalTimeoutMs + " ms");
            state = SEND_CFP;
        }

        private void sendRoundCfp() {
            if (System.currentTimeMillis() >= auctionEndTime) {
                System.out.println("[" + getLocalName() + "] Timeout global atteint avant un nouveau round.");
                state = FINISH;
                return;
            }

            if (activeBuyerNames.isEmpty()) {
                System.out.println("[" + getLocalName() + "] Aucun buyer actif.");
                state = FINISH;
                return;
            }

            roundId = conversationId + "-round-" + round;
            roundOffers.clear();
            waitingBuyers.clear();
            waitingBuyers.addAll(activeBuyerNames);
            roundEndTime = Math.min(System.currentTimeMillis() + ROUND_TIMEOUT_MS, auctionEndTime);

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setConversationId(conversationId);
            cfp.setReplyWith(roundId);
            cfp.setContent("product=" + product.getName()
                    + ";currentPrice=" + currentPrice
                    + ";round=" + round);

            for (int i = 0; i < activeBuyerNames.size(); i++) {
                cfp.addReceiver(new AID(activeBuyerNames.get(i), AID.ISLOCALNAME));
            }

            send(cfp);
            System.out.println("[" + getLocalName() + "] Round " + round + " CFP envoye. currentPrice="
                    + currentPrice + ", replyWith=" + roundId + ", activeBuyers=" + activeBuyerNames);

            state = COLLECT;
        }

        private void collectRoundAnswers() {
            long now = System.currentTimeMillis();
            if (now >= auctionEndTime || now >= roundEndTime || waitingBuyers.isEmpty()) {
                state = EVALUATE;
                return;
            }

            MessageTemplate template = MessageTemplate.MatchConversationId(conversationId);
            ACLMessage message = myAgent.receive(template);

            if (message == null) {
                long waitTime = Math.min(roundEndTime, auctionEndTime) - now;
                block(Math.max(1L, waitTime));
                return;
            }

            handleRoundMessage(message);
        }

        private void handleRoundMessage(ACLMessage message) {
            String senderName = message.getSender().getLocalName();

            if (!roundId.equals(message.getInReplyTo())) {
                System.out.println("[" + getLocalName() + "] Message hors round ignore depuis " + senderName
                        + ". inReplyTo=" + message.getInReplyTo() + ", round courant=" + roundId);
                return;
            }

            if (!waitingBuyers.contains(senderName)) {
                System.out.println("[" + getLocalName() + "] Reponse dupliquee ou inattendue ignoree depuis " + senderName);
                return;
            }

            waitingBuyers.remove(senderName);

            if (message.getPerformative() == ACLMessage.PROPOSE) {
                handleProposal(message, senderName);
            } else if (message.getPerformative() == ACLMessage.REFUSE) {
                activeBuyerNames.remove(senderName);
                System.out.println("[" + getLocalName() + "] REFUSE recu depuis " + senderName);
            } else {
                System.out.println("[" + getLocalName() + "] Performative ignoree depuis " + senderName + ": "
                        + ACLMessage.getPerformative(message.getPerformative()));
            }
        }

        private void handleProposal(ACLMessage message, String senderName) {
            try {
                double price = readDoubleValue(message.getContent(), "price");
                int offerRound = readIntValue(message.getContent(), "round");

                if (offerRound != round) {
                    System.out.println("[" + getLocalName() + "] Offre ignoree car numero de round invalide: " + offerRound);
                    return;
                }

                if (price > currentPrice) {
                    Offer offer = new Offer(senderName, price, round);
                    roundOffers.add(offer);
                    System.out.println("[" + getLocalName() + "] PROPOSE recu depuis " + senderName
                            + ": price=" + price + ", round=" + round);
                } else {
                    System.out.println("[" + getLocalName() + "] Offre ignoree car non superieure au prix courant: " + price);
                }
            } catch (RuntimeException e) {
                System.out.println("[" + getLocalName() + "] Offre invalide ignoree depuis " + senderName
                        + ": " + message.getContent());
            }
        }

        private void evaluateRound() {
            if (roundOffers.isEmpty()) {
                System.out.println("[" + getLocalName() + "] Aucune meilleure offre au round " + round + ".");
                state = FINISH;
                return;
            }

            Offer roundBest = chooseBestOffer(roundOffers);
            bestOffer = roundBest;
            currentPrice = roundBest.getPrice();

            System.out.println("[" + getLocalName() + "] Meilleure offre actuelle: buyer="
                    + bestOffer.getBuyerName() + ", price=" + bestOffer.getPrice()
                    + ", round=" + bestOffer.getRound());

            informCurrentBest();

            if (System.currentTimeMillis() >= auctionEndTime) {
                System.out.println("[" + getLocalName() + "] Timeout global atteint apres evaluation.");
                state = FINISH;
                return;
            }

            if (activeBuyerNames.isEmpty()) {
                System.out.println("[" + getLocalName() + "] Tous les buyers ont quitte l'enchere.");
                state = FINISH;
                return;
            }

            round++;
            state = SEND_CFP;
        }

        private Offer chooseBestOffer(List<Offer> offers) {
            Offer best = offers.get(0);
            for (int i = 1; i < offers.size(); i++) {
                Offer offer = offers.get(i);
                if (offer.getPrice() > best.getPrice()) {
                    best = offer;
                }
            }
            return best;
        }

        private void informCurrentBest() {
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.setConversationId(conversationId);
            inform.setContent("bestBuyer=" + bestOffer.getBuyerName()
                    + ";bestPrice=" + bestOffer.getPrice()
                    + ";round=" + bestOffer.getRound());

            for (int i = 0; i < activeBuyerNames.size(); i++) {
                inform.addReceiver(new AID(activeBuyerNames.get(i), AID.ISLOCALNAME));
            }

            send(inform);
        }

        private void finishAuction() {
            boolean sold = bestOffer != null && bestOffer.getPrice() >= product.getReservePrice();

            if (sold) {
                System.out.println("[" + getLocalName() + "] RESULTAT: VENDU a " + bestOffer.getBuyerName()
                        + " pour " + bestOffer.getPrice());
                sendFinalMessages(true);
            } else {
                double bestPrice = bestOffer == null ? product.getStartPrice() : bestOffer.getPrice();
                System.out.println("[" + getLocalName() + "] RESULTAT: NON VENDU. Meilleur prix atteint=" + bestPrice);
                sendFinalMessages(false);
            }

            state = DONE;
        }

        private void sendFinalMessages(boolean sold) {
            for (int i = 0; i < allBuyerNames.size(); i++) {
                String buyerName = allBuyerNames.get(i);
                boolean isWinner = sold && bestOffer.getBuyerName().equals(buyerName);

                ACLMessage message = new ACLMessage(isWinner ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
                message.addReceiver(new AID(buyerName, AID.ISLOCALNAME));
                message.setConversationId(conversationId);

                if (isWinner) {
                    message.setContent("result=SOLD;winner=" + bestOffer.getBuyerName()
                            + ";finalPrice=" + bestOffer.getPrice());
                } else if (sold) {
                    message.setContent("result=SOLD;winner=" + bestOffer.getBuyerName()
                            + ";finalPrice=" + bestOffer.getPrice());
                } else {
                    double bestPrice = bestOffer == null ? product.getStartPrice() : bestOffer.getPrice();
                    message.setContent("result=NOT_SOLD;bestPrice=" + bestPrice);
                }

                send(message);
            }
        }

        private double readDoubleValue(String content, String key) {
            return Double.parseDouble(readValue(content, key));
        }

        private int readIntValue(String content, String key) {
            return Integer.parseInt(readValue(content, key));
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
}
