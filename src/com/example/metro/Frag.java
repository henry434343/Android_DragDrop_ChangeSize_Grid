package com.example.metro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.example.metro.ViewItem.ItemSize;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class Frag extends Fragment implements View.OnTouchListener {

	private View _view;
	private ViewGroup _root;
	private int _xDelta;
	private int _yDelta;
	
	private int width;
	private int height;
	private int topBarHeight;
	
	private int itemWidth;
	private int itemHeight;
	
	private ArrayList<ViewItem> views;
	private ArrayList<Point> points;
	
	private int actionBar = 0;
	
	private Action action;
	private static DrawView drawView;
	private enum Action {
		reSize,
		move
	}
	
	private Mode actionMode;
	private enum Mode {
		switchMode,
		editMode
	}

	public View v;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.frag, container, false);
		((MainActivity)getActivity()).f = this;
		getScreenSizeandData();
		initPoints();
		actionMode = Mode.switchMode;
		return v;
	}

	@SuppressLint("NewApi")
	private void getScreenSizeandData(){
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		width = size.x;
		height = size.y;
		topBarHeight = getTopBarHeight();
		itemWidth = width/4;
		itemHeight = (height - topBarHeight - actionBar)/5;
		_root = (ViewGroup)v.findViewById(R.id.root);
		views = new ArrayList<ViewItem>();
		
    	drawView = new DrawView(getActivity(),new int[]{width,height}, new int[]{itemWidth,itemHeight});
        _root.addView(drawView);
        drawView.setVisibility(View.GONE);
        
	} 
	
	private int getTopBarHeight() {
		int statusBar = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBar = getResources().getDimensionPixelSize(resourceId);
		} 
	      
		TypedValue tv = new TypedValue();
		if ( getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			actionBar = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		
		return statusBar+actionBar;
	}
	
	private void initPoints(){
		points = new ArrayList<Point>();
		for (int i = 0; i <= 3; i++) {
			for (int j = 0; j <= 4 ; j++) {
				Point p = new Point();
				p.X = i;
				p.Y = j;
				p.ckeck = false;
				points.add(p);
			}
		}
	}
	
	private void moveViewItem(View view, MotionEvent event){
		action = Action.move;
	    final int X = (int) event.getRawX();
	    final int Y = (int) event.getRawY();
	    ViewItem nowItem = null;
    	for (ViewItem item : views) {
			if (item.view == view) {
				nowItem = item;
				break;
			}
		}
	    switch (event.getAction() & MotionEvent.ACTION_MASK) {
	        case MotionEvent.ACTION_DOWN:
				nowItem.img_delete.setVisibility(View.VISIBLE);
				nowItem.img_resize.setVisibility(View.VISIBLE);
	            view.setAlpha((float)0.5);
	            
	        	RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
	        	lParams.leftMargin = X/itemWidth * itemWidth;
	        	lParams.topMargin = (Y-topBarHeight)/itemHeight * itemHeight;
	            _xDelta = X - lParams.leftMargin;
	            _yDelta = Y - lParams.topMargin;
	            view.bringToFront();
	            break;
	        case MotionEvent.ACTION_UP: {
	        	if (X/itemWidth > 3 || (Y-topBarHeight)/itemHeight > 4) 
					break;
	        	
	        	int rootX = X/itemWidth;
	        	int rootY = (Y-topBarHeight)/itemHeight;
				RelativeLayout.LayoutParams layoutParamsup = (RelativeLayout.LayoutParams) view.getLayoutParams();	
				layoutParamsup.leftMargin = rootX * itemWidth;
				layoutParamsup.topMargin = rootY * itemHeight;
				view.setLayoutParams(layoutParamsup);
				setItemPosition(nowItem, new int[]{rootX,rootY});
				updateScreenPosition();
				
				view.setAlpha((float)1);
				drawView.setVisibility(View.VISIBLE);
	        }
            	break;
	        case MotionEvent.ACTION_MOVE: {
	        	if (X/itemWidth > 3 || (Y-topBarHeight)/itemHeight > 4) 
					break;
	        	
	        	int rootX = X/itemWidth;
	        	int rootY = (Y-topBarHeight)/itemHeight;
	        	if (nowItem.positions.get(0).X != rootX || nowItem.positions.get(0).Y != rootY) {
	        		setItemPosition(nowItem, new int[]{rootX,rootY});
	        		chechOverlap(nowItem);	
				}
	        	
	            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();	        	
	        	if ( 0 <= X - _xDelta && X - _xDelta <= itemWidth*3) layoutParams.leftMargin = X - _xDelta;
	        	if ( 0 <= Y - _yDelta && Y - _yDelta <= itemHeight*4) layoutParams.topMargin = Y - _yDelta;
	        	view.setLayoutParams(layoutParams);
	        }
	        	break;
	    }  
	    _root.invalidate();
	}
	
	public boolean onTouch(View view, MotionEvent event) {
		
		if (actionMode == Mode.editMode) 
			moveViewItem(view, event);
	    return false;
	}
	
	
	public void addViewItem(){
		
		if (views.size() >= 20) {
			showToast("已達上限");
			return;
		}
		
	    LayoutInflater layoutInflater = LayoutInflater.from(getActivity());  
	    _view = layoutInflater.inflate(R.layout.grid_item, null);
	    int x, y;
	    try {
		    x = getNewViewPosition(ItemSize.min)[0];
		    y = getNewViewPosition(ItemSize.min)[1];
		} catch (Exception e) {
			// TODO: handle exception
			showToast("無空間");
			return;
		}
	    
	    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
	    layoutParams.leftMargin = (width/4)*x;
	    layoutParams.topMargin = (height/6)*y;
	    _view.setLayoutParams(layoutParams);

	    _view.setOnClickListener(switchMode);
	    _view.setOnLongClickListener(editMode);
	    _view.setOnTouchListener(this); 
	    _root.addView(_view);
	    
	    ViewItem viewItem = new ViewItem(new int[]{x,y});
	    viewItem.view = _view;
	    viewItem.view.setTag(viewItem);
	    viewItem.size = ItemSize.min;
	    viewItem.tag = views.size();
	    views.add(viewItem);
	    updateScreenPosition();
	    
	    viewItem.textView = (TextView)_view.findViewById(R.id.textView_grid);
	    viewItem.textView.setText(""+ views.size());
	    
	    viewItem.img_resize = (ImageView)_view.findViewById(R.id.resize);
	    viewItem.img_resize.setOnClickListener(resize);
	    viewItem.img_resize.setTag(viewItem);
	    
	    viewItem.img_delete = (ImageView)_view.findViewById(R.id.delete);
	    viewItem.img_delete.setOnClickListener(delete);
	    viewItem.img_delete.setTag(viewItem);
	}
	
	private int[] getNewViewPosition(ItemSize size){
		switch (size) {
		case min:
			for (Point p : points) {
				if (!p.ckeck) 
					return new int[]{p.X,p.Y};
			}
		case mid_width:
			for (int i = 0; i < points.size(); i++) {
				try {
					if (!points.get(i).ckeck && !points.get(i+1).ckeck ) {
						if (points.get(i).X == 3) 
							continue;
						return new int[]{points.get(i).X,points.get(i).Y};
					}
				} catch (Exception e) {}
			}  
		case mid_height:
			for (int i = 0; i < points.size(); i++) {
				try {
					if (!points.get(i).ckeck && !points.get(i+4).ckeck) {
						if (points.get(i).Y == 4)
							continue;
						return new int[]{points.get(i).X,points.get(i).Y};
					}
				} catch (Exception e) {}  
			}
		case max:
			for (int i = 0; i < points.size(); i++) {
				try {
					if (!points.get(i).ckeck && !points.get(i+1).ckeck && !points.get(i+4).ckeck&& !points.get(i+5).ckeck) {
						if (points.get(i).X == 3 || points.get(i).Y == 4)
							continue;
						return new int[]{points.get(i).X,points.get(i).Y};
					}
				} catch (Exception e) {}
			}
		default:
			break;
		}
		return null;
	}
	
	private void updateScreenPosition(){

		initPoints();
		for (ViewItem item : views) {
			for (Point itemP : item.positions) {
				for (Point p : points) {
					if (p.isEqual(itemP)) {
						p.ckeck = true;
					}
				}
			}
		}
		
		Collections.sort(points,  new Comparator<Point>() {
			@Override
			public int compare(Point lhs, Point rhs) {
				// TODO Auto-generated method stub
				if (lhs.Y > rhs.Y) return 1;
				else if (lhs.Y < rhs.Y) return -1;
				else if (lhs.X > rhs.X) return 1;
				else return -1;
			}
		});
	}
	
	private void chechOverlap(ViewItem item){
		for (final ViewItem otherItem : views) {
			if (item != otherItem) {
				boolean isOverlap = false;
				for (Point p : item.positions) {
					for (Point otherP : otherItem.positions) {
						if (p.isEqual(otherP)) {
							isOverlap = true;
							final int[] start = getNewViewPosition(otherItem.size);
							if (start != null) {
								TranslateAnimation animation = new TranslateAnimation(0, (start[0] - otherItem.positions.get(0).X ) * itemWidth, 0, (start[1] - otherItem.positions.get(0).Y )*itemHeight);
								animation.setDuration(500);
								animation.setAnimationListener(new TranslateAnimation.AnimationListener() {
									
									@Override
									public void onAnimationStart(Animation animation) {}
									
									@Override
									public void onAnimationRepeat(Animation animation) {}
									
									@Override
									public void onAnimationEnd(Animation animation) {
										// TODO Auto-generated method stub
										completeAnimation(otherItem.view);
										RelativeLayout.LayoutParams layoutParamsup = (RelativeLayout.LayoutParams) otherItem.view.getLayoutParams();
										layoutParamsup.leftMargin = start[0] * itemWidth;
										layoutParamsup.topMargin = start[1] * itemHeight;
									}
								});
								setItemPosition(otherItem,new int[]{start[0],start[1]});
								updateScreenPosition();
								otherItem.view.startAnimation(animation);
								Log.i("chauster", "item "+otherItem.tag +" 需要被移動");
							}
							else {
								if (action == Action.reSize) {
									itemResize(item);
									showToast("無空間");
								}
								else {
									showToast("移動item碰到其他item，但無空間移動");
								}
							}
							break;
						}
					}

					if (isOverlap) {
						break;
					}
				}
			}
		} 
	}
	
	private void completeAnimation(View view){
		view.clearAnimation();
        view.setVisibility(View.GONE);
        view.setVisibility(View.VISIBLE);
	}
	
	private void itemResize(ViewItem item){
		int startX = item.positions.get(0).X;
		int startY = item.positions.get(0).Y;
		RelativeLayout.LayoutParams layoutParamsup = null;
		switch (item.size) {
		case min: 
			if (startX+1>3) {
				try {
					startX = getNewViewPosition(ItemSize.mid_width)[0];
					startY = getNewViewPosition(ItemSize.mid_width)[1];
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth*2, itemHeight);	
			item.setPositions(new int[]{startX,startY},
							  new int[]{startX+1,startY});
		    item.size = ItemSize.mid_width;
			break;
			
		case mid_width: 
			if (startY+1>5) {
				try {
					startX = getNewViewPosition(ItemSize.mid_width)[0];
					startY = getNewViewPosition(ItemSize.mid_width)[1];
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth, itemHeight*2);
			item.setPositions(new int[]{startX,startY},
							  new int[]{startX,startY+1});
		    item.size = ItemSize.mid_height;
			break;
			
		case mid_height:
			if (startX+1>3 || startY+1>5) {
				try {
					startX = getNewViewPosition(ItemSize.mid_width)[0];
					startY = getNewViewPosition(ItemSize.mid_width)[1];
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth*2, itemHeight*2);
			item.setPositions(new int[]{startX,startY},
							  new int[]{startX+1,startY},
							  new int[]{startX,startY+1},
							  new int[]{startX+1,startY+1});
		    item.size = ItemSize.max;
			break;
			
		case max: 
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
			item.setPositions(new int[]{startX,startY});
		    item.size = ItemSize.min;
			break;

		default:
			break;
		}  
		updateScreenPosition();
		
		layoutParamsup.leftMargin = startX * itemWidth;
		layoutParamsup.topMargin = startY * itemHeight;
		item.view.setLayoutParams(layoutParamsup);
		chechOverlap(item);	
	}
	
	private void setItemPosition(ViewItem item, int[] start){
		switch (item.size) { 
		case min:
			item.positions.get(0).X = start[0];
			item.positions.get(0).Y = start[1];
			break;
		case mid_width:
			item.positions.get(0).X = start[0];
			item.positions.get(0).Y = start[1];
			
			item.positions.get(1).X = start[0]+1;
			item.positions.get(1).Y = start[1];
			break;
		case mid_height:
			item.positions.get(0).X = start[0];
			item.positions.get(0).Y = start[1];
			
			item.positions.get(1).X = start[0];
			item.positions.get(1).Y = start[1]+1;
			break;
		case max:
			item.positions.get(0).X = start[0];
			item.positions.get(0).Y = start[1];
			
			item.positions.get(1).X = start[0]+1;
			item.positions.get(1).Y = start[1];
			
			item.positions.get(2).X = start[0];
			item.positions.get(2).Y = start[1]+1;
			
			item.positions.get(3).X = start[0]+1;
			item.positions.get(3).Y = start[1]+1;
			break;

		default:
			break;
		}
		updateScreenPosition();
	}
	
	/*************************** Listener *************************/
	OnClickListener delete = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			ViewItem item = (ViewItem)v.getTag();
			_root.removeView(item.view);
			views.remove(item);
			updateScreenPosition();
		}
	};
	
	OnClickListener resize = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			action = Action.reSize;
			ViewItem item = (ViewItem)v.getTag();
			itemResize(item);
		}
	};
	
	OnClickListener switchMode = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.i("chauster", "onClick");
			if (actionMode == Mode.switchMode) {
				RelativeLayout r = (RelativeLayout)v.findViewById(R.id.grid_cell);
	            int color = Color.TRANSPARENT;
	            Drawable background = r.getBackground();
	            if (background instanceof ColorDrawable)
	                color = ((ColorDrawable) background).getColor();
	            
	            if (color == Color.RED) 
	            	r.setBackgroundColor(0xff19FF8F);
	            else 
	            	r.setBackgroundColor(Color.RED);
			}
			else {
				showEditButton(v);
			}
		}
	};
	
	OnLongClickListener editMode = new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			// TODO Auto-generated method stub
			actionMode = Mode.editMode;
			showEditButton(v);
			
            drawView.setVisibility(View.VISIBLE);
            v.setAlpha((float)0.5);
			Log.i("chauster", "onLongClick");
			return true;
		}
	};
	
	private void showEditButton(View v){
		for (ViewItem item : views) {
			item.img_delete.setVisibility(View.GONE);
			item.img_resize.setVisibility(View.GONE);
		}
		ViewItem item = (ViewItem) v.getTag();
		item.img_delete.setVisibility(View.VISIBLE);
		item.img_resize.setVisibility(View.VISIBLE);
	}
	
	public void finishEditMode(){
		actionMode = Mode.switchMode;
		drawView.setVisibility(View.GONE);
		for (ViewItem item : views) {
			item.img_delete.setVisibility(View.GONE);
			item.img_resize.setVisibility(View.GONE);
		}
	}
	
	private void showToast(String msg){
		Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
	}

}
