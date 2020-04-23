package com.tw.softmobile.bingo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText m_etName;
    private Button m_btnNext;
    private Button m_btnExit;
    private SharedPreferences m_sharedPreferences = null;
    private int m_iGameTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

    }

    private void initUI() {
        m_etName = findViewById(R.id.et_name);
        m_btnExit = findViewById(R.id.btn_exit);
        m_btnNext = findViewById(R.id.btn_next);

        m_btnNext.setOnClickListener(this);
        m_btnExit.setOnClickListener(this);

        //SharePreference
        m_sharedPreferences = getSharedPreferences("userData", MODE_PRIVATE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_next:
                onNextButtonClick();
                break;
            case R.id.btn_exit:
                onExitButtonClick();
                break;
        }
    }

    private void onNextButtonClick() {
        //點選下一頁，應該要讀ET的內容，並存起來丟到下一頁
        if (!(m_etName.getText().toString().isEmpty())) {
            //獲取該User的遊戲次數，如果沒這個user就給1
            m_iGameTimes = getGameTimes(m_etName.getText().toString(), 1);
            Log.d("times get", m_iGameTimes+"");
            Intent intent = new Intent(this, BingoActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("Name", m_etName.getText().toString());
            bundle.putInt("GameTimes", m_iGameTimes);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, R.string.enter_name, Toast.LENGTH_SHORT).show();
        }
    }

    private void onExitButtonClick() {
        //結束此程式，可能需要存數據之類的
        finish();
    }

    //獲取該使用者遊戲次數
    private int getGameTimes(String key, int defValue) {
        return this.m_sharedPreferences.getInt(key, defValue);
    }
}
