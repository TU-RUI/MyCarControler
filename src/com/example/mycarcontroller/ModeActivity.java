package com.example.mycarcontroller;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ModeActivity extends Activity implements OnClickListener {
	private Button mode1, mode2, mode3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mode);
		mode1 = (Button) findViewById(R.id.mode1);//mode1单独控制小车
		mode2 = (Button) findViewById(R.id.mode2);//mode2控制小车上的手机
		mode3 = (Button) findViewById(R.id.mode3);//mode3放小车上控制小车
		mode1.setOnClickListener(this);
		mode2.setOnClickListener(this);
		mode3.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		switch (v.getId()) {
		case R.id.mode1:
			intent.setClass(ModeActivity.this, Mode1Activity.class);
			break;
		case R.id.mode2:
			intent.setClass(ModeActivity.this, Mode2Activity.class);
			break;
		case R.id.mode3:
			intent.setClass(ModeActivity.this, Mode3Activity.class);
			break;
		}
		startActivity(intent);
		ModeActivity.this.finish();
	}

}
