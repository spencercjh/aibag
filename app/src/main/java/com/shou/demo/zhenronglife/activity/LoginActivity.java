package com.shou.demo.zhenronglife.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.shou.demo.R;
import com.shou.demo.jiuray.BluetoothActivity;
import com.shou.demo.zhenronglife.util.SharedPreferencesUtils;
import com.shou.demo.zhenronglife.widget.LoadingDialog;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 登录界面
 *
 * @author spencercjh
 */

public class LoginActivity extends Activity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    //布局内的控件
    private EditText etName;
    private EditText etPassword;
    private Button mLoginBtn;
    private CheckBox checkboxPassword;
    private CheckBox checkboxLogin;
    private ImageView ivSeePassword;

    private LoadingDialog mLoadingDialog; //显示正在加载的对话框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        setupEvents();
        initData();

    }

    private void initData() {
        //判断用户第一次登陆
        if (firstLogin()) {
            checkboxPassword.setChecked(false);//取消记住密码的复选框
            checkboxLogin.setChecked(false);//取消自动登录的复选框
        }
        //判断是否记住密码
        if (remenberPassword()) {
            checkboxPassword.setChecked(true);//勾选记住密码
            setTextNameAndPassword();//把密码和账号输入到输入框中
        } else {
            setTextName();//把用户账号放到输入账号的输入框中
        }
        //判断是否自动登录
        if (autoLogin()) {
            checkboxLogin.setChecked(true);
            login(true);//去登录就可以
        }
    }

    /**
     * 把本地保存的数据设置数据到输入框中
     */
    @SuppressLint("SetTextI18n")
    private void setTextNameAndPassword() {
        etName.setText("" + getLocalName());
        etPassword.setText("" + getLocalPassword());
    }

    /**
     * 设置数据到输入框中
     */
    private void setTextName() {
        etName.setText("" + getLocalName());
    }

    /**
     * 获得保存在本地的用户名
     */
    private String getLocalName() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getString("name");
    }

    /**
     * 获得保存在本地的密码
     */
    private String getLocalPassword() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return (helper.getString("password"));   //解码一下
