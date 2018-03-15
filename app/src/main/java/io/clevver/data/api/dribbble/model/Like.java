/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble.model;

import android.support.annotation.Nullable;

import java.util.Date;

/**
 * Models a like of a Dribbble shot.
 */
public class Like implements PlayerListable {
	
	public final long id;
	public final Date created_at;
	@Nullable
	public final User user; // some calls do not populate the user field
	@Nullable
	public final Shot shot; // some calls do not populate the shot field
	
	public Like(long id, Date created_at, User user, Shot shot) {
		this.id = id;
		this.created_at = created_at;
		this.user = user;
		this.shot = shot;
	}
	
	@Override
	public User getPlayer() {
		return user;
	}
	
	@Override
	public long getId() {
		return id;
	}
	
	@Override
	public Date getDateCreated() {
		return created_at;
	}
}
