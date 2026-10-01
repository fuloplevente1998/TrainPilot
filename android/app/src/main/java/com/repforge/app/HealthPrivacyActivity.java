package com.repforge.app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import java.util.Locale;
public class HealthPrivacyActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);String lang=getIntent().getStringExtra("language");if(lang==null)lang=Locale.getDefault().getLanguage();TextView text=new TextView(this);text.setTextSize(17);text.setPadding(32,48,32,48);String policy=PrivacyPolicy.text(lang);if(!BuildConfig.DEVELOPER_NAME.isEmpty())policy+="\n\n"+BuildConfig.DEVELOPER_NAME;if(!BuildConfig.SUPPORT_EMAIL.isEmpty())policy+="\n"+BuildConfig.SUPPORT_EMAIL;if(!BuildConfig.PRIVACY_POLICY_URL.isEmpty())policy+="\n"+BuildConfig.PRIVACY_POLICY_URL;text.setText(policy);Linkify.addLinks(text,Linkify.WEB_URLS|Linkify.EMAIL_ADDRESSES);text.setMovementMethod(LinkMovementMethod.getInstance());ScrollView scroll=new ScrollView(this);scroll.addView(text);setContentView(scroll);}
}
