package com.android.ql.lf.carapp.utils;


import com.android.ql.lf.carapp.data.ImageBean;
import com.android.ql.lf.carapp.data.UserInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by lf on 2017/11/24 0024.
 *
 * @author lf on 2017/11/24 0024
 */

public class ImageUploadHelper {

    private OnImageUploadListener onImageUploadListener;

    public static MultipartBody.Builder createMultipartBody() {
        return new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("token", Constants.md5Token())
                .addFormDataPart("uid", UserInfo.getInstance().getMemberId());
    }

    public ImageUploadHelper(OnImageUploadListener onImageUploadListener) {
        this.onImageUploadListener = onImageUploadListener;
    }

    public void upload(final ArrayList<ImageBean> list, final int maxSize) {
        final File dir = new File(Constants.IMAGE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final ArrayList<String> compressList = new ArrayList<>();
        Observable.from(list).map(new Func1<ImageBean, String>() {
            @Override
            public String call(ImageBean imageItem) {
                String path = dir + File.separator + System.currentTimeMillis() + ".jpg";
                try {
                    ImageFactory.compressAndGenImage(imageItem.getUriPath(), path, maxSize, false);
                } catch (IOException e) {
                    return null;
                }
                return path;
            }
        }).subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (onImageUploadListener != null) {
                            onImageUploadListener.onActionStart();
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (s != null) {
                            compressList.add(s);
                            if (compressList.size() == list.size()) {
                                if (onImageUploadListener != null) {
                                    MultipartBody.Builder builder = ImageUploadHelper.createMultipartBody();
                                    for (int i = 0; i < compressList.size(); i++) {
                                        File file = new File(compressList.get(i));
                                        builder.addFormDataPart(i + "", file.getName(), RequestBody.create(MultipartBody.FORM, file));
                                    }
                                    onImageUploadListener.onActionEnd(builder);
                                }
                            }
                        } else {
                            if (onImageUploadListener != null) {
                                onImageUploadListener.onActionFailed();
                            }
                        }
                    }
                });
    }

    public interface OnImageUploadListener {

        public void onActionStart();

        public void onActionEnd(MultipartBody.Builder builder);

        public void onActionFailed();
    }

}
