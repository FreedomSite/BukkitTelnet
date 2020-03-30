package me.totalfreedom.bukkittelnet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.totalfreedom.bukkittelnet.api.TelnetRequestDataTagsEvent;
import me.totalfreedom.bukkittelnet.api.TelnetRequestUsageEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.earth2me.essentials.Essentials;

public class PlayerEventListener implements Listener
{

    private static Essentials essentials = null;

    private final BukkitTelnet plugin;

    public PlayerEventListener(BukkitTelnet plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        triggerPlayerListUpdates();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        triggerPlayerListUpdates();
    }

    public static Essentials getEssentials()
    {
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess instanceof Essentials)
        {
            essentials = (Essentials)ess;
            return essentials;
        }
        return null;

    }

    private static BukkitTask updateTask = null;

    public void triggerPlayerListUpdates()
    {
        if (updateTask != null)
        {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                final SocketListener socketListener = plugin.telnet.getSocketListener();
                if (socketListener != null)
                {
                    final TelnetRequestDataTagsEvent event = new TelnetRequestDataTagsEvent();
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    socketListener.triggerPlayerListUpdates(generatePlayerList(event.getDataTags()));
                }
            }
        }.runTaskLater(plugin, 20L * 2L);
    }

    @SuppressWarnings("unchecked")
    private static String generatePlayerList(final Map<Player, Map<String, Object>> dataTags)
    {
        final JSONArray players = new JSONArray();

        final Iterator<Map.Entry<Player, Map<String, Object>>> dataTagsIt = dataTags.entrySet().iterator();
        while (dataTagsIt.hasNext())
        {
            final HashMap<String, String> info = new HashMap<>();

            final Map.Entry<Player, Map<String, Object>> dataTagsEntry = dataTagsIt.next();
            final Player player = dataTagsEntry.getKey();
            final Map<String, Object> playerTags = dataTagsEntry.getValue();

            info.put("name", player.getName());
            info.put("ip", player.getAddress().getAddress().getHostAddress());
            info.put("displayName", StringUtils.trimToEmpty(player.getDisplayName()));
            info.put("uuid", player.getUniqueId().toString());

            final Iterator<Map.Entry<String, Object>> playerTagsIt = playerTags.entrySet().iterator();
            while (playerTagsIt.hasNext())
            {
                final Map.Entry<String, Object> playerTagsEntry = playerTagsIt.next();
                final Object value = playerTagsEntry.getValue();
                info.put(playerTagsEntry.getKey(), value != null ? value.toString() : "null");
            }

            players.add(info);
        }

        final JSONObject response = new JSONObject();
        response.put("players", players);

        return response.toJSONString();
    }

    private static BukkitTask usageUpdateTask = null;

    // Just putting this stuff here
    public void triggerUsageUpdates()
    {
        if (usageUpdateTask != null)
        {
            return;
        }

        usageUpdateTask= new BukkitRunnable()
        {
            @Override
            public void run()
            {
                final SocketListener socketListener = plugin.telnet.getSocketListener();
                if (socketListener != null)
                {
                    final TelnetRequestUsageEvent event = new TelnetRequestUsageEvent();
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    socketListener.triggerDataUsageUpdates(generateUsageStats());
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // every 5 seconds
    }


    @SuppressWarnings("unchecked")
    private static String generateUsageStats()
    {
        if (essentials == null)
        {
            essentials = getEssentials();
        }

        final HashMap<String, String> info = new HashMap<>();

        String cpuUsage = null;
        String ramUsage = null;
        String tps = null;

        if (essentials != null)
        {
            tps = String.valueOf(String.valueOf(new BigDecimal(essentials.getTimer().getAverageTPS()).setScale(1, RoundingMode.CEILING)));
        }

        info.put("tps", tps);

        final JSONObject data = new JSONObject();
        data.putAll(info);

        return data.toJSONString();
    }
}
