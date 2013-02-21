package com.example.imagegallery;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class ImageActivity extends Activity {
	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.full_image);

		// Instantiate download progress bar
		progressDialog = new ProgressDialog(ImageActivity.this);
		progressDialog.setMessage("Fetching image");
		progressDialog.setIndeterminate(false);
		progressDialog.setMax(100);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		Intent i = getIntent();
		String url = i.getExtras().getString("image");

		// Initiate asynchronous image download
		DownloadFile downloadFile = new DownloadFile();
		downloadFile.execute(url);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class DownloadFile extends AsyncTask<String, Integer, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... urls) {
			try {
				URL url = new URL(urls[0]);
				URLConnection conection = url.openConnection();
				conection.connect();

				// input stream to read file - with 8k buffer
				InputStream input = new BufferedInputStream(url.openStream(), 8192);

				// display progress update
				onProgressUpdate(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
				return BitmapFactory.decodeStream(input);
			} catch (Exception e) {
				Log.e("Error: ", e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			for(int i = 0; i < progress.length; i++){
				progressDialog.setProgress(progress[i]);
				
				// make a short pause between progress update for smoother visual effect
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			progressDialog.cancel();

			TouchImageView imageView = (TouchImageView) findViewById(R.id.full_image_view);
			imageView.setImageBitmap(bitmap);

			// Parse metadata from intent
			Intent intent = getIntent();
			int numLikes = intent.getExtras().getInt("likes");
			String caption = intent.getExtras().getString("caption");
			String creator = intent.getExtras().getString("createdBy");

			// Populate metadata on full image view
			TextView text = (TextView) findViewById(R.id.caption);
			text.setText(caption);

			TextView createdBy = (TextView) findViewById(R.id.createdBy);
			createdBy.setText("Created by: " + creator);

			TextView likes = (TextView) findViewById(R.id.likes);
			likes.setText("Likes: " + numLikes);
		}
	}
}