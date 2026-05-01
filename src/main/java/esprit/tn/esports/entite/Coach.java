package esprit.tn.esports.entite;

public class Coach {

    private int id;
    private String specialite;
    private boolean disponibilite;
    private String pays;

    private User user; // relation

    public Coach() {}

    public Coach(int id, String specialite, boolean disponibilite, String pays, User user) {
        this.id = id;
        this.specialite = specialite;
        this.disponibilite = disponibilite;
        this.pays = pays;
        this.user = user;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}