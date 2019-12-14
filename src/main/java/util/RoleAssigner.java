package util;

import model.Role;
import model.WerewolfSession;
import net.dv8tion.jda.api.entities.User;
import roles.*;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RoleAssigner {

    private static double WEREWOLF_PERCENT = 0.25;

    public static void assignRoles(WerewolfSession session) throws Exception {
        try {
            Set<String> remainingIds = new HashSet<>(session.getRoles().keySet());

            // werewolves
            int numWerewolves = (int) (remainingIds.size() * WEREWOLF_PERCENT);
            Set<String> werewolves = new HashSet<>();
            for (int i = 0; i < numWerewolves; ++i) {
                String werewolfId = randomSelect(remainingIds);
                werewolves.add(werewolfId);
                remainingIds.remove(werewolfId);
            }

            // TODO: special werewolf roles here
            // random select X special roles where x/werewolves = SPECIAL_RATIO
            for (String id : werewolves) {
//            assignRole(id, "Werewolf", session);
                User user = session.getRoles().get(id).getUser();
                session.getRoles().put(id, new Werewolf(user.getName(), user, session));
            }

            // villagers
            // select a seer, doctor
            // random select X special roles where x/villagers = 1
            String seerId = randomSelect(remainingIds);
            User seerUser = session.getRoles().get(seerId).getUser();
            session.getRoles().put(seerId, new Seer(seerUser.getName(), seerUser, session));
//        assignRole(seerId, "Seer", session);
            remainingIds.remove(seerId);

            String doctorId = randomSelect(remainingIds);
            User doctorUser = session.getRoles().get(doctorId).getUser();
            session.getRoles().put(doctorId, new Doctor(doctorUser.getName(), doctorUser, session));
//        assignRole(doctorId, "Doctor", session);
            remainingIds.remove(doctorId);

            String miserId = randomSelect(remainingIds);
            User miserUser = session.getRoles().get(miserId).getUser();
            session.getRoles().put(miserId, new Miser(miserUser.getName(), miserUser, session));
//        assignRole(miserId, "Doctor", session);
            remainingIds.remove(miserId);

            String lycanId = randomSelect(remainingIds);
            User lycanUser = session.getRoles().get(lycanId).getUser();
            session.getRoles().put(lycanId, new Lycan(lycanUser.getName(), lycanUser, session));
//        assignRole(miserId, "Doctor", session);
            remainingIds.remove(lycanId);

            // basic villagers
            for (String id : remainingIds) {
//            assignRole(id, "Villager", session);
                User user = session.getRoles().get(id).getUser();
                session.getRoles().put(id, new Villager(user.getName(), user, session));
            }
        }
        catch (Exception e) {
            System.out.println("not enough players");
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

    private static void assignRole(String id, String roleName, WerewolfSession session) throws Exception {
        Class<?> roleClass = Class.forName("roles." + roleName);
        Constructor<?> constructor = roleClass.getConstructor(String.class, User.class, WerewolfSession.class);
        User seerUser = session.getRoles().get(id).getUser();
        session.getRoles().put(id, new Seer(seerUser.getName(), seerUser, session));
        session.getRoles().put(id, (Role) constructor.newInstance(seerUser.getName(), seerUser, session));
    }
}
