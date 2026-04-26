package esprit.tn.esports.entite;

public class Player {

    private int id;
    private String pays;
    private boolean statut;
    private String niveau;

    private Equipe equipe;
    private User user;

    public Player() {}

    public Player(int id, String pays, boolean statut, String niveau, Equipe equipe, User user) {
        this.id = id;
        this.pays = pays;
        this.statut = statut;
        this.niveau = niveau;
        this.equipe = equipe;
        this.user = user;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public boolean isStatut() { return statut; }
    public void setStatut(boolean statut) { this.statut = statut; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public Equipe getEquipe() { return equipe; }
    public void setEquipe(Equipe equipe) { this.equipe = equipe; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}