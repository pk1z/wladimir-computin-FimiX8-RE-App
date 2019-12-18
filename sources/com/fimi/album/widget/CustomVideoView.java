package com.fimi.album.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.example.album.R;
import com.fimi.kernel.utils.LogUtil;
import java.util.Formatter;
import java.util.Locale;

public class CustomVideoView extends RelativeLayout implements OnClickListener, SurfaceTextureListener, OnPreparedListener, OnCompletionListener, OnInfoListener, OnErrorListener, OnBufferingUpdateListener, OnSeekBarChangeListener {
    private static final int LOAD_TOTAL_COUNT = 3;
    private static final int STATE_ERROR = 1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_PLAYING = 1;
    private static final String TAG = "CustomVideoView";
    private static final int TIME_INVAL = 1000;
    private static final int TIME_MSG = 1;
    private static float VIDEO_HEIGHT_PERCENT = 0.5625f;
    private AudioManager audioManager;
    private boolean canPlay = true;
    private boolean isMute;
    private boolean isUpdateProgressed = false;
    private VideoPlayerListener listener;
    private RelativeLayout mBottomPlayRl;
    private int mCurrentCount;
    private TextView mCurrentTimeTv;
    private int mDestationHeight;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (CustomVideoView.this.isPlaying()) {
                        if (!CustomVideoView.this.isUpdateProgressed) {
                            int currentPosition = (int) (Math.round(((double) CustomVideoView.this.getCurrentPosition()) / 1000.0d) * 1000);
                            CustomVideoView.this.mPlaySb.setProgress(currentPosition);
                            CustomVideoView.this.mCurrentTimeTv.setText(CustomVideoView.this.setTimeFormatter(currentPosition));
                        }
                        if (CustomVideoView.this.listener != null) {
                            CustomVideoView.this.listener.onBufferUpdate(CustomVideoView.this.getCurrentPosition());
                        }
                        sendEmptyMessageDelayed(1, 1000);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mIsComplete;
    private boolean mIsRealPause;
    private ProgressBar mLoadingBar;
    private ImageButton mMiniPlayBtn;
    private ViewGroup mParentContainar;
    private ImageButton mPlayBackIBtn;
    private SeekBar mPlaySb;
    private RelativeLayout mPlayerView;
    private ScreenEventReciver mScreenEventReciver;
    private int mScreenWidth;
    private LinearLayout mTopBarLl;
    private TextView mTotalTimeTv;
    private String mUrl;
    private TextureView mVideoView;
    private MediaPlayer mediaPlayer;
    private TextView nameTv;
    private int playerState = 0;
    private int showTopBottomBarTime = 0;
    private Surface videoSurface;

    public interface VideoPlayerListener {
        void onAdVideoLoadComplete();

        void onAdVideoLoadFailed();

        void onAdVideoLoadSuccess();

        void onBufferUpdate(int i);

        void onClickBackBtn();

        void onClickFullScreenBtn();

        void onClickPlay();

        void onClickVideo();
    }

    public interface ADFrameImageLoadListener {
        void onStartFrameLoad(String str, ImageLoaderListener imageLoaderListener);
    }

    public interface ImageLoaderListener {
        void onLoadingComplete(Bitmap bitmap);
    }

    private class ScreenEventReciver extends BroadcastReceiver {
        private ScreenEventReciver() {
        }

        /* synthetic */ ScreenEventReciver(CustomVideoView x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            Log.i(CustomVideoView.TAG, "onReceive: " + intent.getAction());
            String action = intent.getAction();
            Object obj = -1;
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        int obj2 = 1;
                        break;
                    }
                    break;
                case 823795052:
                    if (action.equals("android.intent.action.USER_PRESENT")) {
                        obj2 = null;
                        break;
                    }
                    break;
            }
            switch (obj2) {
                case null:
                    if (CustomVideoView.this.playerState != 2) {
                        return;
                    }
                    if (CustomVideoView.this.mIsRealPause) {
                        CustomVideoView.this.pause();
                        return;
                    } else {
                        CustomVideoView.this.resume();
                        return;
                    }
                case 1:
                    if (CustomVideoView.this.playerState == 1) {
                        CustomVideoView.this.pause();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void timeFunction() {
        if (this.mBottomPlayRl.getVisibility() == 0) {
            LogUtil.i(TAG, "handleMessage:visible");
            this.showTopBottomBarTime++;
            if (this.showTopBottomBarTime >= 5) {
                this.showTopBottomBarTime = 0;
                showBar(false);
                return;
            }
            return;
        }
        this.showTopBottomBarTime = 0;
    }

    public CustomVideoView(Context context, ViewGroup parentContainer) {
        super(context);
        this.mParentContainar = parentContainer;
        this.audioManager = (AudioManager) getContext().getSystemService("audio");
        initDate();
        initView();
        registerBroadcastReceiver();
    }

    private void initDate() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay().getMetrics(dm);
        this.mScreenWidth = dm.widthPixels;
        this.mDestationHeight = (int) (((float) this.mScreenWidth) * VIDEO_HEIGHT_PERCENT);
        this.mFormatBuilder = new StringBuilder();
        this.mFormatter = new Formatter(this.mFormatBuilder, Locale.getDefault());
    }

    private void initView() {
        this.mPlayerView = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.album_custom_video_view, this);
        this.mPlayerView.setOnClickListener(this);
        this.mLoadingBar = (ProgressBar) this.mPlayerView.findViewById(R.id.load_iv);
        this.mVideoView = (TextureView) this.mPlayerView.findViewById(R.id.play_video_textureview);
        this.mVideoView.setOnClickListener(this);
        this.mVideoView.setKeepScreenOn(true);
        this.mVideoView.setSurfaceTextureListener(this);
        this.mTopBarLl = (LinearLayout) this.mPlayerView.findViewById(R.id.shoto_top_tab_ll);
        this.mBottomPlayRl = (RelativeLayout) this.mPlayerView.findViewById(R.id.bottom_play_rl);
        this.mMiniPlayBtn = (ImageButton) this.mBottomPlayRl.findViewById(R.id.play_btn);
        this.mPlaySb = (SeekBar) this.mBottomPlayRl.findViewById(R.id.play_sb);
        this.mPlaySb.setOnSeekBarChangeListener(this);
        this.mMiniPlayBtn.setOnClickListener(this);
        this.mCurrentTimeTv = (TextView) this.mBottomPlayRl.findViewById(R.id.time_current_tv);
        this.mTotalTimeTv = (TextView) this.mBottomPlayRl.findViewById(R.id.total_time_tv);
        showBar(false);
        this.nameTv = (TextView) findViewById(R.id.photo_name_tv);
        this.mPlayBackIBtn = (ImageButton) findViewById(R.id.media_back_btn);
        this.mPlayBackIBtn.setOnClickListener(this);
        this.mPlayerView.setOnClickListener(this);
        LayoutParams params = new LayoutParams(this.mScreenWidth, this.mDestationHeight);
        params.addRule(13);
        this.mPlayerView.setLayoutParams(params);
    }

    public void setDataSource(String url) {
        this.mUrl = url;
    }

    public void onClick(View view) {
        if (view.getId() == R.id.play_video_textureview) {
            if (this.mTopBarLl.getVisibility() == 0) {
                this.mTopBarLl.setVisibility(8);
                this.mBottomPlayRl.setVisibility(8);
                return;
            }
            this.mTopBarLl.setVisibility(0);
            this.mBottomPlayRl.setVisibility(0);
        } else if (view.getId() == R.id.play_btn) {
            if (this.playerState == 2) {
                seekAndResume(this.mPlaySb.getProgress());
            } else if (this.playerState == 0) {
                load();
            } else {
                pause();
            }
        } else if (view.getId() == R.id.media_back_btn && this.listener != null) {
            this.listener.onClickBackBtn();
        }
    }

    private void registerBroadcastReceiver() {
        if (this.mScreenEventReciver == null) {
            this.mScreenEventReciver = new ScreenEventReciver(this, null);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.USER_PRESENT");
            getContext().registerReceiver(this.mScreenEventReciver, intentFilter);
        }
    }

    private void unRegisterBroadcastReceiver() {
        if (this.mScreenEventReciver != null) {
            getContext().unregisterReceiver(this.mScreenEventReciver);
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        LogUtil.d(TAG, "onSurfaceTextureAvailable");
        this.videoSurface = new Surface(surfaceTexture);
        checkMediaPlayer();
        this.mediaPlayer.setSurface(this.videoSurface);
        load();
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        LogUtil.d(TAG, "onSurfaceTextureSizeChanged");
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        Log.d(TAG, "onVisibilityChanged: " + visibility + "," + this.mMiniPlayBtn.getVisibility());
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != 0 || this.playerState != 2) {
            pause();
        } else if (isRealPause() || isComplete()) {
            pause();
        } else {
            resume();
        }
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        LogUtil.i(TAG, "onPrepare");
        showPlayView();
        this.mPlaySb.setMax(getDuration());
        this.mTotalTimeTv.setText(setTimeFormatter(getDuration()));
        this.mediaPlayer = mediaPlayer;
        if (this.mediaPlayer != null) {
            mediaPlayer.setOnBufferingUpdateListener(this);
            this.mCurrentCount = 0;
            if (this.listener != null) {
                this.listener.onAdVideoLoadSuccess();
            }
            setCurrentPlayState(2);
            resume();
        }
    }

    public void onCompletion(MediaPlayer mp) {
        LogUtil.i(TAG, "onCompletion");
        if (this.listener != null) {
            this.listener.onAdVideoLoadComplete();
        }
        playBack();
        setIsComplete(true);
        setIsRealPause(true);
    }

    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.i(TAG, "do error:" + what + ",extra:" + extra);
        this.playerState = 1;
        this.mediaPlayer = mp;
        if (this.mediaPlayer != null) {
            this.mediaPlayer.reset();
        }
        if (this.mCurrentCount >= 3) {
            showPauseView(false);
            if (this.listener != null) {
                this.listener.onAdVideoLoadFailed();
            }
        }
        stop();
        return false;
    }

