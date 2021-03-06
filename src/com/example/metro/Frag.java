package com.example.metro;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.example.metro.DB.RecordItem;
import com.example.metro.ViewItem.ItemSize;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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
	
	private int screenWidth;
	private int screeHeight;
	private int statusBar = 0;
	private int actionBar = 0;
	private int navigationBar = 0;
	
	private int itemWidth;
	private int itemHeight; 
	
	private int rowCount = 4;
	private int columnCount = 5;
	
	private ArrayList<ViewItem> views;
	private ArrayList<Point> screemPointUse;
	
	private DB db;
	private static DrawView drawView;
	private int tempX;
	private int tempY;
    private static int clickItemPointIndex = 0;

	private static boolean isNoSpaceToMove = false;
	private ItemOperator itemOperator;
	private enum ItemOperator {
		reSize,
		move
	}
	
	private ItemActionMode itemActionMode;
	private enum ItemActionMode {
		switchMode,
		editMode
	}

	public View v;
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.frag, container, false);
		((MainActivity)getActivity()).f = this;
		init();
		return v;
	}
	  
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		recordItems();
	}
	
	private void init(){

		getScreenSizeandData();
		initPoints();
		itemActionMode = ItemActionMode.switchMode;
		db = new DB(getActivity(),"home");
		for (RecordItem item : db.getItems()) {
			addViewItem(item.id,item.size, item.row, item.colume);
		}
		v.setOnTouchListener(screenFinish);
	}
	
	OnTouchListener screenFinish = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if (itemActionMode == ItemActionMode.editMode) {
				finishEditMode();
			}
			return false;
		}
	};

	@SuppressLint("NewApi")
	private void getScreenSizeandData(){
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		
		display.getRealSize(size);
		screenWidth = size.x;  
		screeHeight = size.y;
		getBarHeight();
		itemWidth = screenWidth/rowCount;
		itemHeight = (screeHeight - statusBar - actionBar - navigationBar)/columnCount;

		_root = (ViewGroup)v.findViewById(R.id.root);
		_root.removeAllViews();
		views = new ArrayList<ViewItem>();
		
    	drawView = new DrawView(getActivity(),new int[]{screenWidth,screeHeight}, 
    										  new int[]{itemWidth,itemHeight},
    										  new int[]{rowCount,columnCount});
        _root.addView(drawView);
        drawView.setVisibility(View.GONE);
	} 
	
	private void getBarHeight() {
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBar = getResources().getDimensionPixelSize(resourceId);
		} 
	      
		TypedValue tv = new TypedValue();
		if ( getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			actionBar = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		
	    int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId > 0)
        	navigationBar = getResources().getDimensionPixelSize(resId);
	}
	
	private void initPoints(){
		screemPointUse = new ArrayList<Point>();
		for (int i = 0; i < columnCount ; i++) {
			for (int j = 0; j < rowCount ; j++) {
				Point p = new Point();
				p.X = j;
				p.Y = i;
				p.ckeck = false;
				screemPointUse.add(p);
			}
		}
	} 
	
	private void cloneViewItem(ViewItem item){
		tempX = item.positions.get(0).X;
		tempY = item.positions.get(0).Y;
	}
	
	private void moveViewItem(View view, MotionEvent event){
		itemOperator = ItemOperator.move;
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
	        	touchDownItem(nowItem, X, Y);
	            break;
	        case MotionEvent.ACTION_UP: 
	        	touchUpItem(nowItem, X, Y);
            	break;
	        case MotionEvent.ACTION_MOVE: 
	        	if (clickItemPointIndex == -1) touchDownItem(nowItem, X, Y);
	        	else touchMoveItem(nowItem, X, Y);
	        	break;
	    }  
	    _root.invalidate();
	}
	
	private void touchDownItem(ViewItem nowItem, int X, int Y){
		cloneViewItem(nowItem);
		nowItem.img_delete.setVisibility(View.VISIBLE);
		nowItem.img_resize.setVisibility(View.VISIBLE);
		nowItem.view.setAlpha((float)0.5);
        for (int i = 0; i < nowItem.positions.size(); i++) {
			if (nowItem.positions.get(i).X == X/itemWidth && nowItem.positions.get(i).Y == (Y- statusBar - actionBar)/itemHeight) {
				clickItemPointIndex = i ; 
				break;
			}
		}
        
    	RelativeLayout.LayoutParams lParam = (RelativeLayout.LayoutParams) nowItem.view.getLayoutParams();
        _xDelta = X - lParam.leftMargin;
        _yDelta = Y - lParam.topMargin;
        
        nowItem.view.bringToFront();
	}
	
	private int moveX;
	private int moveY;
	private ViewItem moveItem;
	private void touchMoveItem(final ViewItem nowItem, final int X, final int Y){
    	if (X/itemWidth > rowCount-1 || (Y- statusBar - actionBar)/itemHeight > columnCount-1) 
			return;
       	
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) nowItem.view.getLayoutParams();	        	
    	layoutParams.leftMargin = X - _xDelta;
    	layoutParams.topMargin = Y - _yDelta;
    	nowItem.view.setLayoutParams(layoutParams);
    	
    	int rootX = X/itemWidth;
    	int rootY = (Y- statusBar - actionBar)/itemHeight;
    	int index = clickItemPointIndex >= nowItem.positions.size() ? 0 : clickItemPointIndex;
    	if (nowItem.positions.get(index).X != rootX || nowItem.positions.get(index).Y != rootY) {
    		//設定起始位置
    		if (nowItem.size == ItemSize.min) {
    			setItemPosition(nowItem, new int[]{rootX,rootY});
			}
    		else if (nowItem.size == ItemSize.mid_width) {
				if (clickItemPointIndex == 0) setItemPosition(nowItem, new int[]{rootX,rootY});
				else setItemPosition(nowItem, new int[]{rootX-1,rootY});
			}
    		else if (nowItem.size == ItemSize.mid_height) {
				if (clickItemPointIndex == 0) setItemPosition(nowItem, new int[]{rootX,rootY});
				else setItemPosition(nowItem, new int[]{rootX,rootY-1});
			}
    		else  {
				if (clickItemPointIndex == 0) setItemPosition(nowItem, new int[]{rootX,rootY});
				else if (clickItemPointIndex == 1) setItemPosition(nowItem, new int[]{rootX-1,rootY});
				else if (clickItemPointIndex == 2) setItemPosition(nowItem, new int[]{rootX,rootY-1});
				else setItemPosition(nowItem, new int[]{rootX-1,rootY-1});
			}
    		moveX = X;
    		moveY = Y;
    		
    		new Timer().schedule(new TimerTask() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (moveX == X && moveY == Y) {
						moveItem = nowItem;
						viewOverlap.sendEmptyMessage(0);
					}
				}
			}, 350);
		}
	}
	
	private Handler viewOverlap = new Handler() {
		public void handleMessage(android.os.Message msg) {
			chechOverlap(moveItem);	
		};
	};
	
	private void touchUpItem(ViewItem nowItem, int X, int Y){
    	if (X/itemWidth > rowCount-1 || (Y- statusBar - actionBar)/itemHeight > columnCount-1) 
			return;
        
    	int rootX = nowItem.positions.get(0).X;
    	int rootY = nowItem.positions.get(0).Y;
    	setItemPosition(nowItem, new int[]{rootX,rootY});
    	chechOverlap(nowItem);
    	for (ViewItem item: views) {
			if (item != nowItem) {
				for	(Point itemP : item.positions) {
					for (Point p : nowItem.positions) {
						if (p.isEqual(itemP)) {
							isNoSpaceToMove = true;
							break;
						}
					}
				}
			}
		}
    	
    	if (isNoSpaceToMove) {
        	rootX = tempX;
        	rootY = tempY;
        	isNoSpaceToMove = false;
		} 
    	
        boolean isReset = false;
        if (nowItem.size == ItemSize.mid_width) {
            if (rootX < 0) {
                rootX = 0;
                resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
            else if (rootX+1 >= rowCount) {
            	resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
        }
        else if (nowItem.size == ItemSize.mid_height) {
            if (rootY < 0) {
                rootY = 0;
                resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
            else if (rootY+1 >= columnCount) {
            	resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
        }
        else if (nowItem.size == ItemSize.max) {
            if (rootX < 0 || rootY < 0) {
                rootX = rootX < 0 ? 0 : rootX;
                rootY = rootY < 0 ? 0 : rootY;
                resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
            else if (rootX+1 >= rowCount || rootY+1 >= columnCount) {
            	resetItem(nowItem, rootX, rootY);
                isReset = true;
            }
        }
        
        if (!isReset) {
    		RelativeLayout.LayoutParams layoutParamsup = (RelativeLayout.LayoutParams) nowItem.view.getLayoutParams();	
    		layoutParamsup.leftMargin = rootX * itemWidth;
    		layoutParamsup.topMargin = rootY * itemHeight;
    		if (nowItem.size == ItemSize.min) {
    			layoutParamsup.width = itemWidth;
    			layoutParamsup.height = itemHeight;
    		}
    		nowItem.view.setLayoutParams(layoutParamsup);
        }

		setItemPosition(nowItem, new int[]{rootX,rootY});
		updateScreenPosition();
		chechOverlap(nowItem);
		
		nowItem.view.setAlpha((float)1);
		drawView.setVisibility(View.VISIBLE);
	}
	
	private void resetItem(ViewItem item, int rootX, int rootY) {
		item.size = ItemSize.min;
		RelativeLayout.LayoutParams layoutParamsup = (RelativeLayout.LayoutParams) item.view.getLayoutParams();	
		layoutParamsup.leftMargin = rootX * itemWidth;
		layoutParamsup.topMargin = rootY * itemHeight;
		layoutParamsup.width = itemWidth;
		layoutParamsup.height = itemHeight;
		item.view.setLayoutParams(layoutParamsup);
	}
		
	public boolean onTouch(View view, MotionEvent event) {
		
		if (itemActionMode == ItemActionMode.editMode) 
			moveViewItem(view, event);
	    return false;
	}
	
	public void addViewItem(String id,int size, int x, int y){
		
		if (views.size() >= rowCount * columnCount) {
			showToast("已達上限");
			return;
		}
		
	    LayoutInflater layoutInflater = LayoutInflater.from(getActivity());  
	    _view = layoutInflater.inflate(R.layout.grid_item, null);
	    

	    if (x == -1 && y == -1) {
		    try {
			    x = getNewViewPosition(ItemSize.min)[0];
			    y = getNewViewPosition(ItemSize.min)[1];
			} catch (Exception e) {
				// TODO: handle exception
				showToast("無空間");
				return;
			} 
		}
	    
	    RelativeLayout.LayoutParams layoutParams = null;
	    ViewItem viewItem = null;
	    switch (size) {
		case 0:
			viewItem = new ViewItem(new int[]{x,y});
			layoutParams = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
			break; 
		case 1:
			viewItem = new ViewItem(new int[]{x,y},new int[]{x+1,y});
			layoutParams = new RelativeLayout.LayoutParams(itemWidth*2, itemHeight);
			break;
		case 2:
			viewItem = new ViewItem(new int[]{x,y},new int[]{x,y+1});
			layoutParams = new RelativeLayout.LayoutParams(itemWidth, itemHeight*2);
			break;
		case 3:
			viewItem = new ViewItem(new int[]{x,y},new int[]{x+1,y},new int[]{x,y+1},new int[]{x+1,y+1});
			layoutParams = new RelativeLayout.LayoutParams(itemWidth*2, itemHeight*2);
			break;
  
		default:
			break;
		}
	     
	    layoutParams.leftMargin = (screenWidth/rowCount)*x;
	    layoutParams.topMargin = ((screeHeight - statusBar - actionBar - navigationBar)/columnCount)*y;
	    _view.setLayoutParams(layoutParams);

	    _view.setOnClickListener(switchMode);
	    _view.setOnLongClickListener(editMode);
	    _view.setOnTouchListener(this); 
	    _root.addView(_view);
	    
	    viewItem.view = _view;
	    viewItem.id = id == null? ""+views.size() : id;
	    viewItem.view.setTag(viewItem);
	    viewItem.size =  ItemSize.values()[size];
	    viewItem.tag = views.size();
	    views.add(viewItem);
	    updateScreenPosition();  
	    
	    viewItem.textView = (TextView)_view.findViewById(R.id.textView_grid);
	    viewItem.textView.setText(""+ viewItem.id);
	    
	    viewItem.img_resize = (ImageView)_view.findViewById(R.id.resize);
	    viewItem.img_resize.setOnClickListener(resize);
//	    viewItem.img_resize.setOnTouchListener(zoom);
	    viewItem.img_resize.setTag(viewItem);
	    
	    viewItem.img_delete = (ImageView)_view.findViewById(R.id.delete);
	    viewItem.img_delete.setOnClickListener(delete);
	    viewItem.img_delete.setTag(viewItem);
	}
	
	private int[] getNewViewPosition(ItemSize size){
		switch (size) {
		case min:
			for (Point p : screemPointUse) {
				if (!p.ckeck) 
					return new int[]{p.X,p.Y};
			}
			break;
		case mid_width:
			for (int i = 0; i < screemPointUse.size(); i++) {
				try {
					if (!screemPointUse.get(i).ckeck && !screemPointUse.get(i+1).ckeck ) {
						if (screemPointUse.get(i).X == rowCount-1) 
							continue;
						return new int[]{screemPointUse.get(i).X,screemPointUse.get(i).Y};
					}
				} catch (Exception e) {}
			}  
			break;
		case mid_height:
			for (int i = 0; i < screemPointUse.size(); i++) {
				try {
					if (!screemPointUse.get(i).ckeck && !screemPointUse.get(i+rowCount).ckeck) {
						if (screemPointUse.get(i).Y == columnCount-1)
							continue;
						return new int[]{screemPointUse.get(i).X,screemPointUse.get(i).Y};
					}
				} catch (Exception e) {}  
			}
			break;
		case max:
			for (int i = 0; i < screemPointUse.size(); i++) {
				try {
					if (!screemPointUse.get(i).ckeck && !screemPointUse.get(i+1).ckeck && !screemPointUse.get(i+rowCount).ckeck && !screemPointUse.get(i+rowCount+1).ckeck) {
						if (screemPointUse.get(i).X == rowCount-1 || screemPointUse.get(i).Y == columnCount-1)
							continue;
						return new int[]{screemPointUse.get(i).X,screemPointUse.get(i).Y};
					}
				} catch (Exception e) {}
			}
			break;
		default:
			break;
		}
		return null;
	}
	
	private void clearPointInScreenFromItem(ViewItem viewItem){
		initPoints();
		for (ViewItem item : views) {
			for (Point itemP : item.positions) {
				if (item != viewItem) {
					for (Point p : screemPointUse) {
						if (p.isEqual(itemP)) {
							p.ckeck = true;
							
						}
					} 
				}
			}
		}
	}
	
	private void updateScreenPosition(){

		initPoints();
		for (ViewItem item : views) {
			for (Point itemP : item.positions) {
				for (Point p : screemPointUse) {
					if (p.isEqual(itemP)) {
						p.ckeck = true;
						break;
					}
				}
			}
		}
	}
	
	private void chechOverlap(ViewItem item){
		for (final ViewItem otherItem : views) {
			if (item != otherItem) {
				boolean isOverlap = false;
				for (Point p : item.positions) {
					for (Point otherP : otherItem.positions) {
						if (p.isEqual(otherP)) {
							isOverlap = true;
							clearPointInScreenFromItem(otherItem);
							final int[] start = getNewViewPosition(otherItem.size);
							if (start != null) {
								isNoSpaceToMove = false;
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
							}  
							else {
								if (itemOperator == ItemOperator.reSize) {
									itemResize(item);
									showToast("無空間");
								}
								else {
									Log.i("chauster", "無空間可移動");
									setItemPosition(otherItem, new int[]{otherItem.positions.get(0).X,otherItem.positions.get(0).Y});
									isNoSpaceToMove = true;
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
	
	private void itemResize(final ViewItem item){
		int startX = item.positions.get(0).X;
		int startY = item.positions.get(0).Y;
		RelativeLayout.LayoutParams layoutParamsup = null;

		switch (item.size) {
		case min:  
			if (startX+1>rowCount-1) {
				try {
					startX = getNewViewPosition(ItemSize.mid_width)[0];
					startY = getNewViewPosition(ItemSize.mid_width)[1];
				} catch (Exception e) {}
			}
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth*2, itemHeight);	
			item.setPositions(new int[]{startX,startY},
							  new int[]{startX+1,startY});
		    item.size = ItemSize.mid_width;
			break;
			
		case mid_width: 
			if (startY+1>columnCount-1) {
				try {
					startX = getNewViewPosition(ItemSize.mid_height)[0];
					startY = getNewViewPosition(ItemSize.mid_height)[1];
				} catch (Exception e) {}
			}
			layoutParamsup = new RelativeLayout.LayoutParams(itemWidth, itemHeight*2);
			item.setPositions(new int[]{startX,startY},
							  new int[]{startX,startY+1});
		    item.size = ItemSize.mid_height;
			break;
			
		case mid_height:
			if (startX+1>rowCount-1 || startY+1>columnCount-1) {
				try {
					startX = getNewViewPosition(ItemSize.max)[0];
					startY = getNewViewPosition(ItemSize.max)[1];
				} catch (Exception e) {}
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
			item.positions.get(0).X = start[0]; item.positions.get(1).X = start[0]+1;
			item.positions.get(0).Y = start[1]; item.positions.get(1).Y = start[1];
			break;
		case mid_height:
			item.positions.get(0).X = start[0];
			item.positions.get(0).Y = start[1];
			
			item.positions.get(1).X = start[0];
			item.positions.get(1).Y = start[1]+1;
			break;
		case max:
			item.positions.get(0).X = start[0];   item.positions.get(1).X = start[0]+1;
			item.positions.get(0).Y = start[1];   item.positions.get(1).Y = start[1];
			
			item.positions.get(2).X = start[0];   item.positions.get(3).X = start[0]+1;
			item.positions.get(2).Y = start[1]+1; item.positions.get(3).Y = start[1]+1;
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
			
			db.removeItem(item.id);
		}
	};
	
	OnClickListener resize = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			itemOperator = ItemOperator.reSize;
			ViewItem item = (ViewItem)v.getTag();
			itemResize(item);
		}
	};
	
	OnClickListener switchMode = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (itemActionMode == ItemActionMode.switchMode) {
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
			if (itemActionMode != ItemActionMode.editMode) {
		    	clickItemPointIndex = -1;
				itemActionMode = ItemActionMode.editMode;
	            drawView.setVisibility(View.VISIBLE);
			}
			showEditButton(v);
            v.setAlpha((float)0.5);

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
		itemActionMode = ItemActionMode.switchMode;
		drawView.setVisibility(View.GONE);
		for (ViewItem item : views) {
			item.img_delete.setVisibility(View.GONE);
			item.img_resize.setVisibility(View.GONE);
		}
	}
	
	private void showToast(String msg){
		Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
	}
	
	public void recordItems(){
		for (ViewItem item : views) {
			db.insertItem(item.id, item.size.ordinal(), item.positions.get(0).X, item.positions.get(0).Y);
		}
	}
	
	/****************Resize by zoom out****************/
	int startX = 0;
	int startY = 0;
	OnTouchListener zoom = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			ViewItem item = (ViewItem)v.getTag();
		    switch (event.getAction() & MotionEvent.ACTION_MASK) {
		        case MotionEvent.ACTION_DOWN:
		        	startX = (int) event.getRawX();
		        	startY = (int) event.getRawY();
		            break;
		        case MotionEvent.ACTION_UP: 
	            	break;
		        case MotionEvent.ACTION_MOVE: 
		        	RelativeLayout.LayoutParams lParam = (RelativeLayout.LayoutParams) item.view.getLayoutParams();
		        	int diffX = (int)(event.getRawX() - startX);
		        	int diffY = (int)(event.getRawY() - startY);
		        	lParam.width += diffX;
		        	lParam.height += diffY;
		        	
		        	if (lParam.width > itemWidth * 2) lParam.width = itemWidth * 2;
		        	else if (lParam.width < itemWidth) lParam.width = itemWidth;
		        	
		        	if (lParam.height > itemHeight * 2) lParam.height = itemHeight * 2;
		        	else if (lParam.height < itemHeight) lParam.height = itemHeight;
		        		
		            item.view.setLayoutParams(lParam);
		        	startX = (int) event.getRawX();
		        	startY = (int) event.getRawY();
		        	
		        	
		        	int indexX = ((int)event.getRawX()/itemWidth);
		        	int indexY = ((((int)event.getRawY() - statusBar - actionBar)/itemHeight));
		        	setPostionWhenZoom(item, indexX, indexY);
		        	break;
		    }  
			return true;
		}
	};
	
	private void setPostionWhenZoom(ViewItem item, int X, int Y){
		int x = item.positions.get(0).X;
		int y = item.positions.get(0).Y;
		switch (item.size) {
		case min:
			try {
				if (item.positions.get(0).X < X && item.positions.get(0).Y == Y) setmid_width(item, x, y);
				else if (item.positions.get(0).X == X && item.positions.get(0).Y < Y) setmid_height(item, x, y); 
				else if (item.positions.get(0).X < X && item.positions.get(0).Y < Y) setmax(item, x, y);
			} catch (Exception e) {
				// TODO: handle exception
			}

			break;
		case mid_width:
			try {
				if (X < item.positions.get(1).X && item.positions.get(1).Y == Y) setmin(item, x, y);
				else if (item.positions.get(1).X == X && item.positions.get(1).Y < Y) setmax(item, x, y);
				else if (X < item.positions.get(1).X && item.positions.get(1).Y < Y) setmid_height(item, x, y);
			} catch (Exception e) {
				// TODO: handle exception
			}

			break;
		case mid_height:
			try {
				if (item.positions.get(1).X == X&& Y < item.positions.get(1).Y) setmin(item, x, y);
				else if (item.positions.get(1).X < X && item.positions.get(1).Y == Y) setmax(item, x, y);
				else if (item.positions.get(1).X < X&& Y < item.positions.get(1).Y) setmid_width(item, x, y);
			} catch (Exception e) {
				// TODO: handle exception
			}

			break;
		case max:
			try {
				if (X < item.positions.get(3).X && Y < item.positions.get(3).Y) setmin(item, x, y);
				else if (X < item.positions.get(3).X && item.positions.get(3).Y == Y) setmid_height(item, x, y); 
				else if (item.positions.get(3).X == X && item.positions.get(3).Y < Y) setmid_width(item, x, y);
			} catch (Exception e) {
				// TODO: handle exception
			}

			break;

		default:
			break;
		}
		updateScreenPosition();
		chechOverlap(item);
	}
	
	private void setmin(ViewItem item, int x, int y){
		item.setPositions(new int[]{x,y});
		item.size = ItemSize.min;
	}
	
	private void setmid_width(ViewItem item, int x, int y){
		item.setPositions(new int[]{x,y},
		  		  new int[]{x+1,y});
		item.size = ItemSize.mid_width;
	}
	
	private void setmid_height(ViewItem item, int x, int y){
		item.setPositions(new int[]{x,y},
		  		  new int[]{x,y+1});
		item.size = ItemSize.mid_height;
	}
	
	private void setmax(ViewItem item, int x, int y){
		item.setPositions(new int[]{x,y},
		  		  new int[]{x+1,y},
		  		  new int[]{x,y+1},
		  		  new int[]{x+1,y+1});
		item.size = ItemSize.max;
	}

}
