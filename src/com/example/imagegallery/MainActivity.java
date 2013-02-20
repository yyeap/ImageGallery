package com.example.imagegallery;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
	private static final int CONNECTION_TIMEOUT = 60000; // one minute timeout
	private static final int DATARETRIEVAL_TIMEOUT = 60000;
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get GridView from xml 
		GridView gridView = (GridView) findViewById(R.id.grid_view); 

		// Get images
		final List<Image> images = getImages();

		// Get the url of each images
		List<String> imageUrl = new ArrayList<String>(images.size());

		// Populate url list
		for(Image curr : images)
			imageUrl.add(curr.getThumbnail());

		// instantiate image downloader
		ImageDownloader imageDownloader = new ImageDownloader();

		// Set Adapter for GridView 
		gridView.setAdapter(new ImageAdapter(this, imageUrl, imageDownloader));

		// On Click event for Single GridView Item 
		gridView.setOnItemClickListener(new OnItemClickListener(){
			@SuppressLint("NewApi")
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				Intent intent = new Intent(MainActivity.this, ImageActivity.class);
				intent.putExtra("image", images.get(position).getURL());
				intent.putExtra("likes", images.get(position).getNumLikes());
				intent.putExtra("caption", images.get(position).getCaption());
				intent.putExtra("createdBy", images.get(position).getCcreatedBy());
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public List<Image> getImages() {
		JSONObject serviceResult = requestWebPage("http://cs.wisc.edu/~griepent/instagram.json");
		List<Image> images = new ArrayList<Image>();

		try {
			JSONArray results = serviceResult.getJSONArray("data");

			for (int i = 0; i < results.length(); i++) {
				JSONObject data = results.getJSONObject(i);

				String imageURL = data.getJSONObject("images").getJSONObject("standard_resolution").getString("url");
				String thumbNailsURL = data.getJSONObject("images").getJSONObject("thumbnail").getString("url");
				JSONObject caption = data.getJSONObject("caption");
				String captionText = caption.getString("text");
				String createdBy = caption.getJSONObject("from").getString("username");
				int numLikes = data.getJSONObject("likes").getInt("count");

				images.add(new Image(imageURL, thumbNailsURL,captionText, createdBy, numLikes));
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return images;
	}
	public static JSONObject requestWebService(String serviceUrl) {
		disableConnectionReuseIfNecessary();

		HttpURLConnection urlConnection = null;
		try {
			// create connection
			URL urlToRequest = new URL(serviceUrl);
			urlConnection = (HttpURLConnection)
					urlToRequest.openConnection();
			urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
			urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);

			// handle issues
			int statusCode = urlConnection.getResponseCode();
			if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				// handle unauthorized (if service requires user login)
				System.err.println("Must authenticate first!");
				System.exit(0);
			} else if (statusCode != HttpURLConnection.HTTP_OK) {
				// handle any other errors, like 404, 500,..
				System.err.println("Error occurred!");
				System.exit(0);
			}

			// create JSON object from content
			InputStream in = new BufferedInputStream(
					urlConnection.getInputStream());
			return new JSONObject(getResponseText(in));

		} catch (MalformedURLException e) {
			// URL is invalid
		} catch (SocketTimeoutException e) {
			// data retrieval or connection timed out
		} catch (IOException e) {
			// could not read response body
			// (could not create input stream)
		} catch (JSONException e) {
			// response body is no valid JSON string
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}	

		return null;
	}
	private JSONObject requestWebPage(String url) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet( url );
			HttpResponse responseGet = client.execute(get);
			HttpEntity resEntityGet = responseGet.getEntity();
			//do something with the response
			return new JSONObject(EntityUtils.toString(resEntityGet));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * required in order to prevent issues in earlier Android version.
	 */
	private static void disableConnectionReuseIfNecessary() {
		// see HttpURLConnection API doc
		if (Integer.parseInt(Build.VERSION.SDK)
				< Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	private static String getResponseText(InputStream inStream) {
		// very nice trick from
		// http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
		return new Scanner(inStream).useDelimiter("\\A").next();
	}

	private class Image
	{
		private String imageURL;
		private String thumbnailURL;
		private String caption;
		private String createdBy;
		private int numLikes;

		public Image(String URL, String thumbnailURL, String caption, String createdBy, int likes){
			this.numLikes = likes;
			this.thumbnailURL = thumbnailURL;
			this.imageURL = URL;
			this.caption = caption;
			this.createdBy = createdBy;
		}

		public String getCcreatedBy(){
			return this.createdBy;
		}

		public String getCaption(){
			return this.caption;
		}

		public int getNumLikes(){
			return this.numLikes;
		}

		public String getURL(){
			return this.imageURL;
		}

		public String getThumbnail(){
			return this.thumbnailURL;
		}
	}
}

