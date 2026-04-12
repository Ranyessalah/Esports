package tn.esprit.tournamentmodule.models;

import java.util.List;

public class League {

    private int id;
    private String name;
    private String game;
    private String season;
    private List<String> teams;
    private LeagueStatus status;

    public League() {}

    public League(int id, String name, String game, String season,
                  List<String> teams, LeagueStatus status) {
        this.id = id; this.name = name; this.game = game;
        this.season = season; this.teams = teams; this.status = status;
    }

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public String getGame()                   { return game; }
    public void setGame(String game)          { this.game = game; }
    public String getSeason()                 { return season; }
    public void setSeason(String season)      { this.season = season; }
    public List<String> getTeams()            { return teams; }
    public void setTeams(List<String> teams)  { this.teams = teams; }
    public LeagueStatus getStatus()           { return status; }
    public void setStatus(LeagueStatus status){ this.status = status; }

    public String getTeamsDisplay() {
        if (teams == null || teams.isEmpty()) return "0 Teams";
        return teams.size() + " Team" + (teams.size() == 1 ? "" : "s");
    }

    @Override
    public String toString() { return name; }
}
