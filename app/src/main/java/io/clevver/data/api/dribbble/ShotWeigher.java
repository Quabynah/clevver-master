/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble;


import java.util.List;

import io.clevver.data.PlaidItemSorting;
import io.clevver.data.api.dribbble.model.Shot;

/**
 * Utility class for applying weights to a group of {@link Shot}s for sorting. Weighs shots relative
 * to the most liked shot in the group.
 */
public class ShotWeigher implements PlaidItemSorting.PlaidItemGroupWeigher<Shot> {

    @Override
    public void weigh(List<Shot> shots) {
        float maxLikes = 0f;
        for (Shot shot : shots) {
            maxLikes = Math.max(maxLikes, shot.likes_count);
        }
        for (Shot shot : shots) {
            float weight = 1f - ((float) shot.likes_count / maxLikes);
            shot.weight = shot.page + weight;
        }
    }

}
