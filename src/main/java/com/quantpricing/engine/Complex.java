package com.quantpricing.engine;

/**
 * Complex number implementation for Fourier-based pricing models.
 */
public class Complex {

    private final double real;
    private final double imag;

    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);
    public static final Complex I = new Complex(0, 1);

    public Complex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    public double real() {
        return real;
    }

    public double imag() {
        return imag;
    }

    public Complex add(Complex other) {
        return new Complex(this.real + other.real, this.imag + other.imag);
    }

    public Complex add(double scalar) {
        return new Complex(this.real + scalar, this.imag);
    }

    public Complex subtract(Complex other) {
        return new Complex(this.real - other.real, this.imag - other.imag);
    }

    public Complex subtract(double scalar) {
        return new Complex(this.real - scalar, this.imag);
    }

    public Complex multiply(Complex other) {
        double newReal = this.real * other.real - this.imag * other.imag;
        double newImag = this.real * other.imag + this.imag * other.real;
        return new Complex(newReal, newImag);
    }

    public Complex multiply(double scalar) {
        return new Complex(this.real * scalar, this.imag * scalar);
    }

    public Complex divide(Complex other) {
        double denom = other.real * other.real + other.imag * other.imag;
        if (Math.abs(denom) < 1e-15) {
            throw new ArithmeticException("Division by zero in complex number");
        }
        double newReal = (this.real * other.real + this.imag * other.imag) / denom;
        double newImag = (this.imag * other.real - this.real * other.imag) / denom;
        return new Complex(newReal, newImag);
    }

    public Complex divide(double scalar) {
        if (Math.abs(scalar) < 1e-15) {
            throw new ArithmeticException("Division by zero");
        }
        return new Complex(this.real / scalar, this.imag / scalar);
    }

    public Complex negate() {
        return new Complex(-this.real, -this.imag);
    }

    public Complex conjugate() {
        return new Complex(this.real, -this.imag);
    }

    public double abs() {
        return Math.sqrt(this.real * this.real + this.imag * this.imag);
    }

    public double absSquared() {
        return this.real * this.real + this.imag * this.imag;
    }

    /**
     * Complex exponential: exp(a + bi) = exp(a) * (cos(b) + i*sin(b))
     */
    public Complex exp() {
        double expReal = Math.exp(this.real);
        return new Complex(expReal * Math.cos(this.imag), expReal * Math.sin(this.imag));
    }

    /**
     * Complex natural logarithm: log(z) = log|z| + i*arg(z)
     */
    public Complex log() {
        double modulus = this.abs();
        if (modulus < 1e-15) {
            throw new ArithmeticException("Log of zero");
        }
        double argument = Math.atan2(this.imag, this.real);
        return new Complex(Math.log(modulus), argument);
    }

    /**
     * Complex square root using principal branch
     */
    public Complex sqrt() {
        double r = this.abs();
        double theta = Math.atan2(this.imag, this.real);
        double sqrtR = Math.sqrt(r);
        return new Complex(sqrtR * Math.cos(theta / 2), sqrtR * Math.sin(theta / 2));
    }

    /**
     * Complex power: z^w = exp(w * log(z))
     */
    public Complex pow(Complex w) {
        if (Math.abs(this.real) < 1e-15 && Math.abs(this.imag) < 1e-15) {
            return ZERO;
        }
        return this.log().multiply(w).exp();
    }

    public Complex pow(double exponent) {
        return pow(new Complex(exponent, 0));
    }

    /**
     * Create complex from polar form
     */
    public static Complex fromPolar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }

    /**
     * Create purely imaginary number
     */
    public static Complex imaginary(double imag) {
        return new Complex(0, imag);
    }

    /**
     * Create purely real number
     */
    public static Complex real(double real) {
        return new Complex(real, 0);
    }

    @Override
    public String toString() {
        if (Math.abs(imag) < 1e-10) {
            return String.format("%.6f", real);
        } else if (Math.abs(real) < 1e-10) {
            return String.format("%.6fi", imag);
        } else if (imag >= 0) {
            return String.format("%.6f + %.6fi", real, imag);
        } else {
            return String.format("%.6f - %.6fi", real, -imag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Complex complex = (Complex) obj;
        return Math.abs(this.real - complex.real) < 1e-10
            && Math.abs(this.imag - complex.imag) < 1e-10;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(real) * 31 + Double.hashCode(imag);
    }
}
