package entities;

import java.time.LocalDateTime;

public class Matchs {

    private int id;
    private String statut;
    private LocalDateTime dateMatch;
    private LocalDateTime dateFinMatch;
    private Integer scoreEquipe1;
    private Integer scoreEquipe2;
    private String nomMatch;

    private Equipe equipe1;
    private Equipe equipe2;

    public Matchs() {}

    public Matchs(int id, String statut, LocalDateTime dateMatch, LocalDateTime dateFinMatch,
                  Integer scoreEquipe1, Integer scoreEquipe2, String nomMatch,
                  Equipe equipe1, Equipe equipe2) {
        this.id = id;
        this.statut = statut;
        this.dateMatch = dateMatch;
        this.dateFinMatch = dateFinMatch;
        this.scoreEquipe1 = scoreEquipe1;
        this.scoreEquipe2 = scoreEquipe2;
        this.nomMatch = nomMatch;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateMatch() { return dateMatch; }
    public void setDateMatch(LocalDateTime dateMatch) { this.dateMatch = dateMatch; }

    public LocalDateTime getDateFinMatch() { return dateFinMatch; }
    public void setDateFinMatch(LocalDateTime dateFinMatch) { this.dateFinMatch = dateFinMatch; }

    public Integer getScoreEquipe1() { return scoreEquipe1; }
    public void setScoreEquipe1(Integer scoreEquipe1) { this.scoreEquipe1 = scoreEquipe1; }

    public Integer getScoreEquipe2() { return scoreEquipe2; }
    public void setScoreEquipe2(Integer scoreEquipe2) { this.scoreEquipe2 = scoreEquipe2; }

    public String getNomMatch() { return nomMatch; }
    public void setNomMatch(String nomMatch) { this.nomMatch = nomMatch; }

    public Equipe getEquipe1() { return equipe1; }
    public void setEquipe1(Equipe equipe1) { this.equipe1 = equipe1; }

    public Equipe getEquipe2() { return equipe2; }
    public void setEquipe2(Equipe equipe2) { this.equipe2 = equipe2; }
}