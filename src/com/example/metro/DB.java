package com.example.metro;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

@SuppressLint({ "NewApi" })
public class DB {

	public class RecordItem {
		public String id;
		public int size;
		public int row;
		public int colume;
	}
	
	private Context ctx;
	private String dbName;
	public DB(Context ctx, String dbName){
		this.ctx = ctx;
		this.dbName = dbName;
	}
	
	public ArrayList<RecordItem> getItems(){
		SharedPreferences prefs = ctx.getSharedPreferences(dbName,0);
		Set<String> items = prefs.getStringSet("items", null);
		ArrayList<RecordItem> arrayList = new ArrayList<DB.RecordItem>();
		if (items != null) {
			for (String string : items) {
				SharedPreferences pref = ctx.getSharedPreferences(string,0);
				RecordItem item = new RecordItem();
				item.id = pref.getString("id", null);
				item.row = pref.getInt("row", 0);
				item.colume = pref.getInt("column", 0);
				item.size = pref.getInt("size", 0);
				arrayList.add(item);
			}
		}
		return arrayList;
	} 
	
	public void insertItem(String ID, int size, int row, int column){
		SharedPreferences prefs = ctx.getSharedPreferences(dbName,0);
		Set<String> items = prefs.getStringSet("items", new HashSet<String>());
		items.add(ID);
		Editor editors = prefs.edit();
		editors.clear();
		editors.putStringSet("items", items);
		editors.commit();
		
		SharedPreferences pref = ctx.getSharedPreferences(ID,0);
		Editor editor = pref.edit();
		editor.clear();
		editor.putString("id", ID);
		editor.putInt("size", size);
		editor.putInt("row", row);
		editor.putInt("column", column);
		editor.commit();
	}
	
	public void removeItem(String ID) {
		SharedPreferences prefs = ctx.getSharedPreferences(dbName,0);
		Set<String> items = prefs.getStringSet("items", new HashSet<String>());
		items.remove(ID);
		Editor editors = prefs.edit();
		editors.clear();
		editors.putStringSet("items", items);
		editors.commit();
	}
}
