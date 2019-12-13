package util;

import model.Role;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;
import roles.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RoleAssigner {

    private static double WEREWOLF_PERCENT = 0.25;

    public static void assignRoles(WerewolfSession session) {
        Set<String> remainingdIds = new HashSet<>(session.getRoles().keySet());

        // werewolves
        int numWerewolves = (int)(remainingdIds.size() * WEREWOLF_PERCENT);
        Set<String> werewolves = new HashSet<>();
        for (int i = 0; i < numWerewolves; ++i) {
            String werewolfId = randomSelect(remainingdIds);
            werewolves.add(werewolfId);
            remainingdIds.remove(werewolfId);
        }
        // TODO: special werewolf roles here
        // random select X special roles where x/werewolves = SPECIAL_RATIO
        for (String id : werewolves) {
            User user = session.getRoles().get(id).getUser();
            session.getRoles().put(id, new Werewolf(user.getName(), user, session));
        }

        // villagers
        // select a seer, doctor
        // random select X special roles where x/villagers = 1
        String seerId = randomSelect(remainingdIds);
        User seerUser = session.getRoles().get(seerId).getUser();
        session.getRoles().put(seerId, new Seer(seerUser.getName(), seerUser, session));
        remainingdIds.remove(seerId);

        String doctorId = randomSelect(remainingdIds);
        User doctorUser = session.getRoles().get(doctorId).getUser();
        session.getRoles().put(doctorId, new Doctor(doctorUser.getName(), doctorUser, session));
        remainingdIds.remove(doctorId);

        // basic villagers
        for (String id : remainingdIds) {
            User user = session.getRoles().get(id).getUser();
            session.getRoles().put(id, new Villager(user.getName(), user, session));
        }
    }

    private static String randomSelect(Collection<String> collection) {
        int index = new Random().nextInt(collection.size());
        int i = 0;
        for (String item : collection) {
            if (i == index) {
                return item;
            }
            ++i;
        }
        return null;
    }
}
