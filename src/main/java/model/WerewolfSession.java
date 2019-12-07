package model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import roles.Undetermined;

import java.util.*;
import java.util.stream.Collectors;

public class WerewolfSession {

    public static final int MIN_PLAYER_COUNT = 2;

    private final MessageChannel channel;
    private final User moderator;
    private final Map<String, Role> roles;

    private SessionStatus status;

    public WerewolfSession(MessageChannel channel, User moderator, Collection<User> players) {
        this.channel = channel;
        this.moderator = moderator;
        this.roles = new TreeMap<>();
        for (User player : players) {
            addPlayer(player);
        }
        this.status = SessionStatus.WAITING_FOR_PLAYERS;
    }

    public void addPlayer(User player) { // TODO: try catch if player is already added
        roles.put(player.getId(), new Undetermined(player.getName(), player, this));
    }

    public void removePlayer(User player) {
        roles.remove(player.getId());
    }

    public User getModerator() {
        return moderator;
    }

    public Map<String, Role> getRoles() {
        return roles;
    }

    public Collection<String> promptPlayers() {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Role> player : roles.entrySet()) {
            if (player.getValue().prompt()) {
                ids.add(player.getValue().getUser().getId());
            }
        }
        return ids;
    }

    // returns list of IDs to remove from DMListener
    public Collection<String> sendResponse(User user, String response) {
        if (moderator.getId().equals(user.getId())) {
            return applyModeratorResponse(response);
        }
        else if (roles.containsKey(user.getId()) && roles.get(user.getId()).applyResponse(response)) {
            return Arrays.asList(user.getId());
        }
        else if (!roles.containsKey(user.getId())) {
            System.out.println(user + " is not in this session");
            return Arrays.asList(user.getId()); // removes invalid open channel
        }
        else {
            return new ArrayList<>();
        }
    }

    public SessionStatus checkStatus() {
        if (status == SessionStatus.WAITING_FOR_PLAYERS) {
            if (roles.size() < MIN_PLAYER_COUNT) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Cancelling session. You need at least " + MIN_PLAYER_COUNT + " players to play").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("Cancelling session. Not enough players wanted to play").queue();
                    });
                }
                status = SessionStatus.EXPIRED;
            }
            else if (roles.values().stream().allMatch((role) -> (role.getStatus() == RoleStatus.READY))) {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("All players are ready! The game will begin shortly").queue();
                });
                for (Role role : roles.values()) {
                    role.getUser().openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("All players are ready! The game will begin shortly").queue();
                    });
                }
                status = SessionStatus.STARTED;
            }
            else {
                status = SessionStatus.WAITING_FOR_PLAYERS;
            }
        }
        return status;
    }

    private Collection<String> applyModeratorResponse(String response) {
        if (status == SessionStatus.WAITING_FOR_PLAYERS) {
            if (response.equals("ready")) {
                long readyPlayers = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.READY)).count();
                if (readyPlayers >= MIN_PLAYER_COUNT) {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("Removing inactive players...").queue();
                    });
                    List<String> idsToRemove = roles.values().stream().filter((role) -> (role.getStatus() == RoleStatus.NOT_READY)).map((role) -> (role.getUser().getId())).collect(Collectors.toList());
                    for (String id : idsToRemove) {
                        if (roles.containsKey(id)) {
                            roles.get(id).getUser().openPrivateChannel().queue((channel) -> {
                                channel.sendMessage("You were removed for inactivity");
                            });
                            roles.remove(id);
                        }
                    }
                    idsToRemove.add(moderator.getId());
                    return idsToRemove;
                } else {
                    moderator.openPrivateChannel().queue((channel) -> {
                        channel.sendMessage("You need at least " + MIN_PLAYER_COUNT + " to begin").queue();
                    });
                    return new ArrayList<>();
                }
            }
            else {
                moderator.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("Please respond with 'ready' if you want to begin").queue();
                });
                return new ArrayList<>();
            }
        }
        else {
            return new ArrayList<>();
        }
    }
}
