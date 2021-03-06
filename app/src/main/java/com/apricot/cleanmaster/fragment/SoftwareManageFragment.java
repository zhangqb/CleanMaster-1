package com.apricot.cleanmaster.fragment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.IPackageStatsObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.TextView;

import com.apricot.cleanmaster.R;
import com.apricot.cleanmaster.adapter.SoftwareAdapter;
import com.apricot.cleanmaster.base.BaseFragment;
import com.apricot.cleanmaster.bean.AppInfo;
import com.apricot.cleanmaster.utils.StorageUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Apricot on 2016/9/20.
 */
public class SoftwareManageFragment extends BaseFragment{
    private Context mContext;

    public static final String ARG_POSITION="position";
    private SoftwareAdapter mSoftwareAdapter;
    private int position; // 0:应用软件，1 系统软件
    @BindView(R.id.list_software_fragment)
    ListView mListVIew;
    @BindView(R.id.tv_software_fragment)
    TextView mTopText;
    @BindView(R.id.progressBar)
    View mProgressBar;
    @BindView(R.id.progressBarText)
    TextView mProgressText;

    List<AppInfo> userAppInfos=null;
    List<AppInfo> systemAppInfos=null;

    private Method mGetPackageInfoMethod;
    AsyncTask<Void,Integer,List<AppInfo>> mTask;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position=getArguments().getInt(ARG_POSITION);
        mContext=getActivity();
        try {
            mGetPackageInfoMethod=mContext.getPackageManager().getClass().getMethod(
                    "getPackageSizeInfo",String.class,IPackageStatsObserver.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_software,container,false);
        ButterKnife.bind(this,v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        fillData();

    }

    @Override
    public void onStop() {
        super.onStop();
        mTask.cancel(true);
    }

    private void fillData(){
        if(position==0){
            mTopText.setText("");
        }else{
            mTopText.setText("卸载下列软件，会影响正常使用");
        }

        mTask=new AsyncTask<Void, Integer, List<AppInfo>>() {
            private int mAppCount=0;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressBar(true);
                mProgressText.setText(R.string.scanning);
            }

            @Override
            protected List<AppInfo> doInBackground(Void... params) {
                PackageManager pm=mContext.getPackageManager();
                List<PackageInfo> packageInfos=pm.getInstalledPackages(0);
                publishProgress(0,packageInfos.size());
                List<AppInfo> appInfos=new ArrayList<>();
                for(PackageInfo packageInfo:packageInfos){
                    publishProgress(mAppCount++,packageInfos.size());
                    final AppInfo appInfo=new AppInfo();
                    appInfo.setAppIcon(packageInfo.applicationInfo.loadIcon(pm));
                    appInfo.setAppName(packageInfo.applicationInfo.loadLabel(pm).toString());
                    String packageName=packageInfo.applicationInfo.packageName;
                    appInfo.setPkgName(packageName);
                    appInfo.setVersion(packageInfo.versionName);
                    appInfo.setUid(packageInfo.applicationInfo.uid);
                    int flags=packageInfo.applicationInfo.flags;
                    if((flags&ApplicationInfo.FLAG_SYSTEM)!=0){
                        appInfo.setUserApp(false);
                    }else{
                        appInfo.setUserApp(true);
                    }
                    if((flags&ApplicationInfo.FLAG_EXTERNAL_STORAGE)!=0){
                        appInfo.setInRom(false);
                    }else{
                        appInfo.setInRom(true);
                    }
                    try {
                        mGetPackageInfoMethod.invoke(mContext.getPackageManager(),new Object[]{packageName, new IPackageStatsObserver.Stub() {
                            @Override
                            public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
                                appInfo.setPkgSize(pStats.cacheSize+pStats.codeSize+pStats.dataSize);
                            }
                        }});
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    appInfos.add(appInfo);

                }
                return appInfos;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                mProgressText.setText(getString(R.string.scanning_m_of_n,values[0],values[1]));
            }

            @Override
            protected void onPostExecute(List<AppInfo> appInfos) {
                super.onPostExecute(appInfos);
                showProgressBar(false);
                userAppInfos=new ArrayList<>();
                systemAppInfos=new ArrayList<>();
                long allSize=0;
                for(AppInfo appInfo:appInfos){
                    if(appInfo.isUserApp()){
                        allSize+=appInfo.getPkgSize();
                        userAppInfos.add(appInfo);
                    }else{
                        systemAppInfos.add(appInfo);
                    }
                }

                if(position==0){
                    mTopText.setText(getString(R.string.software_top_text,userAppInfos.size(), StorageUtil.convertStorage(allSize)));
                    mSoftwareAdapter=new SoftwareAdapter(mContext,userAppInfos);
                    mListVIew.setAdapter(mSoftwareAdapter);
                }else{
                    mSoftwareAdapter=new SoftwareAdapter(mContext,systemAppInfos);
                    mListVIew.setAdapter(mSoftwareAdapter);
                }

            }
        };
        mTask.execute();


    }

    private void showProgressBar(boolean show){
        if(show){
            mProgressBar.setVisibility(View.VISIBLE);
        }else{
            mProgressBar.startAnimation(AnimationUtils.loadAnimation(mContext,android.R.anim.fade_out));
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
