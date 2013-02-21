package com.example.imagegallery;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.BaseAdapter;


public class ImageAdapter extends BaseAdapter {
	private Context mContext;
	private List<String> imageUrl;
	private ImageDownloader imageDownloader;

	// Constructor
	public ImageAdapter(Context c, List<String> imageUrl, ImageDownloader imageDownloader){
		this.mContext = c;
		this.imageUrl = imageUrl;
		this.imageDownloader = imageDownloader;
	}

	@Override 
	public int getCount() 
	{ 
		return this.imageUrl.size(); 
	} 

	@Override 
	public Object getItem(int position) 
	{ 
		return this.imageUrl.get(position); 
	}

	@Override 
	public View getView(int position, View convertView, ViewGroup parent) 
	{ 
		ImageView imageView; 
		if(convertView == null) // Recycled View 
		{ 
			imageView = new ImageView(this.mContext); 
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER); 
			imageView.setLayoutParams(new GridView.LayoutParams(220, 220)); 
		} 
		else // Re-use the view 
		{
			imageView = (ImageView) convertView; 
		} 
		this.imageDownloader.download(this.imageUrl.get(position), imageView);
		return imageView; 
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Drawable loadImage(String url)
	{

		Drawable d = null;
		try{
			InputStream is = (InputStream) new URL(url).getContent();
			d = Drawable.createFromStream(is, "src");

		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return d;
	}
}
