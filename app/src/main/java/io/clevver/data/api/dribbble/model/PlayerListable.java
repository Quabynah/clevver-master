/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble.model;

import java.util.Date;

/**
 * An interface for model items that can be displayed as a list of players.
 */
public interface PlayerListable {

    User getPlayer();
    long getId();
    Date getDateCreated();

}