//       return password;   //解码一下

    }

    /**
     * 判断是否自动登录
     */
    private boolean autoLogin() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getBoolean("autoLogin", false);
    }

    /**
     * 判断是否记住密码
     */
    private boolean remenberPassword() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        return helper.getBoolean("remenberPassword", false);
    }

    private void initViews() {
        mLoginBtn = findViewById(R.id.btn_login);
        etName = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        checkboxPassword = findViewById(R.id.checkBox_password);
        checkboxLogin = findViewById(R.id.checkBox_login);
        ivSeePassword = findViewById(R.id.iv_see_password);
    }

    private void setupEvents() {
        mLoginBtn.setOnClickListener(this);
        checkboxPassword.setOnCheckedChangeListener(this);
        checkboxLogin.setOnCheckedChangeListener(this);
        ivSeePassword.setOnClickListener(this);

    }

    /**
     * 判断是否是第一次登陆
     */
    private boolean firstLogin() {
        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
        boolean first = helper.getBoolean("first", true);
        if (first) {
            //创建一个ContentVa对象（自定义的）设置不是第一次登录，,并创建记住密码和自动登录是默认不选，创建账号和密码为空
            helper.putValues(new SharedPreferencesUtils.ContentValue("first", false),
                    new SharedPreferencesUtils.ContentValue("remenberPassword", false),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("name", ""),
                    new SharedPreferencesUtils.ContentValue("password", ""));
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                loadUserName();    //无论如何保存一下用户名
                login(false); //登陆
                break;
            case R.id.iv_see_password:
                setPasswordVisibility();    //改变图片并设置输入框的文本可见或不可见
                break;
            default:
                break;
        }
    }

    /**
     * 模拟登录情况
     * 用户名admin，密码123456，就能登录成功，否则登录失败
     */
    private void login(boolean isAuto) {
        if (isAuto) {
            showToast("登录成功");
            startActivity(new Intent(LoginActivity.this, BluetoothActivity.class));
            finish();//关闭页面
            return;
        }
        //先做一些基本的判断，比如输入的用户命为空，密码为空，网络不可用多大情况，都不需要去链接服务器了，而是直接返回提示错误
        if (getAccount().isEmpty()) {
            showToast("你输入的账号为空！");
            return;
        }

        if (getPassword().isEmpty()) {
            showToast("你输入的密码为空！");
            return;
        }
        showLoading();//显示加载框
        Thread loginRunnable = new Thread() {

            @Override
            public void run() {
                super.run();
                setLoginBtnClickable(false);//点击登录后，设置登录按钮不可点击状态
                //睡眠3秒
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //判断账号和密码
                if (getAccount().equals("admin") && getPassword().equals("123456")) {
                    showToast("登录成功");
                    loadCheckBoxState();//记录下当前用户记住密码和自动登录的状态;
                    startActivity(new Intent(LoginActivity.this, BluetoothActivity.class));
                    finish();//关闭页面
                } else {
                    showToast("输入的登录账号或密码不正确");
                }
                setLoginBtnClickable(true);  //这里解放登录按钮，设置为可以点击
                hideLoading();//隐藏加载框
            }
        };
        loginRunnable.start();
    }

    /**
     * 保存用户账号
     */
    private void loadUserName() {
        if (!"".equals(getAccount()) || !"请输入登录账号".equals(getAccount())) {
            SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");
            helper.putValues(new SharedPreferencesUtils.ContentValue("name", getAccount()));
        }

    }

    /**
     * 设置密码可见和不可见的相互转换
     */
    private void setPasswordVisibility() {
        if (ivSeePassword.isSelected()) {
            ivSeePassword.setSelected(false);
            //密码不可见
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            ivSeePassword.setSelected(true);
            //密码可见
            etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }

    }

    /**
     * 获取账号
     */
    public String getAccount() {
        return etName.getText().toString().trim();//去掉空格
    }

    /**
     * 获取密码
     */
    public String getPassword() {
        return etPassword.getText().toString().trim();//去掉空格
    }

    /**
     * 保存用户选择“记住密码”和“自动登陆”的状态
     */
    private void loadCheckBoxState() {
        loadCheckBoxState(checkboxPassword, checkboxLogin);
    }

    /**
     * 保存按钮的状态值
     */
    private void loadCheckBoxState(CheckBox checkboxPassword, CheckBox checkboxLogin) {

        //获取SharedPreferences对象，使用自定义类的方法来获取对象
        SharedPreferencesUtils helper = new SharedPreferencesUtils(this, "setting");

        //如果设置自动登录
        if (checkboxLogin.isChecked()) {
            //创建记住密码和自动登录是都选择,保存密码数据
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", true),
                    new SharedPreferencesUtils.ContentValue("autoLogin", true),
                    new SharedPreferencesUtils.ContentValue("password", new String(Hex.encodeHex(DigestUtils.md5(getPassword())))));
        } else if (!checkboxPassword.isChecked()) { //如果没有保存密码，那么自动登录也是不选的
            //创建记住密码和自动登录是默认不选,密码为空
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", false),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("password", ""));
        } else if (checkboxPassword.isChecked()) {   //如果保存密码，没有自动登录
            //创建记住密码为选中和自动登录是默认不选,保存密码数据
            helper.putValues(
                    new SharedPreferencesUtils.ContentValue("remenberPassword", true),
                    new SharedPreferencesUtils.ContentValue("autoLogin", false),
                    new SharedPreferencesUtils.ContentValue("password", new String(Hex.encodeHex(DigestUtils.md5(getPassword())))));
        }
    }

    /**
     * 是否可以点击登录按钮
     *
     * @param clickable
     */
    private void setLoginBtnClickable(boolean clickable) {
        mLoginBtn.setClickable(clickable);
    }

    /**
     * 显示加载的进度款
     */
    private void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this, getString(R.string.loading), false);
        }
        mLoadingDialog.show();
    }

    /**
     * 隐藏加载的进度框
     */
    private void hideLoading() {
        if (mLoadingDialog != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLoadingDialog.hide();
                }
            });

        }
    }

    /**
     * CheckBox点击时的回调方法 ,不管是勾选还是取消勾选都会得到回调
     *
     * @param buttonView 按钮对象
     * @param isChecked  按钮的状态
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkboxPassword) {  //记住密码选框发生改变时
            if (!isChecked) {   //如果取消“记住密码”，那么同样取消自动登陆
                checkboxLogin.setChecked(false);
            }
        } else if (buttonView == checkboxLogin) {   //自动登陆选框发生改变时
            if (isChecked) {   //如果选择“自动登录”，那么同样选中“记住密码”
                checkboxPassword.setChecked(true);
            }
        }
    }

    /**
     * 监听回退键
     */
    @Override
    public void onBackPressed() {
        if (mLoadingDialog != null) {
            if (mLoadingDialog.isShowing()) {
                mLoadingDialog.cancel();
            } else {
                finish();
            }
        } else {
            finish();
        }

    }

    /**
     * 页面销毁前回调的方法
     */
    @Override
    protected void onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.cancel();
            mLoadingDialog = null;
        }
        super.onDestroy();
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
