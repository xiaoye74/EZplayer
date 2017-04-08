package com.example.xiangqian.ezplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener
,MediaPlayer.OnCompletionListener,SensorEventListener{
    Uri uri;
    TextView name,Turi;
    boolean isvideo=false;
    Button play,stop;
    CheckBox ch;
    MediaPlayer mper;
    Toast tos;
    SensorManager sm;
    Sensor sr;
    int delay=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        name = (TextView) findViewById(R.id.name);
        play= (Button) findViewById(R.id.play);
        stop= (Button) findViewById(R.id.stop);
        ch = (CheckBox) findViewById(R.id.select);
        Turi = (TextView) findViewById(R.id.Turi);
        uri=Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.better);
        name.setText("better.mp3");
        Turi.setText(uri.toString());
        mper=new MediaPlayer();
        mper.setOnPreparedListener(this);
        mper.setOnErrorListener(this);
        mper.setOnCompletionListener(this);
        tos=Toast.makeText(this,"",Toast.LENGTH_SHORT);

        prepareMusic();
        sm= (SensorManager) getSystemService(SENSOR_SERVICE);
        sr=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    void prepareMusic(){
        play.setText("播放");
        play.setEnabled(false);
        stop.setEnabled(false);
        try {
            mper.reset();
            mper.setDataSource(this,uri);
            mper.setLooping(ch.isChecked());
            mper.prepareAsync();
        }catch (Exception e){
            tos.setText("指定文件出错！"+e.toString());
            tos.show();
        }
    }
    public void onclick(View v){
        Intent it=new Intent(Intent.ACTION_GET_CONTENT);
        if (v.getId()==R.id.audio){
            it.setType("audio/*");
            startActivityForResult(it,100);
        }
        else{
            it.setType("video/*");
            startActivityForResult(it,101);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode== Activity.RESULT_OK){
            isvideo=(requestCode==101);
            uri=convertUri(data.getData());
            name.setText((isvideo?"视频：":"歌曲：")+uri.getLastPathSegment());
            Turi.setText("文件位置："+uri.getPath());
            if (!isvideo) prepareMusic();
        }
    }
    Uri convertUri(Uri uri){
        if (uri.toString().substring(0,7).equals("content")){
            String[]a={MediaStore.MediaColumns.DATA};
            Cursor cursor=getContentResolver().query(uri,a,null,null,null);
            cursor.moveToFirst();
            uri=Uri.parse("file://"+cursor.getString(0));
            cursor.close();
        }
        return uri;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mper.seekTo(0);
        play.setText("播放");
        stop.setEnabled(false);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        tos.setText("发生错误，停止播放");
        tos.show();
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        play.setEnabled(true);
    }
    public void onMpPlayer(View view){
        if (isvideo){
            Intent it=new Intent(this,Video.class);
            it.putExtra("uri",uri.toString());
            startActivity(it);
            return;
        }
        if (mper.isPlaying()){
            mper.pause();
            play.setText("继续");
        }
        else {
            mper.start();
            play.setText("暂停");
            stop.setEnabled(true);
        }
    }
    public void stop(View view){
        mper.pause();
        mper.seekTo(0);
        play.setText("播放");
        stop.setEnabled(false);
    }
    public void loop(View view){
        if (ch.isChecked()) mper.setLooping(true);
        else mper.setLooping(false);
    }
    public void back(View view){
        if (!play.isEnabled()) return;
        int len=mper.getDuration();
        int pos=mper.getCurrentPosition();
        pos-=1000;
        if (pos<0) pos=0;
        mper.seekTo(pos);
        tos.setText("倒退十秒"+pos/1000+"/"+len/1000);
        tos.show();
    }
    public void forw(View view){
        if (!play.isEnabled()) return;
        int len=mper.getDuration();
        int pos=mper.getCurrentPosition();
        pos+=1000;
        if (pos>len) pos=len;
        mper.seekTo(pos);
        tos.setText("前进十秒"+pos/1000+"/"+len/1000);
        tos.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this,sr,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mper.isPlaying()){
            play.setText("继续");
            mper.pause();
        }
        sm.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        mper.release();
        super.onDestroy();

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x,y,z;
        x=sensorEvent.values[0];
        y=sensorEvent.values[1];
        z=sensorEvent.values[2];
        if (Math.abs(y)<1 && Math.abs(x) < 1 && z < -9){
            if (mper.isPlaying()){
                play.setText("继续");
                mper.pause();
            }
        }
        else {
            if (delay>0) delay--;
            else {
                if (Math.abs(x)+Math.abs(y)+Math.abs(z)>32){
                    if (play.isEnabled()) onMpPlayer(null);
                    delay=5;
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
