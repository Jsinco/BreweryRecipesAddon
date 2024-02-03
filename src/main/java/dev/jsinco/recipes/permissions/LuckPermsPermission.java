package dev.jsinco.recipes.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsPermission implements PermissionManager {
    @Override
    public void setPermission(String permission, Player player, boolean value) {
        Node node = Node.builder(permission).value(value).build();
        User user = getLuckPermsInstance().getUserManager().getUser(player.getUniqueId());
        assert user != null;
        user.data().add(node);
        getLuckPermsInstance().getUserManager().saveUser(user);
    }

    @Override
    public void removePermission(String permission, Player player) {
        User user = getLuckPermsInstance().getUserManager().getUser(player.getUniqueId());
        assert user != null;
        user.data().remove(Node.builder(permission).build());
        getLuckPermsInstance().getUserManager().saveUser(user);
    }

    public LuckPerms getLuckPermsInstance() {
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (rsp == null) {
            throw new IllegalStateException("LuckPerms is not installed on the server!");
        }
        return rsp.getProvider();
    }
}
