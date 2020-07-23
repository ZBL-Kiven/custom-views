package com.zj.views.list.holders.anim;

import android.view.animation.Interpolator;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.zj.views.list.holders.BaseItemAnimator;

@SuppressWarnings("unused")
public class FadeInAnimator extends BaseItemAnimator {

    public FadeInAnimator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    protected void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
        ViewCompat.animate(holder.itemView).alpha(0).setDuration(getRemoveDuration()).setInterpolator(mInterpolator).setListener(new DefaultRemoveVpaListener(holder)).setStartDelay(getRemoveDelay(holder)).start();
    }

    @Override
    protected void preAnimateAddImpl(RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha( 0);
    }

    @Override
    protected void animateAddImpl(final RecyclerView.ViewHolder holder) {
        ViewCompat.animate(holder.itemView).alpha(1).setDuration(getAddDuration()).setInterpolator(mInterpolator).setListener(new DefaultAddVpaListener(holder)).setStartDelay(getAddDelay(holder)).start();
    }
}
