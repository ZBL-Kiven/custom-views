package com.zj.views.list.refresh.layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import com.zj.views.R;
import com.zj.views.list.refresh.layout.api.RefreshComponent;
import com.zj.views.list.refresh.layout.api.RefreshContent;
import com.zj.views.list.refresh.layout.api.RefreshFooter;
import com.zj.views.list.refresh.layout.api.RefreshHeader;
import com.zj.views.list.refresh.layout.api.RefreshKernel;
import com.zj.views.list.refresh.layout.api.RefreshLayoutIn;
import com.zj.views.list.refresh.layout.constant.DimensionStatus;
import com.zj.views.list.refresh.layout.constant.RefreshState;
import com.zj.views.list.refresh.layout.constant.SpinnerStyle;
import com.zj.views.list.refresh.layout.listener.DefaultRefreshFooterCreator;
import com.zj.views.list.refresh.layout.listener.DefaultRefreshHeaderCreator;
import com.zj.views.list.refresh.layout.listener.DefaultRefreshInitializer;
import com.zj.views.list.refresh.layout.listener.OnLoadMoreListener;
import com.zj.views.list.refresh.layout.listener.OnMultiListener;
import com.zj.views.list.refresh.layout.listener.OnRefreshListener;
import com.zj.views.list.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.zj.views.list.refresh.layout.listener.OnStateChangedListener;
import com.zj.views.list.refresh.layout.listener.ScrollBoundaryDecider;
import com.zj.views.list.refresh.layout.simple.SimpleBoundaryDecider;
import com.zj.views.list.refresh.layout.util.SmartUtil;
import com.zj.views.list.refresh.layout.wrapper.RefreshContentWrapper;
import com.zj.views.list.refresh.layout.wrapper.RefreshFooterWrapper;
import com.zj.views.list.refresh.layout.wrapper.RefreshHeaderWrapper;

import static android.view.MotionEvent.obtain;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.zj.views.list.refresh.layout.util.SmartUtil.dp2px;
import static com.zj.views.list.refresh.layout.util.SmartUtil.fling;
import static com.zj.views.list.refresh.layout.util.SmartUtil.isContentView;
import static java.lang.System.currentTimeMillis;

@SuppressWarnings({"unused"})
@SuppressLint("RestrictedApi")
public class RefreshLayout extends ViewGroup implements com.zj.views.list.refresh.layout.api.RefreshLayoutIn, NestedScrollingParent/*, NestedScrollingChild*/ {

    protected int mTouchSlop;
    protected int mSpinner;//The current Spinner greater than 0 means pull down, and less than zero means pull up
    protected int mLastSpinner;//finally spinner
    protected int mTouchSpinner;//Spinner when touched
    protected int mFloorDuration = 300;//Expansion time on the second floor
    protected int mReboundDuration = 300;//Rebound animation duration
    protected int mScreenHeightPixels;//Screen height
    protected float mTouchX;
    protected float mTouchY;
    protected float mLastTouchX;//Used to achieve the left and right drag effect of the Header
    protected float mLastTouchY;//Used to achieve multi-touch
    protected float mDragRate = .5f;
    protected char mDragDirection = 'n';//The direction of the drag, none-n horizontal-h vertical-v
    protected boolean mIsBeingDragged;//Is dragging
    protected boolean mSuperDispatchTouchEvent;//Whether the parent class handles touch events
    protected boolean mEnableDisallowIntercept;//Whether to allow interception of events
    protected int mFixedHeaderViewId = View.NO_ID;//View ID fixed to the head
    protected int mFixedFooterViewId = View.NO_ID;//View ID fixed at the bottom
    protected int mHeaderTranslationViewId = View.NO_ID;//View Id of the drop-down Header offset
    protected int mFooterTranslationViewId = View.NO_ID;//View Id of the drop-down Footer offset

    protected int mMinimumVelocity;
    protected int mMaximumVelocity;
    protected int mCurrentVelocity;
    protected Scroller mScroller;
    protected VelocityTracker mVelocityTracker;
    protected Interpolator mReboundInterpolator;
    protected int[] mPrimaryColors;
    protected boolean mEnableRefresh = true;
    protected boolean mEnableLoadMore = false;
    protected boolean mEnableClipHeaderWhenFixedBehind = true;//Whether to clip and occlude the Header when Header FixedBehind
    protected boolean mEnableClipFooterWhenFixedBehind = true;//When Footer FixedBehind, whether to clip or block Footer
    protected boolean mEnableHeaderTranslationContent = true;//Whether to enable the content view drag effect
    protected boolean mEnableFooterTranslationContent = true;//Whether to enable the content view drag effect
    protected boolean mEnableFooterFollowWhenNoMoreData = false;//Whether Footer follows the content after all loading is over, 1.0.4-6
    protected boolean mEnablePreviewInEditMode = true;//Whether to enable preview function in edit mode
    protected boolean mEnableOverScrollBounce = true;//Whether to enable out-of-bounds rebound
    protected boolean mEnableOverScrollDrag = false;//Whether to enable cross-border dragging (imitating apple effect),1.0.4-6
    protected boolean mEnableAutoLoadMore = true;//Whether to automatically load more when the list scrolls to the bottom
    protected boolean mEnablePureScrollMode = false;//Whether to enable pure scrolling mode
    protected boolean mEnableScrollContentWhenLoaded = true;//Whether to scroll the content to display new data after loading more is complete
    protected boolean mEnableScrollContentWhenRefreshed = true;//Whether to scroll the content to display the new data after the refresh is complete
    protected boolean mEnableLoadMoreWhenContentNotFull = true;//When the content is not full of a page, can I pull up to load more
    protected boolean mEnableNestedScrolling = true;//Whether to enable absconding scroll function
    protected boolean mDisableContentWhenRefresh = false;//Whether to enable the content view to be disabled when refreshing
    protected boolean mDisableContentWhenLoading = false;//Whether to enable the content view to be disabled when refreshing
    protected boolean mFooterNoMoreData = false;//Whether the data has been loaded completely, if it is finished, the loading event cannot be triggered
    protected boolean mFooterNoMoreDataEffective = false;//Whether NoMoreData is effective (some Footer may not support it)

    protected boolean mManualLoadMore = false;//haveYouManuallySetLoadMoreForSmartOpening
    protected boolean mManualHeaderTranslationContent = false;//Whether to manually set the content view drag effect
    protected boolean mManualFooterTranslationContent = false;//Whether to manually set the content view drag effect

    protected OnRefreshListener mRefreshListener;
    protected OnLoadMoreListener mLoadMoreListener;
    protected OnMultiListener mOnMultiListener;
    protected ScrollBoundaryDecider mScrollBoundaryDecider;

    protected int mTotalUnconsumed;
    protected boolean mNestedInProgress;
    protected int[] mParentOffsetInWindow = new int[2];
    protected NestedScrollingChildHelper mNestedChild = new NestedScrollingChildHelper(this);
    protected NestedScrollingParentHelper mNestedParent = new NestedScrollingParentHelper(this);

    protected int mHeaderHeight;        //头部高度 和 头部高度状态
    protected DimensionStatus mHeaderHeightStatus = DimensionStatus.DefaultUnNotify;
    protected int mFooterHeight;        //底部高度 和 底部高度状态
    protected DimensionStatus mFooterHeightStatus = DimensionStatus.DefaultUnNotify;

    protected int mHeaderInsetStart;    // Header 起始位置偏移
    protected int mFooterInsetStart;    // Footer 起始位置偏移

    protected float mHeaderMaxDragRate = 2.5f;  //最大拖动比率(最大高度/Header高度)
    protected float mFooterMaxDragRate = 2.5f;  //最大拖动比率(最大高度/Footer高度)
    protected float mHeaderTriggerRate = 1.0f;  //触发刷新距离 与 HeaderHeight 的比率
    protected float mFooterTriggerRate = 1.0f;  //触发加载距离 与 FooterHeight 的比率

    protected float mTwoLevelBottomPullUpToCloseRate = 1 / 6f;//二级刷新打开时，再底部上划关闭区域所占的比率

    protected RefreshComponent mRefreshHeader;     //下拉头部视图
    protected RefreshComponent mRefreshFooter;     //上拉底部视图
    protected RefreshContent mRefreshContent;   //显示内容视图

    protected Paint mPaint;
    protected Handler mHandler;
    protected RefreshKernel mKernel = new RefreshKernelImpl();
    protected RefreshState mState = RefreshState.None;          //主状态
    protected RefreshState mViceState = RefreshState.None;      //副状态（主状态刷新时候的滚动状态）

    protected long mLastOpenTime = 0;                           //上一次 刷新或者加载 时间

    protected int mHeaderBackgroundColor = 0;                   //为Header绘制纯色背景
    protected int mFooterBackgroundColor = 0;

    protected boolean mHeaderNeedTouchEventWhenRefreshing;      //为游戏Header提供独立事件
    protected boolean mFooterNeedTouchEventWhenLoading;

    protected boolean mAttachedToWindow;                        //是否添加到Window

    protected boolean mFooterLocked = false;//Footer 正在loading 的时候是否锁住 列表不能向上滚动


    protected static DefaultRefreshFooterCreator sFooterCreator = null;
    protected static DefaultRefreshHeaderCreator sHeaderCreator = null;
    protected static DefaultRefreshInitializer sRefreshInitializer = null;
    protected static MarginLayoutParams sDefaultMarginLP = new MarginLayoutParams(-1, -1);

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        ViewConfiguration configuration = ViewConfiguration.get(context);

