package zlc.season.rxdownloadproject.download_manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.practicalrecyclerview.AbstractViewHolder;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownload.entity.DownloadEvent;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/11/14
 * Time: 09:43
 * FIXME
 */
public class AppInfoViewHolder extends AbstractViewHolder<AppInfoBean> {
    @BindView(R.id.head)
    ImageView mHead;
    @BindView(R.id.title)
    TextView mTitle;
    @BindView(R.id.content)
    TextView mContent;
    @BindView(R.id.action)
    Button mAction;

    private String defaultPath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();

    private AppInfoBean mData;
    private Context mContext;
    private RxDownload mRxDownload;
    private DownloadController mDownloadController;

    public AppInfoViewHolder(ViewGroup parent) {
        super(parent, R.layout.app_info_item);
        ButterKnife.bind(this, itemView);
        mContext = parent.getContext();

        mRxDownload = RxDownload.getInstance().context(mContext);
        mDownloadController = new DownloadController(new TextView(mContext), mAction);
    }

    @Override
    public void setData(AppInfoBean data) {
        this.mData = data;
        mDownloadController.setState(new DownloadController.Normal());

        Picasso.with(mContext).load(data.img).into(mHead);
        mTitle.setText(data.name);
        mContent.setText(data.info);


        //接收下载进度
        Subscription temp = mRxDownload.receiveDownloadStatus(mData.downloadUrl)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadEvent>() {
                    @Override
                    public void onCompleted() {
                        mDownloadController.setState(new DownloadController.Completed());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mDownloadController.setState(new DownloadController.Failed());
                    }

                    @Override
                    public void onNext(final DownloadEvent event) {
                    }
                });
        mData.mSubscriptions.add(temp);
    }

    @OnClick(R.id.action)
    public void onClick() {
        mDownloadController.handleClick(new DownloadController.Callback() {
            @Override
            public void startDownload() {
                start();
            }

            @Override
            public void pauseDownload() {
                pause();
            }

            @Override
            public void cancelDownload() {
                cancel();
            }

            @Override
            public void install() {
                installApk();
            }
        });
    }

    private void installApk() {
        Uri uri = Uri.fromFile(new File(defaultPath + File.separator + mData.saveName));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    private void start() {
        //开始下载, 先检查权限
        Subscription temp = RxPermissions.getInstance(mContext)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .compose(mRxDownload.transformService(mData.downloadUrl, mData.saveName, null))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
        mData.mSubscriptions.add(temp);
    }

    /**
     * 暂停下载
     */
    private void pause() {
        Subscription subscription = mRxDownload.pauseServiceDownload(mData.downloadUrl)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
        mData.mSubscriptions.add(subscription);
    }

    private void cancel() {
        Subscription subscription = mRxDownload.cancelServiceDownload(mData.downloadUrl)
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                    }
                });
        mData.mSubscriptions.add(subscription);
    }
}
