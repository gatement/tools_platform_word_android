package net.johnsonlau.tool;

import net.johnsonlau.word.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class TouchListView extends ListView {
	public static final int FLING = 0;
	public static final int SLIDE_RIGHT = 1;
	public static final int SLIDE_LEFT = 2;

	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int mDragPos; // which item is being dragged
	private int mFirstDragPos; // where was the dragged item originally
	private int mDragPoint; // at what offset inside the item did the user grab
							// it
	private int mCoordOffset; // the difference between screen coordinates and
								// coordinates in this list view
	private int mUpperBound;
	private int mLowerBound;
	private int mHeight;
	private GestureDetector mGestureDetector;
	private int mRemoveMode = -1; // none
	private Rect mTempRect = new Rect();
	private Bitmap mDragBitmap;
	private final int mTouchSlop;
	private int mItemHeightNormal = -1;
	private int mItemHeightExpanded = -1;
	private int grabberId = -1;
	private int dragndropBackgroundColor = 0x00000000;

	private DragListener mDragListener;
	private DropListener mDropListener;
	private RemoveListener mRemoveListener;

	public TouchListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TouchListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		if (attrs != null) {
			// get attribute values
			TypedArray a = getContext().obtainStyledAttributes(attrs,
					R.styleable.TouchListView, 0, 0);

			mItemHeightNormal = a.getDimensionPixelSize(
					R.styleable.TouchListView_normal_height, 0);
			mItemHeightExpanded = a.getDimensionPixelSize(
					R.styleable.TouchListView_expanded_height,
					mItemHeightNormal);
			grabberId = a.getResourceId(R.styleable.TouchListView_grabber, -1);
			dragndropBackgroundColor = a.getColor(
					R.styleable.TouchListView_dragndrop_background, 0x00000000);
			mRemoveMode = a.getInt(R.styleable.TouchListView_remove_mode, -1);

			a.recycle();
		}
	}

	@Override
	final public void addHeaderView(View v, Object data, boolean isSelectable) {
		throw new RuntimeException(
				"Headers are not supported with TouchListView");
	}

	@Override
	final public void addHeaderView(View v) {
		throw new RuntimeException(
				"Headers are not supported with TouchListView");
	}

	@Override
	final public void addFooterView(View v, Object data, boolean isSelectable) {
		if (mRemoveMode == SLIDE_LEFT || mRemoveMode == SLIDE_RIGHT) {
			throw new RuntimeException(
					"Footers are not supported with TouchListView in conjunction with remove_mode");
		}
	}

	@Override
	final public void addFooterView(View v) {
		if (mRemoveMode == SLIDE_LEFT || mRemoveMode == SLIDE_RIGHT) {
			throw new RuntimeException(
					"Footers are not supported with TouchListView in conjunction with remove_mode");
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mRemoveListener != null && mGestureDetector == null) {
			if (mRemoveMode == FLING) {
				mGestureDetector = new GestureDetector(getContext(),
						new SimpleOnGestureListener() {
							@Override
							public boolean onFling(MotionEvent e1,
									MotionEvent e2, float velocityX,
									float velocityY) {
								if (mDragView != null) {
									if (velocityX > 1000) {
										Rect r = mTempRect;
										mDragView.getDrawingRect(r);
										// fast fling right with release near
										// the right edge of the screen
										if (e2.getX() > r.right * 2 / 3) {
											stopDragging();
											mRemoveListener
													.remove(mFirstDragPos);
											unExpandViews(true);
										}
									}
									// flinging while dragging should have no
									// effect
									return true;
								}
								return false;
							}
						});
			}
		}
		if (mDragListener != null || mDropListener != null) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// get the touched list item
				int x = (int) ev.getX();
				int y = (int) ev.getY();
				int itemNum = pointToPosition(x, y); // Maps a point to a
														// position in the list
														// (not the group), so
														// it includes the
														// invisible item
				if (itemNum == AdapterView.INVALID_POSITION) {
					break;
				}
				View item = (View) getChildAt(itemNum
						- getFirstVisiblePosition()); // Returns the view at the
														// specified position in
														// the group (not the
														// list), so it accepts
														// the index of visible
														// items (not all the
														// items)

				if (isDraggableRow(item)) {
					mDragPoint = y - item.getTop();
					mCoordOffset = ((int) ev.getRawY()) - y;
					View dragger = item.findViewById(grabberId);
					Rect r = mTempRect;

					r.left = dragger.getLeft();
					r.right = dragger.getRight();
					r.top = dragger.getTop();
					r.bottom = dragger.getBottom();

					// it is touching the grabber
					if ((r.left < x) && (x < r.right)) {
						// Create a copy of the drawing cache so that it does
						// not get recycled
						// by the framework when the list tries to clean up
						// memory
						item.setDrawingCacheEnabled(true);
						Bitmap bitmap = Bitmap.createBitmap(item
								.getDrawingCache());
						item.setDrawingCacheEnabled(false);

						Rect listBounds = new Rect();
						getGlobalVisibleRect(listBounds, null);

						startDragging(bitmap, listBounds.left, y);
						mDragPos = itemNum;
						mFirstDragPos = mDragPos;
						mHeight = getHeight();

						int touchSlop = mTouchSlop;
						mUpperBound = Math.min(y - touchSlop, mHeight / 3);
						mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);

						return true;
					}
				}

				break;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mGestureDetector != null) {
			mGestureDetector.onTouchEvent(ev);
		}
		if ((mDragListener != null || mDropListener != null)
				&& mDragView != null) {
			int action = ev.getAction();
			switch (action) {
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				Rect r = mTempRect;
				mDragView.getDrawingRect(r);
				stopDragging();

				if (mRemoveMode == SLIDE_RIGHT
						&& ev.getX() > r.left + (r.width() * 3 / 4)) {
					if (mRemoveListener != null) {
						mRemoveListener.remove(mFirstDragPos);
					}
					unExpandViews(true);
				} else if (mRemoveMode == SLIDE_LEFT
						&& ev.getX() < r.left + (r.width() / 4)) {
					if (mRemoveListener != null) {
						mRemoveListener.remove(mFirstDragPos);
					}
					unExpandViews(true);
				} else {
					if (mDropListener != null && mDragPos >= 0
							&& mDragPos < getCount()) {
						mDropListener.drop(mFirstDragPos, mDragPos);
					}
					unExpandViews(false);
				}
				break;

			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				int x = (int) ev.getX();
				int y = (int) ev.getY();

				dragView(x, y);

				int itemNum = getItemPosition(y);

				if (itemNum != AdapterView.INVALID_POSITION) {
					if (action == MotionEvent.ACTION_DOWN
							|| itemNum != mDragPos) {
						if (mDragListener != null) {
							mDragListener.drag(mDragPos, itemNum);
						}
						mDragPos = itemNum;
						doExpansion();
					}

					scrollListView(y);
				}
				break;
			}
			return true;
		}
		return super.onTouchEvent(ev);
	}

	protected boolean isDraggableRow(View view) {
		return (view.findViewById(grabberId) != null);
	}

	// scroll the list view if the dragger is under the lower bound or above the
	// upper bound
	protected void scrollListView(int y) {
		int speed = 0;
		adjustScrollBounds(y);
		if (y > mLowerBound) {
			// scroll the list up a bit
			speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
		} else if (y < mUpperBound) {
			// scroll the list down a bit
			speed = y < mUpperBound / 2 ? -16 : -4;
		}

		if (speed != 0) {
			int ref = pointToPosition(0, mHeight / 2);

			// we hit a divider or an invisible view, check somewhere else
			if (ref == AdapterView.INVALID_POSITION) {
				ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
			}

			View v = getChildAt(ref - getFirstVisiblePosition());
			if (v != null) {
				int pos = v.getTop();
				setSelectionFromTop(ref, pos - speed);
			}
		}
	}

	private int getItemPosition(int y) {
		int adjustedy = y - mDragPoint - (mItemHeightNormal / 2);
		int pos = pointToPosition(0, adjustedy);
		if (adjustedy < 0) {
			pos = 0;
		} else if (pos != AdapterView.INVALID_POSITION) {
			if (pos <= mFirstDragPos) {
				pos += 1;
			}
		}
		return pos;
	}

	// if a list item is dragged to the list view vertical center, adjust scroll
	// bounds
	private void adjustScrollBounds(int y) {
		if (y >= mHeight / 3) {
			mUpperBound = mHeight / 3;
		}
		if (y <= mHeight * 2 / 3) {
			mLowerBound = mHeight * 2 / 3;
		}
	}

	// Restore size and visibility for all list items
	private void unExpandViews(boolean deletion) {
		for (int i = 0;; i++) {
			View v = getChildAt(i);
			if (v == null) {
				if (deletion) {
					// HACK force update of list item count
					int position = getFirstVisiblePosition();
					int y = getChildAt(0).getTop();
					setAdapter(getAdapter());
					setSelectionFromTop(position, y);
					// end HACK
				}
				layoutChildren(); // force children to be recreated where needed
				v = getChildAt(i);
				if (v == null) {
					break;
				}
			}

			if (isDraggableRow(v)) {
				ViewGroup.LayoutParams params = v.getLayoutParams();
				params.height = mItemHeightNormal;
				v.setLayoutParams(params);
				v.setVisibility(View.VISIBLE);
			}
		}

		// if original position is scrolled out of view it won't be
		// re-expanded, so we need to scroll the list to compensate
		int visiblePos = getFirstVisiblePosition();
		if (visiblePos > mFirstDragPos) {
			int y = getChildAt(0).getTop();
			setSelectionFromTop(visiblePos, y + mItemHeightNormal);
		}
	}

	/*
	 * Adjust visibility and size to make it appear as though an item is being
	 * dragged around and other items are making room for it: If dropping the
	 * item would result in it still being in the same place, then make the
	 * dragged list item's size normal, but make the item invisible. Otherwise,
	 * if the dragged item is still on screen, make it as small as possible and
	 * expand the item below the insert point. If the dragged item is not on
	 * screen, only expand the item below the current insert point.
	 */
	private void doExpansion() {
		int childNum = mDragPos - getFirstVisiblePosition();
		if (mDragPos > mFirstDragPos) {
			childNum++;
		}

		View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

		for (int i = 0;; i++) {
			View vv = getChildAt(i);
			if (vv == null) {
				break;
			}
			int height = mItemHeightNormal;
			int visibility = View.VISIBLE;

			// processing the item that is being dragged
			if (vv.equals(first)) {
				if (mDragPos == mFirstDragPos) {
					// hovering over the original location
					visibility = View.INVISIBLE;
				} else {
					// not hovering over it, make it as small as possible
					height = 1;
				}
			} else if (i == childNum
					|| (mDragPos == (getCount() - 1) && getChildAt(i + 1) == null)) {
				height = mItemHeightExpanded;
			}

			if (isDraggableRow(vv)) {
				ViewGroup.LayoutParams params = vv.getLayoutParams();
				params.height = height;
				vv.setLayoutParams(params);
				vv.setVisibility(visibility);
			}
		}

		// Request re-layout since we changed the items layout
		layoutChildren();
	}

	private void startDragging(Bitmap bm, int x, int y) {
		stopDragging();

		mWindowParams = new WindowManager.LayoutParams();
		mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
		mWindowParams.x = x;
		mWindowParams.y = y - mDragPoint + mCoordOffset;

		mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		mWindowParams.format = PixelFormat.TRANSLUCENT;
		mWindowParams.windowAnimations = 0;

		ImageView v = new ImageView(getContext());
		v.setBackgroundColor(dragndropBackgroundColor);
		v.setImageBitmap(bm);
		mDragBitmap = bm;

		mWindowManager = (WindowManager) getContext()
				.getSystemService("window");
		mWindowManager.addView(v, mWindowParams);
		mDragView = v;
	}

	private void dragView(int x, int y) {
		if (mRemoveListener != null) {
			float alpha = 1.0f;
			int width = mDragView.getWidth();

			if (mRemoveMode == SLIDE_RIGHT) {
				if (x > width / 2) {
					alpha = ((float) (width - x)) / (width / 2);
				}
				mWindowParams.alpha = alpha;
			} else if (mRemoveMode == SLIDE_LEFT) {
				if (x < width / 2) {
					alpha = ((float) x) / (width / 2);
				}
				mWindowParams.alpha = alpha;
			}
		}

		mWindowParams.y = y - mDragPoint + mCoordOffset;
		mWindowManager.updateViewLayout(mDragView, mWindowParams);
	}

	private void stopDragging() {
		if (mDragView != null) {
			WindowManager wm = (WindowManager) getContext().getSystemService(
					"window");
			wm.removeView(mDragView);
			mDragView.setImageDrawable(null);
			mDragView = null;
		}
		if (mDragBitmap != null) {
			mDragBitmap.recycle();
			mDragBitmap = null;
		}
	}

	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}

	public interface DragListener {
		void drag(int from, int to);
	}

	public interface DropListener {
		void drop(int from, int to);
	}

	public interface RemoveListener {
		void remove(int which);
	}
}
