package com.oskarsmc.tablist.module;

import com.oskarsmc.tablist.TabList;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GlobalTabList {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final TabList plugin;

    public GlobalTabList(TabList plugin, ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.plugin = plugin;
    }

    @Subscribe
    public void connect(ServerPostConnectEvent event) {
        proxyServer.getScheduler()
                .buildTask(plugin, this::update)
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Subscribe
    public void disconnect(DisconnectEvent event) {
        proxyServer.getScheduler()
                .buildTask(plugin, this::update)
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void update() {
        for (Player player : this.proxyServer.getAllPlayers()) {
            for (Player player1 : this.proxyServer.getAllPlayers()) {
                logger.info("player: {}, player1: {}", player, player1);
                var tabList = player.getTabList();
                var existing = tabList.getEntries().stream()
                        .filter(ent -> Objects.equals(ent.getProfile().getId(), player1.getUniqueId()))
                        .collect(Collectors.toUnmodifiableList());
                var prefixString = player1.getCurrentServer()
                        .map(ServerConnection::getServerInfo)
                        .map(ServerInfo::getName)
                        .map(s -> "[" + s + "] ")
                        .orElse("");
                var nameWithServerComponent = Component.text(prefixString).color(NamedTextColor.GREEN)
                        .append(Component.text(player1.getUsername()).color(NamedTextColor.WHITE));
                if (existing.isEmpty()) {
                    logger.info("add entry in player {}'s list: {}", player.getUsername(), player1.getUsername());
                    tabList.addEntry(
                            TabListEntry.builder()
                                    .displayName(nameWithServerComponent)
                                    .profile(player1.getGameProfile())
                                    .tabList(tabList)
                                    .build()
                    );
                } else {
                    var entry = existing.get(0);
                    logger.info("update player {}'s entry: {}", player.getUsername(), entry);
                    entry.setDisplayName(nameWithServerComponent);
                    tabList.removeEntry(entry.getProfile().getId());
                    tabList.addEntry(entry);
                }
            }
        }
    }
}
