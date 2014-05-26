package com.example.metro;

import java.util.ArrayList;

import android.view.View;

public class ViewItem {

	public View view;
	public int tag;
	public ArrayList<Point> positions;
	
	public void setPositions(int[]... positions){
		this.positions = new ArrayList<Point>();
		for (int[] position : positions) {
			Point p = new Point();
			p.X = position[0];
			p.Y = position[1];
			this.positions.add(p);
		}
	}
	
	public ViewItem(int[]... positions){
		this.positions = new ArrayList<Point>();
		for (int[] position : positions) {
			Point p = new Point();
			p.X = position[0];
			p.Y = position[1];
			this.positions.add(p);
		}
	}
	
	public ItemSize size;
	public enum ItemSize {
		min,
		mid_width,
		mid_height,
		max
	}
}
