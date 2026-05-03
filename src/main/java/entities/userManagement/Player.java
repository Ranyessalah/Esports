package entities.userManagement;

public class Player {
    private String pays;
    private String niveau;
    private boolean statut;
    private int equipe_id;
    private int id;
    private User user;
    public Player(String pays, String niveau, boolean statut, int equipe_id, int id,  User user) {
        this.pays = pays;
        this.niveau = niveau;
        this.statut = statut;
        this.equipe_id = equipe_id;
        this.id = id;
        this.user = user;
    }
    public Player() {}

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public int getEquipe_id() {
        return equipe_id;
    }

    public void setEquipe_id(int equipe_id) {
        this.equipe_id = equipe_id;
    }

    public boolean isStatut() {
        return statut;
    }

    public void setStatut(boolean statut) {
        this.statut = statut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    @Override
    public String toString() {
        return "Player{" +
                "pays='" + pays + '\'' +
                ", niveau='" + niveau + '\'' +
                ", statut=" + statut +
                ", equipe_id=" + equipe_id +
                ", id=" + id +
                '}';
    }
}
