// ==========================
// StatsRow.java
// package esprit.tn.esports.entite;
// ==========================

package esprit.tn.esports.entite;

public class StatsRow {

    private int rank;
    private String team;
    private String game;

    private int played;
    private int wins;
    private int draws;
    private int losses;

    private int bp;
    private int bc;
    private int diff;
    private int points;

    public StatsRow() {
    }

    public StatsRow(int rank, String team, String game,
                    int played, int wins, int draws, int losses,
                    int bp, int bc, int diff, int points) {

        this.rank = rank;
        this.team = team;
        this.game = game;
        this.played = played;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.bp = bp;
        this.bc = bc;
        this.diff = diff;
        this.points = points;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public int getPlayed() { return played; }
    public void setPlayed(int played) { this.played = played; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getBp() { return bp; }
    public void setBp(int bp) { this.bp = bp; }

    public int getBc() { return bc; }
    public void setBc(int bc) { this.bc = bc; }

    public int getDiff() { return diff; }
    public void setDiff(int diff) { this.diff = diff; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}