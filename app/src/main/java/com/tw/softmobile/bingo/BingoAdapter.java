package com.tw.softmobile.bingo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BingoAdapter extends RecyclerView.Adapter<BingoAdapter.ViewHolder> {
    private Listener listener;

    private int[] m_arInput;
    private int m_iMax;
    private int m_iMin;
    private DataModel dataModel = new DataModel();
    private int m_iLength;
    private boolean m_bIsRandomMode;

    private int[] m_arPicked; //用來看有沒有被選起來的陣列
    private int[] m_arCheckRepeat; //用來存放關鍵數字特定的位置，若數字在他的位置內，代表他有連線過
    private int m_iBingoLineNum;
    Activity context;


    // 加入介面，讓使用者按下卡片視區時能呼叫onClick
    public interface Listener {
        void onClick(int position);
    }

    //activities and fragment會使用此方法來註冊listener
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    //建立view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // 定義各個資料項目使用的視區
        //recyclerview需顯示cardview，所以宣告viewHolder裡有cardview。若要在recycler視區內顯示別的類型的資料須在此處定義
        private CardView cardView;

        public ViewHolder(CardView itemView) {
            super(itemView);
            cardView = itemView;
        }
    }

    //空白輸入模式建構子，只要告知長度，在這裡生成全新陣列
    public BingoAdapter(int length) {
        //此為空表格的陣列
        this.m_iLength = length;
        m_arInput = new int[m_iLength];
        //如果是輸入模式才需要default
        if (!dataModel.getMode()) {
            for (int i = 0; i < m_arInput.length; i++) {
                m_arInput[i] = -1; //input the defaut value to array
            }
        }

        //取得range
        m_iMax = dataModel.getRangeMax();
        m_iMin = dataModel.getRangeMin();
    }

    //遊戲模式會由輸入模式生成儲存到datamodel的陣列，再由activity傳過來這裡的ar_input，亂數產生陣列也會直接存在datamodel
    public BingoAdapter(int[] ar_input, Activity c) {
        this.m_arInput = ar_input;

        //如果是輸入模式，代表是亂數產生
        if (!dataModel.getMode()) {
            m_bIsRandomMode = true;
        } else {
            //此為遊戲模式，創建pick array
            this.context = c; //取得activity context
            m_arPicked = new int[m_arInput.length];
        }

        //取得range
        m_iMax = dataModel.getRangeMax();
        m_iMin = dataModel.getRangeMin();
    }

    //視區建立新view holder時呼叫這邊(使用cardview版面)
    @NonNull
    @Override
    public BingoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 建立項目使用的畫面配置元件，指定viewholder需使用哪個版面(card)
        // 用layoutinflater將版面轉為cardview
        /** 判斷要用哪個cardview **/
        CardView cv;
        if (!dataModel.getMode()) {
            //如果是輸入模式，就使用輸入的cardview
            cv = (CardView) LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_bingo, parent, false);
        } else {
            //如果是遊戲魔模式，就使用顯示的cardview
            cv = (CardView) LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_bingo_game, parent, false);
        }
        // 建立與回傳包裝好的畫面配置元件
        return new ViewHolder(cv);
    }

    //當recycler視區想要使用或重複使用view holder來顯示新資料時會呼叫此方法(且將資料放進去view相對應位置裡面)
    @Override
    public void onBindViewHolder(@NonNull BingoAdapter.ViewHolder holder, final int position) {

        if (!dataModel.getMode()) {
            //輸入模式
            final CardView cardView = holder.cardView;
            EditText etBingoNum = cardView.findViewById(R.id.et_bingo_num);
            final ConstraintLayout clBingo = cardView.findViewById(R.id.cl_cardview);

            //如果是Random button案進來的話
            if (m_bIsRandomMode) {
                //把ET設成亂數陣列
                etBingoNum.setText(Integer.toString(m_arInput[position]));
                //亂數條件一定對，若有編輯則textWatcher會處理
                dataModel.setIsObey(true);
                dataModel.setCompleteArray(m_arInput);
            }

            //監聽Edittext的文字變化
            etBingoNum.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    Log.d("isempty?", "" + charSequence.toString().isEmpty());

                    //這邊是做表格輸入的規則錯誤之介面顯示標記
                    if (!charSequence.toString().isEmpty()) {
                        m_arInput[position] = Integer.parseInt(charSequence.toString());
                        Log.d("testt", "min= " + m_iMin + "max= " + m_iMax);
                        checkEditTextRangeAndRepeat(position, clBingo, charSequence);
                    } else {
                        //變空的，所以填入預設值
                        m_arInput[position] = -1;
                    }

                    /**  檢查賓果盤的規則，並決定是否可切換至遊戲模式 **/
                    checkBingoFormRule();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    //Log.d("testt", "after = " + editable.toString());
                }
            });

            setCardviewListener(cardView, position);
        } else {
            //遊戲模式
            final CardView cardViewGame = holder.cardView;
            TextView tvBingoNum = cardViewGame.findViewById(R.id.tv_bingo_num);
            tvBingoNum.setText(Integer.toString(m_arInput[position]));

            final ConstraintLayout clBingoGame = cardViewGame.findViewById(R.id.cl_cardview_game);

            final int iBingoSize = (int) Math.pow(m_arInput.length, 0.5); //陣列長度是格數，要換回邊長

            /**             ----------------------------------------------------------------------------  **/
            m_arCheckRepeat = new int[2 * iBingoSize + 2];
            //此陣列為用來檢查連線是否重複
            /**             ----------------------------------------------------------------------------  **/

            m_iBingoLineNum = 0; //累積的連線數
            final int iWinConSet = dataModel.getWinCon(); //設定的勝利條件

            clBingoGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("pick position?", position + "");
                    //產生標記有無被點選的陣列，並產生顏色標記效果
                    generatePickArrayAndClickColor(position, clBingoGame, iBingoSize);

                    int[] arTmpPostion = new int[m_arInput.length];
                    int[] arTmpMinus = new int[(arTmpPostion.length * (arTmpPostion.length - 1)) / 2];
                    //最多會有(n*(n-1))/2種差的可能

                    //將標記選取的陣列轉換成value為選取的位置
                    generateAndTransformPickedToPosition(arTmpPostion);

                    //以position陣列製作出相差的陣列
                    generateMinusArray(arTmpPostion, arTmpMinus);

                    //連線邏輯判斷，有連線就會計算連線數
                    checkConnect(arTmpPostion, arTmpMinus, iBingoSize);

                    //判斷是否連線達到勝利條件，若有則show dialog
                    checkAndShowWinDialog(iWinConSet);
                }
            });
        }
    }

    private void checkEditTextRepeat(int position, ConstraintLayout cl) {
        /** 單次重複判斷顯示，每次輸入數字都做判斷，若重複則標記紅 **/
        boolean bIsRepeat = false;
        //查看陣列內是否有重複值
        //以目前輸入的數字為原點，向前與向後找有沒有重複的，若有就紅起來
        //往後找
        for (int j = position + 1; j < m_arInput.length; j++) {
            Log.d("fortest", "position: " + m_arInput[position] + "\n j: " + m_arInput[j]);
            if (m_arInput[position] == m_arInput[j]) {
                cl.setBackgroundResource(R.color.red);
                bIsRepeat = true;
                break;
            }
        }
        //往前找
        for (int k = position - 1; k > -1; k--) {
            if (m_arInput[position] == m_arInput[k]) {
                cl.setBackgroundResource(R.color.red);
                bIsRepeat = true;
                break;
            }
        }

        if (!bIsRepeat) {
            //沒重覆
            cl.setBackgroundResource(R.color.original);
        }
    }

    private void checkEditTextRangeAndRepeat(int position, ConstraintLayout cl, CharSequence charSequence) {
        //若超出範圍，背景紅色
        if (m_iMin > Integer.parseInt(charSequence.toString()) || m_iMax < Integer.parseInt(charSequence.toString())) {
            cl.setBackgroundResource(R.color.red);
        } else {
            //沒超出範圍的話，檢查是否重複
            checkEditTextRepeat(position, cl);
        }
    }

    private void checkBingoFormRule() {
        /** 以下為完整表格判斷 ，看是否遵守規則，並填入isObey，來決定是否可變更為遊戲模式**/
        boolean bNotEmpty = false;
        boolean bNotOutOfRange = false;

        int iNotEmptyNum = 0;
        int iNotOutOfRange = 0;
        //找有無空的
        for (int j = 0; j < m_arInput.length; j++) {
            //不是空的加上去
            if (m_arInput[j] != -1) {
                iNotEmptyNum++;
                Log.d("isempty", "" + iNotEmptyNum);
            }

            //符合範圍加上去
            if (m_arInput[j] <= m_iMax && m_arInput[j] >= m_iMin) {
                iNotOutOfRange++;
                Log.d("isoutofrange", "" + iNotOutOfRange);
            }
        }
        //若不是空的格數與陣列長度相符，代表沒有空的
        if (iNotEmptyNum == m_arInput.length) {
            bNotEmpty = true;
        }
        //若符合範圍格數與陣列長度相符，代表全部符合
        if (iNotOutOfRange == m_arInput.length) {
            bNotOutOfRange = true;
        }

        Log.d("rule?", "not empty?" + bNotEmpty);
        //如果不是空的且符合範圍
        if (bNotEmpty && bNotOutOfRange) {
            //先set true，若找完發現重複錯誤則設false
            dataModel.setIsObey(true);
            dataModel.setCompleteArray(m_arInput);

            //規則完全正確，可改變模式開始遊戲，傳boolean過去activity
            //檢查陣列內是否有重複值
            for (int j = 0; j < m_arInput.length - 1; j++) {
                for (int k = j + 1; k < m_arInput.length; k++) {
                    if (m_arInput[j] == m_arInput[k]) {
                        dataModel.setIsObey(false);
                        break;
                    }
                }
            }

            Log.d("rule?", "obey?" + dataModel.getIsObey());
        } else {
            //如果空的設false
            dataModel.setIsObey(false);
        }
    }

    /**此方法主要用來辨識該位置有沒有被按過，如果有就消除，沒有就標起來**/
    private void generatePickArrayAndClickColor(int position, ConstraintLayout clBingoGame, int iBingoSize) {
        if (m_arPicked[position] == 1) {
            //代表被點過
            m_arPicked[position] = 0;
            clBingoGame.setBackgroundResource(R.color.original);
            //查看取消的這格是不是有被連線過，如果有就扣掉累積連線數
            checkLineCancel(position, iBingoSize);
        } else {
            //代表沒被點過
            m_arPicked[position] = 1;
            clBingoGame.setBackgroundResource(dataModel.getPickColor()); //去抓選哪個顏色
        }
    }

    private void checkLineCancel(int position, int iBingoSize) {
        /** 先看按的這格是不是關鍵位置，如果是就直接先去找重複陣列內有沒有，如果有代表連過線-1
         *  如果不是關鍵位置的話，抓按的那格是是哪個位置的，並把他關聯的關鍵位置去除掉並連線-1
         * **/
        for (int i=0; i<m_arCheckRepeat.length; i++) {
            if (m_arCheckRepeat[i] == position + 1) {
                m_iBingoLineNum--;
                Log.d("xxx", m_arCheckRepeat[i] + "/" + m_iBingoLineNum);
                m_arCheckRepeat[i] = -1; //設為預設值
            }

            if (m_arCheckRepeat[i] != 0 && m_arCheckRepeat[i] != -1) {
                if (i<=(iBingoSize-1)) {
                    for (int j=1; j<iBingoSize;j++) {
                        Log.d("xxx", "position"+(position+1)+"j"+(m_arCheckRepeat[i]+j));
                        if (m_arCheckRepeat[i]+j == position+1) {
                            //代表已連線的後面幾個被按掉了
                            m_iBingoLineNum--;
                            Log.d("xxx","ui1");
                            m_arCheckRepeat[i] = -1;
                            break;
                        }
                    }
                } else if (i>(iBingoSize-1) && (i <= iBingoSize+(iBingoSize-1))) {
                    Log.d("xxx",i+"");
                    for (int j=1; j<iBingoSize;j++) {
                        if (m_arCheckRepeat[i]+j*iBingoSize == position+1) {
                            //代表已連線的後面幾個被按掉了
                            m_iBingoLineNum--;
                            Log.d("xxx","ui2"+ position);
                            m_arCheckRepeat[i] = -1;
                            break;
                        }
                    }
                } else if (i == 2*iBingoSize) {
                    for (int j=1; j<iBingoSize;j++) {
                        //左斜
                        if (m_arCheckRepeat[i]+j*(iBingoSize+1) == position+1) {
                            //代表已連線的後面幾個被按掉了
                            m_iBingoLineNum--;
                            m_arCheckRepeat[i] = -1;
                            break;
                        }
                    }
                } else if (i == 2*iBingoSize+1) {
                    //右斜
                    for (int j=1; j<iBingoSize;j++) {
                        if (m_arCheckRepeat[i]+j*(iBingoSize-1) == position+1) {
                            //代表已連線的後面幾個被按掉了
                            m_iBingoLineNum--;
                            m_arCheckRepeat[i] = -1;
                            break;
                        }
                    }
                }
                Log.d("xxx", m_arCheckRepeat[i] + "/" + m_iBingoLineNum);
            }
        }
    }

    private void generateMinusArray(int[] arTmpPostion, int[] arTmpMinus) {
        int iTmpMinusIndex = 0;
        int iMinus;
        for (int i = 0; i < arTmpPostion.length - 1; i++) {
            if (arTmpPostion[i] == -1) {
                break;
            } else {
                for (int j = i + 1; j < arTmpPostion.length; j++) {
                    if (arTmpPostion[j] == -1) {
                        break;
                    } else {
                        //有值的範圍
                        iMinus = arTmpPostion[j] - arTmpPostion[i];
                        arTmpMinus[iTmpMinusIndex] = iMinus;
                        iTmpMinusIndex++;
                    }
                }
            }
        }
    }

    private void generateAndTransformPickedToPosition(int[] arTmpPostion) {
        //arTmpPosition初始化，預設值為-1
        for (int i = 0; i < arTmpPostion.length; i++) {
            arTmpPostion[i] = -1;
        }

        int iTmpPositionIndex = 0;
        for (int j = 0; j < m_arPicked.length; j++) {
            if (m_arPicked[j] == 1) {
                Log.d("pick array", "[ " + j + " ] " + m_arPicked[j]);
                arTmpPostion[iTmpPositionIndex] = j + 1; //不從0開始，從1開始計
                iTmpPositionIndex++;
            }
        }
    }

    //連線邏輯判斷
    private void checkConnect(int[] arTmpPostion, int[] arTmpMinus, int iBingoSize) {
        int iMinusNum;
        //找重複的差，以0~2來說，從0去找1.2，1去找2，去看有沒有相同值
        for (int j = 0; j < arTmpMinus.length; j++) {
            iMinusNum = 1; //算自己，所以是1
            //J是零就停止搜尋
            if (arTmpMinus[j] != 0) {
                Log.d("tmparrayMinus", "[" + j + "]" + arTmpMinus[j] + "");
                //若同差的總數大於等於n-1 (代表可能會有連線的機會 成立step 1)
                if (computeNumOfDuplicateDifference(j, arTmpMinus, iMinusNum) >= (iBingoSize - 1)) {
                    //差1.n.n-1.n+1可能成立
                    /**   若差為1  **/
                    if (arTmpMinus[j] == 1) {
                        for (int x = 0; x < iBingoSize; x++) {
                            for (int i = 0; i < arTmpPostion.length; i++) {
                                if (arTmpPostion[i] == ((x * iBingoSize) + 1) &&
                                        m_arCheckRepeat[x] != arTmpPostion[i]) {
                                    //抓到關鍵位置，繼續下一條件
                                    int iCompleteNum = 0;
                                    //判斷是否達成連線條件(看是不是後面n-1個數字都在選取陣列內)
                                    // 若是則將依關鍵數字放入重複陣列內，並增加連線總數
                                    computeConnectLines(x, i, arTmpPostion,
                                            checkAfterKeyNumberRulesAndReturnCompleteNum
                                                    (i, iBingoSize, arTmpPostion, iCompleteNum, "橫")
                                            , iBingoSize, "橫");
                                    break; //找到關鍵位置後就不用再找position陣列了，值是唯一
                                }
                            }
                        }
                        /**   若差為N  **/
                    } else if (arTmpMinus[j] == iBingoSize) {
                        for (int x = 1; x <= iBingoSize; x++) {
                            for (int i = 0; i < arTmpPostion.length; i++) {
                                if (arTmpPostion[i] == x &&
                                        m_arCheckRepeat[x + (iBingoSize - 1)] != arTmpPostion[i]) {
                                    //抓到關鍵位置，繼續下一條件
                                    int iCompleteNum = 0;
                                    //判斷是否達成連線條件，若是則將依此數字放入重複陣列內，並增加連線總數
                                    computeConnectLines(x, i, arTmpPostion,
                                            checkAfterKeyNumberRulesAndReturnCompleteNum
                                                    (i, iBingoSize, arTmpPostion, iCompleteNum, "直")
                                            , iBingoSize, "直");
                                    break;
                                }
                            }
                        }
                        /**   若差為N-1 **/
                    } else if (arTmpMinus[j] == (iBingoSize - 1)) {
                        for (int i = 0; i < arTmpPostion.length; i++) {
                            //如果是關鍵位置，並且不再重複陣列最後項內
                            if (arTmpPostion[i] == iBingoSize && arTmpPostion[i] != m_arCheckRepeat[2 * iBingoSize + 1]) {
                                //抓到關鍵位置，繼續下一條件
                                int iCompleteNum = 0;
                                //判斷是否達成連線條件，若是則將依此數字放入重複陣列內，並增加連線總數
                                computeConnectSlashLines(i, arTmpPostion,
                                        checkAfterKeyNumberRulesAndReturnCompleteNum
                                                (i, iBingoSize, arTmpPostion, iCompleteNum, "右斜")
                                        , iBingoSize, "右斜");

                                break;
                            }
                        }
                        /**   若差為N+1  **/
                    } else if (arTmpMinus[j] == (iBingoSize + 1)) {
                        for (int i = 0; i < arTmpPostion.length; i++) {
                            //如果關鍵位置為1並且不在重複陣列倒數第二項
                            if (arTmpPostion[i] == 1 && arTmpPostion[i] != m_arCheckRepeat[2 * iBingoSize]) {
                                //抓到關鍵位置，繼續下一條件
                                int iCompleteNum = 0;
                                //判斷是否達成連線條件，若是則將依此數字放入重複陣列內，並增加連線總數
                                computeConnectSlashLines(i, arTmpPostion,
                                        checkAfterKeyNumberRulesAndReturnCompleteNum
                                                (i, iBingoSize, arTmpPostion, iCompleteNum, "左斜")
                                        , iBingoSize, "左斜");

                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // 檢查該連線之關鍵數字規則下後面n-1個數字是不是在選取陣列內，並計算符合的數字個數
    private int checkAfterKeyNumberRulesAndReturnCompleteNum(int i, int iBingoSize, int[] arTmpPostion,
                                                             int iCompleteNum, String type) {
        for (int y = 1; y < iBingoSize; y++) {
            for (int a = 0; a < arTmpPostion.length; a++) {
                //查看後面n-1個是否都在選取陣列內
                if (type.equals("橫")) {
                    if (arTmpPostion[a] == (arTmpPostion[i] + y)) {
                        //代表條件達成
                        iCompleteNum++;
                    }
                } else if (type.equals("直")) {
                    //查看後面n-1個是否都在選取陣列內(ex:1.2.3 陣列李若有147.258.369都差3)
                    if (arTmpPostion[a] == (arTmpPostion[i] + (iBingoSize * y))) {
                        //代表條件達成
                        iCompleteNum++;
                    }
                } else if (type.equals("右斜")) {
                    //查看後面n-1個是否都在選取陣列內(ex:以3來說，就看3+2跟3+4)
                    if (arTmpPostion[a] == (arTmpPostion[i] + y * (iBingoSize - 1))) {
                        //代表條件達成，只有一條
                        iCompleteNum++;
                    }
                } else if (type.equals("左斜")) {
                    //查看後面n-1個是否都在選取陣列內(ex:以3來說，就看3+2跟3+4)
                    if (arTmpPostion[a] == (arTmpPostion[i] + y * (iBingoSize + 1))) {
                        //代表條件達成，只有一條
                        iCompleteNum++;
                    }
                }
            }
        }
        return iCompleteNum;
    }

    //判斷是否達成連線條件(看是不是後面n-1個數字都在選取陣列內)
    // 若是則將依關鍵數字放入重複陣列內，並增加連線總數
    private void computeConnectLines(int x, int i, int[] arTmpPostion,
                                     int iCompleteNum, int iBingoSize, String s) {
        if (iCompleteNum == (iBingoSize - 1)) {
            //若後面n-1個都在，就代表連線
            Log.d("check", s + "complete!");
            if (s.equals("橫")) {
                m_arCheckRepeat[x] = arTmpPostion[i];
            } else if (s.equals("直")) {
                m_arCheckRepeat[x + (iBingoSize - 1)] = arTmpPostion[i];
            }
            //以3*3來說，X為0.1.2，讓這些位置代表橫的index，並且特定index只讓特定數字擁有
            //ex:index=0, value一定是1
            m_iBingoLineNum++; //累積連線數+1
        }
    }

    private void computeConnectSlashLines(int i, int[] arTmpPostion, int iCompleteNum,
                                          int iBingoSize, String s) {
        if (iCompleteNum == (iBingoSize - 1)) {
            //若後面n-1個都在，就代表連線
            Log.d("check", s + "complete!");
            if (s.equals("右斜")) {
                m_arCheckRepeat[2 * iBingoSize + 1] = arTmpPostion[i];
                //加入重複陣列的最後項
            } else if (s.equals("左斜")) {
                m_arCheckRepeat[2 * iBingoSize] = arTmpPostion[i]; //加入重複陣列倒數第二項
            }

            m_iBingoLineNum++; //累積連線數+1
        }
    }

    //計算重複差的數量
    private int computeNumOfDuplicateDifference(int j, int[] arTmpMinus, int iMinusNum) {
        for (int k = j + 1; k < arTmpMinus.length - 1; k++) {
            if (arTmpMinus[k] != 0) {
                //算有幾個相同差
                if (arTmpMinus[j] == arTmpMinus[k]) {
                    iMinusNum++;
                }
            }
        }
        return iMinusNum;
    }

    private void checkAndShowWinDialog(int iWinConSet) {
        if (m_iBingoLineNum >= iWinConSet) {
            dataModel.setIsObey(false); //遊戲結束，賓果盤條件歸零
            //勝利
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle(R.string.alertdialog_title);
            alertDialog.setMessage(String.format(context.getString(R.string.alertdialog_context), m_iBingoLineNum));
            alertDialog.setCancelable(false); //點旁邊不能被按掉
            alertDialog.setNegativeButton(R.string.restart, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (context instanceof BingoActivity) {
                        //呼叫創建隨機的遊戲模式
                        ((BingoActivity) context).createRandomGameAdapter();
                        //執行遊戲次數+1
                        ((BingoActivity) context).putGameTimes(
                                ((BingoActivity) context).m_strName, (((BingoActivity) context).m_iGameTimes));
                    }
                }
            });
            alertDialog.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (context instanceof BingoActivity) {
                        //呼叫創建隨機的遊戲模式
                        ((BingoActivity) context).setGameToInputAdpater();
                    }
                }
            });
            alertDialog.show();
        }
    }

    @Override
    public int getItemCount() {
        //原本是m_arIndex.length
        return m_arInput.length;
    }

    private void setCardviewListener(final CardView cv, final int ps) {
        //讓cardview可以被按下並啟動別的(將listener加入cardview)
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //當cardview被按下時呼叫listener.onClick()
                if (listener != null) {
                    listener.onClick(ps);
                }
            }
        });
    }

}
