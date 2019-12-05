package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Undetermined;

import java.util.*;

public class WerewolfSession {

    private final MessageChannel channel;
    private final User moderator;
    private final Map<User, Role> roles;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        roles.put(player, new Undetermined(player.getName(), player, this));
    }

    public void removePlayer(User player) {
        roles.remove(player);
    }

    public User getModerator() {
        return moderator;
    }

    public Map<User, Role> getRoles() {
        return roles;
    }
}