    public void load() {
        LogUtil.i(TAG, "load:" + this.mUrl);
        if (this.playerState == 0) {
            showLoadingView();
            try {
                setCurrentPlayState(0);
                checkMediaPlayer();
                mute(true);
                this.mediaPlayer.setDataSource(this.mUrl);
                this.nameTv.setText(this.mUrl.substring(this.mUrl.lastIndexOf("/") + 1));
                this.mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
                stop();
            }
        }
    }

    public void stop() {
        LogUtil.i(TAG, "do stop");
        if (this.mediaPlayer != null) {
            this.mediaPlayer.reset();
            this.mediaPlayer.setOnSeekCompleteListener(null);
            this.mediaPlayer.stop();
            this.mediaPlayer.release();
            this.mediaPlayer = null;
        }
        this.mHandler.removeCallbacksAndMessages(null);
        setCurrentPlayState(0);
        if (this.mCurrentCount <= 3) {
            this.mCurrentCount++;
            load();
            return;
        }
        showPauseView(false);
    }

    public void pause() {
        if (this.playerState == 1) {
            LogUtil.i(TAG, "do full pause");
            setCurrentPlayState(2);
            if (isPlaying()) {
                this.mediaPlayer.pause();
                if (!this.canPlay) {
                    this.mediaPlayer.seekTo(0);
                }
            }
            showPauseView(false);
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void resume() {
        if (this.playerState == 2) {
            mute(false);
            LogUtil.i(TAG, "resume");
            if (isPlaying()) {
                showPauseView(false);
                return;
            }
            entryResumeState();
            this.mediaPlayer.setOnSeekCompleteListener(null);
            this.mediaPlayer.start();
            this.mHandler.sendEmptyMessage(1);
            showPauseView(true);
        }
    }

    public void destory() {
        LogUtil.i(TAG, "do destory");
        if (this.mediaPlayer != null) {
            this.mediaPlayer.setOnSeekCompleteListener(null);
            this.mediaPlayer.stop();
            this.mediaPlayer.release();
            this.mediaPlayer = null;
        }
        setCurrentPlayState(0);
        this.mCurrentCount = 0;
        setIsComplete(false);
        setIsRealPause(false);
        unRegisterBroadcastReceiver();
        this.mHandler.removeCallbacksAndMessages(null);
        showPauseView(false);
    }

    public void playBack() {
        LogUtil.i(TAG, " do play back");
        setCurrentPlayState(2);
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mediaPlayer != null) {
            this.mediaPlayer.setOnSeekCompleteListener(null);
            this.mediaPlayer.seekTo(0);
            this.mediaPlayer.pause();
        }
        showPauseView(false);
    }

    private void setCurrentPlayState(int state) {
        this.playerState = state;
    }

    public boolean isPlaying() {
        if (this.mediaPlayer == null || !this.mediaPlayer.isPlaying()) {
            return false;
        }
        return true;
    }

    private synchronized void checkMediaPlayer() {
        if (this.mediaPlayer == null) {
            this.mediaPlayer = createMediaPlayer();
        }
    }

    private MediaPlayer createMediaPlayer() {
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.reset();
        this.mediaPlayer.setOnPreparedListener(this);
        this.mediaPlayer.setOnCompletionListener(this);
        this.mediaPlayer.setOnInfoListener(this);
        this.mediaPlayer.setOnErrorListener(this);
        this.mediaPlayer.setAudioStreamType(3);
        if (this.videoSurface == null || !this.videoSurface.isValid()) {
            stop();
        } else {
            this.mediaPlayer.setSurface(this.videoSurface);
        }
        return this.mediaPlayer;
    }

    private void entryResumeState() {
        this.canPlay = true;
        setCurrentPlayState(1);
        setIsRealPause(false);
        setIsComplete(false);
    }

    public void setIsComplete(boolean isComplete) {
        this.mIsComplete = isComplete;
    }

    public void setIsRealPause(boolean isRealPause) {
        this.mIsRealPause = isRealPause;
    }

    public void mute(boolean mute) {
        this.isMute = mute;
        if (this.mediaPlayer != null && this.audioManager != null) {
            float volume = this.isMute ? 0.0f : 1.0f;
            this.mediaPlayer.setVolume(volume, volume);
        }
    }

    public void seekAndResume(int position) {
        if (this.mediaPlayer != null) {
            showPauseView(true);
            entryResumeState();
            this.mediaPlayer.seekTo(position);
            this.mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                public void onSeekComplete(MediaPlayer mp) {
                    LogUtil.d(CustomVideoView.TAG, "do seek and resume");
                    CustomVideoView.this.mediaPlayer.start();
                    CustomVideoView.this.mHandler.sendEmptyMessage(1);
                }
            });
        }
    }

    public void seekAndPause(int position) {
        if (this.playerState == 1) {
            showPauseView(false);
            setCurrentPlayState(2);
            if (isPlaying()) {
                this.mediaPlayer.seekTo(position);
                this.mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                    public void onSeekComplete(MediaPlayer mp) {
                        LogUtil.d(CustomVideoView.TAG, "do seek and pause");
                        CustomVideoView.this.mediaPlayer.pause();
                        CustomVideoView.this.mHandler.removeCallbacksAndMessages(null);
                    }
                });
            }
        }
    }

    private void showPauseView(boolean show) {
        this.mLoadingBar.setVisibility(8);
        this.mMiniPlayBtn.setImageResource(show ? R.drawable.album_btn_pause_media : R.drawable.album_icon_play_media);
    }

    private void showLoadingView() {
        this.mLoadingBar.setVisibility(0);
        this.mMiniPlayBtn.setImageResource(R.drawable.album_icon_play_media);
        this.showTopBottomBarTime = 0;
    }

    private void showPlayView() {
        this.mLoadingBar.setVisibility(8);
        this.mMiniPlayBtn.setImageResource(R.drawable.album_btn_pause_media);
        this.showTopBottomBarTime = 0;
    }

    private void showBar(boolean isShow) {
        int i;
        int i2 = 0;
        LinearLayout linearLayout = this.mTopBarLl;
        if (isShow) {
            i = 0;
        } else {
            i = 8;
        }
        linearLayout.setVisibility(i);
        RelativeLayout relativeLayout = this.mBottomPlayRl;
        if (!isShow) {
            i2 = 8;
        }
        relativeLayout.setVisibility(i2);
    }

    public int getCurrentPosition() {
        if (this.mediaPlayer != null) {
            return this.mediaPlayer.getCurrentPosition();
        }
        return -1;
    }

    public int getDuration() {
        if (this.mediaPlayer != null) {
            return this.mediaPlayer.getDuration();
        }
        return -1;
    }

    private String setTimeFormatter(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        this.mFormatBuilder.setLength(0);
        if (hours > 0) {
            return this.mFormatter.format("%d:%02d:%02d", new Object[]{Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)}).toString();
        }
        return this.mFormatter.format("%02d:%02d", new Object[]{Integer.valueOf(minutes), Integer.valueOf(seconds)}).toString();
    }

    public boolean isRealPause() {
        return this.mIsRealPause;
    }

    public boolean isComplete() {
        return this.mIsComplete;
    }

    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            LogUtil.i(TAG, "onProgressChanged:" + this.isUpdateProgressed);
            this.isUpdateProgressed = true;
            this.showTopBottomBarTime = 0;
            this.mCurrentTimeTv.setText(setTimeFormatter(seekBar.getProgress()));
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        LogUtil.i(TAG, "onStartTrackingTouch:" + this.isUpdateProgressed);
        this.isUpdateProgressed = true;
        this.showTopBottomBarTime = 0;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        LogUtil.i(TAG, "onStopTrackingTouch:" + this.isUpdateProgressed);
        this.isUpdateProgressed = false;
        if (this.playerState == 1) {
            seekAndResume(seekBar.getProgress());
        }
    }

    public void setListener(VideoPlayerListener listener) {
        this.listener = listener;
    }
}
