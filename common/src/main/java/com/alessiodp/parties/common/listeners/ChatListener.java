package com.alessiodp.parties.common.listeners;

import com.alessiodp.parties.api.events.common.player.IChatEvent;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.configuration.Constants;
import com.alessiodp.parties.common.configuration.data.ConfigMain;
import com.alessiodp.parties.common.configuration.data.ConfigParties;
import com.alessiodp.parties.common.configuration.data.Messages;
import com.alessiodp.parties.common.logging.LogLevel;
import com.alessiodp.parties.common.logging.LoggerManager;
import com.alessiodp.parties.common.parties.objects.PartyImpl;
import com.alessiodp.parties.common.players.PartiesPermission;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;
import com.alessiodp.parties.common.tasks.ChatTask;
import com.alessiodp.parties.common.user.User;

public abstract class ChatListener {
	private PartiesPlugin plugin;
	
	protected ChatListener(PartiesPlugin instance) {
		plugin = instance;
	}
	
	/**
	 * Used by Bukkit, Bungeecord
	 *
	 * @return Returns if the event is cancelled
	 */
	protected boolean onPlayerChat(User sender, String message) {
		boolean eventCancelled = false;
		PartyPlayerImpl pp = plugin.getPlayerManager().getPlayer(sender.getUUID());
		
		boolean partyChat = false;
		PartyImpl party = pp.getPartyName().isEmpty() ? null : plugin.getPartyManager().getParty(pp.getPartyName());
		if (party != null) {
			if (pp.isChatParty()) {
				partyChat = true;
			} else if (ConfigParties.GENERAL_CHAT_DIRECT_ENABLED && message.startsWith(ConfigParties.GENERAL_CHAT_DIRECT_PREFIX)) {
				partyChat = true;
				message = message.substring(1);
			}
		}
		
		if (partyChat) {
			String finalMessage = message;
			// Make it async
			plugin.getPartiesScheduler().getEventsExecutor().execute(() -> {
				if (plugin.getRankManager().checkPlayerRankAlerter(pp, PartiesPermission.PRIVATE_SENDMESSAGE)) {
					// Chat allowed
					boolean mustWait = false;
					
					if (ConfigParties.GENERAL_CHAT_CHATCD > 0
							&& !plugin.getRankManager().checkPlayerRank(pp, PartiesPermission.PRIVATE_BYPASSCOOLDOWN)) {
						Long unixTimestamp = plugin.getCooldownManager().getChatCooldown().get(pp.getPlayerUUID());
						long unixNow = System.currentTimeMillis() / 1000L;
						// Check cooldown
						if (unixTimestamp != null) {
							pp.sendMessage(Messages.MAINCMD_P_COOLDOWN
									.replace("%seconds%", String.valueOf(ConfigParties.GENERAL_CHAT_CHATCD - (unixNow - unixTimestamp))));
							mustWait = true;
						} else {
							plugin.getCooldownManager().getChatCooldown().put(pp.getPlayerUUID(), unixNow);
							
							plugin.getPartiesScheduler().scheduleTaskLater(new ChatTask(plugin, pp.getPlayerUUID()), ConfigParties.GENERAL_CHAT_CHATCD);
							
							LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_CMD_P_TASK
									.replace("{value}", Integer.toString(ConfigParties.GENERAL_CHAT_CHATCD * 20))
									.replace("{player}", pp.getName()), true);
						}
					}
					
					if (!mustWait) {
						// Calling API event
						IChatEvent partiesChatEvent = plugin.getEventManager().prepareChatEvent(pp, party, finalMessage);
						plugin.getEventManager().callEvent(partiesChatEvent);
						
						String newMessage = partiesChatEvent.getMessage();
						if (!partiesChatEvent.isCancelled()) {
							party.sendChatMessage(pp, newMessage);
							if (ConfigMain.STORAGE_LOG_CHAT)
								LoggerManager.log(LogLevel.BASIC, Constants.DEBUG_CMD_P
										.replace("{party}", party.getName())
										.replace("{player}", pp.getName())
										.replace("{message}", newMessage), true);
						} else
							LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_API_CHATEVENT_DENY
									.replace("{player}", pp.getName())
									.replace("{message}", finalMessage), true);
					}
				} else
					pp.sendNoPermission(PartiesPermission.PRIVATE_SENDMESSAGE);
			});
			eventCancelled = true;
		}
		return eventCancelled;
	}
	
	/**
	 * Used by Bukkit
	 *
	 * @return Returns the new message to set, null if is not changed
	 */
	protected String onPlayerCommandPreprocess(User sender, String message) {
		String ret = null;
		if (ConfigMain.ADDITIONAL_AUTOCMD_ENABLE) {
			if (message.endsWith("\t")) {
				// This is an auto command
				ret = message.replace("\t", "");
			} else {
				// This is a normal command to replicate
				// Make it async
				plugin.getPartiesScheduler().getEventsExecutor().execute(() -> {
					boolean cancel = false;
					
					for (String str : ConfigMain.ADDITIONAL_AUTOCMD_BLACKLIST) {
						if (str.equalsIgnoreCase("*") || message.toLowerCase().startsWith(str.toLowerCase())) {
							cancel = true;
							break;
						}
					}
					for (String str : ConfigMain.ADDITIONAL_AUTOCMD_WHITELIST) {
						if (str.equalsIgnoreCase("*") || message.toLowerCase().startsWith(str.toLowerCase())) {
							cancel = false;
							break;
						}
					}
					
					if (!cancel) {
						PartyPlayerImpl pp = plugin.getPlayerManager().getPlayer(sender.getUUID());
						if (!pp.getPartyName().isEmpty()) {
							if (plugin.getRankManager().checkPlayerRank(pp, PartiesPermission.PRIVATE_AUTOCOMMAND)) {
								PartyImpl party = plugin.getPartyManager().getParty(pp.getPartyName());
								
								for (PartyPlayerImpl pl : party.getOnlinePlayers()) {
									if (!pl.getPlayerUUID().equals(sender.getUUID())) {
										// Make it sync
										plugin.getPartiesScheduler().runSync(() -> {
											plugin.getPlayer(pl.getPlayerUUID()).chat(message + "\t");
											LoggerManager.log(LogLevel.MEDIUM, Constants.DEBUG_AUTOCMD_PERFORM
													.replace("{player}", pl.getName())
													.replace("{command}", message), true);
										});
									}
								}
							}
						}
					}
				});
			}
		}
		return ret;
	}
}
