package part1.negotiation;

import java.util.HashSet;
import java.util.Set;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class BuyerAgent extends Agent {
    private String sellerName;
    private double maxPrice;
    private double increment;
    private boolean active;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args == null || args.length < 3) {
            System.out.println("[" + getLocalName() + "] Arguments invalides: sellerName, maxPrice, increment attendus.");
            doDelete();
            return;
        }

        sellerName = (String) args[0];
        maxPrice = ((Number) args[1]).doubleValue();
        increment = ((Number) args[2]).doubleValue();
        active = true;

        System.out.println("[" + getLocalName() + "] Demarrage buyer. Seller=" + sellerName
                + ", maxPrice=" + maxPrice + ", increment=" + increment);

        addBehaviour(new AuctionResponseBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + getLocalName() + "] Arret buyer.");
    }

    private class AuctionResponseBehaviour extends CyclicBehaviour {
        private final Set<String> answeredRounds = new HashSet<String>();

        @Override
        public void action() {
            ACLMessage message = myAgent.receive();

            if (message == null) {
                block();
                return;
            }

            if (message.getPerformative() == ACLMessage.CFP) {
                handleCfp(message);
            } else if (message.getPerformative() == ACLMessage.INFORM) {
                System.out.println("[" + getLocalName() + "] Info enchere: " + message.getContent());
            } else if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                System.out.println("[" + getLocalName() + "] Proposition acceptee: " + message.getContent());
                active = false;
                System.out.println("[" + getLocalName() + "] Agent conserve dans la table JADE.");
            } else if (message.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                System.out.println("[" + getLocalName() + "] Proposition rejetee: " + message.getContent());
                active = false;
                System.out.println("[" + getLocalName() + "] Agent conserve dans la table JADE.");
            } else {
                System.out.println("[" + getLocalName() + "] Message ignore: performative="
                        + ACLMessage.getPerformative(message.getPerformative()));
            }
        }

        private void handleCfp(ACLMessage cfp) {
            String senderName = cfp.getSender().getLocalName();
            String roundId = cfp.getReplyWith();

            if (!sellerName.equals(senderName)) {
                System.out.println("[" + getLocalName() + "] CFP ignore depuis un vendeur inconnu: " + senderName);
                return;
            }

            if (roundId == null || answeredRounds.contains(roundId)) {
                System.out.println("[" + getLocalName() + "] CFP ignore car le round a deja ete traite: " + roundId);
                return;
            }

            answeredRounds.add(roundId);

            if (!active) {
                System.out.println("[" + getLocalName() + "] CFP ignore car le buyer a deja quitte l'enchere.");
                return;
            }

            try {
                double currentPrice = readDoubleValue(cfp.getContent(), "currentPrice");
                int round = readIntValue(cfp.getContent(), "round");
                double proposedPrice = currentPrice + increment;

                if (proposedPrice <= maxPrice) {
                    sendProposal(cfp, proposedPrice, round);
                } else {
                    active = false;
                    sendRefuse(cfp, round);
                }
            } catch (RuntimeException e) {
                active = false;
                System.out.println("[" + getLocalName() + "] CFP invalide, refus par securite: " + cfp.getContent());
                sendRefuse(cfp, -1);
            }
        }

        private void sendProposal(ACLMessage cfp, double price, int round) {
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setConversationId(cfp.getConversationId());
            reply.setInReplyTo(cfp.getReplyWith());
            reply.setContent("buyer=" + getLocalName() + ";price=" + price + ";round=" + round);
            send(reply);

            System.out.println("[" + getLocalName() + "] PROPOSE price=" + price + " round=" + round
                    + " inReplyTo=" + cfp.getReplyWith());
        }

        private void sendRefuse(ACLMessage cfp, int round) {
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setConversationId(cfp.getConversationId());
            reply.setInReplyTo(cfp.getReplyWith());
            reply.setContent("buyer=" + getLocalName() + ";maxPrice=" + maxPrice + ";round=" + round);
            send(reply);

            System.out.println("[" + getLocalName() + "] REFUSE round=" + round + " inReplyTo=" + cfp.getReplyWith());
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
