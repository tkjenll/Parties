package com.alessiodp.parties.api.events.bukkit.party;

import com.alessiodp.parties.api.enums.DeleteCause;
import com.alessiodp.parties.api.events.bukkit.BukkitPartiesEvent;
import com.alessiodp.parties.api.events.common.party.IPartyPreDeleteEvent;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitPartiesPartyPreDeleteEvent extends BukkitPartiesEvent implements IPartyPreDeleteEvent, Cancellable {
	private boolean cancelled;
	private final Party party;
	private final DeleteCause cause;
	private final PartyPlayer kickedPlayer;
	private final PartyPlayer commandSender;
	
	public BukkitPartiesPartyPreDeleteEvent(Party party, DeleteCause cause, PartyPlayer kickedPlayer, PartyPlayer commandSender) {
		super(false);
		this.party = party;
		this.cause = cause;
		this.kickedPlayer = kickedPlayer;
		this.commandSender = commandSender;
	}
	
	@NotNull
	@Override
	public Party getParty() {
		return party;
	}
	
	@NotNull
	@Override
	public DeleteCause getCause() {
		return cause;
	}
	
	@Nullable
	@Override
	public PartyPlayer getKickedPlayer() {
		return kickedPlayer;
	}
	
	@Nullable
	@Override
	public PartyPlayer getCommandSender() {
		return commandSender;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
}
