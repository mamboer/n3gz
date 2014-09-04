package com.tencent.tgiapp1.common;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.tgiapp1.AppException;
import com.tencent.tgiapp1.api.ApiClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
/**
 * 异步线程加载图片工具类
 * 使用说明：
 * BitmapManager bmpManager;
 * bmpManager = new BitmapManager(BitmapFactory.decodeResource(context.getResources(), R.drawable.loading));
 * bmpManager.loadBitmap(imageURL, imageView);
 * @author lv (http://t.qq.com/badstyle)
 * @version 1.0
 * @created 2014-6-25
 */
public class BitmapManager {  

    private static HashMap<String, SoftReference<Bitmap>> cache;  
    private static ExecutorService pool;  
    private static Map<ImageView, String> imageViews;  
    private Bitmap defaultBmp;  
    
    static {  
        cache = new HashMap<String, SoftReference<Bitmap>>();  
        pool = Executors.newFixedThreadPool(5);  //固定线程池
        imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    }  
    
    public BitmapManager(){}
    
    public BitmapManager(Bitmap def) {
        this.defaultBmp = def;
    }
    
    /**
     * 设置默认图片
     * @param bmp
     */
    public void setDefaultBmp(Bitmap bmp) {  
        defaultBmp = bmp;
    }   
  
    /**
     * 加载图片
     * @param url
     * @param imageView
     */
    public void loadBitmap(String url, ImageView imageView) {  
        loadBitmap(url, imageView, this.defaultBmp, 0, 0);
    }

    /**
     * 加载图片-可设置加载失败后显示的默认图片
     * @param url
     * @param imageView
     * @param defaultBmp
     */
    public void loadBitmap(String url, ImageView imageView, Bitmap defaultBmp) {  
        loadBitmap(url, imageView, defaultBmp, 0, 0);
    }
    
    /**
     * 加载图片-可指定显示图片的高宽
     * @param url
     * @param imageView
     * @param width
     * @param height
     */
    public void loadBitmap(String url, ImageView imageView, Bitmap defaultBmp, int width, int height) {  
        imageViews.put(imageView, url);  
        Bitmap bitmap = getBitmapFromCache(url);

        if(null==bitmap){
            bitmap = loadLocalBitmap(url,width,height);
        }
   
        if (bitmap != null) {  
            //显示缓存图片
            imageView.setImageBitmap(bitmap);  
        } else {

            //加载SD卡中的图片缓存
            String filename = FileUtils.getFileName(url);
            String filepath = imageView.getContext().getFilesDir() + File.separator + filename;
            File file = new File(filepath);
            if(file.exists()){
                //显示SD卡中的图片缓存
                Bitmap bmp = ImageUtils.getBitmap(imageView.getContext(), filename);
                imageView.setImageBitmap(bmp);
            }else{
                //线程加载网络图片
                imageView.setImageBitmap(defaultBmp);
                queueJob(url, imageView, width, height);
            }
        }  
    }

    /**
     * 加载本地图片
     * @param path
     * @return
     */
    public static Bitmap loadLocalBitmap(String path,int width,int height){

        if(path.indexOf("http://")==0 || path.indexOf("https://")==0 || path.indexOf("file://")==0 ){
            return null;
        }
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeFile(path, options);

            if(width > 0 && height > 0) {
                //指定显示图片的高宽
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 加载图片-可指定显示图片的高宽
     * @param url
     * @param width
     * @param height
     */
    public void loadBitmap(final Context ct,String url, int width, int height,final Handler handler) {

        Bitmap bitmap = getBitmapFromCache(url);
        Message msg = new Message();

        if(null==bitmap){
            bitmap = loadLocalBitmap(url,width,height);
        }

        if (bitmap != null) {
            //显示缓存图片
            msg.what =0;
            msg.obj = bitmap;
            handler.sendMessage(msg);
            return;
        }

        //加载SD卡中的图片缓存
        String filename = FileUtils.getFileName(url);
        String filepath = ct.getFilesDir() + File.separator + filename;
        File file = new File(filepath);
        if(file.exists()){
            //显示SD卡中的图片缓存
            bitmap = ImageUtils.getBitmap(ct, filename);
            msg.what =0;
            msg.obj = bitmap;
            handler.sendMessage(msg);
            return;
        }
        //线程加载网络图片
        queueJob(ct,url, width, height,handler);
    }
  
    /**
     * 从缓存中获取图片
     * @param url
     */
    public Bitmap getBitmapFromCache(String url) {  
        Bitmap bitmap = null;
        if (cache.containsKey(url)) {  
            bitmap = cache.get(url).get();  
        }  
        return bitmap;  
    }  
    
    /**
     * 从网络中加载图片
     * @param url
     * @param imageView
     * @param width
     * @param height
     */
    public void queueJob(final String url, final ImageView imageView, final int width, final int height) {  
        /* Create handler in UI thread. */  
        final Handler handler = new Handler() {  
            public void handleMessage(Message msg) {  
                String tag = imageViews.get(imageView);  
                if (tag != null && tag.equals(url)) {  
                    if (msg.obj != null) {  
                        imageView.setImageBitmap((Bitmap) msg.obj);  
                        try {
                            //向SD卡中写入图片缓存
                            ImageUtils.saveImage(imageView.getContext(), FileUtils.getFileName(url), (Bitmap) msg.obj);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } 
                }  
            }  
        };  
  
        pool.execute(new Runnable() {   
            public void run() {  
                Message message = Message.obtain();  
                message.obj = downloadBitmap(url, width, height);  
                handler.sendMessage(message);  
            }  
        });  
    }

    /**
     * 从网络中加载图片
     * @param url
     * @param handler
     * @param width
     * @param height
     */
    public void queueJob(final Context ct,final String url, final int width, final int height,final Handler handler) {
        /* Create handler in UI thread. */
        final Handler handler0 = new Handler() {
            public void handleMessage(Message msg) {

                Message msg1 = new Message();

                if (msg.obj != null) {
                    msg1.what = 0;
                    msg1.obj = msg.obj;

                    try {
                        //向SD卡中写入图片缓存
                        ImageUtils.saveImage(ct, FileUtils.getFileName(url), (Bitmap) msg.obj);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    msg1.what = -1;
                    msg1.obj = null;
                }
                handler.sendMessage(msg1);
            }
        };

        pool.execute(new Runnable() {
            public void run() {
                Message message = Message.obtain();
                message.obj = downloadBitmap(url, width, height);
                handler0.sendMessage(message);
            }
        });
    }

    /**
     * 下载图片-可指定显示图片的高宽
     * @param url
     * @param width
     * @param height
     */
    private Bitmap downloadBitmap(String url, int width, int height) {   
        Bitmap bitmap = null;
        try {
            //http加载图片
            bitmap = ApiClient.getNetBitmap(url);
            if(width > 0 && height > 0) {
                //指定显示图片的高宽
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            //放入缓存
            cache.put(url, new SoftReference<Bitmap>(bitmap));
        } catch (AppException e) {
            e.printStackTrace();
        }
        return bitmap;  
    }  
}