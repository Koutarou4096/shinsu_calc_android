package com.example.shinsu_calc;

import android.text.TextUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BaseCalculator {
    public static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static class StepItem {
        public final int number;
        public final String title;
        public final String formulaHtml;

        public StepItem(int number, String title, String formulaHtml) {
            this.number = number;
            this.title = title;
            this.formulaHtml = formulaHtml;
        }
    }

    public static Rational parseBaseNumber(String input, int base) {
        String s = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("数値を入力してください。");
        }

        int sign = 1;
        if (s.charAt(0) == '+' || s.charAt(0) == '-') {
            if (s.charAt(0) == '-') sign = -1;
            s = s.substring(1);
        }

        if (s.isEmpty() || s.equals(".")) {
            throw new IllegalArgumentException(base + "進数として有効な数値を入力してください。");
        }

        String[] p = s.split("\\.", -1);
        if (p.length > 2) {
            throw new IllegalArgumentException("小数点は1個まで使用できます。");
        }

        String ip = p[0].isEmpty() ? "0" : p[0];
        String fp = p.length > 1 ? p[1] : "";

        validateDigits(ip, base);
        if (!fp.isEmpty()) {
            validateDigits(fp, base);
        }

        BigInteger bb = BigInteger.valueOf(base);
        BigInteger i = BigInteger.ZERO;
        for (int idx = 0; idx < ip.length(); idx++) {
            int val = DIGITS.indexOf(ip.charAt(idx));
            i = i.multiply(bb).add(BigInteger.valueOf(val));
        }

        BigInteger n = i;
        BigInteger d = BigInteger.ONE;
        if (!fp.isEmpty()) {
            BigInteger f = BigInteger.ZERO;
            for (int idx = 0; idx < fp.length(); idx++) {
                int val = DIGITS.indexOf(fp.charAt(idx));
                f = f.multiply(bb).add(BigInteger.valueOf(val));
            }
            d = bb.pow(fp.length());
            n = n.multiply(d).add(f);
        }

        return new Rational(sign < 0 ? n.negate() : n, d);
    }

    private static void validateDigits(String str, int base) {
        for (int i = 0; i < str.length(); i++) {
            int idx = DIGITS.indexOf(str.charAt(i));
            if (idx < 0 || idx >= base) {
                throw new IllegalArgumentException(base + "進数として無効な文字が含まれています。");
            }
        }
    }

    public static String rationalToBase(Rational x, int base) {
        return rationalToBase(x, base, 64);
    }

    public static String rationalToBase(Rational x, int base, int max) {
        if (x.n.equals(BigInteger.ZERO)) return "0";

        boolean neg = x.n.compareTo(BigInteger.ZERO) < 0;
        BigInteger n = neg ? x.n.negate() : x.n;
        BigInteger d = x.d;
        BigInteger bb = BigInteger.valueOf(base);

        BigInteger ip = n.divide(d);
        BigInteger r = n.remainder(d);
        String out = ip.toString(base).toUpperCase(Locale.ROOT);

        if (r.equals(BigInteger.ZERO)) {
            return (neg ? "-" : "") + out;
        }

        List<Character> ds = new ArrayList<>();
        Map<BigInteger, Integer> seen = new HashMap<>();
        int repeat = -1;

        while (!r.equals(BigInteger.ZERO) && ds.size() < max) {
            if (seen.containsKey(r)) {
                repeat = seen.get(r);
                break;
            }
            seen.put(r, ds.size());
            r = r.multiply(bb);
            BigInteger q = r.divide(d);
            r = r.remainder(d);
            ds.add(DIGITS.charAt(q.intValue()));
        }

        StringBuilder f = new StringBuilder();
        if (repeat >= 0) {
            for (int i = 0; i < repeat; i++) f.append(ds.get(i));
            f.append('(');
            for (int i = repeat; i < ds.size(); i++) f.append(ds.get(i));
            f.append(')');
        } else {
            for (char c : ds) f.append(c);
            if (!r.equals(BigInteger.ZERO)) f.append('…');
        }

        return (neg ? "-" : "") + out + "." + f;
    }

    public static List<StepItem> buildConversionSteps(String input, int fb, int tb, Rational value, String result) {
        List<StepItem> steps = new ArrayList<>();
        String clean = input.trim().toUpperCase(Locale.ROOT);

        if (fb == tb) {
            steps.add(new StepItem(
                    1,
                    "変換元と変換先が同じ進数です",
                    esc(clean) + "<sub>" + fb + "</sub> = <b>" + esc(result) + "</b><sub>" + tb + "</sub>"
            ));
            return steps;
        }

        String unsigned = clean.replaceFirst("^[+-]", "");
        String[] raw = unsigned.split("\\.", -1);
        String ip = raw[0].isEmpty() ? "0" : raw[0];
        String fp = raw.length > 1 ? raw[1] : "";

        List<String> parts = new ArrayList<>();
        for (int i = 0; i < ip.length(); i++) {
            int d = DIGITS.indexOf(ip.charAt(i));
            parts.add(d + "×" + fb + "<sup>" + (ip.length() - 1 - i) + "</sup>");
        }
        for (int i = 0; i < fp.length(); i++) {
            int d = DIGITS.indexOf(fp.charAt(i));
            parts.add(d + "×" + fb + "<sup>−" + (i + 1) + "</sup>");
        }

        String prefix = clean.startsWith("-") ? "−" : "";
        String formula1 = prefix + TextUtils.join(" ＋ ", parts) + "<br><b>= " + esc(value.toString()) + "</b>";
        steps.add(new StepItem(1, fb + "進数を正確な分数へ変換", formula1));

        String formula2 = esc(value.toString()) + "<sub>10</sub> → <b>" + esc(result) + "</b><sub>" + tb + "</sub>";
        steps.add(new StepItem(2, tb + "進数へ変換", formula2));

        return steps;
    }

    public static List<StepItem> buildCalcSteps(
            String at, int ab, String bt, int bb, String opSymbol, int rb,
            Rational a, Rational b, Rational r, String out
    ) {
        List<StepItem> steps = new ArrayList<>();
        steps.add(new StepItem(1, "第1項を正確な分数へ",
                esc(at) + "<sub>" + ab + "</sub> = <b>" + esc(a.toString()) + "</b><sub>10</sub>"));
        steps.add(new StepItem(2, "第2項を正確な分数へ",
                esc(bt) + "<sub>" + bb + "</sub> = <b>" + esc(b.toString()) + "</b><sub>10</sub>"));
        steps.add(new StepItem(3, "分数として演算",
                esc(a.toString()) + " " + esc(opSymbol) + " " + esc(b.toString()) + " = <b>" + esc(r.toString()) + "</b>"));
        steps.add(new StepItem(4, rb + "進数へ変換",
                esc(r.toString()) + "<sub>10</sub> → <b>" + esc(out) + "</b><sub>" + rb + "</sub>"));
        return steps;
    }

    private static String esc(String s) {
        return TextUtils.htmlEncode(s);
    }
}