package com.alessiodp.parties.api.events.bukkit.player;

import com.alessiodp.parties.api.enums.LeaveCause;
import com.alessiodp.parties.api.events.bukkit.BukkitPartiesEvent;
import com.alessiodp.parties.api.events.common.player.IPlayerPreLeaveEvent;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitPartiesPlayerPreLeaveEvent extends BukkitPartiesEvent implements IPlayerPreLeaveEvent, Cancellable {
	private boolean cancelled;
	private final PartyPlayer player;
	private final Party party;
	private final LeaveCause cause;
	private final PartyPlayer kicker;
	
	public BukkitPartiesPlayerPreLeaveEvent(PartyPlayer player, Party party, LeaveCause cause, PartyPlayer kicker) {
		super(false);
		this.player = player;
		this.party = party;
		this.cause = cause;
		this.kicker = kicker;
	}
	
	@NotNull
	@Override
	public PartyPlayer getPartyPlayer() {
		return player;
	}
	
	@NotNull
	@Override
	public Party getParty() {
		return party;
	}
	
	@NotNull
	@Override
	public LeaveCause getCause() {
		return cause;
	}
	
	@Nullable
	@Override
	public PartyPlayer getKicker() {
		return kicker;
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
