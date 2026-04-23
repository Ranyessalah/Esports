package entities;


import java.util.List;

public class Equipe {

    private int id;
    private String nom;
    private String logo;
    private String game;
    private String categorie;

    private Coach coach;
    private List<Player> players;

    public Equipe() {}

    public Equipe(int id, String nom, String logo, String game, String categorie, Coach coach) {
        this.id = id;
        this.nom = nom;
        this.logo = logo;
        this.game = game;
        this.categorie = categorie;
        this.coach = coach;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
}