package com.tencent.sgz.ui;

import com.tencent.sgz.AppContext;
import com.tencent.sgz.AppException;
import com.tencent.sgz.AppManager;
import com.tencent.sgz.R;
import com.tencent.sgz.activity.BaseActivity;
import com.tencent.sgz.bean.Report;
import com.tencent.sgz.common.StringUtils;
import com.tencent.sgz.common.UIHelper;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
/**
 * 举报操作窗口
 * @author lv (http://t.qq.com/badstyle)
 * @version 1.0
 * @created 2014-02-13
 */
public class ReportUi extends BaseActivity {
	private final String TAG = "ReportUi";
	private AppContext ac;
	private TextView mLink;
	private Spinner mReason;
	private TextView mOtherReason;

	private Button mPublish;
	private ImageButton mClose;
	private ProgressDialog mProgress;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report);
		this.initView();
		initData();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		AppManager.getAppManager().finishActivity(this);
	}
	
	private void initView() {
		ac = (AppContext)getApplication();
		mLink = (TextView) findViewById(R.id.report_link);
		mReason = (Spinner) findViewById(R.id.report_reason);
		mOtherReason = (TextView) findViewById(R.id.report_other_reson);
		mPublish = (Button) findViewById(R.id.report_publish);
		mClose = (ImageButton) findViewById(R.id.report_close_button);
		
		mClose.setOnClickListener(UIHelper.finish(this));
		mPublish.setOnClickListener(publishListener);
	}
	
	private void initData() {
		Intent data = this.getIntent();
		mLink.setText(data.getStringExtra(Report.REPORT_LINK));
	}
	
	private OnClickListener publishListener = new OnClickListener() {
		public void onClick(View v) {
			if (mPublish.getTag() == null) {
				return;
			}
			final Report report = new Report();
			report.setLinkAddress(mLink.getText() + "");
			report.setReportId(ac.getLoginUid());
			String otherReason = mOtherReason.getText().toString();
			if (StringUtils.isEmpty(otherReason)) {
				report.setOtherReason("其他原因");
			} else {
				report.setOtherReason(otherReason);
			}
			report.setReason(mReason.getSelectedItem().toString());
			mProgress = ProgressDialog.show(v.getContext(), null, "举报信息发送中...",true,true);	
			final Handler handler = new Handler(){
				public void handleMessage(Message msg) {
					if(mProgress!=null)mProgress.dismiss();
					if(msg.what == 0){
						UIHelper.ToastMessage(ReportUi.this, "发送成功");
						finish();
					} else if (msg.what == 1) {
						UIHelper.ToastMessage(ReportUi.this, "发送失败");
					}
					else {
						((AppException)msg.obj).makeToast(ReportUi.this);
					}
				}
			};
			new Thread(){
				public void run() {
					Message msg = new Message();
					String res = "";
					try {
						res = ac.report(report);
						msg.obj = res;
						if (res == "" || res == null) {
							msg.what = 0;
						} else {
							msg.what = 1;
						}
		            } catch (AppException e) {
		            	e.printStackTrace();
						msg.what = -1;
		            }
					handler.sendMessage(msg);
				}
			}.start();
		}
	};
}
