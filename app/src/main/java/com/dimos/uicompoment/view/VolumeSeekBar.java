package com.dimos.uicompoment.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;

public class VolumeSeekBar extends SeekBar {

    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private Context mContext;
    private AudioManager mAudioManager;
    private VolumeChangeObserver mVolumeChangeObserver;
    private boolean isTouched;
    private int mMaxVolume;
    private static final int MEDIA_TYPE = AudioManager.STREAM_MUSIC;

    public VolumeSeekBar(Context context) {
        this(context, null);
    }

    public VolumeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VolumeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mContext = context;
        mVolumeChangeObserver = new VolumeChangeObserver();
        mMaxVolume = mAudioManager.getStreamMaxVolume(MEDIA_TYPE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 0 || progress > 100) {
                    return;
                }
                mAudioManager.setStreamVolume(MEDIA_TYPE, (int) Math.ceil(progress / 100.0 * mMaxVolume), 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTouched = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTouched = false;
            }
        });
        mVolumeChangeObserver.registerReceiver();
        mVolumeChangeObserver.setVolumeChangeListener(new VolumeChangeListener() {
            @Override
            public void onVolumeChanged(int volume) {
                if (!isTouched) {
                    setSeekProgress(volume);
                }
            }
        });
        setSeekProgress(mAudioManager.getStreamVolume(MEDIA_TYPE));
    }

    private void setSeekProgress(int volume) {
        setProgress((int) (volume / (float) getMaxVolume() * 100), true);
    }

    private int getMaxVolume() {
        return mMaxVolume;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVolumeChangeObserver.unregisterReceiver();
    }

    interface VolumeChangeListener {
        void onVolumeChanged(int volume);
    }

    class VolumeChangeObserver {

        private VolumeChangeListener mVolumeChangeListener;
        private VolumeBroadcastReceiver mVolumeBroadcastReceiver;
        private boolean mRegistered = false;

        public VolumeChangeObserver() {
        }

        public int getCurrentMusicVolume() {
            return mAudioManager != null ? mAudioManager.getStreamVolume(MEDIA_TYPE) : -1;
        }

        public VolumeChangeListener getVolumeChangeListener() {
            return mVolumeChangeListener;
        }

        public void setVolumeChangeListener(VolumeChangeListener volumeChangeListener) {
            this.mVolumeChangeListener = volumeChangeListener;
        }

        /**
         * 注册音量广播接收器
         *
         * @return
         */
        public void registerReceiver() {
            mVolumeBroadcastReceiver = new VolumeBroadcastReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(VOLUME_CHANGED_ACTION);
            mContext.registerReceiver(mVolumeBroadcastReceiver, filter);
            mRegistered = true;
        }

        /**
         * 解注册音量广播监听器，需要与 registerReceiver 成对使用
         */
        public void unregisterReceiver() {
            if (mRegistered) {
                try {
                    mContext.unregisterReceiver(mVolumeBroadcastReceiver);
                    mVolumeChangeListener = null;
                    mRegistered = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static class VolumeBroadcastReceiver extends BroadcastReceiver {
        private WeakReference<VolumeChangeObserver> mObserverWeakReference;

        public VolumeBroadcastReceiver(VolumeChangeObserver volumeChangeObserver) {
            mObserverWeakReference = new WeakReference<>(volumeChangeObserver);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //媒体音量改变才通知
            if (VOLUME_CHANGED_ACTION.equals(intent.getAction())
                    && (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == MEDIA_TYPE)) {
                VolumeChangeObserver observer = mObserverWeakReference.get();
                if (observer != null) {
                    VolumeChangeListener listener = observer.getVolumeChangeListener();
                    if (listener != null) {
                        int volume = observer.getCurrentMusicVolume();
                        if (volume >= 0) {
                            listener.onVolumeChanged(volume);
                        }
                    }
                }
            }
        }
    }


}
