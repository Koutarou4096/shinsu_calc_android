package com.example.shinsu_calc;

import androidx.annotation.NonNull;

import java.math.BigInteger;

public class Rational {
    public final BigInteger n; // 分子
    public final BigInteger d; // 分母

    public Rational(BigInteger numerator, BigInteger denominator) {
        if (denominator.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("0で割ることはできません。");
        }
        if (denominator.compareTo(BigInteger.ZERO) < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger g = numerator.abs().gcd(denominator);
        this.n = numerator.divide(g);
        this.d = denominator.divide(g);
    }

    public Rational add(Rational x) {
        return new Rational(
                this.n.multiply(x.d).add(x.n.multiply(this.d)),
                this.d.multiply(x.d)
        );
    }

    public Rational sub(Rational x) {
        return new Rational(
                this.n.multiply(x.d).subtract(x.n.multiply(this.d)),
                this.d.multiply(x.d)
        );
    }

    public Rational mul(Rational x) {
        return new Rational(
                this.n.multiply(x.n),
                this.d.multiply(x.d)
        );
    }

    public Rational div(Rational x) {
        if (x.n.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("0で割ることはできません。");
        }
        return new Rational(
                this.n.multiply(x.d),
                this.d.multiply(x.n)
        );
    }

    /**
     * 余り: this - x * trunc(this / x)
     * 符号は被除数(this)に従う（0方向への切り捨て除算に基づく剰余）
     */
    public Rational mod(Rational x) {
        if (x.n.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("0で割ることはできません。");
        }
        BigInteger q = this.n.multiply(x.d).divide(this.d.multiply(x.n));
        return this.sub(x.mul(new Rational(q, BigInteger.ONE)));
    }

    @NonNull
    @Override
    public String toString() {
        return d.equals(BigInteger.ONE) ? n.toString() : n + "/" + d;
    }
}