        mHandler = new Handler(Looper.getMainLooper());
        mScroller = new Scroller(context);
        mVelocityTracker = VelocityTracker.obtain();
        mScreenHeightPixels = context.getResources().getDisplayMetrics().heightPixels;
        mReboundInterpolator = new SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        mFooterHeight = SmartUtil.dp2px(60);
        mHeaderHeight = SmartUtil.dp2px(100);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);

        if (!ta.hasValue(R.styleable.RefreshLayout_android_clipToPadding)) {
            super.setClipToPadding(false);
        }
        if (!ta.hasValue(R.styleable.RefreshLayout_android_clipChildren)) {
            super.setClipChildren(false);
        }

        if (sRefreshInitializer != null) {
            sRefreshInitializer.initialize(context, this);//调用全局初始化
        }

        mDragRate = ta.getFloat(R.styleable.RefreshLayout_rlDragRate, mDragRate);
        mHeaderMaxDragRate = ta.getFloat(R.styleable.RefreshLayout_rlHeaderMaxDragRate, mHeaderMaxDragRate);
        mFooterMaxDragRate = ta.getFloat(R.styleable.RefreshLayout_rlFooterMaxDragRate, mFooterMaxDragRate);
        mHeaderTriggerRate = ta.getFloat(R.styleable.RefreshLayout_rlHeaderTriggerRate, mHeaderTriggerRate);
        mFooterTriggerRate = ta.getFloat(R.styleable.RefreshLayout_rlFooterTriggerRate, mFooterTriggerRate);
        mEnableRefresh = ta.getBoolean(R.styleable.RefreshLayout_rlEnableRefresh, mEnableRefresh);
        mReboundDuration = ta.getInt(R.styleable.RefreshLayout_rlReboundDuration, mReboundDuration);
        mEnableLoadMore = ta.getBoolean(R.styleable.RefreshLayout_rlEnableLoadMore, mEnableLoadMore);
        mHeaderHeight = ta.getDimensionPixelOffset(R.styleable.RefreshLayout_rlHeaderHeight, mHeaderHeight);
        mFooterHeight = ta.getDimensionPixelOffset(R.styleable.RefreshLayout_rlFooterHeight, mFooterHeight);
        mHeaderInsetStart = ta.getDimensionPixelOffset(R.styleable.RefreshLayout_rlHeaderInsetStart, mHeaderInsetStart);
        mFooterInsetStart = ta.getDimensionPixelOffset(R.styleable.RefreshLayout_rlFooterInsetStart, mFooterInsetStart);
        mDisableContentWhenRefresh = ta.getBoolean(R.styleable.RefreshLayout_rlDisableContentWhenRefresh, mDisableContentWhenRefresh);
        mDisableContentWhenLoading = ta.getBoolean(R.styleable.RefreshLayout_rlDisableContentWhenLoading, mDisableContentWhenLoading);
        mEnableHeaderTranslationContent = ta.getBoolean(R.styleable.RefreshLayout_rlEnableHeaderTranslationContent, mEnableHeaderTranslationContent);
        mEnableFooterTranslationContent = ta.getBoolean(R.styleable.RefreshLayout_rlEnableFooterTranslationContent, mEnableFooterTranslationContent);
        mEnablePreviewInEditMode = ta.getBoolean(R.styleable.RefreshLayout_rlEnablePreviewInEditMode, mEnablePreviewInEditMode);
        mEnableAutoLoadMore = ta.getBoolean(R.styleable.RefreshLayout_rlEnableAutoLoadMore, mEnableAutoLoadMore);
        mEnableOverScrollBounce = ta.getBoolean(R.styleable.RefreshLayout_rlEnableOverScrollBounce, mEnableOverScrollBounce);
        mEnablePureScrollMode = ta.getBoolean(R.styleable.RefreshLayout_rlEnablePureScrollMode, mEnablePureScrollMode);
        mEnableScrollContentWhenLoaded = ta.getBoolean(R.styleable.RefreshLayout_rlEnableScrollContentWhenLoaded, mEnableScrollContentWhenLoaded);
        mEnableScrollContentWhenRefreshed = ta.getBoolean(R.styleable.RefreshLayout_rlEnableScrollContentWhenRefreshed, mEnableScrollContentWhenRefreshed);
        mEnableLoadMoreWhenContentNotFull = ta.getBoolean(R.styleable.RefreshLayout_rlEnableLoadMoreWhenContentNotFull, mEnableLoadMoreWhenContentNotFull);
        mEnableFooterFollowWhenNoMoreData = ta.getBoolean(R.styleable.RefreshLayout_rlEnableFooterFollowWhenLoadFinished, mEnableFooterFollowWhenNoMoreData);
        mEnableFooterFollowWhenNoMoreData = ta.getBoolean(R.styleable.RefreshLayout_rlEnableFooterFollowWhenNoMoreData, mEnableFooterFollowWhenNoMoreData);
        mEnableClipHeaderWhenFixedBehind = ta.getBoolean(R.styleable.RefreshLayout_rlEnableClipHeaderWhenFixedBehind, mEnableClipHeaderWhenFixedBehind);
        mEnableClipFooterWhenFixedBehind = ta.getBoolean(R.styleable.RefreshLayout_rlEnableClipFooterWhenFixedBehind, mEnableClipFooterWhenFixedBehind);
        mEnableOverScrollDrag = ta.getBoolean(R.styleable.RefreshLayout_rlEnableOverScrollDrag, mEnableOverScrollDrag);
        mFixedHeaderViewId = ta.getResourceId(R.styleable.RefreshLayout_rlFixedHeaderViewId, mFixedHeaderViewId);
        mFixedFooterViewId = ta.getResourceId(R.styleable.RefreshLayout_rlFixedFooterViewId, mFixedFooterViewId);
        mHeaderTranslationViewId = ta.getResourceId(R.styleable.RefreshLayout_rlHeaderTranslationViewId, mHeaderTranslationViewId);
        mFooterTranslationViewId = ta.getResourceId(R.styleable.RefreshLayout_rlFooterTranslationViewId, mFooterTranslationViewId);
        mEnableNestedScrolling = ta.getBoolean(R.styleable.RefreshLayout_rlEnableNestedScrolling, mEnableNestedScrolling);
        mNestedChild.setNestedScrollingEnabled(mEnableNestedScrolling);

        mManualLoadMore = mManualLoadMore || ta.hasValue(R.styleable.RefreshLayout_rlEnableLoadMore);
        mManualHeaderTranslationContent = mManualHeaderTranslationContent || ta.hasValue(R.styleable.RefreshLayout_rlEnableHeaderTranslationContent);
        mManualFooterTranslationContent = mManualFooterTranslationContent || ta.hasValue(R.styleable.RefreshLayout_rlEnableFooterTranslationContent);
        mHeaderHeightStatus = ta.hasValue(R.styleable.RefreshLayout_rlHeaderHeight) ? DimensionStatus.XmlLayoutUnNotify : mHeaderHeightStatus;
        mFooterHeightStatus = ta.hasValue(R.styleable.RefreshLayout_rlFooterHeight) ? DimensionStatus.XmlLayoutUnNotify : mFooterHeightStatus;

        int accentColor = ta.getColor(R.styleable.RefreshLayout_rlAccentColor, 0);
        int primaryColor = ta.getColor(R.styleable.RefreshLayout_rlPrimaryColor, 0);
        if (primaryColor != 0) {
            if (accentColor != 0) {
                mPrimaryColors = new int[]{primaryColor, accentColor};
            } else {
                mPrimaryColors = new int[]{primaryColor};
            }
        } else if (accentColor != 0) {
            mPrimaryColors = new int[]{0, accentColor};
        }

        if (mEnablePureScrollMode && !mManualLoadMore && !mEnableLoadMore) {
            mEnableLoadMore = true;
        }

        ta.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int count = super.getChildCount();
        if (count > 3) {
            throw new RuntimeException("Most only support three sub view");
        }

        int contentLevel = 0;
        int indexContent = -1;
        for (int i = 0; i < count; i++) {
            View view = super.getChildAt(i);
            if (isContentView(view) && (contentLevel < 2 || i == 1)) {
                indexContent = i;
                contentLevel = 2;
            } else if (!(view instanceof RefreshComponent) && contentLevel < 1) {
                indexContent = i;
                contentLevel = i > 0 ? 1 : 0;
            }
        }

        int indexHeader = -1;
        int indexFooter = -1;
        if (indexContent >= 0) {
            mRefreshContent = new RefreshContentWrapper(super.getChildAt(indexContent));
            if (indexContent == 1) {
                indexHeader = 0;
                if (count == 3) {
                    indexFooter = 2;
                }
            } else if (count == 2) {
                indexFooter = 1;
            }
        }

        for (int i = 0; i < count; i++) {
            View view = super.getChildAt(i);
            if (i == indexHeader || (i != indexFooter && indexHeader == -1 && mRefreshHeader == null && view instanceof RefreshHeader)) {
                mRefreshHeader = (view instanceof RefreshHeader) ? (RefreshHeader) view : new RefreshHeaderWrapper(view);
            } else if (i == indexFooter || (indexFooter == -1 && view instanceof RefreshFooter)) {
                mEnableLoadMore = (mEnableLoadMore || !mManualLoadMore);
                mRefreshFooter = (view instanceof RefreshFooter) ? (RefreshFooter) view : new RefreshFooterWrapper(view);
            }
        }

    }

    /**
     * Override onAttachedToWindow to complete specific functions
     * 1. Add default or globally set Header and Footer (only available by default)
     * 2. Make TextView prompt when Content is empty
     * 3. Enable nested scrolling intelligently NestedScrollingEnabled
     * 4. Initialize the theme color and adjust the display order of Header Footer Content
     **/
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        final View thisView = this;
        if (!thisView.isInEditMode()) {

            if (mRefreshHeader == null) {
                if (sHeaderCreator != null) {
                    RefreshHeader header = sHeaderCreator.createRefreshHeader(thisView.getContext(), this);
                    setRefreshHeader(header);
                }
            }
            if (mRefreshFooter == null) {
                if (sFooterCreator != null) {
                    RefreshFooter footer = sFooterCreator.createRefreshFooter(thisView.getContext(), this);
                    setRefreshFooter(footer);
                }
            } else {
                mEnableLoadMore = mEnableLoadMore || !mManualLoadMore;
            }

            if (mRefreshContent == null) {
                for (int i = 0, len = getChildCount(); i < len; i++) {
                    View view = getChildAt(i);
                    if ((mRefreshHeader == null || view != mRefreshHeader.getView()) && (mRefreshFooter == null || view != mRefreshFooter.getView())) {
                        mRefreshContent = new RefreshContentWrapper(view);
                    }
                }
            }
            if (mRefreshContent == null) {
                final int padding = SmartUtil.dp2px(20);
                final TextView errorView = new TextView(thisView.getContext());
                errorView.setTextColor(0xffff6600);
                errorView.setGravity(Gravity.CENTER);
                errorView.setTextSize(20);
                errorView.setText(R.string.rl_content_empty);
                super.addView(errorView, 0, new RLLayoutParams(MATCH_PARENT, MATCH_PARENT));
                mRefreshContent = new RefreshContentWrapper(errorView);
                mRefreshContent.getView().setPadding(padding, padding, padding, padding);
            }

            View fixedHeaderView = thisView.findViewById(mFixedHeaderViewId);
            View fixedFooterView = thisView.findViewById(mFixedFooterViewId);

            mRefreshContent.setScrollBoundaryDecider(mScrollBoundaryDecider);
            mRefreshContent.setEnableLoadMoreWhenContentNotFull(mEnableLoadMoreWhenContentNotFull);
            mRefreshContent.setUpComponent(mKernel, fixedHeaderView, fixedFooterView);

            if (mSpinner != 0) {
                notifyStateChanged(RefreshState.None);
                mRefreshContent.moveSpinner(mSpinner = 0, mHeaderTranslationViewId, mFooterTranslationViewId);
            }

        }

        if (mPrimaryColors != null) {
            if (mRefreshHeader != null) {
                mRefreshHeader.setPrimaryColors(mPrimaryColors);
            }
            if (mRefreshFooter != null) {
                mRefreshFooter.setPrimaryColors(mPrimaryColors);
            }
        }

        //重新排序
        if (mRefreshContent != null) {
            super.bringChildToFront(mRefreshContent.getView());
        }
        if (mRefreshHeader != null && mRefreshHeader.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshHeader.getView());
        }
        if (mRefreshFooter != null && mRefreshFooter.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshFooter.getView());
        }

    }

    /**
     * Measuring Header Footer Content *
     * 1. The measurement code looks very complicated, because the Header Footer has four stretch transformation styles {@link SpinnerStyle}, each style has its own measurement method*
     * 2. Preview measurement is provided. Preview directly when editing XML (isInEditMode) * 3. Restore the horizontal touch position buffer mLastTouchX to the center of the screen*
     *
     * @param widthMeasureSpec  horizontal measurement parameters
     * @param heightMeasureSpec vertical measurement parameters
     */
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int minimumWidth = 0;
        int minimumHeight = 0;
        final View thisView = this;
        final boolean needPreview = thisView.isInEditMode() && mEnablePreviewInEditMode;

        for (int i = 0, len = super.getChildCount(); i < len; i++) {
            View child = super.getChildAt(i);

            if (child.getVisibility() == GONE || "GONE".equals(child.getTag(R.id.rl_tag))) {
                continue;
            }

            if (mRefreshHeader != null && mRefreshHeader.getView() == child) {
                final View headerView = mRefreshHeader.getView();
                final ViewGroup.LayoutParams lp = headerView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                final int widthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, mlp.leftMargin + mlp.rightMargin, lp.width);
                int height = mHeaderHeight;

                if (mHeaderHeightStatus.ordinal < DimensionStatus.XmlLayoutUnNotify.ordinal) {
                    if (lp.height > 0) {
                        height = lp.height + mlp.bottomMargin + mlp.topMargin;
                        if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.XmlExactUnNotify)) {
                            mHeaderHeight = lp.height + mlp.bottomMargin + mlp.topMargin;
                            mHeaderHeightStatus = DimensionStatus.XmlExactUnNotify;
                        }
                    } else if (lp.height == WRAP_CONTENT && (mRefreshHeader.getSpinnerStyle() != SpinnerStyle.MatchLayout || !mHeaderHeightStatus.notified)) {
                        final int maxHeight = Math.max(getSize(heightMeasureSpec) - mlp.bottomMargin - mlp.topMargin, 0);
                        headerView.measure(widthSpec, makeMeasureSpec(maxHeight, AT_MOST));
                        final int measuredHeight = headerView.getMeasuredHeight();
                        if (measuredHeight > 0) {
                            height = -1;
                            if (measuredHeight != (maxHeight) && mHeaderHeightStatus.canReplaceWith(DimensionStatus.XmlWrapUnNotify)) {
                                mHeaderHeight = measuredHeight + mlp.bottomMargin + mlp.topMargin;
                                mHeaderHeightStatus = DimensionStatus.XmlWrapUnNotify;
                            }
                        }
                    }
                }

                if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.MatchLayout) {
                    height = getSize(heightMeasureSpec);
                } else if (mRefreshHeader.getSpinnerStyle().scale && !needPreview) {
                    height = Math.max(0, isEnableRefreshOrLoadMore(mEnableRefresh) ? mSpinner : 0);
                }

                if (height != -1) {
                    headerView.measure(widthSpec, makeMeasureSpec(Math.max(height - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                }

                if (!mHeaderHeightStatus.notified) {
                    mHeaderHeightStatus = mHeaderHeightStatus.notified();
                    mRefreshHeader.onInitialized(mKernel, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
                }

                if (needPreview && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                    minimumWidth += headerView.getMeasuredWidth();
                    minimumHeight += headerView.getMeasuredHeight();
                }
            }

            if (mRefreshFooter != null && mRefreshFooter.getView() == child) {
                final View footerView = mRefreshFooter.getView();
                final ViewGroup.LayoutParams lp = footerView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                final int widthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, mlp.leftMargin + mlp.rightMargin, lp.width);
                int height = mFooterHeight;

                if (mFooterHeightStatus.ordinal < DimensionStatus.XmlLayoutUnNotify.ordinal) {
                    if (lp.height > 0) {
                        height = lp.height + mlp.topMargin + mlp.bottomMargin;
                        if (mFooterHeightStatus.canReplaceWith(DimensionStatus.XmlExactUnNotify)) {
                            mFooterHeight = lp.height + mlp.topMargin + mlp.bottomMargin;
                            mFooterHeightStatus = DimensionStatus.XmlExactUnNotify;
                        }
                    } else if (lp.height == WRAP_CONTENT && (mRefreshFooter.getSpinnerStyle() != SpinnerStyle.MatchLayout || !mFooterHeightStatus.notified)) {
                        int maxHeight = Math.max(getSize(heightMeasureSpec) - mlp.bottomMargin - mlp.topMargin, 0);
                        footerView.measure(widthSpec, makeMeasureSpec(maxHeight, AT_MOST));
                        int measuredHeight = footerView.getMeasuredHeight();
                        if (measuredHeight > 0) {
                            height = -1;
                            if (measuredHeight != (maxHeight) && mFooterHeightStatus.canReplaceWith(DimensionStatus.XmlWrapUnNotify)) {
                                mFooterHeight = measuredHeight + mlp.topMargin + mlp.bottomMargin;
                                mFooterHeightStatus = DimensionStatus.XmlWrapUnNotify;
                            }
                        }
                    }
                }

                if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.MatchLayout) {
                    height = getSize(heightMeasureSpec);
                } else if (mRefreshFooter.getSpinnerStyle().scale && !needPreview) {
                    height = Math.max(0, isEnableRefreshOrLoadMore(mEnableLoadMore) ? -mSpinner : 0);
                }

                if (height != -1) {
                    footerView.measure(widthSpec, makeMeasureSpec(Math.max(height - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                }

                if (!mFooterHeightStatus.notified) {
                    mFooterHeightStatus = mFooterHeightStatus.notified();
                    mRefreshFooter.onInitialized(mKernel, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
                }

                if (needPreview && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                    minimumWidth += footerView.getMeasuredWidth();
                    minimumHeight += footerView.getMeasuredHeight();
                }
            }

            if (mRefreshContent != null && mRefreshContent.getView() == child) {
                final View contentView = mRefreshContent.getView();
                final ViewGroup.LayoutParams lp = contentView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                final boolean showHeader = (mRefreshHeader != null && isEnableRefreshOrLoadMore(mEnableRefresh) && isEnableTranslationContent(mEnableHeaderTranslationContent, mRefreshHeader));
                final boolean showFooter = (mRefreshFooter != null && isEnableRefreshOrLoadMore(mEnableLoadMore) && isEnableTranslationContent(mEnableFooterTranslationContent, mRefreshFooter));
                final int widthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, thisView.getPaddingLeft() + thisView.getPaddingRight() + mlp.leftMargin + mlp.rightMargin, lp.width);
                final int heightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, thisView.getPaddingTop() + thisView.getPaddingBottom() + mlp.topMargin + mlp.bottomMargin + ((needPreview && showHeader) ? mHeaderHeight : 0) + ((needPreview && showFooter) ? mFooterHeight : 0), lp.height);
                contentView.measure(widthSpec, heightSpec);
                minimumWidth += contentView.getMeasuredWidth() + mlp.leftMargin + mlp.rightMargin;
                minimumHeight += contentView.getMeasuredHeight() + mlp.topMargin + mlp.bottomMargin;
            }
        }
        minimumWidth += thisView.getPaddingLeft() + thisView.getPaddingRight();
        minimumHeight += thisView.getPaddingTop() + thisView.getPaddingBottom();
        super.setMeasuredDimension(View.resolveSize(Math.max(minimumWidth, super.getSuggestedMinimumWidth()), widthMeasureSpec), View.resolveSize(Math.max(minimumHeight, super.getSuggestedMinimumHeight()), heightMeasureSpec));

        mLastTouchX = thisView.getMeasuredWidth() / 2f;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final View thisView = this;
        final int paddingLeft = thisView.getPaddingLeft();
        final int paddingTop = thisView.getPaddingTop();
        final int paddingBottom = thisView.getPaddingBottom();

        for (int i = 0, len = super.getChildCount(); i < len; i++) {
            View child = super.getChildAt(i);

            if (child.getVisibility() == GONE || "GONE".equals(child.getTag(R.id.rl_tag))) {
                continue;
            }

            if (mRefreshContent != null && mRefreshContent.getView() == child) {
                boolean isPreviewMode = thisView.isInEditMode() && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(mEnableRefresh) && mRefreshHeader != null;
                final View contentView = mRefreshContent.getView();
                final ViewGroup.LayoutParams lp = contentView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                int left = paddingLeft + mlp.leftMargin;
                int top = paddingTop + mlp.topMargin;
                int right = left + contentView.getMeasuredWidth();
                int bottom = top + contentView.getMeasuredHeight();
                if (isPreviewMode && (isEnableTranslationContent(mEnableHeaderTranslationContent, mRefreshHeader))) {
                    top = top + mHeaderHeight;
                    bottom = bottom + mHeaderHeight;
                }

                contentView.layout(left, top, right, bottom);
            }
            if (mRefreshHeader != null && mRefreshHeader.getView() == child) {
                boolean isPreviewMode = thisView.isInEditMode() && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(mEnableRefresh);
                final View headerView = mRefreshHeader.getView();
                final ViewGroup.LayoutParams lp = headerView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                int left = mlp.leftMargin;
                int top = mlp.topMargin + mHeaderInsetStart;
                int right = left + headerView.getMeasuredWidth();
                int bottom = top + headerView.getMeasuredHeight();
                if (!isPreviewMode) {
                    if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
                        top = top - mHeaderHeight;
                        bottom = bottom - mHeaderHeight;
                    }
                }
                headerView.layout(left, top, right, bottom);
            }
            if (mRefreshFooter != null && mRefreshFooter.getView() == child) {
                final boolean isPreviewMode = thisView.isInEditMode() && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(mEnableLoadMore);
                final View footerView = mRefreshFooter.getView();
                final ViewGroup.LayoutParams lp = footerView.getLayoutParams();
                final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                final SpinnerStyle style = mRefreshFooter.getSpinnerStyle();
                int left = mlp.leftMargin;
                int top = mlp.topMargin + thisView.getMeasuredHeight() - mFooterInsetStart;
                if (mFooterNoMoreData && mFooterNoMoreDataEffective && mEnableFooterFollowWhenNoMoreData && mRefreshContent != null && mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                    final View contentView = mRefreshContent.getView();
                    final ViewGroup.LayoutParams clp = contentView.getLayoutParams();
                    final int topMargin = clp instanceof MarginLayoutParams ? ((MarginLayoutParams) clp).topMargin : 0;
                    top = paddingTop + paddingTop + topMargin + contentView.getMeasuredHeight();
                }

                if (style == SpinnerStyle.MatchLayout) {
                    top = mlp.topMargin - mFooterInsetStart;
                } else if (isPreviewMode || style == SpinnerStyle.FixedFront || style == SpinnerStyle.FixedBehind) {
                    top = top - mFooterHeight;
                } else if (style.scale && mSpinner < 0) {
                    top = top - Math.max(isEnableRefreshOrLoadMore(mEnableLoadMore) ? -mSpinner : 0, 0);
                }

                int right = left + footerView.getMeasuredWidth();
                int bottom = top + footerView.getMeasuredHeight();
                footerView.layout(left, top, right, bottom);
            }
        }
    }

    /**
     * Rewrite onDetachedFromWindow to complete specific functions of smart * 1. Restore original state * 2. Clear animation data (to prevent memory leaks)
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        mManualLoadMore = true;
        animationRunnable = null;
        if (reboundAnimator != null) {
            Animator animator = reboundAnimator;
            animator.removeAllListeners();
            reboundAnimator.removeAllUpdateListeners();
            reboundAnimator.setDuration(0);//Cancel will trigger the End call, you can judge 0 to determine whether it is canceled
            reboundAnimator.cancel();//Will trigger cancel and end calls
            reboundAnimator = null;
        }
        if (mRefreshHeader != null && mState == RefreshState.Refreshing) {
            mRefreshHeader.onFinish(this, false);
        }
        if (mRefreshFooter != null && mState == RefreshState.Loading) {
            mRefreshFooter.onFinish(this, false);
        }
        if (mSpinner != 0) {
            mKernel.moveSpinner(0, true);
        }
        if (mState != RefreshState.None) {
            notifyStateChanged(RefreshState.None);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mFooterLocked = false;
    }

    /**
     * Override drawChild to complete specific functions * 1. Draw the background for the Header and Footer (only when the background is set) * 2. When the Header and Footer are in the FixedBehind style, do the clipping function mEnableClipHeaderWhenFixedBehind=true
     */
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final View thisView = this;
        final View contentView = mRefreshContent != null ? mRefreshContent.getView() : null;
        if (mRefreshHeader != null && mRefreshHeader.getView() == child) {
            if (!isEnableRefreshOrLoadMore(mEnableRefresh) || (!mEnablePreviewInEditMode && thisView.isInEditMode())) {
                return true;
            }
            if (contentView != null) {
                int bottom = Math.max(contentView.getTop() + contentView.getPaddingTop() + mSpinner, child.getTop());
                if (mHeaderBackgroundColor != 0 && mPaint != null) {
                    mPaint.setColor(mHeaderBackgroundColor);
                    if (mRefreshHeader.getSpinnerStyle().scale) {
                        bottom = child.getBottom();
                    } else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
                        bottom = child.getBottom() + mSpinner;
                    }
                    canvas.drawRect(0, child.getTop(), thisView.getWidth(), bottom, mPaint);
                }
                if ((mEnableClipHeaderWhenFixedBehind && mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind) || mRefreshHeader.getSpinnerStyle().scale) {
                    canvas.save();
                    canvas.clipRect(child.getLeft(), child.getTop(), child.getRight(), bottom);
                    boolean ret = super.drawChild(canvas, child, drawingTime);
                    canvas.restore();
                    return ret;
                }
            }
        }
        if (mRefreshFooter != null && mRefreshFooter.getView() == child) {
            if (!isEnableRefreshOrLoadMore(mEnableLoadMore) || (!mEnablePreviewInEditMode && thisView.isInEditMode())) {
                return true;
            }
            if (contentView != null) {
                int top = Math.min(contentView.getBottom() - contentView.getPaddingBottom() + mSpinner, child.getBottom());
                if (mFooterBackgroundColor != 0 && mPaint != null) {
                    mPaint.setColor(mFooterBackgroundColor);
                    if (mRefreshFooter.getSpinnerStyle().scale) {
                        top = child.getTop();
                    } else if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate) {
                        top = child.getTop() + mSpinner;
                    }
                    canvas.drawRect(0, top, thisView.getWidth(), child.getBottom(), mPaint);
                }
                if ((mEnableClipFooterWhenFixedBehind && mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind) || mRefreshFooter.getSpinnerStyle().scale) {
                    canvas.save();
                    canvas.clipRect(child.getLeft(), top, child.getRight(), child.getBottom());
                    boolean ret = super.drawChild(canvas, child, drawingTime);
                    canvas.restore();
                    return ret;
                }
            }

        }
        return super.drawChild(canvas, child, drawingTime);
    }

    protected boolean mVerticalPermit = false; //Vertical communication certificate (used to determine the authority of special events)

    //Rewrite computeScroll to complete out-of-bounds rebound boundary collision
    @Override
    public void computeScroll() {
        int lastCurY = mScroller.getCurrY();
        if (mScroller.computeScrollOffset()) {
            int finalY = mScroller.getFinalY();
            if ((finalY < 0 && (mEnableRefresh || mEnableOverScrollDrag) && mRefreshContent.canRefresh()) || (finalY > 0 && (mEnableLoadMore || mEnableOverScrollDrag) && mRefreshContent.canLoadMore())) {
                if (mVerticalPermit) {
                    float velocity;
                    velocity = finalY > 0 ? -mScroller.getCurrVelocity() : mScroller.getCurrVelocity();
                    animSpinnerBounce(velocity);
                }
                mScroller.forceFinished(true);
            } else {
                mVerticalPermit = true;//打开竖直通行证
                final View thisView = this;
                thisView.invalidate();
            }
        }
    }

    protected MotionEvent mFalsifyEvent = null;

    /**
     * Event distribution (gesture core)
     * 1. Multi-touch
     * 2. Seamless content scrolling
     *
     * @param e event
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        final int action = e.getActionMasked();
        final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? e.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = e.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += e.getX(i);
            sumY += e.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float touchX = sumX / div;
        final float touchY = sumY / div;
        if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_POINTER_DOWN) && mIsBeingDragged) {
            mTouchY += touchY - mLastTouchY;
        }
        mLastTouchX = touchX;
        mLastTouchY = touchY;
        final View thisView = this;
        if (mNestedInProgress) {//When nested scrolling, the supplementary vertical direction does not scroll, but the horizontal direction scrolls, you need to notify onHorizontalDrag
            int totalUnconsumed = mTotalUnconsumed;
            boolean ret = super.dispatchTouchEvent(e);
            if (action == MotionEvent.ACTION_MOVE) {
                if (totalUnconsumed == mTotalUnconsumed) {
                    final int offsetX = (int) mLastTouchX;
                    final int offsetMax = thisView.getWidth();
                    final float percentX = mLastTouchX / (offsetMax == 0 ? 1 : offsetMax);
                    if (isEnableRefreshOrLoadMore(mEnableRefresh) && mSpinner > 0 && mRefreshHeader != null && mRefreshHeader.isSupportHorizontalDrag()) {
                        mRefreshHeader.onHorizontalDrag(percentX, offsetX, offsetMax);
                    } else if (isEnableRefreshOrLoadMore(mEnableLoadMore) && mSpinner < 0 && mRefreshFooter != null && mRefreshFooter.isSupportHorizontalDrag()) {
                        mRefreshFooter.onHorizontalDrag(percentX, offsetX, offsetMax);
                    }
                }
            }
            return ret;
        } else if (!thisView.isEnabled() || (!mEnableRefresh && !mEnableLoadMore && !mEnableOverScrollDrag) || (mHeaderNeedTouchEventWhenRefreshing && ((mState.isOpening || mState.isFinishing) && mState.isHeader)) || (mFooterNeedTouchEventWhenLoading && ((mState.isOpening || mState.isFinishing) && mState.isFooter))) {
            return super.dispatchTouchEvent(e);
        }

        if (interceptAnimatorByAction(action) || mState.isFinishing || (mState == RefreshState.Loading && mDisableContentWhenLoading) || (mState == RefreshState.Refreshing && mDisableContentWhenRefresh)) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurrentVelocity = 0;
                mVelocityTracker.addMovement(e);
                mScroller.forceFinished(true);
                mTouchX = touchX;
                mTouchY = touchY;
                mLastSpinner = 0;
                mTouchSpinner = mSpinner;
                mIsBeingDragged = false;
                mEnableDisallowIntercept = false;
                mSuperDispatchTouchEvent = super.dispatchTouchEvent(e);
                if (mState == RefreshState.TwoLevel && mTouchY < thisView.getMeasuredHeight() * (1 - mTwoLevelBottomPullUpToCloseRate)) {
                    mDragDirection = 'h';//Level 2 refresh mark scrolls horizontally to prohibit drag
                    return mSuperDispatchTouchEvent;
                }
                if (mRefreshContent != null) {
                    //Pass the coordinates of the current touch event to RefreshContent, which is used to intelligently judge the scroll boundary and related information of the corresponding coordinate position View
                    mRefreshContent.onActionDown(e);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = touchX - mTouchX;
                float dy = touchY - mTouchY;
                mVelocityTracker.addMovement(e);
                if (!mIsBeingDragged && !mEnableDisallowIntercept && mDragDirection != 'h' && mRefreshContent != null) {//Before dragging, check canRefresh canLoadMore to start dragging
                    if (mDragDirection == 'v' || (Math.abs(dy) >= mTouchSlop && Math.abs(dx) < Math.abs(dy))) {//The maximum sliding angle allowed is 45 degrees
                        mDragDirection = 'v';
                        if (dy > 0 && (mSpinner < 0 || ((mEnableOverScrollDrag || mEnableRefresh) && mRefreshContent.canRefresh()))) {
                            mIsBeingDragged = true;
                            mTouchY = touchY - mTouchSlop;
                        } else if (dy < 0 && (mSpinner > 0 || ((mEnableOverScrollDrag || mEnableLoadMore) && ((mState == RefreshState.Loading && mFooterLocked) || mRefreshContent.canLoadMore())))) {
                            mIsBeingDragged = true;
                            mTouchY = touchY + mTouchSlop;
                        }
                        if (mIsBeingDragged) {
                            dy = touchY - mTouchY;//dyAdjust mTouchSlop deviation and recalculate dy
                            if (mSuperDispatchTouchEvent) {//If the parent class intercepts the event, send a cancellation event notification
                                e.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(e);
                            }
                            mKernel.setState((mSpinner > 0 || (mSpinner == 0 && dy > 0)) ? RefreshState.PullDownToRefresh : RefreshState.PullUpToLoad);
                            final ViewParent parent = thisView.getParent();
                            if (parent instanceof ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(true);//Notify the parent control not to intercept events
                            }
                        }
                    } else if (Math.abs(dx) >= mTouchSlop && Math.abs(dx) > Math.abs(dy) && mDragDirection != 'v') {
                        mDragDirection = 'h';//Marked as horizontal drag, it will not be triggered again Pull down to refresh Pull up to load
                    }
                }
                if (mIsBeingDragged) {
                    int spinner = (int) dy + mTouchSpinner;
                    if ((mViceState.isHeader && (spinner < 0 || mLastSpinner < 0)) || (mViceState.isFooter && (spinner > 0 || mLastSpinner > 0))) {
                        mLastSpinner = spinner;
                        long time = e.getEventTime();
                        if (mFalsifyEvent == null) {
                            mFalsifyEvent = obtain(time, time, MotionEvent.ACTION_DOWN, mTouchX + dx, mTouchY, 0);
                            super.dispatchTouchEvent(mFalsifyEvent);
                        }
                        MotionEvent em = obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mTouchY + spinner, 0);
                        super.dispatchTouchEvent(em);
                        if (mFooterLocked && dy > mTouchSlop && mSpinner < 0) {
                            mFooterLocked = false;//Unlock Footer's lock when content scrolls down
                        }
                        if (spinner > 0 && ((mEnableOverScrollDrag || mEnableRefresh) && mRefreshContent.canRefresh())) {
                            mTouchY = mLastTouchY = touchY;
                            mTouchSpinner = spinner = 0;
                            mKernel.setState(RefreshState.PullDownToRefresh);
                        } else if (spinner < 0 && ((mEnableOverScrollDrag || mEnableLoadMore) && mRefreshContent.canLoadMore())) {
                            mTouchY = mLastTouchY = touchY;
                            mTouchSpinner = spinner = 0;
                            mKernel.setState(RefreshState.PullUpToLoad);
                        }
                        if ((mViceState.isHeader && spinner < 0) || (mViceState.isFooter && spinner > 0)) {
                            if (mSpinner != 0) {
                                moveSpinnerInfinitely(0);
                            }
                            return true;
                        } else if (mFalsifyEvent != null) {
                            mFalsifyEvent = null;
                            em.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(em);
                        }
                        em.recycle();
                    }
                    moveSpinnerInfinitely(spinner);
                    return true;
                } else if (mFooterLocked && dy > mTouchSlop && mSpinner < 0) {
                    mFooterLocked = false;//Unlock Footer's lock when content scrolls down
                }
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(e);
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                mCurrentVelocity = (int) mVelocityTracker.getYVelocity();
                startFlingIfNeed(0);
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.clear();
                mDragDirection = 'n';
                if (mFalsifyEvent != null) {
                    mFalsifyEvent.recycle();
                    mFalsifyEvent = null;
                    long time = e.getEventTime();
                    MotionEvent ec = obtain(time, time, action, mTouchX, touchY, 0);
                    super.dispatchTouchEvent(ec);
                    ec.recycle();
                }
                overSpinner();
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;//关闭拖动状态
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(e);
    }

    /**
     * This code comes from Google's official SwipeRefreshLayout mainly to allow the old version of ListView to drop down smoothly and selectively block requestDisallowInterceptTouchEvent
     */
    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        View target = mRefreshContent.getScrollableView();
        if ((android.os.Build.VERSION.SDK_INT >= 21 || !(target instanceof AbsListView)) && (ViewCompat.isNestedScrollingEnabled(target))) {
            mEnableDisallowIntercept = disallowIntercept;
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    /**
     * When necessary, start Fling mode
     *
     * @param flingVelocity speed
     * @return true can intercept nested scrolling Fling
     */
    protected boolean startFlingIfNeed(float flingVelocity) {
        float velocity = flingVelocity == 0 ? mCurrentVelocity : flingVelocity;
        if (Build.VERSION.SDK_INT > 27 && mRefreshContent != null) {
            float scaleY = getScaleY();
            final View thisView = this;
            final View contentView = mRefreshContent.getView();
            if (thisView.getScaleY() == -1 && contentView.getScaleY() == -1) {
                velocity = -velocity;
            }
        }
        if (Math.abs(velocity) > mMinimumVelocity) {
            if (velocity * mSpinner < 0) {
                if (mState == RefreshState.Refreshing || mState == RefreshState.Loading || (mSpinner < 0 && mFooterNoMoreData)) {
                    animationRunnable = new FlingRunnable(velocity).start();
                    return true;
                } else if (mState.isReleaseToOpening) {
                    return true;//Fling that is about to refresh or load when intercepting nested scrolling
                }
            }
            if ((velocity < 0 && ((mEnableOverScrollBounce && (mEnableLoadMore || mEnableOverScrollDrag)) || (mState == RefreshState.Loading && mSpinner >= 0) || (mEnableAutoLoadMore && isEnableRefreshOrLoadMore(mEnableLoadMore)))) || (velocity > 0 && ((mEnableOverScrollBounce && mEnableRefresh || mEnableOverScrollDrag) || (mState == RefreshState.Refreshing && mSpinner <= 0)))) {
                mVerticalPermit = false;//Turn off vertical pass
                mScroller.fling(0, 0, 0, (int) -velocity, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
                mScroller.computeScrollOffset();
                final View thisView = this;
                thisView.invalidate();
            }
        }
        return false;
    }

    /**
     * When the animation is executing, touch the screen to interrupt the animation,
     * and turn to the drag state
     *
     * @param action MotionEvent
     * @return Whether the interruption is successful
     */
    protected boolean interceptAnimatorByAction(int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            if (reboundAnimator != null) {
                if (mState.isFinishing || mState == RefreshState.TwoLevelReleased || mState == RefreshState.RefreshReleased || mState == RefreshState.LoadReleased) {
                    return true;
                }
                if (mState == RefreshState.PullDownCanceled) {
                    mKernel.setState(RefreshState.PullDownToRefresh);
                } else if (mState == RefreshState.PullUpCanceled) {
                    mKernel.setState(RefreshState.PullUpToLoad);
                }
                reboundAnimator.setDuration(0);
                reboundAnimator.cancel();
                reboundAnimator = null;
            }
            animationRunnable = null;
        }
        return reboundAnimator != null;
    }

    /**
     * Set and notify state changes (setState)
     *
     * @param state state
     */
    protected void notifyStateChanged(RefreshState state) {
        final RefreshState oldState = mState;
        if (oldState != state) {
            mState = state;
            mViceState = state;
            final OnStateChangedListener refreshHeader = mRefreshHeader;
            final OnStateChangedListener refreshFooter = mRefreshFooter;
            final OnStateChangedListener refreshListener = mOnMultiListener;
            if (refreshHeader != null) {
                refreshHeader.onStateChanged(this, oldState, state);
            }
            if (refreshFooter != null) {
                refreshFooter.onStateChanged(this, oldState, state);
            }
            if (refreshListener != null) {
                refreshListener.onStateChanged(this, oldState, state);
            }
            if (state == RefreshState.LoadFinish) {
                mFooterLocked = false;
            }
        } else if (mViceState != mState) {
            mViceState = mState;
        }
    }

    /**
     * Set the status directly to Loading
     *
     * @param triggerLoadMoreEvent Whether to trigger the loading callback
     */
    protected void setStateDirectLoading(boolean triggerLoadMoreEvent) {
        if (mState != RefreshState.Loading) {
            mLastOpenTime = currentTimeMillis();
            mFooterLocked = true;
            notifyStateChanged(RefreshState.Loading);
            if (mLoadMoreListener != null) {
                if (triggerLoadMoreEvent) {
                    mLoadMoreListener.onLoadMore(this);
                }
            } else if (mOnMultiListener == null) {
                finishLoadMore(2000);
            }
            if (mRefreshFooter != null) {
                mRefreshFooter.onStartAnimator(this, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
            }
            if (mOnMultiListener != null && mRefreshFooter instanceof RefreshFooter) {
                final OnLoadMoreListener listener = mOnMultiListener;
                if (triggerLoadMoreEvent) {
                    listener.onLoadMore(this);
                }
                mOnMultiListener.onFooterStartAnimator((RefreshFooter) mRefreshFooter, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
            }
        }
    }

    /**
     * Set the status to Loading Loading
     *
     * @param notify Whether to trigger the notification event
     */
    protected void setStateLoading(final boolean notify) {
        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation != null && animation.getDuration() == 0) {
                    return;//0 表示被取消
                }
                setStateDirectLoading(notify);
            }
        };
        notifyStateChanged(RefreshState.LoadReleased);
        ValueAnimator animator = mKernel.animSpinner(-mFooterHeight);
        if (animator != null) {
            animator.addListener(listener);
        }
        if (mRefreshFooter != null) {
            //The execution order of onReleased is set after animSpinner and before onAnimationEnd.
            //In this way, onReleased can overwrite the previous animSpinner.
            mRefreshFooter.onReleased(this, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
        }
        if (mOnMultiListener != null && mRefreshFooter instanceof RefreshFooter) {
            //The same as mRefreshFooter.onReleased
            mOnMultiListener.onFooterReleased((RefreshFooter) mRefreshFooter, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
        }
        if (animator == null) {
            //onAnimationEnd will change the state to loading and must be called after onReleased
            listener.onAnimationEnd(null);
        }
    }

    /**
     * Set the status to Refreshing Refreshing
     *
     * @param notify Whether to trigger the notification event
     */
    protected void setStateRefreshing(final boolean notify) {
        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation != null && animation.getDuration() == 0) {
                    return;//0 表示被取消
                }
                mLastOpenTime = currentTimeMillis();
                notifyStateChanged(RefreshState.Refreshing);
                if (mRefreshListener != null) {
                    if (notify) {
                        mRefreshListener.onRefresh(RefreshLayout.this);
                    }
                } else if (mOnMultiListener == null) {
                    finishRefresh(5000);
                }
                if (mRefreshHeader != null) {
                    mRefreshHeader.onStartAnimator(RefreshLayout.this, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
                }
                if (mOnMultiListener != null && mRefreshHeader instanceof RefreshHeader) {
                    if (notify) {
                        mOnMultiListener.onRefresh(RefreshLayout.this);
                    }
                    mOnMultiListener.onHeaderStartAnimator((RefreshHeader) mRefreshHeader, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
                }
            }
        };
        notifyStateChanged(RefreshState.RefreshReleased);
        ValueAnimator animator = mKernel.animSpinner(mHeaderHeight);
        if (animator != null) {
            animator.addListener(listener);
        }
        if (mRefreshHeader != null) {
            // The execution order of onReleased is set after animSpinner and before onAnimationEnd.
            // In this way, onRefreshReleased can overwrite the previous animSpinner.
            mRefreshHeader.onReleased(this, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
        }
        if (mOnMultiListener != null && mRefreshHeader instanceof RefreshHeader) {
            //the same as mRefreshHeader.onReleased
            mOnMultiListener.onHeaderReleased((RefreshHeader) mRefreshHeader, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
        }
        if (animator == null) {
            //onAnimationEnd will change the state to Refreshing and must be called after onReleased
            listener.onAnimationEnd(null);
        }
    }

    /**
     * Set the secondary status
     */
    protected void setViceState(RefreshState state) {
        if (mState.isDragging && mState.isHeader != state.isHeader) {
            notifyStateChanged(RefreshState.None);
        }
        if (mViceState != state) {
            mViceState = state;
        }
    }

    /**
     * Determine whether the content needs to be moved when pulling down
     *
     * @param enable   mEnableHeaderTranslationContent or mEnableFooterTranslationContent
     * @param internal mRefreshHeader or mRefreshFooter
     * @return enable
     */
    protected boolean isEnableTranslationContent(boolean enable, @Nullable RefreshComponent internal) {
        return enable || mEnablePureScrollMode || internal == null || internal.getSpinnerStyle() == SpinnerStyle.FixedBehind;
    }

    /**
     * Whether it’s real can be refreshed or loaded (to distinguish it from the pure scrolling mode of cross-border dragging).
     * When judging, it can be refreshed or loaded (directly affects whether Header and Footer are displayed)
     *
     * @param enable mEnableRefresh or mEnableLoadMore
     * @return enable
     */
    protected boolean isEnableRefreshOrLoadMore(boolean enable) {
        return enable && !mEnablePureScrollMode;
    }

    protected Runnable animationRunnable;
    protected ValueAnimator reboundAnimator;

    protected class FlingRunnable implements Runnable {
        int mOffset;
        int mFrame = 0;
        int mFrameDelay = 10;
        float mVelocity;
        float mDamping = 0.98f;
        long mStartTime = 0;
        long mLastTime = AnimationUtils.currentAnimationTimeMillis();

        FlingRunnable(float velocity) {
            mVelocity = velocity;
            mOffset = mSpinner;
        }

        public Runnable start() {
            if (mState.isFinishing) {
                return null;
            }
            if (mSpinner != 0 && (!(mState.isOpening || (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(mEnableLoadMore))) || ((mState == RefreshState.Loading || (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(mEnableLoadMore))) && mSpinner < -mFooterHeight) || (mState == RefreshState.Refreshing && mSpinner > mHeaderHeight))) {
                int frame = 0;
                int offset = mSpinner;
                int spinner = mSpinner;
                float velocity = mVelocity;
                while (spinner * offset > 0) {
                    velocity *= Math.pow(mDamping, (++frame) * mFrameDelay / 10f);
                    float velocityFrame = (velocity * (1f * mFrameDelay / 1000));
                    if (Math.abs(velocityFrame) < 1) {
                        if (!mState.isOpening || (mState == RefreshState.Refreshing && offset > mHeaderHeight) || (mState != RefreshState.Refreshing && offset < -mFooterHeight)) {
                            return null;
                        }
                        break;
                    }
                    offset += velocityFrame;
                }
            }
            mStartTime = AnimationUtils.currentAnimationTimeMillis();
            mHandler.postDelayed(this, mFrameDelay);
            return this;
        }

        @Override
        public void run() {
            if (animationRunnable == this && !mState.isFinishing) {
                long now = AnimationUtils.currentAnimationTimeMillis();
                long span = now - mLastTime;
                mVelocity *= Math.pow(mDamping, (now - mStartTime) / (1000f / mFrameDelay));
                float velocity = (mVelocity * (1f * span / 1000));
                if (Math.abs(velocity) > 1) {
                    mLastTime = now;
                    mOffset += velocity;
                    if (mSpinner * mOffset > 0) {
                        mKernel.moveSpinner(mOffset, true);
                        mHandler.postDelayed(this, mFrameDelay);
                    } else {
                        animationRunnable = null;
                        mKernel.moveSpinner(0, true);
                        fling(mRefreshContent.getScrollableView(), (int) -mVelocity);
                        if (mFooterLocked && velocity > 0) {
                            mFooterLocked = false;
                        }
                    }
                } else {
                    animationRunnable = null;
                }
            }
        }
    }

    protected class BounceRunnable implements Runnable {
        int mFrame = 0;
        int mFrameDelay = 10;
        int mSmoothDistance;
        long mLastTime;
        float mOffset = 0;
        float mVelocity;

        BounceRunnable(float velocity, int smoothDistance) {
            mVelocity = velocity;
            mSmoothDistance = smoothDistance;
            mLastTime = AnimationUtils.currentAnimationTimeMillis();
            mHandler.postDelayed(this, mFrameDelay);
            if (velocity > 0) {
                mKernel.setState(RefreshState.PullDownToRefresh);
            } else {
                mKernel.setState(RefreshState.PullUpToLoad);
            }
        }

        @Override
        public void run() {
            if (animationRunnable == this && !mState.isFinishing) {
                if (Math.abs(mSpinner) >= Math.abs(mSmoothDistance)) {
                    if (mSmoothDistance != 0) {
                        mVelocity *= Math.pow(0.45f, ++mFrame * 2);
                    } else {
                        mVelocity *= Math.pow(0.85f, ++mFrame * 2);
                    }
                } else {
                    mVelocity *= Math.pow(0.95f, ++mFrame * 2);
                }
                long now = AnimationUtils.currentAnimationTimeMillis();
                float t = 1f * (now - mLastTime) / 1000;
                float velocity = mVelocity * t;
                if (Math.abs(velocity) >= 1) {
                    mLastTime = now;
                    mOffset += velocity;
                    moveSpinnerInfinitely(mOffset);
                    mHandler.postDelayed(this, mFrameDelay);
                } else {
                    if (mViceState.isDragging && mViceState.isHeader) {
                        mKernel.setState(RefreshState.PullDownCanceled);
                    } else if (mViceState.isDragging && mViceState.isFooter) {
                        mKernel.setState(RefreshState.PullUpCanceled);
                    }
                    animationRunnable = null;
                    if (Math.abs(mSpinner) >= Math.abs(mSmoothDistance)) {
                        int duration = 10 * Math.min(Math.max((int) SmartUtil.px2dp(Math.abs(mSpinner - mSmoothDistance)), 30), 100);
                        animSpinner(mSmoothDistance, 0, mReboundInterpolator, duration);
                    }
                }
            }
        }
    }

    /**
     * Perform rebound animation @param endSpinner target value
     *
     * @param startDelay   delay parameter
     * @param interpolator accelerator
     * @param duration     duration
     * @return ValueAnimator or null
     */
    protected ValueAnimator animSpinner(int endSpinner, int startDelay, Interpolator interpolator, int duration) {
        if (mSpinner != endSpinner) {
            if (reboundAnimator != null) {
                reboundAnimator.setDuration(0);
                reboundAnimator.cancel();
                reboundAnimator = null;
            }
            animationRunnable = null;
            reboundAnimator = ValueAnimator.ofInt(mSpinner, endSpinner);
            reboundAnimator.setDuration(duration);
            reboundAnimator.setInterpolator(interpolator);
            reboundAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation != null && animation.getDuration() == 0) {
                        return;
                    }
                    reboundAnimator = null;
                    if (mSpinner == 0 && mState != RefreshState.None && !mState.isOpening && !mState.isDragging) {
                        notifyStateChanged(RefreshState.None);
                    } else if (mState != mViceState) {
                        setViceState(mState);
                    }
                }
            });
            reboundAnimator.addUpdateListener(animation -> mKernel.moveSpinner((int) animation.getAnimatedValue(), false));
            reboundAnimator.setStartDelay(startDelay);
            reboundAnimator.start();
            return reboundAnimator;
        }
        return null;
    }

    /**
     * Out of bounds rebound animation
     *
     * @param velocity speed
     */
    protected void animSpinnerBounce(final float velocity) {
        if (reboundAnimator == null) {
            if (velocity > 0 && (mState == RefreshState.Refreshing || mState == RefreshState.TwoLevel)) {
                animationRunnable = new BounceRunnable(velocity, mHeaderHeight);
            } else if (velocity < 0 && (mState == RefreshState.Loading || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(mEnableLoadMore)) || (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(mEnableLoadMore) && mState != RefreshState.Refreshing))) {
                animationRunnable = new BounceRunnable(velocity, -mFooterHeight);
            } else if (mSpinner == 0 && mEnableOverScrollBounce) {
                animationRunnable = new BounceRunnable(velocity, 0);
            }
        }
    }

    /**
     * The gesture drag ends and the rebound animation starts
     */
    @SuppressWarnings("StatementWithEmptyBody")
    protected void overSpinner() {
        if (mState == RefreshState.TwoLevel) {
            final View thisView = this;
            if (mCurrentVelocity > -1000 && mSpinner > thisView.getHeight() / 2) {
                ValueAnimator animator = mKernel.animSpinner(thisView.getHeight());
                if (animator != null) {
                    animator.setDuration(mFloorDuration);
                }
            } else if (mIsBeingDragged) {
                mKernel.finishTwoLevel();
            }
        } else if (mState == RefreshState.Loading || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && mSpinner < 0 && isEnableRefreshOrLoadMore(mEnableLoadMore))) {
            if (mSpinner < -mFooterHeight) {
                mKernel.animSpinner(-mFooterHeight);
            } else if (mSpinner > 0) {
                mKernel.animSpinner(0);
            }
        } else if (mState == RefreshState.Refreshing) {
            if (mSpinner > mHeaderHeight) {
                mKernel.animSpinner(mHeaderHeight);
            } else if (mSpinner < 0) {
                mKernel.animSpinner(0);
            }
        } else if (mState == RefreshState.PullDownToRefresh) {
            mKernel.setState(RefreshState.PullDownCanceled);
        } else if (mState == RefreshState.PullUpToLoad) {
            mKernel.setState(RefreshState.PullUpCanceled);
        } else if (mState == RefreshState.ReleaseToRefresh) {
            mKernel.setState(RefreshState.Refreshing);
        } else if (mState == RefreshState.ReleaseToLoad) {
            mKernel.setState(RefreshState.Loading);
        } else if (mState == RefreshState.ReleaseToTwoLevel) {
            mKernel.setState(RefreshState.TwoLevelReleased);
        } else if (mState == RefreshState.RefreshReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(mHeaderHeight);
            }
        } else if (mState == RefreshState.LoadReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(-mFooterHeight);
            }
        } else if (mState == RefreshState.LoadFinish) {
        } else if (mSpinner != 0) {
            mKernel.animSpinner(0);
        }
    }

    /**
     * Sticky move spinner
     *
     * @param spinner offset
     */
    protected void moveSpinnerInfinitely(float spinner) {
        final View thisView = this;
        if (mNestedInProgress && !mEnableLoadMoreWhenContentNotFull && spinner < 0) {
            if (!mRefreshContent.canLoadMore()) {
                spinner = 0;
            }
        }
        if (mState == RefreshState.TwoLevel && spinner > 0) {
            mKernel.moveSpinner(Math.min((int) spinner, thisView.getMeasuredHeight()), true);
        } else if (mState == RefreshState.Refreshing && spinner >= 0) {
            if (spinner < mHeaderHeight) {
                mKernel.moveSpinner((int) spinner, true);
            } else {
                final double M = (mHeaderMaxDragRate - 1) * mHeaderHeight;
                final double H = Math.max(mScreenHeightPixels * 4 / 3, thisView.getHeight()) - mHeaderHeight;
                final double x = Math.max(0, (spinner - mHeaderHeight) * mDragRate);
                final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
                mKernel.moveSpinner((int) y + mHeaderHeight, true);
            }
        } else if (spinner < 0 && (mState == RefreshState.Loading || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(mEnableLoadMore)) || (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(mEnableLoadMore)))) {
            if (spinner > -mFooterHeight) {
                mKernel.moveSpinner((int) spinner, true);
            } else {
                final double M = (mFooterMaxDragRate - 1) * mFooterHeight;
                final double H = Math.max(mScreenHeightPixels * 4 / 3, thisView.getHeight()) - mFooterHeight;
                final double x = -Math.min(0, (spinner + mFooterHeight) * mDragRate);
                final double y = -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
                mKernel.moveSpinner((int) y - mFooterHeight, true);
            }
        } else if (spinner >= 0) {
            final double M = mHeaderMaxDragRate * mHeaderHeight;
            final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
            final double x = Math.max(0, spinner * mDragRate);
            final double y = Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
            mKernel.moveSpinner((int) y, true);
        } else {
            final double M = mFooterMaxDragRate * mFooterHeight;
            final double H = Math.max(mScreenHeightPixels / 2, thisView.getHeight());
            final double x = -Math.min(0, spinner * mDragRate);
            final double y = -Math.min(M * (1 - Math.pow(100, -x / (H == 0 ? 1 : H))), x);// 公式 y = M(1-100^(-x/H))
            mKernel.moveSpinner((int) y, true);
        }
        if (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(mEnableLoadMore) && spinner < 0 && mState != RefreshState.Refreshing && mState != RefreshState.Loading && mState != RefreshState.LoadFinish) {
            if (mDisableContentWhenLoading) {
                animationRunnable = null;
                mKernel.animSpinner(-mFooterHeight);
            }
            setStateDirectLoading(false);
            /*
             *In auto-loading mode, delay trigger onLoadMore and mReboundDuration to ensure smooth execution of animation
             */
            mHandler.postDelayed(() -> {
                if (mLoadMoreListener != null) {
                    mLoadMoreListener.onLoadMore(RefreshLayout.this);
                } else if (mOnMultiListener == null) {
                    finishLoadMore(2000);
                }
                final OnLoadMoreListener listener = mOnMultiListener;
                if (listener != null) {
                    listener.onLoadMore(RefreshLayout.this);
                }
            }, mReboundDuration);
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        final View thisView = this;
        return new RLLayoutParams(thisView.getContext(), attrs);
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedParent.getNestedScrollAxes();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
        final View thisView = this;
        boolean accepted = thisView.isEnabled() && isNestedScrollingEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        accepted = accepted && (mEnableOverScrollDrag || mEnableRefresh || mEnableLoadMore);
        return accepted;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedParent.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        mNestedChild.startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);

        mTotalUnconsumed = mSpinner;//0;
        mNestedInProgress = true;

        interceptAnimatorByAction(MotionEvent.ACTION_DOWN);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        int consumedY = 0;

        //dy mTotalUnconsumed> 0 means that mSpinner has been pulled out,
        // and now it is about to push back mTotalUnconsumed will subtract the distance of dy and then calculate the new mSpinner
        if (dy * mTotalUnconsumed > 0) {
            if (Math.abs(dy) > Math.abs(mTotalUnconsumed)) {
                consumedY = mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                consumedY = dy;
                mTotalUnconsumed -= dy;
            }
            moveSpinnerInfinitely(mTotalUnconsumed);
        } else if (dy > 0 && mFooterLocked) {
            consumedY = dy;
            mTotalUnconsumed -= dy;
            moveSpinnerInfinitely(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        mNestedChild.dispatchNestedPreScroll(dx, dy - consumedY, consumed, null);
        consumed[1] += consumedY;

    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        // Dispatch up to the nested parent first
        boolean scrolled = mNestedChild.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if ((dy < 0 && (mEnableRefresh || mEnableOverScrollDrag) && (mTotalUnconsumed != 0 || mScrollBoundaryDecider == null || mScrollBoundaryDecider.canRefresh(mRefreshContent.getView()))) || (dy > 0 && (mEnableLoadMore || mEnableOverScrollDrag) && (mTotalUnconsumed != 0 || mScrollBoundaryDecider == null || mScrollBoundaryDecider.canLoadMore(mRefreshContent.getView())))) {
            if (mViceState == RefreshState.None || mViceState.isOpening) {
                mKernel.setState(dy > 0 ? RefreshState.PullUpToLoad : RefreshState.PullDownToRefresh);
                if (!scrolled) {
                    final View thisView = this;
                    final ViewParent parent = thisView.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            moveSpinnerInfinitely(mTotalUnconsumed -= dy);
        }

        if (mFooterLocked && dyConsumed < 0) {
            mFooterLocked = false;
        }

    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return (mFooterLocked && velocityY > 0) || startFlingIfNeed(-velocityY) || mNestedChild.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        return mNestedChild.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        mNestedParent.onStopNestedScroll(target);
        mNestedInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        mTotalUnconsumed = 0;
        overSpinner();
        // Dispatch up our nested parent
        mNestedChild.stopNestedScroll();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mEnableNestedScrolling = enabled;
        //        mManualNestedScrolling = true;
        mNestedChild.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mEnableNestedScrolling && (mEnableOverScrollDrag || mEnableRefresh || mEnableLoadMore);
    }

    /**
     * Set the Header's height.
     *
     * @param heightDp Density-independent Pixels ,virtual pixels (px need to call px 2 dp conversion)
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderHeight(float heightDp) {
        return setHeaderHeightPx(dp2px(heightDp));
    }

    /**
     * @param height pixel
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderHeightPx(int height) {
        if (height == mHeaderHeight) {
            return this;
        }
        if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
            mHeaderHeight = height;
            if (mRefreshHeader != null && mAttachedToWindow && mHeaderHeightStatus.notified) {
                SpinnerStyle style = mRefreshHeader.getSpinnerStyle();
                if (style != SpinnerStyle.MatchLayout && !style.scale) {
                    View headerView = mRefreshHeader.getView();
                    final ViewGroup.LayoutParams lp = headerView.getLayoutParams();
                    final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                    final int widthSpec = makeMeasureSpec(headerView.getMeasuredWidth(), EXACTLY);
                    headerView.measure(widthSpec, makeMeasureSpec(Math.max(mHeaderHeight - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                    final int left = mlp.leftMargin;
                    int top = mlp.topMargin + mHeaderInsetStart - ((style == SpinnerStyle.Translate) ? mHeaderHeight : 0);
                    headerView.layout(left, top, left + headerView.getMeasuredWidth(), top + headerView.getMeasuredHeight());
                }
                mHeaderHeightStatus = DimensionStatus.CodeExact;
                mRefreshHeader.onInitialized(mKernel, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
            } else {
                mHeaderHeightStatus = DimensionStatus.CodeExactUnNotify;
            }
        }
        return this;
    }

    /**
     * Set the Footer's height.
     */
    @Override
    public RefreshLayout setFooterHeight(float heightDp) {
        return setFooterHeightPx(dp2px(heightDp));
    }

    @Override
    public RefreshLayout setFooterHeightPx(int height) {
        if (height == mFooterHeight) {
            return this;
        }
        if (mFooterHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
            mFooterHeight = height;
            if (mRefreshFooter != null && mAttachedToWindow && mFooterHeightStatus.notified) {
                SpinnerStyle style = mRefreshFooter.getSpinnerStyle();
                if (style != SpinnerStyle.MatchLayout && !style.scale) {
                    View thisView = this;
                    View footerView = mRefreshFooter.getView();
                    final ViewGroup.LayoutParams lp = footerView.getLayoutParams();
                    final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                    final int widthSpec = makeMeasureSpec(footerView.getMeasuredWidth(), EXACTLY);
                    footerView.measure(widthSpec, makeMeasureSpec(Math.max(mFooterHeight - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                    final int left = mlp.leftMargin;
                    final int top = mlp.topMargin + thisView.getMeasuredHeight() - mFooterInsetStart - ((style != SpinnerStyle.Translate) ? mFooterHeight : 0);
                    footerView.layout(left, top, left + footerView.getMeasuredWidth(), top + footerView.getMeasuredHeight());
                }
                mFooterHeightStatus = DimensionStatus.CodeExact;
                mRefreshFooter.onInitialized(mKernel, mFooterHeight, (int) (mFooterMaxDragRate * mFooterHeight));
            } else {
                mFooterHeightStatus = DimensionStatus.CodeExactUnNotify;
            }
        }
        return this;
    }

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderInsetStart(float insetDp) {
        mHeaderInsetStart = dp2px(insetDp);
        return this;
    }

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderInsetStartPx(int insetPx) {
        mHeaderInsetStart = insetPx;
        return this;
    }

    /**
     * Set the Footer's start offset.
     *
     * @see RefreshLayout#setHeaderInsetStart(float)
     */
    @Override
    public RefreshLayout setFooterInsetStart(float insetDp) {
        mFooterInsetStart = dp2px(insetDp);
        return this;
    }

    /**
     * Set the Footer's start offset.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFooterInsetStartPx(int insetPx) {
        mFooterInsetStart = insetPx;
        return this;
    }

    /**
     * Set the damping effect.
     * Display drag height ratio of true drag height (default 0.5, damping effect)
     *
     * @param rate ratio = (The drag height of the view)/(The actual drag height of the finger)
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setDragRate(float rate) {
        this.mDragRate = rate;
        return this;
    }

    /**
     * Set the ratio of the maximum height to drag header.
     *
     * @param rate ratio = (the maximum height to drag header)/(the height of header)
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderMaxDragRate(float rate) {
        this.mHeaderMaxDragRate = rate;
        if (mRefreshHeader != null && mAttachedToWindow) {
            mRefreshHeader.onInitialized(mKernel, mHeaderHeight, (int) (mHeaderMaxDragRate * mHeaderHeight));
        } else {
            mHeaderHeightStatus = mHeaderHeightStatus.unNotify();
        }
        return this;
    }

    /**
     * Set the ratio of the maximum height to drag footer.
     *
     * @param rate ratio = (the maximum height to drag footer)/(the height of footer)
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFooterMaxDragRate(float rate) {
        this.mFooterMaxDragRate = rate;
        if (mRefreshFooter != null && mAttachedToWindow) {
            mRefreshFooter.onInitialized(mKernel, mFooterHeight, (int) (mFooterHeight * mFooterMaxDragRate));
        } else {
            mFooterHeightStatus = mFooterHeightStatus.unNotify();
        }
        return this;
    }

    /**
     * Set the ratio at which the refresh is triggered.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderTriggerRate(float rate) {
        this.mHeaderTriggerRate = rate;
        return this;
    }

    /**
     * Set the ratio at which the load more is triggered.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFooterTriggerRate(float rate) {
        this.mFooterTriggerRate = rate;
        return this;
    }

    /**
     * Set the rebound interpolator.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setReboundInterpolator(@NonNull Interpolator interpolator) {
        this.mReboundInterpolator = interpolator;
        return this;
    }

    /**
     * Set the duration of the rebound animation.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setReboundDuration(int duration) {
        this.mReboundDuration = duration;
        return this;
    }

    /**
     * Set whether to enable pull-up loading more (enabled by default).
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableLoadMore(boolean enabled) {
        this.mManualLoadMore = true;
        this.mEnableLoadMore = enabled;
        return this;
    }

    /**
     * Whether to enable pull-down refresh (enabled by default)
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableRefresh(boolean enabled) {
        this.mEnableRefresh = enabled;
        return this;
    }

    /**
     * Whether to enable pull-down refresh (enabled by default).
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableHeaderTranslationContent(boolean enabled) {
        this.mEnableHeaderTranslationContent = enabled;
        this.mManualHeaderTranslationContent = true;
        return this;
    }

    /**
     * Set whether to pull up the content while pulling up the header.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableFooterTranslationContent(boolean enabled) {
        this.mEnableFooterTranslationContent = enabled;
        this.mManualFooterTranslationContent = true;
        return this;
    }

    /**
     * Sets whether to listen for the list to trigger a load event when scrolling to the bottom (default true).
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableAutoLoadMore(boolean enabled) {
        this.mEnableAutoLoadMore = enabled;
        return this;
    }

    /**
     * Set whether to enable cross-border rebound function.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableOverScrollBounce(boolean enabled) {
        this.mEnableOverScrollBounce = enabled;
        return this;
    }

    /**
     * Set whether to enable the pure scroll mode.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnablePureScrollMode(boolean enabled) {
        this.mEnablePureScrollMode = enabled;
        return this;
    }

    /**
     * Set whether to scroll the content to display new data after loading more complete.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableScrollContentWhenLoaded(boolean enabled) {
        this.mEnableScrollContentWhenLoaded = enabled;
        return this;
    }

    /**
     * Set whether to scroll the content to display new data after the refresh is complete.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableScrollContentWhenRefreshed(boolean enabled) {
        this.mEnableScrollContentWhenRefreshed = enabled;
        return this;
    }

    /**
     * Set whether to pull up and load more when the content is not full of one page.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableLoadMoreWhenContentNotFull(boolean enabled) {
        this.mEnableLoadMoreWhenContentNotFull = enabled;
        if (mRefreshContent != null) {
            mRefreshContent.setEnableLoadMoreWhenContentNotFull(enabled);
        }
        return this;
    }

    /**
     * Set whether to enable cross-border drag (imitation iphone effect).
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableOverScrollDrag(boolean enabled) {
        this.mEnableOverScrollDrag = enabled;
        return this;
    }

    /**
     * Set whether or not Footer follows the content after there is no more data.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableFooterFollowWhenNoMoreData(boolean enabled) {
        this.mEnableFooterFollowWhenNoMoreData = enabled;
        return this;
    }

    /**
     * Set whether to clip header when the Header is in the FixedBehind state.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableClipHeaderWhenFixedBehind(boolean enabled) {
        this.mEnableClipHeaderWhenFixedBehind = enabled;
        return this;
    }

    /**
     * Set whether to clip footer when the Footer is in the FixedBehind state.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableClipFooterWhenFixedBehind(boolean enabled) {
        this.mEnableClipFooterWhenFixedBehind = enabled;
        return this;
    }

    /**
     * Setting whether nesting scrolling is enabled (default off + smart on).
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setEnableNestedScroll(boolean enabled) {
        setNestedScrollingEnabled(enabled);
        return this;
    }

    /**
     * Set the view Id fixed below the Header, you can keep it from scrolling when the Footer is scrolling up and down
     *
     * @param id View ID fixed to the head
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFixedHeaderViewId(int id) {
        this.mFixedHeaderViewId = id;
        return this;
    }

    /**
     * @param id View ID fixed at the bottom
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFixedFooterViewId(int id) {
        this.mFixedFooterViewId = id;
        return this;
    }

    /**
     * Set the view ID that needs to follow the scrolling when scrolling up and down in the Header, the entire content view is by default
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setHeaderTranslationViewId(int id) {
        this.mHeaderTranslationViewId = id;
        return this;
    }

    /**
     * 设置在 Footer 上下滚动时，需要跟随滚动的视图Id，默认整个内容视图
     *
     * @param id 固定在头部的视图Id
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setFooterTranslationViewId(int id) {
        this.mFooterTranslationViewId = id;
        return this;
    }

    /**
     * Set whether to enable the action content view when refreshing.
     * 设置是否开启在刷新时候禁止操作内容视图
     *
     * @param disable 是否禁止
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setDisableContentWhenRefresh(boolean disable) {
        this.mDisableContentWhenRefresh = disable;
        return this;
    }

    /**
     * Set whether to enable the action content view when loading.
     * 设置是否开启在加载时候禁止操作内容视图
     *
     * @param disable 是否禁止
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setDisableContentWhenLoading(boolean disable) {
        this.mDisableContentWhenLoading = disable;
        return this;
    }

    /**
     * Set the header of RefreshLayout.
     * 设置指定的 Header
     *
     * @param header RefreshHeader 刷新头
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshHeader(@NonNull RefreshHeader header) {
        return setRefreshHeader(header, 0, 0);
    }

    /**
     * Set the header of RefreshLayout.
     * 设置指定的 Header
     *
     * @param header RefreshHeader 刷新头
     * @param width  the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshHeader(@NonNull RefreshHeader header, int width, int height) {
        if (mRefreshHeader != null) {
            super.removeView(mRefreshHeader.getView());
        }
        this.mRefreshHeader = header;
        this.mHeaderBackgroundColor = 0;
        this.mHeaderNeedTouchEventWhenRefreshing = false;
        this.mHeaderHeightStatus = DimensionStatus.DefaultUnNotify;
        width = width == 0 ? MATCH_PARENT : width;
        height = height == 0 ? WRAP_CONTENT : height;
        RLLayoutParams lp = new RLLayoutParams(width, height);
        Object olp = header.getView().getLayoutParams();
        if (olp instanceof RLLayoutParams) {
            lp = ((RLLayoutParams) olp);
        }
        if (mRefreshHeader.getSpinnerStyle().front) {
            final ViewGroup thisGroup = this;
            super.addView(mRefreshHeader.getView(), thisGroup.getChildCount(), lp);
        } else {
            super.addView(mRefreshHeader.getView(), 0, lp);
        }
        if (mPrimaryColors != null && mRefreshHeader != null) {
            mRefreshHeader.setPrimaryColors(mPrimaryColors);
        }
        return this;
    }

    /**
     * Set the footer of RefreshLayout.
     * 设置指定的 Footer
     *
     * @param footer RefreshFooter 刷新尾巴
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshFooter(@NonNull RefreshFooter footer) {
        return setRefreshFooter(footer, 0, 0);
    }

    /**
     * Set the footer of RefreshLayout.
     * 设置指定的 Footer
     *
     * @param footer RefreshFooter 刷新尾巴
     * @param width  the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshFooter(@NonNull RefreshFooter footer, int width, int height) {
        if (mRefreshFooter != null) {
            super.removeView(mRefreshFooter.getView());
        }
        this.mRefreshFooter = footer;
        this.mFooterLocked = false;
        this.mFooterBackgroundColor = 0;
        this.mFooterNoMoreDataEffective = false;
        this.mFooterNeedTouchEventWhenLoading = false;
        this.mFooterHeightStatus = DimensionStatus.DefaultUnNotify;//2020-5-23 修复动态切换时，不能及时测量新的高度
        this.mEnableLoadMore = !mManualLoadMore || mEnableLoadMore;
        width = width == 0 ? MATCH_PARENT : width;
        height = height == 0 ? WRAP_CONTENT : height;
        RLLayoutParams lp = new RLLayoutParams(width, height);
        Object olp = footer.getView().getLayoutParams();
        if (olp instanceof RLLayoutParams) {
            lp = ((RLLayoutParams) olp);
        }
        if (mRefreshFooter.getSpinnerStyle().front) {
            final ViewGroup thisGroup = this;
            super.addView(mRefreshFooter.getView(), thisGroup.getChildCount(), lp);
        } else {
            super.addView(mRefreshFooter.getView(), 0, lp);
        }
        if (mPrimaryColors != null && mRefreshFooter != null) {
            mRefreshFooter.setPrimaryColors(mPrimaryColors);
        }
        return this;
    }

    /**
     * Set the content of RefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）。
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     *
     * @param content View 内容视图
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshContent(@NonNull View content) {
        return setRefreshContent(content, 0, 0);
    }

    /**
     * Set the content of RefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）.
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     *
     * @param content View 内容视图
     * @param width   the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *                宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height  the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *                高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setRefreshContent(@NonNull View content, int width, int height) {
        final View thisView = this;
        if (mRefreshContent != null) {
            super.removeView(mRefreshContent.getView());
        }
        final ViewGroup thisGroup = this;
        width = width == 0 ? MATCH_PARENT : width;
        height = height == 0 ? MATCH_PARENT : height;
        RLLayoutParams lp = new RLLayoutParams(width, height);
        Object olp = content.getLayoutParams();
        if (olp instanceof RLLayoutParams) {
            lp = ((RLLayoutParams) olp);
        }
        super.addView(content, thisGroup.getChildCount(), lp);
        mRefreshContent = new RefreshContentWrapper(content);
        if (mAttachedToWindow) {
            View fixedHeaderView = thisView.findViewById(mFixedHeaderViewId);
            View fixedFooterView = thisView.findViewById(mFixedFooterViewId);
            mRefreshContent.setScrollBoundaryDecider(mScrollBoundaryDecider);
            mRefreshContent.setEnableLoadMoreWhenContentNotFull(mEnableLoadMoreWhenContentNotFull);
            mRefreshContent.setUpComponent(mKernel, fixedHeaderView, fixedFooterView);
        }

        if (mRefreshHeader != null && mRefreshHeader.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshHeader.getView());
        }
        if (mRefreshFooter != null && mRefreshFooter.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshFooter.getView());
        }
        return this;
    }

    /**
     * Get footer of RefreshLayout
     * 获取当前 Footer
     *
     * @return RefreshLayout
     */
    @Nullable
    @Override
    public RefreshFooter getRefreshFooter() {
        return mRefreshFooter instanceof RefreshFooter ? (RefreshFooter) mRefreshFooter : null;
    }

    /**
     * Get header of RefreshLayout
     * 获取当前 Header
     *
     * @return RefreshLayout
     */
    @Nullable
    @Override
    public RefreshHeader getRefreshHeader() {
        return mRefreshHeader instanceof RefreshHeader ? (RefreshHeader) mRefreshHeader : null;
    }

    /**
     * Get the current state of RefreshLayout
     * 获取当前状态
     *
     * @return RefreshLayout
     */
    @NonNull
    @Override
    public RefreshState getState() {
        return mState;
    }

    /**
     * Get the ViewGroup of RefreshLayout
     * 获取实体布局视图
     *
     * @return ViewGroup
     */
    @NonNull
    @Override
    public ViewGroup getLayout() {
        return this;
    }

    /**
     * Set refresh listener separately.
     * 单独设置刷新监听器
     *
     * @param listener OnRefreshListener 刷新监听器
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setOnRefreshListener(OnRefreshListener listener) {
        this.mRefreshListener = listener;
        return this;
    }

    /**
     * Set load more listener separately.
     * 单独设置加载监听器
     *
     * @param listener OnLoadMoreListener 加载监听器
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.mLoadMoreListener = listener;
        this.mEnableLoadMore = mEnableLoadMore || (!mManualLoadMore && listener != null);
        return this;
    }

    /**
     * Set refresh and load listeners at the same time.
     * 同时设置刷新和加载监听器
     *
     * @param listener OnRefreshLoadMoreListener 刷新加载监听器
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setOnRefreshLoadMoreListener(OnRefreshLoadMoreListener listener) {
        this.mRefreshListener = listener;
        this.mLoadMoreListener = listener;
        this.mEnableLoadMore = mEnableLoadMore || (!mManualLoadMore && listener != null);
        return this;
    }

    /**
     * Set up a multi-function listener.
     * Recommended {@link SimpleBoundaryDecider}
     * 设置滚动边界判断器
     * 建议使用 {@link SimpleBoundaryDecider}
     *
     * @param listener OnMultiListener 多功能监听器
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setOnMultiListener(OnMultiListener listener) {
        this.mOnMultiListener = listener;
        return this;
    }

    /**
     * Set theme color int (primaryColor and accentColor).
     * 设置主题颜色
     *
     * @param primaryColors ColorInt 主题颜色
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setPrimaryColors(@ColorInt int... primaryColors) {
        if (mRefreshHeader != null) {
            mRefreshHeader.setPrimaryColors(primaryColors);
        }
        if (mRefreshFooter != null) {
            mRefreshFooter.setPrimaryColors(primaryColors);
        }
        mPrimaryColors = primaryColors;
        return this;
    }

    /**
     * Set theme color id (primaryColor and accentColor).
     * 设置主题颜色
     *
     * @param primaryColorId ColorRes 主题颜色ID
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setPrimaryColorsId(@ColorRes int... primaryColorId) {
        final View thisView = this;
        final int[] colors = new int[primaryColorId.length];
        for (int i = 0; i < primaryColorId.length; i++) {
            colors[i] = ContextCompat.getColor(thisView.getContext(), primaryColorId[i]);
        }
        setPrimaryColors(colors);
        return this;
    }

    /**
     * Set the scroll boundary Decider, Can customize when you can refresh.
     * Recommended {@link SimpleBoundaryDecider}
     * 设置滚动边界判断器
     * 建议使用 {@link SimpleBoundaryDecider}
     *
     * @param boundary ScrollBoundaryDecider 判断器
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setScrollBoundaryDecider(ScrollBoundaryDecider boundary) {
        mScrollBoundaryDecider = boundary;
        if (mRefreshContent != null) {
            mRefreshContent.setScrollBoundaryDecider(boundary);
        }
        return this;
    }

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     * Restore the original state with no more data
     *
     * @param noMoreData has more data
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout setNoMoreData(boolean noMoreData) {
        if (mState == RefreshState.Refreshing && noMoreData) {
            finishRefreshWithNoMoreData();
        } else if (mState == RefreshState.Loading && noMoreData) {
            finishLoadMoreWithNoMoreData();
        } else if (mFooterNoMoreData != noMoreData) {
            mFooterNoMoreData = noMoreData;
            if (mRefreshFooter instanceof RefreshFooter) {
                if (((RefreshFooter) mRefreshFooter).setNoMoreData(noMoreData)) {
                    mFooterNoMoreDataEffective = true;
                    if (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mSpinner > 0 && mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate && isEnableRefreshOrLoadMore(mEnableLoadMore) && isEnableTranslationContent(mEnableRefresh, mRefreshHeader)) {
                        mRefreshFooter.getView().setTranslationY(mSpinner);
                    }
                } else {
                    mFooterNoMoreDataEffective = false;
                }
            }

        }
        return this;
    }

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout resetNoMoreData() {
        return setNoMoreData(false);
    }

    /**
     * finish refresh.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishRefresh() {
        return finishRefresh(true);
    }

    /**
     * finish load more.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishLoadMore() {
        return finishLoadMore(true);
    }

    /**
     * finish refresh.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishRefresh(int delayed) {
        return finishRefresh(delayed, true, Boolean.FALSE);
    }

    /**
     * finish refresh.
     *
     * @param success Whether the data is refreshed successfully (it will affect the change of the last update time)
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishRefresh(boolean success) {
        if (success) {
            long passTime = System.currentTimeMillis() - mLastOpenTime;
            int delayed = (Math.min(Math.max(0, 300 - (int) passTime), 300) << 16);
            return finishRefresh(delayed, true, Boolean.FALSE);
        } else {
            return finishRefresh(0, false, null);
        }
    }

    /**
     * finish refresh.
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishRefresh(final int delayed, final boolean success, final Boolean noMoreData) {
        final int more = delayed >> 16;//Animation remaining delay
        int delay = delayed << 16 >> 16;//User specified delay
        Runnable runnable = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count == 0) {
                    if (mState == RefreshState.None && mViceState == RefreshState.Refreshing) {
                        //autoRefresh will be executed but not started
                        mViceState = RefreshState.None;
                    } else if (reboundAnimator != null && mState.isHeader && (mState.isDragging || mState == RefreshState.RefreshReleased)) {
                        //autoRefresh executing
                        reboundAnimator.setDuration(0);//cancel会触发End调用，可以判断0来确定是否被cancel
                        reboundAnimator.cancel();//会触发 cancel 和 end 调用
                        reboundAnimator = null;
                        if (mKernel.animSpinner(0) == null) {
                            notifyStateChanged(RefreshState.None);
                        } else {
                            notifyStateChanged(RefreshState.PullDownCanceled);
                        }
                    } else if (mState == RefreshState.Refreshing && mRefreshHeader != null && mRefreshContent != null) {
                        count++;
                        mHandler.postDelayed(this, more);
                        //提前设置 状态为 RefreshFinish 防止 postDelayed 导致 finishRefresh 过后，外部判断 state 还是 Refreshing
                        notifyStateChanged(RefreshState.RefreshFinish);
                        if (noMoreData == Boolean.FALSE) {
                            setNoMoreData(false);//真正有刷新状态的时候才可以重置 noMoreData
                        }
                    }
                    if (noMoreData == Boolean.TRUE) {
                        setNoMoreData(true);
                    }
                } else {
                    int startDelay = mRefreshHeader.onFinish(RefreshLayout.this, success);
                    if (mOnMultiListener != null && mRefreshHeader instanceof RefreshHeader) {
                        mOnMultiListener.onHeaderFinish((RefreshHeader) mRefreshHeader, success);
                    }
                    //startDelay < Integer.MAX_VALUE 表示 延时 startDelay 毫秒之后，回弹关闭刷新
                    if (startDelay < Integer.MAX_VALUE) {
                        //如果正在拖动的话，偏移初始点击事件 【两种情况都是结束刷新时，手指还按住屏幕不放手哦】
                        if (mIsBeingDragged || mNestedInProgress) {
                            long time = System.currentTimeMillis();
                            if (mIsBeingDragged) {
                                mTouchY = mLastTouchY;
                                mTouchSpinner = 0;
                                mIsBeingDragged = false;
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_DOWN, mLastTouchX, mLastTouchY + mSpinner - mTouchSlop * 2, 0));
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_MOVE, mLastTouchX, mLastTouchY + mSpinner, 0));
                            }
                            if (mNestedInProgress) {
                                mTotalUnconsumed = 0;
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_UP, mLastTouchX, mLastTouchY, 0));
                                mNestedInProgress = false;
                                mTouchSpinner = 0;
                            }
                        }
                        if (mSpinner > 0) {
                            AnimatorUpdateListener updateListener = null;
                            ValueAnimator valueAnimator = animSpinner(0, startDelay, mReboundInterpolator, mReboundDuration);
                            if (mEnableScrollContentWhenRefreshed) {
                                updateListener = mRefreshContent.scrollContentWhenFinished(mSpinner);
                            }
                            if (valueAnimator != null && updateListener != null) {
                                valueAnimator.addUpdateListener(updateListener);
                            }
                        } else if (mSpinner < 0) {
                            animSpinner(0, startDelay, mReboundInterpolator, mReboundDuration);
                        } else {
                            mKernel.moveSpinner(0, false);
                            //                            resetStatus();
                            mKernel.setState(RefreshState.None);
                        }
                    }
                }
            }
        };
        if (delay > 0) {
            mHandler.postDelayed(runnable, delay);
        } else {
            runnable.run();
        }
        return this;
    }

    /**
     * finish load more with no more data.
     * 完成刷新并标记没有更多数据
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishRefreshWithNoMoreData() {
        long passTime = System.currentTimeMillis() - mLastOpenTime;
        return finishRefresh((Math.min(Math.max(0, 300 - (int) passTime), 300) << 16), true, Boolean.TRUE);
    }

    /**
     * finish load more.
     * 完成加载
     *
     * @param delayed 开始延时
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishLoadMore(int delayed) {
        return finishLoadMore(delayed, true, false);
    }

    /**
     * finish load more.
     * 完成加载
     *
     * @param success 数据是否成功
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishLoadMore(boolean success) {
        long passTime = System.currentTimeMillis() - mLastOpenTime;
        return finishLoadMore(success ? (Math.min(Math.max(0, 300 - (int) passTime), 300) << 16) : 0, success, false);
    }

    /**
     * finish load more.
     * 完成加载
     *
     * @param delayed    开始延时
     * @param success    数据是否成功
     * @param noMoreData 是否有更多数据
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishLoadMore(final int delayed, final boolean success, final boolean noMoreData) {
        final int more = delayed >> 16;//动画剩余延时
        int delay = delayed << 16 >> 16;//用户指定延时
        Runnable runnable = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count == 0) {
                    if (mState == RefreshState.None && mViceState == RefreshState.Loading) {
                        //autoLoadMore 即将执行，但未开始
                        mViceState = RefreshState.None;
                    } else if (reboundAnimator != null && (mState.isDragging || mState == RefreshState.LoadReleased) && mState.isFooter) {
                        //autoLoadMore 正在执行，但未结束
                        reboundAnimator.setDuration(0);//cancel会触发End调用，可以判断0来确定是否被cancel
                        reboundAnimator.cancel();//会触发 cancel 和 end 调用
                        reboundAnimator = null;
                        if (mKernel.animSpinner(0) == null) {
                            notifyStateChanged(RefreshState.None);
                        } else {
                            notifyStateChanged(RefreshState.PullUpCanceled);
                        }
                        //mKernel.setState(RefreshState.None);
                    } else if (mState == RefreshState.Loading && mRefreshFooter != null && mRefreshContent != null) {
                        count++;
                        mHandler.postDelayed(this, more);
                        //提前设置 状态为 LoadFinish 防止 postDelayed 导致 finishLoadMore 过后，外部判断 state 还是 Loading
                        notifyStateChanged(RefreshState.LoadFinish);
                        return;
                    }
                    if (noMoreData) {
                        setNoMoreData(true);
                    }
                } else {
                    final int startDelay = mRefreshFooter.onFinish(RefreshLayout.this, success);
                    if (mOnMultiListener != null && mRefreshFooter instanceof RefreshFooter) {
                        mOnMultiListener.onFooterFinish((RefreshFooter) mRefreshFooter, success);
                    }
                    if (startDelay < Integer.MAX_VALUE) {
                        //计算布局将要移动的偏移量
                        final boolean needHoldFooter = noMoreData && mEnableFooterFollowWhenNoMoreData && mSpinner < 0 && mRefreshContent.canLoadMore();
                        final int offset = mSpinner - (needHoldFooter ? Math.max(mSpinner, -mFooterHeight) : 0);
                        //如果正在拖动的话，偏移初始点击事件
                        if (mIsBeingDragged || mNestedInProgress) {
                            final long time = System.currentTimeMillis();
                            if (mIsBeingDragged) {
                                mTouchY = mLastTouchY;
                                mTouchSpinner = mSpinner - offset;
                                mIsBeingDragged = false;
                                int offsetY = mEnableFooterTranslationContent ? offset : 0;
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_DOWN, mLastTouchX, mLastTouchY + offsetY + mTouchSlop * 2, 0));
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_MOVE, mLastTouchX, mLastTouchY + offsetY, 0));
                            }
                            if (mNestedInProgress) {
                                mTotalUnconsumed = 0;
                                RefreshLayout.super.dispatchTouchEvent(obtain(time, time, MotionEvent.ACTION_UP, mLastTouchX, mLastTouchY, 0));
                                mNestedInProgress = false;
                                mTouchSpinner = 0;
                            }
                        }
                        //准备：偏移并结束状态
                        mHandler.postDelayed(() -> {
                            AnimatorUpdateListener updateListener = null;
                            if (mEnableScrollContentWhenLoaded && offset < 0) {
                                updateListener = mRefreshContent.scrollContentWhenFinished(mSpinner);
                                if (updateListener != null) {//如果内容需要滚动显示新数据
                                    updateListener.onAnimationUpdate(ValueAnimator.ofInt(0, 0));//直接滚动, Footer 的距离
                                }
                            }
                            ValueAnimator animator = null;//动议动画和动画结束回调
                            AnimatorListenerAdapter listenerAdapter = new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (animation != null && animation.getDuration() == 0) {
                                        return;//0 表示被取消
                                    }
                                    mFooterLocked = false;
                                    if (noMoreData) {
                                        setNoMoreData(true);
                                    }
                                    if (mState == RefreshState.LoadFinish) {
                                        notifyStateChanged(RefreshState.None);
                                    }
                                }
                            };
                            if (mSpinner > 0) { //大于0表示下拉, 这是 Header 可见, Footer 不可见
                                animator = mKernel.animSpinner(0);//关闭 Header 回到原始状态
                            } else if (updateListener != null || mSpinner == 0) {//如果 Header 和 Footer 都不可见 或者内容需要滚动显示新内容
                                if (reboundAnimator != null) {
                                    reboundAnimator.setDuration(0);//cancel会触发End调用，可以判断0来确定是否被cancel
                                    reboundAnimator.cancel();//会触发 cancel 和 end 调用
                                    reboundAnimator = null;//取消之前的任何动画
                                }
                                //直接关闭 Header 或者 Header 到原始状态
                                mKernel.moveSpinner(0, false);
                                mKernel.setState(RefreshState.None);
                            } else {//准备按正常逻辑关闭Footer
                                if (noMoreData && mEnableFooterFollowWhenNoMoreData) {//如果需要显示没有更多数据
                                    if (mSpinner >= -mFooterHeight) {//如果 Footer 的位置再可见范围内
                                        notifyStateChanged(RefreshState.None);//直接通知重置状态,不关闭 Footer
                                    } else {//如果 Footer 的位置超出 Footer 显示高度 (这个情况的概率应该很低, 手指故意拖拽 Footer 向上超出原位置时会触发)
                                        animator = mKernel.animSpinner(-mFooterHeight);//通过动画让 Footer 回到全显示状态位置
                                    }
                                } else {
                                    animator = mKernel.animSpinner(0);//动画正常关闭 Footer
                                }
                            }
                            if (animator != null) {
                                animator.addListener(listenerAdapter);//如果通过动画关闭,绑定动画结束回调
                            } else {
                                listenerAdapter.onAnimationEnd(null);//如果没有动画,立即执行结束回调(必须逻辑)
                            }
                        }, mSpinner < 0 ? startDelay : 0);
                    }
                }
            }
        };
        if (delay > 0) {
            mHandler.postDelayed(runnable, delay);
        } else {
            runnable.run();
        }
        return this;
    }

    /**
     * finish load more with no more data.
     * 完成加载并标记没有更多数据
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout finishLoadMoreWithNoMoreData() {
        long passTime = System.currentTimeMillis() - mLastOpenTime;
        return finishLoadMore((Math.min(Math.max(0, 300 - (int) passTime), 300) << 16), true, true);
    }

    /**
     * Close the Header or Footer, can't replace finishRefresh and finishLoadMore.
     * 关闭 Header 或者 Footer
     * 注意：
     * 1.closeHeaderOrFooter 任何时候任何状态都能关闭  header 和 footer
     * 2.finishRefresh 和 finishLoadMore 只能在 刷新 或者 加载 的时候关闭
     *
     * @return RefreshLayout
     */
    @Override
    public RefreshLayout closeHeaderOrFooter() {
        if (mState == RefreshState.None && (mViceState == RefreshState.Refreshing || mViceState == RefreshState.Loading)) {
            //autoRefresh autoLoadMore 即将执行，但未开始
            mViceState = RefreshState.None;
        }
        if (mState == RefreshState.Refreshing) {
            finishRefresh();
        } else if (mState == RefreshState.Loading) {
            finishLoadMore();
        } else {
            /*
             * 2020-3-15 closeHeaderOrFooter 的关闭逻辑，
             * 帮助 FalsifyHeader 取消刷新
             * 邦族 FalsifyFooter 取消加载
             */
            if (mKernel.animSpinner(0) == null) {
                notifyStateChanged(RefreshState.None);
            } else {
                if (mState.isHeader) {
                    notifyStateChanged(RefreshState.PullDownCanceled);
                } else {
                    notifyStateChanged(RefreshState.PullUpCanceled);
                }
            }
        }
        return this;
    }

    /**
     * Display refresh animation and trigger refresh event.
     * 显示刷新动画并且触发刷新事件
     *
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoRefresh() {
        return autoRefresh(mAttachedToWindow ? 0 : 400, mReboundDuration, 1f * ((mHeaderMaxDragRate / 2 + 0.5f) * mHeaderHeight) / (mHeaderHeight == 0 ? 1 : mHeaderHeight), false);
    }

    /**
     * Display refresh animation and trigger refresh event, Delayed start.
     * 显示刷新动画并且触发刷新事件，延时启动
     *
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoRefresh(int delayed) {
        return autoRefresh(delayed, mReboundDuration, 1f * ((mHeaderMaxDragRate / 2 + 0.5f) * mHeaderHeight) / (mHeaderHeight == 0 ? 1 : mHeaderHeight), false);
    }


    /**
     * Display refresh animation without triggering events.
     * 显示刷新动画，不触发事件
     *
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoRefreshAnimationOnly() {
        return autoRefresh(mAttachedToWindow ? 0 : 400, mReboundDuration, 1f * ((mHeaderMaxDragRate / 2 + 0.5f) * mHeaderHeight) / (mHeaderHeight == 0 ? 1 : mHeaderHeight), true);
    }

    /**
     * Display refresh animation, Multifunction.
     * 显示刷新动画并且触发刷新事件
     *
     * @param delayed       开始延时
     * @param duration      拖拽动画持续时间
     * @param dragRate      拉拽的高度比率
     * @param animationOnly animation only 只有动画
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoRefresh(int delayed, final int duration, final float dragRate, final boolean animationOnly) {
        if (mState == RefreshState.None && isEnableRefreshOrLoadMore(mEnableRefresh)) {
            Runnable runnable = () -> {
                if (mViceState != RefreshState.Refreshing) return;
                if (reboundAnimator != null) {
                    reboundAnimator.setDuration(0);//cancel会触发End调用，可以判断0来确定是否被cancel
                    reboundAnimator.cancel();//会触发 cancel 和 end 调用
                    reboundAnimator = null;
                }

                final View thisView = RefreshLayout.this;
                mLastTouchX = thisView.getMeasuredWidth() / 2f;
                mKernel.setState(RefreshState.PullDownToRefresh);

                reboundAnimator = ValueAnimator.ofInt(mSpinner, (int) (mHeaderHeight * dragRate));
                reboundAnimator.setDuration(duration);
                reboundAnimator.setInterpolator(new SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID));
                reboundAnimator.addUpdateListener(animation -> {
                    if (reboundAnimator != null && mRefreshHeader != null) {
                        mKernel.moveSpinner((int) animation.getAnimatedValue(), true);
                    }
                });
                reboundAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animation != null && animation.getDuration() == 0) {
                            return;//0 表示被取消
                        }
                        reboundAnimator = null;
                        if (mRefreshHeader != null) {
                            if (mState != RefreshState.ReleaseToRefresh) {
                                mKernel.setState(RefreshState.ReleaseToRefresh);
                            }
                            setStateRefreshing(!animationOnly);
                        } else {
                            mKernel.setState(RefreshState.None);
                        }
                    }
                });
                reboundAnimator.start();
            };
            setViceState(RefreshState.Refreshing);
            if (delayed > 0) {
                mHandler.postDelayed(runnable, delayed);
            } else {
                runnable.run();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Display load more animation and trigger load more event.
     * 显示加载动画并且触发刷新事件
     *
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoLoadMore() {
        return autoLoadMore(0, mReboundDuration, 1f * (mFooterHeight * (mFooterMaxDragRate / 2 + 0.5f)) / (mFooterHeight == 0 ? 1 : mFooterHeight), false);
    }

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画并且触发刷新事件, 延时启动
     *
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoLoadMore(int delayed) {
        return autoLoadMore(delayed, mReboundDuration, 1f * (mFooterHeight * (mFooterMaxDragRate / 2 + 0.5f)) / (mFooterHeight == 0 ? 1 : mFooterHeight), false);
    }

    /**
     * Display load more animation without triggering events.
     * 显示加载动画，不触发事件
     *
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoLoadMoreAnimationOnly() {
        return autoLoadMore(0, mReboundDuration, 1f * (mFooterHeight * (mFooterMaxDragRate / 2 + 0.5f)) / (mFooterHeight == 0 ? 1 : mFooterHeight), true);
    }

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画, 多功能选项
     *
     * @param delayed  开始延时
     * @param duration 拖拽动画持续时间
     * @param dragRate 拉拽的高度比率
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    @Override
    public boolean autoLoadMore(int delayed, final int duration, final float dragRate, final boolean animationOnly) {
        if (mState == RefreshState.None && (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mFooterNoMoreData)) {
            Runnable runnable = () -> {
                if (mViceState != RefreshState.Loading) return;
                if (reboundAnimator != null) {
                    reboundAnimator.setDuration(0);
                    reboundAnimator.cancel();
                    reboundAnimator = null;
                }

                final View thisView = RefreshLayout.this;
                mLastTouchX = thisView.getMeasuredWidth() / 2f;
                mKernel.setState(RefreshState.PullUpToLoad);

                reboundAnimator = ValueAnimator.ofInt(mSpinner, -(int) (mFooterHeight * dragRate));
                reboundAnimator.setDuration(duration);
                reboundAnimator.setInterpolator(new SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID));
                reboundAnimator.addUpdateListener(animation -> {
                    if (reboundAnimator != null && mRefreshFooter != null) {
                        mKernel.moveSpinner((int) animation.getAnimatedValue(), true);
                    }
                });
                reboundAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animation != null && animation.getDuration() == 0) {
                            return;//0 表示被取消
                        }
                        reboundAnimator = null;
                        if (mRefreshFooter != null) {
                            if (mState != RefreshState.ReleaseToLoad) {
                                mKernel.setState(RefreshState.ReleaseToLoad);
                            }
                            setStateLoading(!animationOnly);
                        } else {
                            mKernel.setState(RefreshState.None);
                        }
                    }
                });
                reboundAnimator.start();
            };
            setViceState(RefreshState.Loading);
            if (delayed > 0) {
                mHandler.postDelayed(runnable, delayed);
            } else {
                runnable.run();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置默认 Header 构建器
     *
     * @param creator Header构建器
     */
    public static void setDefaultRefreshHeaderCreator(@NonNull DefaultRefreshHeaderCreator creator) {
        sHeaderCreator = creator;
    }

    /**
     * 设置默认 Footer 构建器
     *
     * @param creator Footer构建器
     */
    public static void setDefaultRefreshFooterCreator(@NonNull DefaultRefreshFooterCreator creator) {
        sFooterCreator = creator;
    }

    /**
     * 设置默认 Refresh 初始化器
     *
     * @param initializer 全局初始化器
     */
    public static void setDefaultRefreshInitializer(@NonNull DefaultRefreshInitializer initializer) {
        sRefreshInitializer = initializer;
    }


    /**
     * 是否正在刷新
     *
     * @return 是否正在刷新
     */
    @Override
    public boolean isRefreshing() {
        return mState == RefreshState.Refreshing;
    }

    /**
     * 是否正在加载
     *
     * @return 是否正在加载
     */
    @Override
    public boolean isLoading() {
        return mState == RefreshState.Loading;
    }

    /**
     * 刷新布局核心功能接口
     * 为功能复杂的 Header 或者 Footer 开放的接口
     */
    public class RefreshKernelImpl implements RefreshKernel {

        @NonNull
        @Override
        public RefreshLayoutIn getRefreshLayout() {
            return RefreshLayout.this;
        }

        @NonNull
        @Override
        public RefreshContent getRefreshContent() {
            return mRefreshContent;
        }

        @Override
        public RefreshKernel setState(@NonNull RefreshState state) {
            switch (state) {
                case None:
                    if (mState != RefreshState.None && mSpinner == 0) {
                        notifyStateChanged(RefreshState.None);
                    } else if (mSpinner != 0) {
                        animSpinner(0);
                    }
                    break;
                case PullDownToRefresh:
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.PullDownToRefresh);
                    } else {
                        setViceState(RefreshState.PullDownToRefresh);
                    }
                    break;
                case PullUpToLoad:
                    if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !mState.isFinishing && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                        notifyStateChanged(RefreshState.PullUpToLoad);
                    } else {
                        setViceState(RefreshState.PullUpToLoad);
                    }
                    break;
                case PullDownCanceled:
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.PullDownCanceled);
                        //                        resetStatus();
                        setState(RefreshState.None);
                    } else {
                        setViceState(RefreshState.PullDownCanceled);
                    }
                    break;
                case PullUpCanceled:
                    if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                        notifyStateChanged(RefreshState.PullUpCanceled);
                        //                        resetStatus();
                        setState(RefreshState.None);
                    } else {
                        setViceState(RefreshState.PullUpCanceled);
                    }
                    break;
                case ReleaseToRefresh:
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.ReleaseToRefresh);
                    } else {
                        setViceState(RefreshState.ReleaseToRefresh);
                    }
                    break;
                case ReleaseToLoad:
                    if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !mState.isFinishing && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                        notifyStateChanged(RefreshState.ReleaseToLoad);
                    } else {
                        setViceState(RefreshState.ReleaseToLoad);
                    }
                    break;
                case ReleaseToTwoLevel: {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.ReleaseToTwoLevel);
                    } else {
                        setViceState(RefreshState.ReleaseToTwoLevel);
                    }
                    break;
                }
                case RefreshReleased: {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.RefreshReleased);
                    } else {
                        setViceState(RefreshState.RefreshReleased);
                    }
                    break;
                }
                case LoadReleased: {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                        notifyStateChanged(RefreshState.LoadReleased);
                    } else {
                        setViceState(RefreshState.LoadReleased);
                    }
                    break;
                }
                case Refreshing:
                    setStateRefreshing(true);
                    break;
                case Loading:
                    setStateLoading(true);
                    break;
                default:
                    notifyStateChanged(state);
                    break;
            }
            return null;
        }

        @Override
        public RefreshKernel startTwoLevel(boolean open) {
            if (open) {
                AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animation != null && animation.getDuration() == 0) {
                            return;//0 means cancelled
                        }
                        mKernel.setState(RefreshState.TwoLevel);
                    }
                };
                final View thisView = RefreshLayout.this;
                ValueAnimator animator = animSpinner(thisView.getMeasuredHeight());
                if (animator != null && animator == reboundAnimator) {
                    animator.setDuration(mFloorDuration);
                    animator.addListener(listener);
                } else {
                    listener.onAnimationEnd(null);
                }
            } else {
                if (animSpinner(0) == null) {
                    notifyStateChanged(RefreshState.None);
                }
            }
            return this;
        }

        @Override
        public RefreshKernel finishTwoLevel() {
            if (mState == RefreshState.TwoLevel) {
                mKernel.setState(RefreshState.TwoLevelFinish);
                if (mSpinner == 0) {
                    moveSpinner(0, false);
                    notifyStateChanged(RefreshState.None);
                } else {
                    animSpinner(0).setDuration(mFloorDuration);
                }
            }
            return this;
        }

        /**
         * The name of Scroll moveSpinner comes from Google's official {android.support.v4.widget.SwipeRefreshLayoutmoveSpinner(float)}
         * moveSpinner The name comes from {android.support.v4.widget.SwipeRefreshLayoutmoveSpinner(float)}
         *
         * @param spinner    new spinner
         * @param isDragging Whether it is scrolling caused by dragging,
         *                   only the rebound animation of finishRefresh, finishLoadMore, overSpinner will be false, dispatchTouchEvent, nestScroll, etc. are all true autoRefresh, autoLoadMore, need to simulate dragging, also true
         */
        public RefreshKernel moveSpinner(final int spinner, final boolean isDragging) {
            if (mSpinner == spinner && (mRefreshHeader == null || !mRefreshHeader.isSupportHorizontalDrag()) && (mRefreshFooter == null || !mRefreshFooter.isSupportHorizontalDrag())) {
                return this;
            }
            final View thisView = RefreshLayout.this;
            final int oldSpinner = mSpinner;
            mSpinner = spinner;
            // 附加 mViceState.isDragging 的判断，是因为 isDragging 有时候时动画模拟的，如 autoRefresh 动画
            //
            if (isDragging && (mViceState.isDragging || mViceState.isOpening)) {
                if (mSpinner > mHeaderHeight * mHeaderTriggerRate) {
                    if (mState != RefreshState.ReleaseToTwoLevel) {
                        mKernel.setState(RefreshState.ReleaseToRefresh);
                    }
                } else if (-mSpinner > mFooterHeight * mFooterTriggerRate && !mFooterNoMoreData) {
                    mKernel.setState(RefreshState.ReleaseToLoad);
                } else if (mSpinner < 0 && !mFooterNoMoreData) {
                    mKernel.setState(RefreshState.PullUpToLoad);
                } else if (mSpinner > 0) {
                    mKernel.setState(RefreshState.PullDownToRefresh);
                }
            }
            if (mRefreshContent != null) {
                int tSpinner = 0;
                boolean changed = false;
                if (spinner >= 0 /*&& mRefreshHeader != null*/) {
                    if (isEnableTranslationContent(mEnableHeaderTranslationContent, mRefreshHeader)) {
                        changed = true;
                        tSpinner = spinner;
                    } else if (oldSpinner < 0) {
                        changed = true;
                        tSpinner = 0;
                    }
                }
                if (spinner <= 0 /*&& mRefreshFooter != null*/) {
                    if (isEnableTranslationContent(mEnableFooterTranslationContent, mRefreshFooter)) {
                        changed = true;
                        tSpinner = spinner;
                    } else if (oldSpinner > 0) {
                        changed = true;
                        tSpinner = 0;
                    }
                }
                if (changed) {
                    mRefreshContent.moveSpinner(tSpinner, mHeaderTranslationViewId, mFooterTranslationViewId);
                    if (mFooterNoMoreData && mFooterNoMoreDataEffective && mEnableFooterFollowWhenNoMoreData && mRefreshFooter instanceof RefreshFooter && mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                        mRefreshFooter.getView().setTranslationY(Math.max(0, tSpinner));
                    }
                    boolean header = mEnableClipHeaderWhenFixedBehind && mRefreshHeader != null && mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind;
                    header = header || mHeaderBackgroundColor != 0;
                    boolean footer = mEnableClipFooterWhenFixedBehind && mRefreshFooter != null && mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind;
                    footer = footer || mFooterBackgroundColor != 0;
                    if ((header && (tSpinner >= 0 || oldSpinner > 0)) || (footer && (tSpinner <= 0 || oldSpinner < 0))) {
                        thisView.invalidate();
                    }
                }
            }
            if ((spinner >= 0 || oldSpinner > 0) && mRefreshHeader != null) {

                final int offset = Math.max(spinner, 0);
                final int headerHeight = mHeaderHeight;
                final int maxDragHeight = (int) (mHeaderHeight * mHeaderMaxDragRate);
                final float percent = 1f * offset / (mHeaderHeight == 0 ? 1 : mHeaderHeight);
                //Because the user may directly enable=false to close after finish, so the state judgment should be added
                if (isEnableRefreshOrLoadMore(mEnableRefresh) || (mState == RefreshState.RefreshFinish && !isDragging)) {
                    if (oldSpinner != mSpinner) {
                        if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
                            mRefreshHeader.getView().setTranslationY(mSpinner);
                            if (mHeaderBackgroundColor != 0 && mPaint != null && !isEnableTranslationContent(mEnableHeaderTranslationContent, mRefreshHeader)) {
                                thisView.invalidate();
                            }
                        } else if (mRefreshHeader.getSpinnerStyle().scale) {
                            View headerView = mRefreshHeader.getView();
                            final ViewGroup.LayoutParams lp = headerView.getLayoutParams();
                            final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                            final int widthSpec = makeMeasureSpec(headerView.getMeasuredWidth(), EXACTLY);
                            headerView.measure(widthSpec, makeMeasureSpec(Math.max(mSpinner - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                            final int left = mlp.leftMargin;
                            final int top = mlp.topMargin + mHeaderInsetStart;
                            headerView.layout(left, top, left + headerView.getMeasuredWidth(), top + headerView.getMeasuredHeight());
                        }
                        mRefreshHeader.onMoving(isDragging, percent, offset, headerHeight, maxDragHeight);
                    }
                    if (isDragging && mRefreshHeader.isSupportHorizontalDrag()) {
                        final int offsetX = (int) mLastTouchX;
                        final int offsetMax = thisView.getWidth();
                        final float percentX = mLastTouchX / (offsetMax == 0 ? 1 : offsetMax);
                        mRefreshHeader.onHorizontalDrag(percentX, offsetX, offsetMax);
                    }
                }

                if (oldSpinner != mSpinner && mOnMultiListener != null && mRefreshHeader instanceof RefreshHeader) {
                    mOnMultiListener.onHeaderMoving((RefreshHeader) mRefreshHeader, isDragging, percent, offset, headerHeight, maxDragHeight);
                }

            }
            if ((spinner <= 0 || oldSpinner < 0) && mRefreshFooter != null) {

                final int offset = -Math.min(spinner, 0);
                final int footerHeight = mFooterHeight;
                final int maxDragHeight = (int) (mFooterHeight * mFooterMaxDragRate);
                final float percent = offset * 1f / (mFooterHeight == 0 ? 1 : mFooterHeight);

                if (isEnableRefreshOrLoadMore(mEnableLoadMore) || (mState == RefreshState.LoadFinish && !isDragging)) {
                    if (oldSpinner != mSpinner) {
                        if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate) {
                            mRefreshFooter.getView().setTranslationY(mSpinner);
                            if (mFooterBackgroundColor != 0 && mPaint != null && !isEnableTranslationContent(mEnableFooterTranslationContent, mRefreshFooter)) {
                                thisView.invalidate();
                            }
                        } else if (mRefreshFooter.getSpinnerStyle().scale) {
                            View footerView = mRefreshFooter.getView();
                            final ViewGroup.LayoutParams lp = footerView.getLayoutParams();
                            final MarginLayoutParams mlp = lp instanceof MarginLayoutParams ? (MarginLayoutParams) lp : sDefaultMarginLP;
                            final int widthSpec = makeMeasureSpec(footerView.getMeasuredWidth(), EXACTLY);
                            footerView.measure(widthSpec, makeMeasureSpec(Math.max(-mSpinner - mlp.bottomMargin - mlp.topMargin, 0), EXACTLY));
                            final int left = mlp.leftMargin;
                            final int bottom = mlp.topMargin + thisView.getMeasuredHeight() - mFooterInsetStart;
                            footerView.layout(left, bottom - footerView.getMeasuredHeight(), left + footerView.getMeasuredWidth(), bottom);
                        }
                        mRefreshFooter.onMoving(isDragging, percent, offset, footerHeight, maxDragHeight);
                    }
                    if (isDragging && mRefreshFooter.isSupportHorizontalDrag()) {
                        final int offsetX = (int) mLastTouchX;
                        final int offsetMax = thisView.getWidth();
                        final float percentX = mLastTouchX / (offsetMax == 0 ? 1 : offsetMax);
                        mRefreshFooter.onHorizontalDrag(percentX, offsetX, offsetMax);
                    }
                }

                if (oldSpinner != mSpinner && mOnMultiListener != null && mRefreshFooter instanceof RefreshFooter) {
                    mOnMultiListener.onFooterMoving((RefreshFooter) mRefreshFooter, isDragging, percent, offset, footerHeight, maxDragHeight);
                }
            }
            return this;
        }

        public ValueAnimator animSpinner(int endSpinner) {
            return RefreshLayout.this.animSpinner(endSpinner, 0, mReboundInterpolator, mReboundDuration);
        }

        @Override
        public RefreshKernel requestDrawBackgroundFor(@NonNull RefreshComponent internal, int backgroundColor) {
            if (mPaint == null && backgroundColor != 0) {
                mPaint = new Paint();
            }
            if (internal.equals(mRefreshHeader)) {
                mHeaderBackgroundColor = backgroundColor;
            } else if (internal.equals(mRefreshFooter)) {
                mFooterBackgroundColor = backgroundColor;
            }
            return this;
        }

        @Override
        public RefreshKernel requestNeedTouchEventFor(@NonNull RefreshComponent internal, boolean request) {
            if (internal.equals(mRefreshHeader)) {
                mHeaderNeedTouchEventWhenRefreshing = request;
            } else if (internal.equals(mRefreshFooter)) {
                mFooterNeedTouchEventWhenLoading = request;
            }
            return this;
        }

        @Override
        public RefreshKernel requestDefaultTranslationContentFor(@NonNull RefreshComponent internal, boolean translation) {
            if (internal.equals(mRefreshHeader)) {
                if (!mManualHeaderTranslationContent) {
                    mManualHeaderTranslationContent = true;
                    mEnableHeaderTranslationContent = translation;
                }
            } else if (internal.equals(mRefreshFooter)) {
                if (!mManualFooterTranslationContent) {
                    mManualFooterTranslationContent = true;
                    mEnableFooterTranslationContent = translation;
                }
            }
            return this;
        }

        @Override
        public RefreshKernel requestRemeasureHeightFor(@NonNull RefreshComponent internal) {
            if (internal.equals(mRefreshHeader)) {
                if (mHeaderHeightStatus.notified) {
                    mHeaderHeightStatus = mHeaderHeightStatus.unNotify();
                }
            } else if (internal.equals(mRefreshFooter)) {
                if (mFooterHeightStatus.notified) {
                    mFooterHeightStatus = mFooterHeightStatus.unNotify();
                }
            }
            return this;
        }

        @Override
        public RefreshKernel requestFloorDuration(int duration) {
            mFloorDuration = duration;
            return this;
        }

        @Override
        public RefreshKernel requestFloorBottomPullUpToCloseRate(float rate) {
            mTwoLevelBottomPullUpToCloseRate = rate;
            return this;
        }
    }
}
