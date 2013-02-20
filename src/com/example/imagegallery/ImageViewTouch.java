package com.example.imagegallery;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewConfiguration;

public class ImageViewTouch extends ImageViewTouchBase {

	static final float SCROLL_DELTA_THRESHOLD = 1.0f;
	
	protected ScaleGestureDetector mScaleDetector;
	protected GestureDetector mGestureDetector;
	protected int mTouchSlop;
	protected float mCurrentScaleFactor;
	protected float mScaleFactor;
	protected int mDoubleTapDirection;
	protected OnGestureListener mGestureListener;
	protected OnScaleGestureListener mScaleListener;
	protected boolean mDoubleTapEnabled = true;
	protected boolean mScaleEnabled = true;
	protected boolean mScrollEnabled = true;
   private OnImageViewTouchDoubleTapListener mDoubleTapListener;

	public ImageViewTouch( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	@Override
	protected void init() {
		super.init();
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		mGestureListener = getGestureListener();
		mScaleListener = getScaleListener();

		mScaleDetector = new ScaleGestureDetector( getContext(), mScaleListener );
		mGestureDetector = new GestureDetector( getContext(), mGestureListener, null, true );

		mCurrentScaleFactor = 1f;
		mDoubleTapDirection = 1;
	}
	
	public void setDoubleTapListener( OnImageViewTouchDoubleTapListener listener ){
		mDoubleTapListener = listener;
	}

	public void setDoubleTapEnabled( boolean value ) {
		mDoubleTapEnabled = value;
	}

	public void setScaleEnabled( boolean value ) {
		mScaleEnabled = value;
	}

	public void setScrollEnabled( boolean value ) {
		mScrollEnabled = value;
	}

	public boolean getDoubleTapEnabled() {
		return mDoubleTapEnabled;
	}

	protected OnGestureListener getGestureListener() {
		return new GestureListener();
	}

	protected OnScaleGestureListener getScaleListener() {
		return new ScaleListener();
	}

	@Override
	protected void onBitmapChanged( Drawable drawable ) {
		super.onBitmapChanged( drawable );

		float v[] = new float[9];
		mSuppMatrix.getValues( v );
		mCurrentScaleFactor = v[Matrix.MSCALE_X];
	}

	@Override
	protected void _setImageDrawable( final Drawable drawable, final boolean reset, final Matrix initial_matrix, final float maxZoom ) {
		super._setImageDrawable( drawable, reset, initial_matrix, maxZoom );
		mScaleFactor = getMaxZoom() / 3;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		mScaleDetector.onTouchEvent( event );
		if ( !mScaleDetector.isInProgress() ) mGestureDetector.onTouchEvent( event );
		int action = event.getAction();
		switch ( action & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_UP:
				if ( getScale() < getMinZoom() ) {
					zoomTo( getMinZoom(), 50 );
				}
				break;
		}
		return true;
	}

	@Override
	protected void onZoom( float scale ) {
		super.onZoom( scale );
		if ( !mScaleDetector.isInProgress() ) mCurrentScaleFactor = scale;
	}
	
	@Override
	protected void onZoomAnimationCompleted( float scale ) {
		super.onZoomAnimationCompleted( scale );
		if ( !mScaleDetector.isInProgress() ) mCurrentScaleFactor = scale;
		
		if( scale < getMinZoom() ) {
			zoomTo( getMinZoom(), 50 );
		}
	}

	protected float onDoubleTapPost( float scale, float maxZoom ) {
		if ( mDoubleTapDirection == 1 ) {
			if ( ( scale + ( mScaleFactor * 2 ) ) <= maxZoom ) {
				return scale + mScaleFactor;
			} else {
				mDoubleTapDirection = -1;
				return maxZoom;
			}
		} else {
			mDoubleTapDirection = 1;
			return 1f;
		}
	}
	
	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
		if ( !mScrollEnabled ) return false;
		
		if ( e1 == null || e2 == null ) return false;
		if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
		if ( mScaleDetector.isInProgress() ) return false;
		if ( getScale() == 1f ) return false;
		
		Log.d( LOG_TAG, "onScroll: " + distanceX + ", " + distanceY );
		scrollBy( -distanceX, -distanceY );
		invalidate();
		return true;
	}
	
	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
		if ( !mScrollEnabled ) return false;

		if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
		if ( mScaleDetector.isInProgress() ) return false;

		float diffX = e2.getX() - e1.getX();
		float diffY = e2.getY() - e1.getY();

		if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
			scrollBy( diffX / 2, diffY / 2, 300 );
			invalidate();
			return true;
		}
		return false;
	}
	
	/**
	 * Determines whether this ImageViewTouch can be scrolled.
	 * @param direction
	 * 				- positive direction value means scroll from right to left, 
	 * 				negative value means scroll from left to right
	 * 
	 * @return true if there is some more place to scroll, false - otherwise.
	 */
	public boolean canScroll(int direction) {
		RectF bitmapRect = getBitmapRect();
		updateRect(bitmapRect, mScrollRect);
		Rect imageViewRect = new Rect();
		getGlobalVisibleRect(imageViewRect);
		
		if (bitmapRect.right >= imageViewRect.right) {
			if (direction < 0) {
				return Math.abs(bitmapRect.right - imageViewRect.right) > SCROLL_DELTA_THRESHOLD;
			}
		}
		
		double bitmapScrollRectDelta = Math.abs(bitmapRect.left - mScrollRect.left);
		return bitmapScrollRectDelta > SCROLL_DELTA_THRESHOLD;
	}	

	public class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap( MotionEvent e ) {
			Log.i( LOG_TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled );
			if ( mDoubleTapEnabled ) {
				float scale = getScale();
				float targetScale = scale;
				targetScale = onDoubleTapPost( scale, getMaxZoom() );
				targetScale = Math.min( getMaxZoom(), Math.max( targetScale, getMinZoom() ) );
				mCurrentScaleFactor = targetScale;
				zoomTo( targetScale, e.getX(), e.getY(), DEFAULT_ANIMATION_DURATION );
				invalidate();
			}
			
			if( null != mDoubleTapListener ){
				mDoubleTapListener.onDoubleTap();
         }
			
			return super.onDoubleTap( e );
		}

		@Override
		public void onLongPress( MotionEvent e ) {
			if ( isLongClickable() ) {
				if ( !mScaleDetector.isInProgress() ) {
					setPressed( true );
					performLongClick();
				}
			}
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			return ImageViewTouch.this.onScroll( e1, e2, distanceX, distanceY );
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
			return ImageViewTouch.this.onFling( e1, e2, velocityX, velocityY );
		}
	}

	public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@SuppressWarnings("unused")
		@Override
		public boolean onScale( ScaleGestureDetector detector ) {
			Log.d( LOG_TAG, "onScale" );
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = mCurrentScaleFactor * detector.getScaleFactor();
			if ( mScaleEnabled ) {
				targetScale = Math.min( getMaxZoom(), Math.max( targetScale, getMinZoom()-0.1f ) );
				zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
				mCurrentScaleFactor = Math.min( getMaxZoom(), Math.max( targetScale, getMinZoom()-1.0f ) );
				mDoubleTapDirection = 1;
				invalidate();
				return true;
			}
			return false;
		}
	}
	
	public interface OnImageViewTouchDoubleTapListener {
		void onDoubleTap();
	}
}
