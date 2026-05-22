package part2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class SellerAgent extends Agent {
    private ProductOffer offer;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args == null || args.length < 1) {
            System.out.println("[" + getLocalName() + "] Arguments invalides: ProductOffer attendu.");
            doDelete();
            return;
        }

        offer = (ProductOffer) args[0];

        System.out.println("[" + getLocalName() + "] Demarrage seller."
                + " product=" + offer.getProductName()
                + ", price=" + offer.getPrice()
                + ", quality=" + offer.getQuality()
                + ", deliveryCost=" + offer.getDeliveryCost());

        addBehaviour(new SellerProposalBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("[" + getLocalName() + "] Arret seller.");
    }

    private class SellerProposalBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage message = receive();

            if (message == null) {
                block();
                return;
            }

            if (message.getPerformative() == ACLMessage.CFP) {
                handleCfp(message);
            } else if (message.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                System.out.println("[" + getLocalName() + "] Offre acceptee: " + message.getContent());
                System.out.println("[" + getLocalName() + "] Agent conserve dans la table JADE.");
            } else if (message.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                System.out.println("[" + getLocalName() + "] Offre rejetee: " + message.getContent());
                System.out.println("[" + getLocalName() + "] Agent conserve dans la table JADE.");
            } else {
                System.out.println("[" + getLocalName() + "] Message ignore: "
                        + ACLMessage.getPerformative(message.getPerformative()));
            }
        }

        private void handleCfp(ACLMessage cfp) {
            String requestedProduct = readValue(cfp.getContent(), "product");

            if (offer.getProductName().equalsIgnoreCase(requestedProduct)) {
                ACLMessage reply = cfp.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setConversationId(cfp.getConversationId());
                reply.setInReplyTo(cfp.getReplyWith());
                reply.setContent("seller=" + getLocalName()
                        + ";product=" + offer.getProductName()
                        + ";price=" + offer.getPrice()
                        + ";quality=" + offer.getQuality()
                        + ";deliveryCost=" + offer.getDeliveryCost());
                send(reply);

                System.out.println("[" + getLocalName() + "] PROPOSE envoye pour " + requestedProduct
                        + " inReplyTo=" + cfp.getReplyWith());
            } else {
                ACLMessage reply = cfp.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setConversationId(cfp.getConversationId());
                reply.setInReplyTo(cfp.getReplyWith());
                reply.setContent("seller=" + getLocalName() + ";reason=product-not-available");
                send(reply);

                System.out.println("[" + getLocalName() + "] REFUSE produit indisponible: " + requestedProduct);
            }
        }

        private String readValue(String content, String key) {
            if (content == null) {
                return "";
            }

            String[] parts = content.split(";");
            for (int i = 0; i < parts.length; i++) {
                String[] pair = parts[i].split("=", 2);
                if (pair.length == 2 && key.equals(pair[0].trim())) {
                    return pair[1].trim();
                }
            }

            return "";
        }
    }
}
