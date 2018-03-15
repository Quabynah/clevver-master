/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;

import java.util.List;

import io.clevver.ui.recyclerview.SlideInItemAnimator;
import io.clevver.util.AnimUtils;

/**
 * A {@link RecyclerView.ItemAnimator} for running animations specific to our home grid.
 */
public class HomeGridItemAnimator extends SlideInItemAnimator {
	
	// Constant payloads, for use with Adapter#notifyItemChanged
	public static final int PRODUCT_HUNT_RETURN = 3;
	
	// Pending animations
	private FeedAdapter.ProductHuntStoryHolder pendingProductReturn;
	
	// Currently running animations
	private Pair<FeedAdapter.ProductHuntStoryHolder, AnimatorSet> runningProductReturn;
	
	@Override
	public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		return true;
	}
	
	@Override
	public ItemHolderInfo obtainHolderInfo() {
		return new HomeGridItemHolderInfo();
	}
	
	@NonNull
	@Override
	public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
	                                                 @NonNull RecyclerView.ViewHolder viewHolder,
	                                                 int changeFlags,
	                                                 @NonNull List<Object> payloads) {
		ItemHolderInfo info =
				super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
		if (info instanceof HomeGridItemHolderInfo) {
			HomeGridItemHolderInfo elementInfo = (HomeGridItemHolderInfo) info;
			elementInfo.returnFromProduct = payloads.contains(PRODUCT_HUNT_RETURN);
			return elementInfo;
		}
		return info;
	}
	
	@Override
	public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
	                             @NonNull RecyclerView.ViewHolder newHolder,
	                             @NonNull ItemHolderInfo preInfo,
	                             @NonNull ItemHolderInfo postInfo) {
		boolean runPending = super.animateChange(oldHolder, newHolder, preInfo, postInfo);
		
		if (preInfo instanceof HomeGridItemHolderInfo) {
			HomeGridItemHolderInfo info = (HomeGridItemHolderInfo) preInfo;
			if (info.returnFromProduct) {
				pendingProductReturn = (FeedAdapter.ProductHuntStoryHolder) newHolder;
				runPending = true;
			}
		}
		return runPending;
	}
	
	@Override
	public void runPendingAnimations() {
		super.runPendingAnimations();
		if (pendingProductReturn != null) {
			animateProductReturn(pendingProductReturn);
			pendingProductReturn = null;
		}
	}
	
	@Override
	public void endAnimation(RecyclerView.ViewHolder holder) {
		super.endAnimation(holder);
		if (holder == pendingProductReturn) {
			dispatchChangeFinished(pendingProductReturn, false);
			pendingProductReturn = null;
		}
		if (runningProductReturn != null && runningProductReturn.first == holder) {
			runningProductReturn.second.cancel();
		}
	}
	
	@Override
	public void endAnimations() {
		super.endAnimations();
		if (pendingProductReturn != null) {
			dispatchChangeFinished(pendingProductReturn, false);
			pendingProductReturn = null;
		}
		if (runningProductReturn != null) {
			runningProductReturn.second.cancel();
		}
	}
	
	@Override
	public boolean isRunning() {
		return super.isRunning()
				|| (runningProductReturn != null
				&& runningProductReturn.second.isRunning());
	}
	
	private void animateProductReturn(FeedAdapter.ProductHuntStoryHolder holder) {
		endAnimation(holder);
		
		AnimatorSet commentsReturnAnim = new AnimatorSet();
		commentsReturnAnim.playTogether(
				ObjectAnimator.ofFloat(holder.comments, View.ALPHA, 0f, 1f));
		commentsReturnAnim.setDuration(120L);
		commentsReturnAnim.setInterpolator(
				AnimUtils.getLinearOutSlowInInterpolator(holder.itemView.getContext()));
		commentsReturnAnim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				dispatchChangeStarting(holder, false);
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				runningProductReturn = null;
				dispatchChangeFinished(holder, false);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				holder.comments.setAlpha(1f);
				runningProductReturn = null;
				dispatchChangeFinished(holder, false);
			}
		});
		runningProductReturn = Pair.create(holder, commentsReturnAnim);
		commentsReturnAnim.start();
	}
	
	private class HomeGridItemHolderInfo extends ItemHolderInfo {
		boolean animateAddToPocket;
		boolean returnFromComments;
		boolean returnFromProduct;
	}
	
}
