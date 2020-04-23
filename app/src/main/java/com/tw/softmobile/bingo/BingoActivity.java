package com.tw.softmobile.bingo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Random;

public class BingoActivity extends AppCompatActivity implements View.OnClickListener {

    public String m_strName;
    public int m_iGameTimes;

    private TextView m_tvIntro;
    private TextView m_tvInstruction;
    private Button m_btnComplete;
    private Button m_btnBack;
    private EditText m_etMinNumberScope;
    private EditText m_etMaxNumberScope;
    private EditText m_etWin;
    private Switch m_swSwitch;
    private Button m_btnRandom;
    public Button m_btnBlue;
    public Button m_btnGreen;

    /* for spinner */
    private Spinner m_bingoSpinner;
    private ArrayAdapter<String> m_listAdapter;
    private int m_iSize = 3; //目前預設3種size(3*3.4*4.5*5)，之後要幾種都可以改

    private int m_iMin;
    private int m_iMax;
    private int m_iWin;
    private int m_iBingoSize;
    private boolean m_bIsDefault = true;
    private boolean m_bIsbtnRandom = false;
    private boolean m_bIsbtnEmpty = false;

    private Random random = new Random();

    private RecyclerView rv_bingorecycler;

    DataModel dataModel = new DataModel();

    private SharedPreferences mSharedPreferences = null;
    private SharedPreferences.Editor mEditor = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bingo);

        mSharedPreferences = getSharedPreferences("userData", MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();

        initUI();
        getNameAndTimes();

        //進此頁面，一定從輸入模式開始
        dataModel.setMode(false);
        //一開始一定規則是錯的(空的)
        dataModel.setIsObey(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initUI() {
        m_tvIntro = findViewById(R.id.tv_Intro);
        m_tvInstruction = findViewById(R.id.tv_instruction);

        m_btnComplete = findViewById(R.id.btn_complete);
        m_btnBack = findViewById(R.id.btn_back);
        m_btnRandom = findViewById(R.id.btn_random);
        m_btnBlue = findViewById(R.id.btn_blue);
        m_btnGreen = findViewById(R.id.btn_green);
        m_btnBack.setOnClickListener(this);
        m_btnComplete.setOnClickListener(this);
        m_btnRandom.setOnClickListener(this);
        m_btnBlue.setOnClickListener(this);
        m_btnGreen.setOnClickListener(this);

        m_etMinNumberScope = findViewById(R.id.et_minnumberscope);
        m_etMaxNumberScope = findViewById(R.id.et_maxnumberscope);
        m_etWin = findViewById(R.id.et_win);

        m_swSwitch = findViewById(R.id.sw_switch);
        setOnCheckSwitch();

        //for spinner
        m_bingoSpinner = findViewById(R.id.bingo_spinner);
        String[] arBingoSize = {getString(R.string.chooose), "3*3", "4*4", "5*5"};
        //若要做更多的格數，可直接在這改，或是換做editview
        m_listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arBingoSize);
        m_bingoSpinner.setAdapter(m_listAdapter);
        //選第一個default
        m_bingoSpinner.setSelection(0, true);
        setBingoSizeSpinner();

        //for recyclerview
        rv_bingorecycler = findViewById(R.id.rv_bingo);
    }

    private void setOnCheckSwitch() {
        m_swSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!dataModel.getIsObey()) {
                    //輸入規則錯誤
                    Log.d("pppp", "" + dataModel.getIsObey());
                    m_swSwitch.setChecked(false);
                    Toast.makeText(BingoActivity.this, R.string.switch_check, Toast.LENGTH_SHORT).show();
                } else if (m_swSwitch.isChecked() && dataModel.getIsObey()) {
                    Log.d("pppp", "" + dataModel.getIsObey());
                    //規則對，on
                    Toast.makeText(BingoActivity.this, R.string.switch_on, Toast.LENGTH_SHORT).show();
                    //告知cardview目前為自動模式
                    dataModel.setMode(true);
                    createGameAdapter();
                    m_iGameTimes += 1; //遊戲次數+1
                    Log.d("times", m_strName + "/" + m_iGameTimes);
                    putGameTimes(m_strName, m_iGameTimes); //存入此使用者遊戲次數
                    m_tvInstruction.setText(R.string.swich_on_instruction);
                } else {
                    //off
                    Toast.makeText(BingoActivity.this, R.string.switch_off, Toast.LENGTH_SHORT).show();
                    setGameToInputAdpater();
                    m_tvInstruction.setText(R.string.switch_off_instruction);
                }
            }
        });
    }

    private void getNameAndTimes() {
        //取得上一個activity輸入的名字和取得的遊戲次數
        Bundle bundle = getIntent().getExtras();
        m_strName = bundle.getString("Name");
        m_iGameTimes = bundle.getInt("GameTimes");
        Log.d("BingoActivity", m_strName + " / " + m_iGameTimes);
        setIntroText();
    }

    private void setIntroText() {
        String str_introText = String.format(getString(R.string.bingowords), m_strName, m_iGameTimes);
        m_tvIntro.setText(str_introText);
    }

    private void setBingoSizeSpinner() {
        m_bingoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //被選取後的動作 (選0代表沒選，選其他判斷是否設定完成and範圍是否正確and勝利條件極限(2n+2)，
                // 若完成則產出表格和start btn(?)，否則alert)
                Log.d("spinner i", Integer.toString(i));
                Log.d("spinner select", m_bingoSpinner.getSelectedItem().toString());
                //如果不是default被選，選default不做事
                if (i != 0) {
                    //此變數用來確認不是default被選
                    m_bIsDefault = false;
                    //去看哪一項被選到，用迴圈去看，之後新增更多大小就不用if寫一堆
                    for (int j = 1; j <= m_iSize; j++) {
                        if (i == j) {
                            m_iBingoSize = i + 2;
                            break;
                        }
                    }
                } else {
                    m_bIsDefault = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //沒有選取東西
                m_bIsDefault = true;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackButtonClick();
                break;
            case R.id.btn_complete:
                //遊戲模式下不能按按鈕
                if (dataModel.getMode()) {
                    Toast.makeText(BingoActivity.this, R.string.game_mode_alert, Toast.LENGTH_SHORT).show();
                } else {
                    onCompleteButtonClick();
                }
                break;
            case R.id.btn_random:
                if (dataModel.getMode()) {
                    Toast.makeText(BingoActivity.this, R.string.game_mode_alert, Toast.LENGTH_SHORT).show();
                } else {
                    onRandomButtonClick();
                }
                break;
            case R.id.btn_blue:
                dataModel.setPickColor(R.color.picked_blue);
                break;
            case R.id.btn_green:
                dataModel.setPickColor(R.color.picked_green);
                break;
        }
    }

    private void onRandomButtonClick() {
        //產生亂數表格
        m_bIsbtnRandom = true;
        judgeSettingCondiction();
    }

    private void onBackButtonClick() {
        //關閉此Activity
        BingoActivity.this.finish();
    }

    private void onCompleteButtonClick() {
        //產生空的輸入表格
        m_bIsbtnEmpty = true;
        judgeSettingCondiction();
    }

    /**
     * 判斷設定條件是否正確 ，若正確則生成表格，否則不給按按鈕和toggle
     **/
    private void judgeSettingCondiction() {
        Log.d("isdefault", "" + m_bIsDefault);
        //設定完成產生賓果盤的所有判斷
        //如果任一欄位未填(此部分判斷是否全填)
        if (m_etMaxNumberScope.getText().toString().isEmpty() ||
                m_etMinNumberScope.getText().toString().isEmpty() ||
                m_etWin.getText().toString().isEmpty() || m_bIsDefault) {
            Toast.makeText(BingoActivity.this, R.string.fill_alert, Toast.LENGTH_SHORT).show();
            m_bingoSpinner.setSelection(0, true); //spinner回到預設
        } else { //此部分判斷設定條件
            m_iMax = Integer.parseInt(m_etMaxNumberScope.getText().toString());
            m_iMin = Integer.parseInt(m_etMinNumberScope.getText().toString());
            m_iWin = Integer.parseInt(m_etWin.getText().toString());

            //如果最大範圍與最小範圍差距小於總格數，則錯誤
            //總格數 = (bingo size)平方
            if (m_iMax - m_iMin < Math.pow(m_iBingoSize, 2)) {
                String strRangeAlert = getString(R.string.range_alert, (int) Math.pow(m_iBingoSize, 2));
                //告知必須大於等於總格數
                Toast.makeText(BingoActivity.this, strRangeAlert, Toast.LENGTH_SHORT).show();
                m_bingoSpinner.setSelection(0, true); //spinner回到預設
            } else if (m_iWin > (2 * m_iBingoSize + 2) || m_iWin < 1) {
                //或是勝利條件個數大於2(邊長)+2，此為最多的賓果條數，或小於1
                String strWinAlert = getString(R.string.win_alert, 2 * m_iBingoSize + 2); //告知不得超過限制
                Toast.makeText(BingoActivity.this, strWinAlert, Toast.LENGTH_SHORT).show();
                m_bingoSpinner.setSelection(0, true); //spinner回到預設
            } else {
                //將range填入adapter做判斷
                dataModel.setRange(m_iMax, m_iMin);
                dataModel.setWinCon(m_iWin); //設定勝利條件進去物件

                Log.d("random", "btn:" + m_bIsbtnEmpty + "/ random: " + m_btnRandom);
                //判斷是哪個按鈕按的
                if (m_bIsbtnEmpty) {
                    setEmptyFormAdapter();
                } else if (m_bIsbtnRandom) {
                    setRandomFormAdpater();
                }
            }
        }
        m_bIsbtnEmpty = false; //歸零
        m_bIsbtnRandom = false; //歸零
    }

    private void checkRepeatNum(int[] arr, int index) {
        for (int i = index; i > 0; i--) {
            //跟前面的那個比，看是否重複
            if (arr[i - 1] == arr[index]) {
                arr[index] = random.nextInt(m_iMax - m_iMin + 1) + m_iMin; //此位置生成新亂數
                checkRepeatNum(arr, index); //遞迴，直到不重複
            }
        }
    }

    private int[] createRandomArray() {
        //做出符合範圍且不重複的隨機陣列 imax imin size
        int[] arRandom = new int[(int) Math.pow(m_iBingoSize, 2)];
        for (int x = 0; x < arRandom.length; x++) {
            arRandom[x] = random.nextInt(m_iMax - m_iMin + 1) + m_iMin;
            checkRepeatNum(arRandom, x);
        }
        return arRandom;
    }

    /**
     * --------------------------------------------創建不同Adapter for 賓果盤--------------------------------------------------
     **/

    private void setEmptyFormAdapter() {
        //確認設定都沒問題，產生表格
        //將bingo size配給adapter，由他做新陣列
        BingoAdapter adapter = new BingoAdapter((int) Math.pow(m_iBingoSize, 2));
        rv_bingorecycler.setAdapter(adapter);
        //設定為網格排列模式
        GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), m_iBingoSize);
        rv_bingorecycler.setLayoutManager(layoutManager);
    }

    private void setRandomFormAdpater() {
        //做出符合範圍且不重複的隨機陣列 imax imin size
        setRecyclerviewAdapter(createRandomArray(), BingoActivity.this);
    }

    private void createGameAdapter() {
        //取得正確的數字位置陣列，把它塞到新的adpater
        setRecyclerviewAdapter(dataModel.getCompleteArray(), BingoActivity.this);
    }

    //for 遊戲結束restart
    public void createRandomGameAdapter() {
        dataModel.setIsObey(true); //隨機產生，一定遵守賓果盤規則
//        //做出符合範圍且不重複的隨機陣列 imax imin size
        int[] arRandom = createRandomArray();

        //放入完成陣列，這樣重新開始後如果又要回到編輯才會是這次的數字
        dataModel.setCompleteArray(arRandom);

        setRecyclerviewAdapter(arRandom, BingoActivity.this);
    }

    public void setGameToInputAdpater() {
        //switch回輸入模式
        m_swSwitch.setChecked(false);
        dataModel.setMode(false);
        m_tvInstruction.setText(R.string.switch_off_instruction); //指示文字換成輸入的

        //取得正確的數字位置陣列，把它塞到新的adpater
        //將原遊戲陣列配給adapter
        setRecyclerviewAdapter(dataModel.getCompleteArray(), BingoActivity.this);
    }

    /**
     * --------------------------------------------創建Adapter for 賓果盤--------------------------------------------------
     **/

    private void setRecyclerviewAdapter(int[] arr, Activity ac) {
        BingoAdapter adapter = new BingoAdapter(arr, ac);
        rv_bingorecycler.setAdapter(adapter);
        //設定為網格排列模式
        GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), m_iBingoSize);
        rv_bingorecycler.setLayoutManager(layoutManager);
    }

    //存入Value類型為String的數據
    public void putGameTimes(String key, int value) {
        mEditor.putInt(key, value);
        this.mEditor.commit();
    }
}
