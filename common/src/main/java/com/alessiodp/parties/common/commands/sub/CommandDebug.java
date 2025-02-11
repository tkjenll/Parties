package com.alessiodp.parties.common.commands.sub;

import com.alessiodp.core.common.ADPPlugin;
import com.alessiodp.core.common.commands.utils.ADPMainCommand;
import com.alessiodp.core.common.commands.utils.CommandData;
import com.alessiodp.core.common.user.User;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.addons.external.LLAPIHandler;
import com.alessiodp.parties.common.commands.list.CommonCommands;
import com.alessiodp.parties.common.commands.utils.PartiesCommandData;
import com.alessiodp.parties.common.commands.utils.PartiesSubCommand;
import com.alessiodp.parties.common.configuration.PartiesConfigurationManager;
import com.alessiodp.parties.common.configuration.data.ConfigMain;
import com.alessiodp.parties.common.configuration.data.ConfigParties;
import com.alessiodp.parties.common.configuration.data.Messages;
import com.alessiodp.parties.common.parties.ExpManager;
import com.alessiodp.parties.common.parties.objects.PartyImpl;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;
import com.alessiodp.parties.common.players.objects.PartyRankImpl;
import com.alessiodp.parties.common.utils.PartiesPermission;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandDebug extends PartiesSubCommand {
	private final String syntaxConfig;
	private final String syntaxExp;
	private final String syntaxParty;
	private final String syntaxPlayer;
	
	public CommandDebug(ADPPlugin plugin, ADPMainCommand mainCommand) {
		super(
				plugin,
				mainCommand,
				CommonCommands.DEBUG,
				PartiesPermission.ADMIN_DEBUG,
				ConfigMain.COMMANDS_SUB_DEBUG,
				true
		);
		
		syntax = String.format("%s <%s/%s/%s/%s> ...",
				baseSyntax(),
				ConfigMain.COMMANDS_MISC_CONFIG,
				ConfigMain.COMMANDS_MISC_EXP,
				ConfigMain.COMMANDS_MISC_PARTY,
				ConfigMain.COMMANDS_MISC_PLAYER
		);
		syntaxConfig = String.format("%s %s",
				baseSyntax(),
				ConfigMain.COMMANDS_MISC_CONFIG
		);
		syntaxExp = String.format("%s %s",
				baseSyntax(),
				ConfigMain.COMMANDS_MISC_EXP
		);
		syntaxParty = String.format("%s %s <%s>",
				baseSyntax(),
				ConfigMain.COMMANDS_MISC_PARTY,
				Messages.PARTIES_SYNTAX_PARTY
		);
		syntaxPlayer = String.format("%s %s [%s]",
				baseSyntax(),
				ConfigMain.COMMANDS_MISC_PLAYER,
				Messages.PARTIES_SYNTAX_PLAYER
		);
		
		description = Messages.HELP_ADDITIONAL_DESCRIPTIONS_DEBUG;
		help = Messages.HELP_ADDITIONAL_COMMANDS_DEBUG;
	}

	@Override
	public boolean preRequisites(CommandData commandData) {
		return handlePreRequisitesFull(commandData, null, 2, Integer.MAX_VALUE);
	}
	
	@Override
	public void onCommand(CommandData commandData) {
		User sender = commandData.getSender();
		PartyPlayerImpl partyPlayer = ((PartiesCommandData) commandData).getPartyPlayer();
		
		// Command handling
		String playerName;
		PartyImpl targetParty = null;
		PartyPlayerImpl targetPlayer = null;
		CommandType commandType;
		if (commandData.getArgs()[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_CONFIG)) {
			commandType = CommandType.CONFIG;
			if (commandData.getArgs().length != 2) {
				sendMessage(sender, partyPlayer, Messages.PARTIES_SYNTAX_WRONG_MESSAGE
						.replace("%syntax%", syntaxConfig));
				return;
			}
		} else if (commandData.getArgs()[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_EXP)) {
			commandType = CommandType.EXP;
			if (commandData.getArgs().length != 2) {
				sendMessage(sender, partyPlayer, Messages.PARTIES_SYNTAX_WRONG_MESSAGE
						.replace("%syntax%", syntaxExp));
				return;
			}
		} else if (commandData.getArgs()[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_PARTY)) {
			commandType = CommandType.PARTY;
			if (commandData.getArgs().length == 3) {
				targetParty = ((PartiesPlugin) plugin).getPartyManager().getParty(commandData.getArgs()[2]);
				
				if (targetParty == null) {
					sendMessage(sender, partyPlayer, Messages.PARTIES_COMMON_PARTYNOTFOUND
							.replace("%party%", commandData.getArgs()[2]));
					return;
				}
			} else {
				sendMessage(sender, partyPlayer, Messages.PARTIES_SYNTAX_WRONG_MESSAGE
						.replace("%syntax%", syntaxParty));
				return;
			}
		} else if (commandData.getArgs()[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_PLAYER)) {
			commandType = CommandType.PLAYER;
			if (commandData.getArgs().length == 2) {
				targetPlayer = partyPlayer;
			} else if (commandData.getArgs().length == 3) {
				playerName = commandData.getArgs()[2];
				
				User targetUser = plugin.getPlayerByName(playerName);
				if (targetUser != null) {
					targetPlayer = ((PartiesPlugin) plugin).getPlayerManager().getPlayer(targetUser.getUUID());
				} else {
					Set<UUID> targetPlayersUuid = LLAPIHandler.getPlayerByName(playerName);
					if (targetPlayersUuid.size() > 0) {
						targetPlayer = ((PartiesPlugin) plugin).getPlayerManager().getPlayer(targetPlayersUuid.iterator().next());
					} else {
						// Not found
						sendMessage(sender, partyPlayer, Messages.ADDCMD_DEBUG_PLAYER_PLAYER_OFFLINE
								.replace("%player%", playerName));
						return;
					}
				}
			} else {
				sendMessage(sender, partyPlayer, Messages.PARTIES_SYNTAX_WRONG_MESSAGE
						.replace("%syntax%", syntaxPlayer));
				return;
			}
		} else {
			sendMessage(sender, partyPlayer, Messages.PARTIES_SYNTAX_WRONG_MESSAGE
					.replace("%syntax%", syntax));
			return;
		}
		
		// Command starts
		if (commandType == CommandType.CONFIG) {
			// Config
			sendMessage(sender, partyPlayer, Messages.ADDCMD_DEBUG_CONFIG_HEADER);
			
			StringBuilder ranks = new StringBuilder();
			for (PartyRankImpl rank : ConfigParties.RANK_LIST) {
				if (ranks.length() > 0)
					ranks.append(Messages.ADDCMD_DEBUG_CONFIG_RANK_SEPARATOR);
				ranks.append(rank.parseWithPlaceholders((PartiesPlugin) plugin, Messages.ADDCMD_DEBUG_CONFIG_RANK_FORMAT));
			}
			
			for (String line : Messages.ADDCMD_DEBUG_CONFIG_TEXT) {
				sendMessage(sender, partyPlayer, line
						.replace("%outdated_config%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(((PartiesConfigurationManager) plugin.getConfigurationManager()).getConfigMain().isOutdated()))
						.replace("%outdated_parties%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(((PartiesConfigurationManager) plugin.getConfigurationManager()).getConfigParties().isOutdated()))
						.replace("%outdated_messages%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(((PartiesConfigurationManager) plugin.getConfigurationManager()).getMessages().isOutdated()))
						.replace("%storage%", plugin.getDatabaseManager().getDatabaseType().toString())
						.replace("%ranks%", ranks.toString())
				);
			}
		} else if (commandType == CommandType.EXP) {
			// Config
			sendMessage(sender, partyPlayer, Messages.ADDCMD_DEBUG_EXP_HEADER);
			
			for (String line : Messages.ADDCMD_DEBUG_EXP_TEXT) {
				sendMessage(sender, partyPlayer, parseDebugExp(line));
			}
		} else if (commandType == CommandType.PARTY) {
			// Party
			sendMessage(sender, partyPlayer, Messages.ADDCMD_DEBUG_PARTY_HEADER);
			
			for (String line : Messages.ADDCMD_DEBUG_PARTY_TEXT) {
				sendMessage(sender, partyPlayer, line
						.replace("%id%", targetParty.getId().toString())
						.replace("%name%", ((PartiesPlugin) plugin).getMessageUtils().formatText(targetParty.getName()))
						.replace("%tag%", ((PartiesPlugin) plugin).getMessageUtils().formatText(targetParty.getTag()))
						.replace("%leader%", targetParty.getLeader() != null ? targetParty.getLeader().toString() : Messages.PARTIES_OPTIONS_NONE)
						.replace("%members%", Integer.toString(targetParty.getMembers().size()))
						.replace("%members_online%", Integer.toString(targetParty.getOnlineMembers(true).size()))
						.replace("%description%", ((PartiesPlugin) plugin).getMessageUtils().formatText(targetParty.getDescription()))
						.replace("%motd_size%", Integer.toString(targetParty.getMotd() != null ? targetParty.getMotd().length() : 0))
						.replace("%homes%", Integer.toString(targetParty.getHomes().size()))
						.replace("%kills%", Integer.toString(targetParty.getKills()))
						.replace("%password%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetParty.getPassword() != null))
						.replace("%protection%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetParty.getProtection()))
						.replace("%follow%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetParty.isFollowEnabled()))
						.replace("%color%", (targetParty.getColor() != null ? targetParty.getColor().getName() : Messages.PARTIES_OPTIONS_NONE))
						.replace("%color_active%", (targetParty.getCurrentColor() != null ? targetParty.getCurrentColor().getName() : Messages.PARTIES_OPTIONS_NONE))
						.replace("%color_dynamic%", (targetParty.getDynamicColor() != null ? targetParty.getDynamicColor().getName() : Messages.PARTIES_OPTIONS_NONE))
						.replace("%experience%", Integer.toString((int) targetParty.getExperience()))
				);
			}
		} else {
			// Player
			User targetUser = plugin.getPlayer(targetPlayer.getPlayerUUID());
			
			sendMessage(sender, partyPlayer, Messages.ADDCMD_DEBUG_PLAYER_HEADER);
			
			for (String line : Messages.ADDCMD_DEBUG_PLAYER_TEXT) {
				sendMessage(sender, partyPlayer, line
						.replace("%uuid%", targetPlayer.getPlayerUUID().toString())
						.replace("%name%", targetPlayer.getName())
						.replace("%rank%", Integer.toString(targetPlayer.getRank()))
						.replace("%party%", targetPlayer.getPartyId() != null ? targetPlayer.getPartyId().toString() : Messages.PARTIES_OPTIONS_NONE)
						.replace("%chat%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetPlayer.isChatParty()))
						.replace("%spy%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetPlayer.isSpy()))
						.replace("%muted%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetPlayer.isMuted()))
						.replace("%protection_bypass%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(targetUser.hasPermission(PartiesPermission.ADMIN_PROTECTION_BYPASS)))
				);
			}
		}
	}
	
	@Override
	public List<String> onTabComplete(@NonNull User sender, String[] args) {
		List<String> ret = new ArrayList<>();
		if (sender.hasPermission(permission)) {
			if (args.length == 2) {
				ret.add(ConfigMain.COMMANDS_MISC_CONFIG);
				ret.add(ConfigMain.COMMANDS_MISC_EXP);
				ret.add(ConfigMain.COMMANDS_MISC_PARTY);
				ret.add(ConfigMain.COMMANDS_MISC_PLAYER);
			} else if(args.length == 3) {
				if (args[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_PARTY)) {
					((PartiesPlugin) plugin).getPartyManager().getCacheParties().values().stream()
							.filter(p -> p.getName() != null && !p.getName().isEmpty())
							.forEach(p -> ret.add(p.getName()));
				} else if (args[1].equalsIgnoreCase(ConfigMain.COMMANDS_MISC_PLAYER)) {
					return plugin.getCommandManager().getCommandUtils().tabCompletePlayerList(args, 2);
				}
			}
		}
		return plugin.getCommandManager().getCommandUtils().tabCompleteParser(ret, args[args.length - 1]);
	}
	
	protected String parseDebugExp(String line) {
		String newLine = line;
		if (newLine.contains("%levels_options%")) {
			if (((PartiesPlugin) plugin).getExpManager().getMode() == ExpManager.ExpMode.PROGRESSIVE) {
				newLine = newLine.replace("%levels_options%", Messages.ADDCMD_DEBUG_EXP_LEVEL_OPTIONS_PROGRESSIVE
						.replace("%start%", Integer.toString((int) ConfigMain.ADDITIONAL_EXP_LEVELS_PROGRESSIVE_START))
						.replace("%formula%", ((PartiesPlugin) plugin).getMessageUtils().formatText(ConfigMain.ADDITIONAL_EXP_LEVELS_PROGRESSIVE_LEVEL_EXP)
						));
			} else {
				newLine = newLine.replace("%levels_options%", Messages.ADDCMD_DEBUG_EXP_LEVEL_OPTIONS_FIXED
						.replace("%repeat%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(ConfigMain.ADDITIONAL_EXP_LEVELS_FIXED_REPEAT))
						.replace("%levels%", Integer.toString(ConfigMain.ADDITIONAL_EXP_LEVELS_FIXED_LIST.size())
						));
			}
		}
		return newLine
				.replace("%exp%", ((PartiesPlugin) plugin).getMessageUtils().formatOnOff(ConfigMain.ADDITIONAL_EXP_ENABLE))
				.replace("%levels%", ((PartiesPlugin) plugin).getMessageUtils().formatYesNo(ConfigMain.ADDITIONAL_EXP_LEVELS_ENABLE))
				.replace("%levels_mode%", ConfigMain.ADDITIONAL_EXP_LEVELS_MODE);
	}
	
	private enum CommandType {
		CONFIG, EXP, PARTY, PLAYER
	}
}
