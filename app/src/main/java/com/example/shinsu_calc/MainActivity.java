package com.example.shinsu_calc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int[] BASES = {2, 8, 10, 16};
    private static final String[] OP_LABELS = {"＋", "−", "×", "÷", "mod"};
    private static final String[] OP_KEYS = {"+", "-", "*", "/", "%"};

    private ScrollView rootScroll;
    private Button btnModeConvert, btnModeCalc;
    private LinearLayout panelConvert, panelCalc, sectionResult, containerSteps;

    // 進数変換UI
    private Spinner spinnerFromBase, spinnerToBase;
    private EditText editNumInput;
    private TextView textConvertError;

    // 進数計算UI
    private Spinner spinnerCalcBaseA, spinnerCalcBaseB, spinnerCalcOp, spinnerCalcResultBase;
    private EditText editCalcA, editCalcB;
    private TextView textCalcError;

    // 結果UI
    private TextView textResultLabel, textFinalResult, textResultSubtext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupSpinners();
        setupListeners();
    }

    private void bindViews() {
        rootScroll = findViewById(R.id.root_scroll);
        btnModeConvert = findViewById(R.id.btn_mode_convert);
        btnModeCalc = findViewById(R.id.btn_mode_calc);
        panelConvert = findViewById(R.id.panel_convert);
        panelCalc = findViewById(R.id.panel_calc);

        spinnerFromBase = findViewById(R.id.spinner_from_base);
        spinnerToBase = findViewById(R.id.spinner_to_base);
        editNumInput = findViewById(R.id.edit_num_input);
        textConvertError = findViewById(R.id.text_convert_error);

        spinnerCalcBaseA = findViewById(R.id.spinner_calc_base_a);
        spinnerCalcBaseB = findViewById(R.id.spinner_calc_base_b);
        spinnerCalcOp = findViewById(R.id.spinner_calc_op);
        spinnerCalcResultBase = findViewById(R.id.spinner_calc_result_base);
        editCalcA = findViewById(R.id.edit_calc_a);
        editCalcB = findViewById(R.id.edit_calc_b);
        textCalcError = findViewById(R.id.text_calc_error);

        sectionResult = findViewById(R.id.section_result);
        textResultLabel = findViewById(R.id.text_result_label);
        textFinalResult = findViewById(R.id.text_final_result);
        textResultSubtext = findViewById(R.id.text_result_subtext);
        containerSteps = findViewById(R.id.container_steps);
    }

    private void setupSpinners() {
        String[] baseItems = new String[BASES.length];
        for (int i = 0; i < BASES.length; i++) {
            baseItems[i] = BASES[i] + "進数";
        }

        ArrayAdapter<String> baseAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, baseItems);
        baseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFromBase.setAdapter(baseAdapter);
        spinnerToBase.setAdapter(baseAdapter);
        spinnerCalcBaseA.setAdapter(baseAdapter);
        spinnerCalcBaseB.setAdapter(baseAdapter);
        spinnerCalcResultBase.setAdapter(baseAdapter);

        // 初期選択: 変換元=10進数(index 2), 変換先=2進数(index 0)
        spinnerFromBase.setSelection(2);
        spinnerToBase.setSelection(0);

        // 計算初期選択: A=2進数(0), B=16進数(3), 結果=2進数(0)
        spinnerCalcBaseA.setSelection(0);
        spinnerCalcBaseB.setSelection(3);
        spinnerCalcResultBase.setSelection(0);

        ArrayAdapter<String> opAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, OP_LABELS);
        opAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCalcOp.setAdapter(opAdapter);
    }

    private void setupListeners() {
        // タブ切替
        btnModeConvert.setOnClickListener(v -> switchMode(false));
        btnModeCalc.setOnClickListener(v -> switchMode(true));

        // 進数入れ替えボタン
        findViewById(R.id.btn_swap).setOnClickListener(v -> {
            int fromPos = spinnerFromBase.getSelectedItemPosition();
            int toPos = spinnerToBase.getSelectedItemPosition();
            spinnerFromBase.setSelection(toPos);
            spinnerToBase.setSelection(fromPos);
            clearResult();
            validateConvertInput();
        });

        // リアルタイムバリデーション（変換モード）
        editNumInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearResult();
                validateConvertInput();
            }
        });

        AdapterView.OnItemSelectedListener convertSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                clearResult();
                validateConvertInput();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerFromBase.setOnItemSelectedListener(convertSpinnerListener);
        spinnerToBase.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                clearResult();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 変換実行
        findViewById(R.id.btn_convert).setOnClickListener(v -> executeConvert());
        editNumInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                executeConvert();
                return true;
            }
            return false;
        });

        // 計算モードの変更時クリア
        TextWatcher clearWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearResult();
            }
        };
        editCalcA.addTextChangedListener(clearWatcher);
        editCalcB.addTextChangedListener(clearWatcher);

        AdapterView.OnItemSelectedListener clearSelectListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                clearResult();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerCalcBaseA.setOnItemSelectedListener(clearSelectListener);
        spinnerCalcBaseB.setOnItemSelectedListener(clearSelectListener);
        spinnerCalcOp.setOnItemSelectedListener(clearSelectListener);
        spinnerCalcResultBase.setOnItemSelectedListener(clearSelectListener);

        // 計算実行
        findViewById(R.id.btn_calc).setOnClickListener(v -> executeCalc());
        editCalcB.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                executeCalc();
                return true;
            }
            return false;
        });

        // 結果カードタップでクリップボードへコピー
        findViewById(R.id.card_final_result).setOnClickListener(v -> {
            String resultText = textFinalResult.getText().toString();
            if (!resultText.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("計算結果", resultText));
                Toast.makeText(this, "結果をコピーしました: " + resultText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchMode(boolean isCalcMode) {
        clearResult();
        textConvertError.setText("");
        textCalcError.setText("");

        panelConvert.setVisibility(isCalcMode ? View.GONE : View.VISIBLE);
        panelCalc.setVisibility(isCalcMode ? View.VISIBLE : View.GONE);

        btnModeConvert.setBackgroundTintList(ColorStateList.valueOf(isCalcMode ? Color.parseColor("#E2E8F0") : Color.WHITE));
        btnModeConvert.setTextColor(Color.parseColor(isCalcMode ? "#64748B" : "#0F766E"));

        btnModeCalc.setBackgroundTintList(ColorStateList.valueOf(isCalcMode ? Color.WHITE : Color.parseColor("#E2E8F0")));
        btnModeCalc.setTextColor(Color.parseColor(isCalcMode ? "#0F766E" : "#64748B"));
    }

    private void validateConvertInput() {
        String raw = editNumInput.getText().toString();
        if (raw.trim().isEmpty()) {
            textConvertError.setText("");
            return;
        }
        int fb = BASES[spinnerFromBase.getSelectedItemPosition()];
        try {
            BaseCalculator.parseBaseNumber(raw, fb);
            textConvertError.setText("");
        } catch (Exception e) {
            textConvertError.setText(e.getMessage());
        }
    }

    private void executeConvert() {
        hideKeyboard();
        try {
            String input = editNumInput.getText().toString().trim().toUpperCase(Locale.ROOT);
            int fb = BASES[spinnerFromBase.getSelectedItemPosition()];
            int tb = BASES[spinnerToBase.getSelectedItemPosition()];

            Rational value = BaseCalculator.parseBaseNumber(input, fb);
            String result = BaseCalculator.rationalToBase(value, tb);
            textConvertError.setText("");

            List<BaseCalculator.StepItem> steps =
                    BaseCalculator.buildConversionSteps(input, fb, tb, value, result);
            showResult("変換結果", result, fb + "進数 → " + tb + "進数", steps);
        } catch (Exception e) {
            textConvertError.setText(e.getMessage());
            clearResult();
        }
    }

    private void executeCalc() {
        hideKeyboard();
        try {
            int ab = BASES[spinnerCalcBaseA.getSelectedItemPosition()];
            int bb = BASES[spinnerCalcBaseB.getSelectedItemPosition()];
            int rb = BASES[spinnerCalcResultBase.getSelectedItemPosition()];
            int opIndex = spinnerCalcOp.getSelectedItemPosition();
            String opKey = OP_KEYS[opIndex];
            String opSymbol = OP_LABELS[opIndex];

            String at = editCalcA.getText().toString().trim().toUpperCase(Locale.ROOT);
            String bt = editCalcB.getText().toString().trim().toUpperCase(Locale.ROOT);

            Rational a = BaseCalculator.parseBaseNumber(at, ab);
            Rational b = BaseCalculator.parseBaseNumber(bt, bb);
            Rational r;

            switch (opKey) {
                case "+":
                    r = a.add(b);
                    break;
                case "-":
                    r = a.sub(b);
                    break;
                case "*":
                    r = a.mul(b);
                    break;
                case "%":
                    r = a.mod(b);
                    break;
                default:
                    r = a.div(b);
                    break;
            }

            textCalcError.setText("");
            String out = BaseCalculator.rationalToBase(r, rb);
            String subText = ab + "進数 " + at + " " + opSymbol + " " + bb + "進数 " + bt + " → " + rb + "進数";

            List<BaseCalculator.StepItem> steps =
                    BaseCalculator.buildCalcSteps(at, ab, bt, bb, opSymbol, rb, a, b, r, out);
            showResult("計算結果", out, subText, steps);
        } catch (Exception e) {
            textCalcError.setText(e.getMessage());
            clearResult();
        }
    }

    private void clearResult() {
        sectionResult.setVisibility(View.GONE);
        textFinalResult.setText("");
        textResultSubtext.setText("");
        containerSteps.removeAllViews();
    }

    private void showResult(String label, String result, String subText, List<BaseCalculator.StepItem> steps) {
        textResultLabel.setText(label);
        textFinalResult.setText(result);
        textResultSubtext.setText(subText);

        containerSteps.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (BaseCalculator.StepItem step : steps) {
            // ステップ見出し
            TextView titleView = new TextView(this);
            titleView.setText(step.number + ". " + step.title);
            titleView.setTextColor(Color.parseColor("#334155"));
            titleView.setTextSize(14f);
            titleView.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.topMargin = (int) (12 * density);
            titleParams.bottomMargin = (int) (4 * density);
            containerSteps.addView(titleView, titleParams);

            // 数式ブロック（上付き・下付き文字をHtmlCompatでレンダリング）
            TextView formulaView = new TextView(this);
            formulaView.setText(HtmlCompat.fromHtml(step.formulaHtml, HtmlCompat.FROM_HTML_MODE_LEGACY));
            formulaView.setTextColor(Color.parseColor("#1E293B"));
            formulaView.setTextSize(14f);
            formulaView.setTypeface(Typeface.MONOSPACE);
            formulaView.setBackgroundColor(Color.WHITE);
            int pad = (int) (12 * density);
            formulaView.setPadding(pad, pad, pad, pad);
            containerSteps.addView(formulaView);
        }

        sectionResult.setVisibility(View.VISIBLE);
        rootScroll.post(() -> rootScroll.smoothScrollTo(0, sectionResult.getTop()));
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}