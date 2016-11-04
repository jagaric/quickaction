package me.piruin.quickaction;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StyleRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * QuickAction dialog, shows action list as icon and text like the one in Gallery3D app. Currently
 * supports vertical and horizontal layout.
 *
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 *
 *         Contributors: - Kevin Peck <kevinwpeck@gmail.com>
 */
public class QuickAction extends PopupWindows implements OnDismissListener {

  public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
  public static final int VERTICAL = LinearLayout.VERTICAL;

  private View mRootView;
  private ImageView mArrowUp;
  private ImageView mArrowDown;
  private LayoutInflater mInflater;
  private LinearLayout mTrack;
  private ScrollView mScroller;
  private OnActionItemClickListener mItemClickListener;
  private OnDismissListener mDismissListener;
  private List<ActionItem> actionItems = new ArrayList<ActionItem>();
  private boolean mDidAction;
  private int mChildPos = 0;
  private int mInsertPos;
  private Animation mAnimStyle = Animation.ANIM_AUTO;
  private int mOrientation;
  private int rootWidth = 0;
  private int mTextColor = Color.BLACK;

  /**
   * Constructor for default vertical layout
   *
   * @param context Context
   */
  public QuickAction(Context context) {
    this(context, VERTICAL);
  }

  /**
   * Constructor allowing orientation override
   *
   * @param context Context
   * @param orientation Layout orientation, can be vartical or horizontal
   */
  public QuickAction(Context context, int orientation) {
    super(context);
    mOrientation = orientation;
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    setRootViewId(R.layout.popup);
    setColor(Color.WHITE);
  }

  /**
   * Set root view.
   *
   * @param id Layout resource id
   */
  public void setRootViewId(@LayoutRes int id) {
    mRootView = mInflater.inflate(id, null);
    mTrack = (LinearLayout)mRootView.findViewById(R.id.tracks);
    mTrack.setOrientation(mOrientation);

    mArrowDown = (ImageView)mRootView.findViewById(R.id.arrow_down);
    mArrowUp = (ImageView)mRootView.findViewById(R.id.arrow_up);

    mScroller = (ScrollView)mRootView.findViewById(R.id.scroller);

    // This was previously defined on show() method, moved here to prevent
    // force close that occured
    // when tapping fastly on a view to show quickaction dialog.
    // Thanx to zammbi (github.com/zammbi)
    mRootView.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

    setContentView(mRootView);
  }

