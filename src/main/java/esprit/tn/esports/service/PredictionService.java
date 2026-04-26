package esprit.tn.esports.service;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Matchs;

import java.util.List;
import java.util.stream.Collectors;

public class PredictionService {

    private final MatchService matchService = new MatchService();

    public static class PredictionResult {
        public double probTeam1;
        public double probTeam2;
        public String favoriteName;

        public PredictionResult(double probTeam1, double probTeam2, String favoriteName) {
            this.probTeam1 = probTeam1;
            this.probTeam2 = probTeam2;
            this.favoriteName = favoriteName;
        }
    }

    public PredictionResult predictWinner(Matchs upcomingMatch) {
        if (upcomingMatch == null || upcomingMatch.getEquipe1() == null || upcomingMatch.getEquipe2() == null) {
            return new PredictionResult(50.0, 50.0, "Inconnu");
        }
        return predictWinner(upcomingMatch.getEquipe1(), upcomingMatch.getEquipe2());
    }

    public PredictionResult predictWinner(Equipe eq1, Equipe eq2) {
        if (eq1 == null || eq2 == null) {
            return new PredictionResult(50.0, 50.0, "Inconnu");
        }

        // Get all matches with results (terminé or matches with actual scores)
        List<Matchs> allMatches = matchService.getAll().stream()
                .filter(m -> {
                    if (m.getStatut() == null) return false;
                    String st = m.getStatut().toLowerCase();
                    boolean isFinished = st.contains("termin"); // matches "termine" and "terminé"
                    boolean hasScore = (m.getScoreEquipe1() != null && m.getScoreEquipe2() != null) 
                                       && (m.getScoreEquipe1() > 0 || m.getScoreEquipe2() > 0);
                    return isFinished || hasScore;
                })
                .collect(Collectors.toList());

        // If literally no data exists, we give a slight edge based on team name hash to avoid exactly 50/50
        if (allMatches.isEmpty()) {
            double randomEdge = (Math.abs(eq1.getNom().hashCode() % 10) - 5) * 1.5; // +/- 7.5%
            double p1 = 50.0 + randomEdge;
            double p2 = 100.0 - p1;
            String fav = p1 > p2 ? eq1.getNom() : (p2 > p1 ? eq2.getNom() : "Match Nul");
            return new PredictionResult(p1, p2, fav);
        }

        // --- Head-to-Head Stats ---
        List<Matchs> h2hMatches = allMatches.stream()
                .filter(m -> (m.getEquipe1().getId() == eq1.getId() && m.getEquipe2().getId() == eq2.getId()) ||
                             (m.getEquipe1().getId() == eq2.getId() && m.getEquipe2().getId() == eq1.getId()))
                .collect(Collectors.toList());

        double h2hWinsEq1 = 0;
        double h2hWinsEq2 = 0;
        int totalH2H = h2hMatches.size();

        for (Matchs m : h2hMatches) {
            boolean isEq1Home = m.getEquipe1().getId() == eq1.getId();
            int scoreEq1Match = isEq1Home ? m.getScoreEquipe1() : m.getScoreEquipe2();
            int scoreEq2Match = isEq1Home ? m.getScoreEquipe2() : m.getScoreEquipe1();

            if (scoreEq1Match > scoreEq2Match) h2hWinsEq1++;
            else if (scoreEq2Match > scoreEq1Match) h2hWinsEq2++;
            else {
                h2hWinsEq1 += 0.5;
                h2hWinsEq2 += 0.5;
            }
        }

        // --- Overall Stats Team 1 ---
        double overallWinsEq1 = 0;
        int totalMatchesEq1 = 0;

        for (Matchs m : allMatches) {
            if (m.getEquipe1().getId() == eq1.getId()) {
                totalMatchesEq1++;
                if (m.getScoreEquipe1() > m.getScoreEquipe2()) overallWinsEq1++;
                else if (m.getScoreEquipe1().equals(m.getScoreEquipe2())) overallWinsEq1 += 0.5;
            } else if (m.getEquipe2().getId() == eq1.getId()) {
                totalMatchesEq1++;
                if (m.getScoreEquipe2() > m.getScoreEquipe1()) overallWinsEq1++;
                else if (m.getScoreEquipe2().equals(m.getScoreEquipe1())) overallWinsEq1 += 0.5;
            }
        }

        // --- Overall Stats Team 2 ---
        double overallWinsEq2 = 0;
        int totalMatchesEq2 = 0;

        for (Matchs m : allMatches) {
            if (m.getEquipe1().getId() == eq2.getId()) {
                totalMatchesEq2++;
                if (m.getScoreEquipe1() > m.getScoreEquipe2()) overallWinsEq2++;
                else if (m.getScoreEquipe1().equals(m.getScoreEquipe2())) overallWinsEq2 += 0.5;
            } else if (m.getEquipe2().getId() == eq2.getId()) {
                totalMatchesEq2++;
                if (m.getScoreEquipe2() > m.getScoreEquipe1()) overallWinsEq2++;
                else if (m.getScoreEquipe2().equals(m.getScoreEquipe1())) overallWinsEq2 += 0.5;
            }
        }

        // Calculate Rates
        double h2hRateEq1 = totalH2H > 0 ? h2hWinsEq1 / totalH2H : 0.5;
        double overallRateEq1 = totalMatchesEq1 > 0 ? overallWinsEq1 / totalMatchesEq1 : 0.5;
        double overallRateEq2 = totalMatchesEq2 > 0 ? overallWinsEq2 / totalMatchesEq2 : 0.5;

        // Blending Algorithm
        double prob1;
        if (totalH2H >= 2) {
            // Strong H2H weight if they played multiple times
            prob1 = (h2hRateEq1 * 0.6) + (overallRateEq1 * 0.2) + ((1.0 - overallRateEq2) * 0.2);
        } else if (totalH2H == 1) {
            // Weak H2H weight if they only played once
            prob1 = (h2hRateEq1 * 0.3) + (overallRateEq1 * 0.35) + ((1.0 - overallRateEq2) * 0.35);
        } else {
            // No H2H, rely purely on overall performance against others
            prob1 = (overallRateEq1 * 0.5) + ((1.0 - overallRateEq2) * 0.5);
        }

        // Add a slight regression to the mean (prevent 100% or 0%)
        prob1 = (prob1 * 0.8) + (0.5 * 0.2);

        // Convert to percentages
        double prob1Pct = Math.round(prob1 * 1000.0) / 10.0;
        double prob2Pct = Math.round((1.0 - prob1) * 1000.0) / 10.0;

        // Determine favorite
        String favorite = prob1Pct > prob2Pct ? eq1.getNom() : (prob2Pct > prob1Pct ? eq2.getNom() : "Match Nul");

        return new PredictionResult(prob1Pct, prob2Pct, favorite);
    }
}
