package com.anthony.video.business.presenter;

import com.anthony.common.base.net.UrlConstant;
import com.anthony.common.base.net.client.request.form.child.BaobabFormRequestClient;
import com.anthony.common.base.net.common.business.BasePresenter;
import com.anthony.common.base.net.common.observer.AppObserver;
import com.anthony.video.bean.VideoListResult;
import com.anthony.video.business.contact.VideoListContact;

import java.util.HashMap;

/**
 * 创建时间:2019/8/9
 * 创建人：anthony.wang
 * 功能描述：
 */
public class VideoListPresenter extends BasePresenter<VideoListContact.View> implements VideoListContact.Presenter{

    public VideoListPresenter(VideoListContact.View view) {
        super(view);
    }

    @Override
    public void getVideoList(int id) {
        HashMap<String,Object> params = new HashMap<>();
        params.put("id",id+"");
        params.put("udid","d2807c895f0348a180148c9dfa6f2feeac0781b5");
        BaobabFormRequestClient.getInstance().executePost(UrlConstant.POST_CATEGORIES_VIDEO_LIST, params, new AppObserver<VideoListResult>() {
            @Override
            public void onNext(VideoListResult videoListResult) {
                view.setVideoList(videoListResult.getItemList());
            }
        });
    }
}