  /**
   * Set color of popup
   *
   * @param popupColor Color to fill popup
   */
  public void setColor(@ColorInt int popupColor) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(popupColor);
    drawable.setCornerRadius(mContext.getResources().getDimension(R.dimen.popup_corner));
    mArrowDown.setImageDrawable(new ArrowDrawable(popupColor, ArrowDrawable.ARROW_DOWN));
    mArrowUp.setImageDrawable(new ArrowDrawable(popupColor, ArrowDrawable.ARROW_UP));
    mScroller.setBackground(drawable);
  }

  /**
   * Set color of popup
   *
   * @param popupColor Color resource id to fill popup
   */
  public void setColorRes(@ColorRes int popupColor) {
    setColor(mContext.getResources().getColor(popupColor));
  }

  /**
   * Set color for text of each action item. MUST call this before add action item, sorry I'm just
   * lazy.
   *
   * @param textColorRes Color resource id to use
   */
  public void setTextColorRes(@ColorRes int textColorRes) {
    setTextColor(mContext.getResources().getColor(textColorRes));
  }

  /**
   * Set color for text of each action item. MUST call this before add action item, sorry I'm just
   * lazy.
   *
   * @param textColor Color to use
   */
  public void setTextColor(@ColorInt int textColor) {
    mTextColor = textColor;
  }

  /**
   * Set animation style
   *
   * @param mAnimStyle animation style, default is set to ANIM_AUTO
   */
  public void setAnimStyle(Animation mAnimStyle) {
    this.mAnimStyle = mAnimStyle;
  }

  /**
   * Set listener for action item clicked.
   *
   * @param listener Listener
   */
  public void setOnActionItemClickListener(OnActionItemClickListener listener) {
    mItemClickListener = listener;
  }

  /**
   * Add action item
   *
   * @param action {@link ActionItem}
   */
  public void addActionItem(ActionItem action) {
    actionItems.add(action);

    String title = action.getTitle();
    View container;

    if (mOrientation == HORIZONTAL)
      container = mInflater.inflate(R.layout.action_item_horizontal, null);
    else
      container = mInflater.inflate(R.layout.action_item_vertical, null);

    ImageView img = (ImageView)container.findViewById(R.id.iv_icon);
    TextView text = (TextView)container.findViewById(R.id.tv_title);
    text.setTextColor(mTextColor);

    if (action.getIcon() <= 0) {
      img.setVisibility(View.GONE);
    } else {
      Drawable icon = mContext.getResources().getDrawable(action.getIcon());
      img.setImageDrawable(icon);
    }

    if (title != null) {
      text.setText(title);
    } else {
      text.setVisibility(View.GONE);
    }

    final int pos = mChildPos;
    final int actionId = action.getActionId();

    container.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        if (mItemClickListener != null) {
          mItemClickListener.onItemClick(QuickAction.this, pos, actionId);
        }

        if (!getActionItem(pos).isSticky()) {
          mDidAction = true;

          dismiss();
        }
      }
    });
    container.setFocusable(true);
    container.setClickable(true);

    if (mOrientation == HORIZONTAL && mChildPos != 0) {
      View separator = new View(mContext);
      separator.setBackgroundColor(Color.argb(32, 0, 0, 0));
      int width = mContext.getResources().getDimensionPixelOffset(R.dimen.separator_width);
      mTrack.addView(separator, mInsertPos++, new LayoutParams(width, MATCH_PARENT));
    }
    mTrack.addView(container, mInsertPos);
    mChildPos++;
    mInsertPos++;
  }

  /**
   * Get action item at an index
   *
   * @param index Index of item (position from callback)
   * @return Action Item at the position
   */
  public ActionItem getActionItem(int index) {
    return actionItems.get(index);
  }

  /**
   * Show quickaction popup. Popup is automatically positioned, on top or bottom of anchor view.
   */
  public void show(View anchor) {
    if (mContext != null) {

      preShow();

      int xPos, yPos, arrowPos;

      mDidAction = false;

      int[] location = new int[2];

      anchor.getLocationOnScreen(location);

      Rect anchorRect = new Rect(location[0], location[1], location[0]+anchor.getWidth(),
                                 location[1]+anchor.getHeight());

      // mRootView.setLayoutParams(new
      // LayoutParams(LayoutParams.WRAP_CONTENT,
      // LayoutParams.WRAP_CONTENT));

      mRootView.measure(WRAP_CONTENT, WRAP_CONTENT);

      int rootHeight = mRootView.getMeasuredHeight();

      if (rootWidth == 0) {
        rootWidth = mRootView.getMeasuredWidth();
      }

      int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
      int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

      // automatically get X coord of popup (top left)
      if ((anchorRect.left+rootWidth) > screenWidth) {
        xPos = anchorRect.left-(rootWidth-anchor.getWidth());
        xPos = (xPos < 0) ? 0 : xPos;

        arrowPos = anchorRect.centerX()-xPos;
      } else {
        if (anchor.getWidth() > rootWidth) {
          xPos = anchorRect.centerX()-(rootWidth/2);
        } else {
          xPos = anchorRect.left;
        }

        arrowPos = anchorRect.centerX()-xPos;
      }

      int dyTop = anchorRect.top;
      int dyBottom = screenHeight-anchorRect.bottom;

      boolean onTop = (dyTop > dyBottom) ? true : false;

      if (onTop) {
        if (rootHeight > dyTop) {
          yPos = 15;
          LayoutParams l = mScroller.getLayoutParams();
          l.height = dyTop-anchor.getHeight();
        } else {
          yPos = anchorRect.top-rootHeight;
        }
      } else {
        yPos = anchorRect.bottom;

        if (rootHeight > dyBottom) {
          LayoutParams l = mScroller.getLayoutParams();
          l.height = dyBottom;
        }
      }

      showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), arrowPos);

      setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);

      mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }
  }

  /**
   * Set animation style
   *
   * @param screenWidth screen width
   * @param requestedX distance from left edge
   * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top
   * of anchor view and vice versa
   */
  private void setAnimationStyle(
    int screenWidth, int requestedX, boolean onTop)
  {
    int arrowPos = requestedX-mArrowUp.getMeasuredWidth()/2;
    switch (mAnimStyle) {
      case ANIM_AUTO:
        if (arrowPos <= screenWidth/4)
          mWindow.setAnimationStyle(Animation.ANIM_GROW_FROM_LEFT.get(onTop));
        else if (arrowPos > screenWidth/4 && arrowPos < 3*(screenWidth/4))
          mWindow.setAnimationStyle(Animation.ANIM_GROW_FROM_CENTER.get(onTop));
        else
          mWindow.setAnimationStyle(Animation.ANIM_GROW_FROM_RIGHT.get(onTop));
        break;
      default:
        mWindow.setAnimationStyle(mAnimStyle.get(onTop));
    }
  }

  /**
   * Show arrow
   *
   * @param whichArrow arrow type resource id
   * @param requestedX distance from left screen
   */
  private void showArrow(int whichArrow, int requestedX) {
    final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
    final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

    final int arrowWidth = mArrowUp.getMeasuredWidth();

    showArrow.setVisibility(View.VISIBLE);

    ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)showArrow.getLayoutParams();

    param.leftMargin = requestedX-arrowWidth/2;

    hideArrow.setVisibility(View.INVISIBLE);
  }

  /**
   * Set listener for window dismissed. This listener will only be fired if the quicakction dialog
   * is dismissed by clicking outside the dialog or clicking on sticky item.
   */
  public void setOnDismissListener(QuickAction.OnDismissListener listener) {
    setOnDismissListener(this);

    mDismissListener = listener;
  }

  public void close() {
    mContext = null;
    mRootView = null;
    mArrowUp = null;
    mArrowDown = null;
    mInflater = null;
    mTrack = null;
    mScroller = null;
    mItemClickListener = null;
    mDismissListener = null;
    actionItems = null;
  }

  @Override public void onDismiss() {
    if (!mDidAction && mDismissListener != null) {
      mDismissListener.onDismiss();
    }
  }

  public enum Animation {
    ANIM_GROW_FROM_LEFT {
      @Override int get(boolean onTop) {
        return (onTop) ? R.style.Animation_PopUpMenu_Left : R.style.Animation_PopDownMenu_Left;
      }
    },
    ANIM_GROW_FROM_RIGHT {
      @Override int get(boolean onTop) {
        return (onTop) ? R.style.Animation_PopUpMenu_Right : R.style.Animation_PopDownMenu_Right;
      }
    },
    ANIM_GROW_FROM_CENTER {
      @Override int get(boolean onTop) {
        return (onTop) ? R.style.Animation_PopUpMenu_Center : R.style.Animation_PopDownMenu_Center;
      }
    },
    ANIM_REFLECT {
      @Override int get(boolean onTop) {
        return (onTop) ? R.style.Animation_PopUpMenu_Reflect
                       : R.style.Animation_PopDownMenu_Reflect;
      }
    },
    ANIM_AUTO {
      @Override int get(boolean onTop) {
        throw new UnsupportedOperationException("Can't use this");
      }
    };

    @StyleRes abstract int get(boolean onTop);
  }

  /**
   * Listener for item click
   */
  public interface OnActionItemClickListener {
    public abstract void onItemClick(
      QuickAction source, int pos, int actionId);
  }

  /**
   * Listener for window dismiss
   */
  public interface OnDismissListener {
    public abstract void onDismiss();
  }
}
