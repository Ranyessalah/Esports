

package esprit.tn.esports.service;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Matchs;
import esprit.tn.esports.entite.StatsRow;

import java.util.*;

public class StatsService {

    private final EquipeService equipeService = new EquipeService();
    private final MatchService matchService = new MatchService();


    public List<StatsRow> getClassement() {

        List<Equipe> equipes = equipeService.getAll();
        List<Matchs> matchs = matchService.getAll();

        Map<Integer, StatsRow> map = new HashMap<>();


        // INIT équipes
        for (Equipe e : equipes) {

            StatsRow row = new StatsRow();

            row.setRank(0);
            row.setTeam(e.getNom());
            row.setGame(e.getGame());

            row.setPlayed(0);
            row.setWins(0);
            row.setDraws(0);
            row.setLosses(0);

            row.setBp(0);
            row.setBc(0);
            row.setDiff(0);
            row.setPoints(0);

            map.put(e.getId(), row);
        }


        // CALCUL matchs
        for (Matchs m : matchs) {

            if (m.getEquipe1() == null || m.getEquipe2() == null)
                continue;

            StatsRow t1 = map.get(m.getEquipe1().getId());
            StatsRow t2 = map.get(m.getEquipe2().getId());

            if (t1 == null || t2 == null)
                continue;

            int s1 = m.getScoreEquipe1();
            int s2 = m.getScoreEquipe2();

            t1.setPlayed(t1.getPlayed() + 1);
            t2.setPlayed(t2.getPlayed() + 1);

            t1.setBp(t1.getBp() + s1);
            t1.setBc(t1.getBc() + s2);

            t2.setBp(t2.getBp() + s2);
            t2.setBc(t2.getBc() + s1);


            // RESULT
            if (s1 > s2) {

                t1.setWins(t1.getWins() + 1);
                t1.setPoints(t1.getPoints() + 3);

                t2.setLosses(t2.getLosses() + 1);

            } else if (s2 > s1) {

                t2.setWins(t2.getWins() + 1);
                t2.setPoints(t2.getPoints() + 3);

                t1.setLosses(t1.getLosses() + 1);

            } else {

                t1.setDraws(t1.getDraws() + 1);
                t2.setDraws(t2.getDraws() + 1);

                t1.setPoints(t1.getPoints() + 1);
                t2.setPoints(t2.getPoints() + 1);
            }
        }


        // DIFF
        for (StatsRow row : map.values()) {
            row.setDiff(row.getBp() - row.getBc());
        }


        List<StatsRow> result = new ArrayList<>(map.values());


        // SORT OFFICIEL
        result.sort(
                Comparator.comparingInt(StatsRow::getPoints).reversed()
                        .thenComparingInt(StatsRow::getWins).reversed()
                        .thenComparingInt(StatsRow::getDiff).reversed()
        );


        // RANK
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }
}