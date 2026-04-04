package entities;

public class Coach {
    private String specialite;
    private boolean disponibilite;
    private String pays;
    private int id;

    public Coach(String specialite, boolean disponibilite, String pays, int id) {
        this.specialite = specialite;
        this.disponibilite = disponibilite;
        this.pays = pays;
        this.id = id;
    }

    public Coach() {
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public boolean isDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(boolean disponibilite) {
        this.disponibilite = disponibilite;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Coach{" +
                "specialite='" + specialite + '\'' +
                ", disponibilite=" + disponibilite +
                ", pays='" + pays + '\'' +
                ", id=" + id +
                '}';
    }
}
