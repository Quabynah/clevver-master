/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.producthunt;

import java.util.List;

import io.clevver.data.PlaidItemSorting;
import io.clevver.data.api.producthunt.model.Post;

/**
 * Utility class for applying weights to a group of {@link Post}s for sorting. Weighs posts relative
 * to the most upvoted & commented hunts in the group.
 */
public class PostWeigher implements PlaidItemSorting.PlaidItemGroupWeigher<Post> {

    @Override
    public void weigh(List<Post> posts) {
        float maxVotes = 0f;
        float maxComments = 0f;
        for (Post post : posts) {
            maxVotes = Math.max(maxVotes, post.votes_count);
            maxComments = Math.max(maxComments, post.comments_count);
        }
        for (Post post : posts) {
            float weight = 1f - ((((float) post.comments_count) / maxComments) +
                    ((float) post.votes_count / maxVotes)) / 2f;
            post.weight = post.page + weight;
        }
    }

}
