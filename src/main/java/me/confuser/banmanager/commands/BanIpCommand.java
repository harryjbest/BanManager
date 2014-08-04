package me.confuser.banmanager.commands;

import java.sql.SQLException;
import me.confuser.banmanager.BanManager;
import me.confuser.banmanager.data.IpBanData;
import me.confuser.banmanager.data.PlayerData;
import me.confuser.banmanager.util.IPUtils;
import me.confuser.bukkitutil.Message;
import me.confuser.bukkitutil.commands.BukkitCommand;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.net.InetAddresses;

public class BanIpCommand extends BukkitCommand<BanManager> {

	public BanIpCommand() {
		super("banip");
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String commandName, String[] args) {
		if (args.length < 2)
			return false;
		
		// TODO Player names
		
		// Check if ip
		final String ipStr = args[0];
		
		if (!InetAddresses.isInetAddress(ipStr)) {
			Message message = Message.get("invalidIp");
			message.set("ip", ipStr);

			sender.sendMessage(message.toString());
			return true;
		}
		
		final long ip = IPUtils.toLong(ipStr);
		
		if (plugin.getIpBanStorage().isBanned(ip)) {
			Message message = Message.get("ipAlreadyBanned");
			message.set("ip", ipStr);

			sender.sendMessage(message.toString());
			return true;
		}
		
		final String reason = StringUtils.join(args, " ", 1, args.length - 1);
		
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				final PlayerData actor;
				
				if (sender instanceof Player) {
					actor = plugin.getPlayerStorage().getOnline((Player) sender);
				} else {
					actor = plugin.getPlayerStorage().getConsole();
				}
				
				final IpBanData ban = new IpBanData(ip, actor, reason);
				boolean created = false;
				
				try {
					created = plugin.getIpBanStorage().ban(ban);
				} catch (SQLException e) {
					sender.sendMessage(Message.get("errorOccurred").toString());
					e.printStackTrace();
					return;
				}
				
				if (!created)
					return;
				
				// Find online players
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					public void run() {
						Message kickMessage = Message.get("ipBanKick")
							.set("reason", ban.getReason())
							.set("actor", actor.getName());

						for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
							if (IPUtils.toLong(onlinePlayer.getAddress().getAddress()) == ip) {
								onlinePlayer.kickPlayer(kickMessage.toString());
							}
						}
					}
				});
				
				Message message = Message.get("ipBanned");
				message
					.set("ip", ipStr)
					.set("actor", actor.getName())
					.set("reason", ban.getReason());
				
				plugin.getServer().broadcast(message.toString(), "bm.notify.ipban");
			}
			
		});
		
		return true;
	}

}