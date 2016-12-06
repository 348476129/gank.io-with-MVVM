package com.fall.gank.presenter;

import android.util.Log;

import com.anupcowkur.reservoir.Reservoir;
import com.fall.gank.Utils.RxUtils;
import com.fall.gank.Utils.TimeUtils;
import com.fall.gank.core.BasePresenter;
import com.fall.gank.network.converter.ResultException;
import com.fall.gank.network.model.IGankModel;
import com.fall.gank.network.model.impl.GankModel;
import com.fall.gank.viewmodel.ClassificationViewModel;
import com.fall.gank.viewmodel.ClassificationItemViewModel;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import rx.Observable;

/**
 * Created by 康颢曦 on 2016/11/27.
 */

public class ClassificationPresenter extends BasePresenter<ClassificationViewModel> {
    private String KEY = "ClassificationPresenter.Key";
    private String type;
    private IGankModel mModel = GankModel.getInstance();
    private List<ClassificationItemViewModel> mList = new ArrayList<>();
    private int page = 1;

    @Override
    public void onPresenterCreate(boolean isNewCreate) {
        if (isNewCreate) {
            Type collectionType = new TypeToken<List<ClassificationItemViewModel>>() {
            }.getType();
            mCompositeSubscription.add(Reservoir.getUsingObservable(KEY + type, ClassificationItemViewModel.class, collectionType)
                    .compose(RxUtils.applyIOToMainThreadSchedulers())
                    .toList()
                    .subscribe(classificationItemViewModels -> {
                        if (classificationItemViewModels.size() > 0) {
                            getViewModel().setClassificationItemViewModelList(classificationItemViewModels);
                            //     mAdapter.set(getViewModel().getHomeItemViewModelList());
                            showList(getViewModel().getClassificationItemViewModelList());
                            getViewModel().isDataEnable.set(true);
                        }
                    }, throwable -> getData(page), () -> getData(page)));
        } else {
                page = getViewModel().getPage();
                if (getViewModel().isRefresh.get()) {
                    getData(page);
                }
        }
    }

    public void getData(int page) {
        if (page == 1) {
            this.page = 1;
        }
        getViewModel().setPage(this.page);
        getViewModel().isRefresh.set(true);
        mCompositeSubscription.add(mModel.getClassifiData(type, page)
                .compose(RxUtils.applyIOToMainThreadSchedulers())
                .map(classificationEntity -> classificationEntity.getResults())
                .flatMap(Observable::from)
                .subscribe(classificationResultsEntity -> {
                            HashMap<String,String> hashMap;
                            try {
                                hashMap = TimeUtils.getTime(classificationResultsEntity.getPublishedAt());
                                mList.add(new ClassificationItemViewModel(classificationResultsEntity.getType(), classificationResultsEntity.getDesc(),hashMap.get(TimeUtils.YEAR),hashMap.get(TimeUtils.MONTH)+"/"+hashMap.get(TimeUtils.DAY), false));

                            } catch (ParseException e) {
                                 Observable.error(e);
                            }
                        }, throwable -> {
                            getViewModel().isRefresh.set(false);
                            if (throwable instanceof ResultException) {
                                showSnakbar("数据错误");
                            } else {
                                showSnakbar("连接失败，请重试");
                            }
                        }
                        , () -> {

                            if (page == 1) {
                                getViewModel().getClassificationItemViewModelList().clear();
                            }
                            getViewModel().getClassificationItemViewModelList().addAll(mList);
                            mList.clear();
                            getViewModel().isRefresh.set(false);
                            showList(getViewModel().getClassificationItemViewModelList());
                            if (page == 1) {
                                mCompositeSubscription.add(Reservoir.putUsingObservable(KEY + type, getViewModel().getClassificationItemViewModelList())
                                        .compose(RxUtils.applyIOToMainThreadSchedulers())
                                        .subscribe(aBoolean -> {
                                        }, throwable -> {
                                        }));
                            }
                            getViewModel().isDataEnable.set(true);
                            this.page++;
                            getViewModel().setPage(this.page);
                        }));
    }

    public void loadNext() {
        if (page == 1) page++;
        if (!getViewModel().isRefresh.get()) {
            getData(page);
        } else return;
    }

    public void setType(String type) {
        this.type = type;
    }
}
