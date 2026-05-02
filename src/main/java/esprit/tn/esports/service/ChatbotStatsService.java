package esprit.tn.esports.service;

import esprit.tn.esports.entite.StatsRow;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatbotStatsService {
    private static final Pattern TOP_PATTERN = Pattern.compile("\\b(?:top|premier(?:es)?|first)\\s*(\\d{1,2})\\b");
    private static final List<String> STOP_WORDS = List.of(
            "points", "point", "classement", "rang", "position", "meilleure", "meilleur",
            "defense", "attaque", "top", "compare", "comparaison", "vs", "contre",
            "combien", "equipe", "equipes", "team", "teams", "stats", "statistiques",
            "buts", "victoire", "defaite", "nul", "draw", "match", "bilan",
            "leader", "premier", "derniere", "dernier"
    );

    private final Supplier<List<StatsRow>> dataSupplier;

    public ChatbotStatsService() {
        StatsService statsService = new StatsService();
        this.dataSupplier = statsService::getClassement;
    }

    public ChatbotStatsService(Supplier<List<StatsRow>> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    public String answer(String question) {
        String raw = question == null ? "" : question.trim();
        if (raw.isEmpty()) {
            return "Posez une question sur le classement (ex: top 3, points de RealMadrid, meilleure attaque).";
        }

        List<StatsRow> rows = dataSupplier.get();
        if (rows == null || rows.isEmpty()) {
            return "Aucune statistique disponible pour le moment.";
        }

        String q = normalize(raw);
        if (isHelpIntent(q)) {
            return helpMessage();
        }

        if (containsAny(q, "leader", "premier", "1er")) {
            StatsRow first = rows.get(0);
            return "Le leader est " + first.getTeam() + " avec " + first.getPoints() + " points.";
        }

        if (containsAny(q, "dernier", "lanterne")) {
            StatsRow last = rows.get(rows.size() - 1);
            return "La derniere equipe est " + last.getTeam() + " avec " + last.getPoints() + " points.";
        }

        if (containsAny(q, "pire defense", "plus de buts encaisses", "plus bc")) {
            StatsRow worst = rows.stream().max(Comparator.comparingInt(StatsRow::getBc)).orElse(rows.get(0));
            return worst.getTeam() + " a la pire defense avec " + worst.getBc() + " BC.";
        }

        if (containsAny(q, "meilleure diff", "meilleure difference", "meilleure diff de buts")) {
            StatsRow best = rows.stream().max(Comparator.comparingInt(StatsRow::getDiff)).orElse(rows.get(0));
            return best.getTeam() + " a la meilleure difference avec " + best.getDiff() + ".";
        }

        if (containsAny(q, "combien", "nombre", "how many") && containsAny(q, "equipe", "equipes", "team", "teams")) {
            return "Il y a " + rows.size() + " equipes dans le classement.";
        }

        if (containsAny(q, "meilleure attaque", "best attack", "plus de buts", "plus bp")) {
            StatsRow best = rows.stream().max(Comparator.comparingInt(StatsRow::getBp)).orElse(rows.get(0));
            return best.getTeam() + " a la meilleure attaque avec " + best.getBp() + " BP.";
        }

        if (containsAny(q, "meilleure defense", "best defense", "moins de buts encaisses", "moins bc")) {
            StatsRow best = rows.stream().min(Comparator.comparingInt(StatsRow::getBc)).orElse(rows.get(0));
            return best.getTeam() + " a la meilleure defense avec " + best.getBc() + " BC.";
        }

        List<StatsRow> mentionedTeams = findMentionedTeams(q, rows);
        if (containsAny(q, "compare", "comparaison", "vs", "contre") && mentionedTeams.size() >= 2) {
            return compareTeams(mentionedTeams.get(0), mentionedTeams.get(1));
        }

        if (containsAny(q, "top", "classement", "ranking", "premier", "meilleur")) {
            int topN = extractTopN(q).orElse(3);
            return buildTopAnswer(rows, topN);
        }

        if (!mentionedTeams.isEmpty()) {
            StatsRow team = mentionedTeams.get(0);
            if (containsAny(q, "point", "points")) {
                return team.getTeam() + " a " + team.getPoints() + " points.";
            }
            if (containsAny(q, "position", "rang", "classement")) {
                return team.getTeam() + " est classee #" + team.getRank() + ".";
            }
            if (containsAny(q, "victoire", "wins", "defaite", "losses", "nul", "draw", "match", "bilan")) {
                return formatTeamSummary(team);
            }
            return formatTeamSummary(team);
        }

        String suggestion = suggestTeam(q, rows);
        if (suggestion != null) {
            return "Je n'ai pas trouve l'equipe. Voulez-vous dire: " + suggestion + " ?";
        }

        return "Je n'ai pas compris. Essayez: top 5, points de ESS, meilleure defense, comparaison ESS vs Esperance.";
    }

    private String formatTeamSummary(StatsRow team) {
        return team.getTeam()
                + " est #" + team.getRank()
                + " avec " + team.getPoints() + " pts ("
                + team.getWins() + "V, "
                + team.getDraws() + "N, "
                + team.getLosses() + "D, diff "
                + team.getDiff() + ").";
    }

    private String compareTeams(StatsRow a, StatsRow b) {
        String winnerByPoints = a.getPoints() == b.getPoints()
                ? "Egalite au points"
                : (a.getPoints() > b.getPoints() ? a.getTeam() + " est devant" : b.getTeam() + " est devant");

        return "Comparaison " + a.getTeam() + " vs " + b.getTeam() + ": "
                + a.getTeam() + " (" + a.getPoints() + " pts, diff " + a.getDiff() + ") - "
                + b.getTeam() + " (" + b.getPoints() + " pts, diff " + b.getDiff() + "). "
                + winnerByPoints + ".";
    }

    private String buildTopAnswer(List<StatsRow> rows, int topN) {
        int safeTop = Math.max(1, Math.min(topN, rows.size()));
        List<StatsRow> top = rows.stream().limit(safeTop).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("Top ").append(safeTop).append(" equipes: ");
        for (int i = 0; i < top.size(); i++) {
            StatsRow r = top.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append("#").append(r.getRank())
              .append(" ").append(r.getTeam())
              .append(" (").append(r.getPoints()).append(" pts)");
        }
        return sb.toString();
    }

    private Optional<Integer> extractTopN(String q) {
        Matcher matcher = TOP_PATTERN.matcher(q);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        if (containsAny(q, "top", "classement") && containsAny(q, "10")) {
            return Optional.of(10);
        }
        if (containsAny(q, "top", "classement") && containsAny(q, "5")) {
            return Optional.of(5);
        }
        return Optional.empty();
    }

    private List<StatsRow> findMentionedTeams(String q, List<StatsRow> rows) {
        List<StatsRow> matches = new ArrayList<>();
        for (StatsRow row : rows) {
            String name = normalize(row.getTeam());
            if (name.isEmpty()) {
                continue;
            }
            if (q.contains(name) || containsAnyToken(q, name)) {
                matches.add(row);
            }
        }

        matches.sort((a, b) -> Integer.compare(b.getTeam().length(), a.getTeam().length()));
        return matches;
    }

    private boolean isHelpIntent(String q) {
        return containsAny(q, "aide", "help", "que peux", "questions", "exemples");
    }

    private boolean containsAny(String q, String... tokens) {
        for (String token : tokens) {
            if (q.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    public String getHelpMessage() {
        return helpMessage();
    }

    private String helpMessage() {
        return "Je peux repondre a: top N, points d'une equipe, position/rang, meilleure attaque/defense, pire defense, leader, comparaison entre 2 equipes.";
    }

    private boolean containsAnyToken(String q, String teamName) {
        String[] parts = teamName.split("\\s+");
        for (String part : parts) {
            if (part.length() >= 3 && q.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String suggestTeam(String q, List<StatsRow> rows) {
        String candidate = stripStopWords(q);
        if (candidate.isEmpty()) {
            return null;
        }
        String bestTeam = null;
        int bestScore = Integer.MAX_VALUE;
        for (StatsRow row : rows) {
            String name = normalize(row.getTeam());
            if (name.isEmpty()) {
                continue;
            }
            int dist = levenshtein(candidate, name);
            if (dist < bestScore) {
                bestScore = dist;
                bestTeam = row.getTeam();
            }
        }
        return bestScore <= 3 ? bestTeam : null;
    }

    private String stripStopWords(String q) {
        String[] parts = q.split("\\s+");
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (!STOP_WORDS.contains(part)) {
                kept.add(part);
            }
        }
        return String.join(" ", kept).trim();
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}

