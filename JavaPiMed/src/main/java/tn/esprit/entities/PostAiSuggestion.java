package tn.esprit.entities;

public class PostAiSuggestion {

    private String titre;
    private String categorie;
    private String hashtags;
    private String contenuReformule;
    private String resume;
    private String ton;
    private String conseilResponsable;

    public PostAiSuggestion() {
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getContenuReformule() {
        return contenuReformule;
    }

    public void setContenuReformule(String contenuReformule) {
        this.contenuReformule = contenuReformule;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getTon() {
        return ton;
    }

    public void setTon(String ton) {
        this.ton = ton;
    }

    public String getConseilResponsable() {
        return conseilResponsable;
    }

    public void setConseilResponsable(String conseilResponsable) {
        this.conseilResponsable = conseilResponsable;
    }
}