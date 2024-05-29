package it.loneliness.mc.treasurehunt.Controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import it.loneliness.mc.treasurehunt.Model.LogHandler;
import it.loneliness.mc.treasurehunt.Plugin;

/**
 * Handles interactions with the scoreboard api
 */
public class ScoreboardController {

    private static ScoreboardController thisHandler;

    public static ScoreboardController getInstance(Plugin plugin) {
        if (thisHandler == null) {
            thisHandler = new ScoreboardController(plugin);
        }
        return thisHandler;
    }

    private final Plugin plugin;
    private final LogHandler logger;
    private final Objective scoreObjective;

    private ScoreboardController(Plugin plugin) {
        this.plugin = plugin;
        this.logger = LogHandler.getInstance(null);

        String scoreboardId = this.plugin.getConfig().getString("scoreboard-id");
        this.scoreObjective = getOrMakeObjective(scoreboardId, scoreboardId);
    }

    public boolean incrementScore(String entityName, int addedPoints) {
        Score s = scoreObjective.getScore(entityName);
        s.setScore(s.getScore() + addedPoints);
        return true;
    }

    public Objective getScoreObjective(){
        return this.scoreObjective;
    }

    public Scoreboard getScoreboard(){
        return this.scoreObjective.getScoreboard();
    }

    public Set<String> getPartecipatingPlayers(){
        return this.scoreObjective.getScoreboard().getEntries();
    }

    public boolean resetAllPlayersScore() {
        Set<String> partecipatingPlayers = scoreObjective.getScoreboard().getEntries();
        for (String playerName : partecipatingPlayers) {
            scoreObjective.getScore(playerName).setScore(0);
        }
        return true;
    }

    public List<String> getSortedPlayersByScore() {
        // Get the scoreboard
        Scoreboard scoreboard = scoreObjective.getScoreboard();
        
        // Get all participating players
        Set<String> participatingPlayers = scoreboard.getEntries();
        
        // Stream the set, map each player to their score, sort by score, and collect to a list
        List<String> sortedPlayers = participatingPlayers.stream()
                .sorted((player1, player2) -> {
                    int score1 = scoreObjective.getScore(player1).getScore();
                    int score2 = scoreObjective.getScore(player2).getScore();
                    return Integer.compare(score2, score1); // Descending order
                })
                .collect(Collectors.toList());
        
        return sortedPlayers;
    }

    private Objective getOrMakeObjective(String key, String displayName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective(key);
        if (objective == null) {
            logger.warning(String.format("No scoreboard objective \"%s\" found, creating new objective", key));
            @SuppressWarnings("deprecation")
            Objective o = board.registerNewObjective(key, "dummy", displayName, RenderType.INTEGER);
            objective = o;

        }

        if (!objective.isModifiable()) {
            String msg = "Could not modify objective \"%s\"! Make sure objective criteria is set to dummy!";
            logger.warning(String.format(msg, objective.getName()));
        }
        return objective;
    }

}