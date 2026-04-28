package tn.esprit.services;

public class DrugInteractionResult {
    private final String medicamentA;
    private final String medicamentB;
    private final String detailInteraction;
    private final boolean dangereuse;

    public DrugInteractionResult(String medicamentA, String medicamentB, String detailInteraction, boolean dangereuse) {
        this.medicamentA = medicamentA;
        this.medicamentB = medicamentB;
        this.detailInteraction = detailInteraction;
        this.dangereuse = dangereuse;
    }

    public String getMedicamentA() {
        return medicamentA;
    }

    public String getMedicamentB() {
        return medicamentB;
    }

    public String getDetailInteraction() {
        return detailInteraction;
    }

    public boolean isDangereuse() {
        return dangereuse;
    }
}