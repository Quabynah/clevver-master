/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Models a a follow of a dribbble user
 */
public class Follow implements PlayerListable {

    public final long id;
    public final Date created_at;
    @SerializedName("follower")
    public final User user;

    public Follow(long id, Date created_at, User user) {
        this.id = id;
        this.created_at = created_at;
        this.user = user;
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
