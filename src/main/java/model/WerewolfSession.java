package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;
import java.util.Set;

public class WerewolfSession {

    private MessageChannel channel;
    private User moderator;
    private Set<User> players;

    public WerewolfSession(MessageChannel channel, User moderator) {
        this.channel = channel;
        this.moderator = moderator;
        this.players = new HashSet<>();
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        players.add(player);
    }
}
