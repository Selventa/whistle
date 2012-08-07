package com.selventa.whistle.score.util;

import org.apache.commons.math3.special.Beta;


public class MathUtil {
	
	public static final double DOUBLE_PRECISION_TOLERANCE = 0.0000000001d; //1e-10
	
	protected static final int MAX_PRECALCULATED_LOG_SUM = 60000; //human genome has about 20k to 25k genes, 30k ought to be enough for our analyses

	//the nth element of the following array contains the value: log(n) + log(n-1) + ... + log(0)
	private static double logSumArray[] = new double[MAX_PRECALCULATED_LOG_SUM + 1]; //+1 to include end point
	
	static {
		//initialize array when class is loaded into memory
		//synchronization is not necessary. class loader guarantees this is executed only once.
		initializeLogSumArray();
	}
	

	private MathUtil() {
		//this is a static helper class, not to be instantiated.
	}
	
	/**
	 * initialize the static logSumArray for later factorial calculations. 
	 */
	private static void initializeLogSumArray() {
		logSumArray[0] = 0;
		for( int i = 1; i < logSumArray.length; i++ ) {
			logSumArray[i] = logSumArray[i - 1] + Math.log(i); 
		}
	}	
	
	/**
	 * Returns sum of log(n) for all n in [0, n].
	 * @param n
	 * @return sum	sum( log(0) + log(1) + log(2) + ... + log(n) )
	 */
	protected static double logSum(int n) {
		double logSum = 0;
		if( n <= MAX_PRECALCULATED_LOG_SUM ) { //lookup value if it is is in precalculated range
			logSum = logSumArray[n]; 
		} else { //calculate value otherwise
			logSum = logSumArray[MAX_PRECALCULATED_LOG_SUM]; //start with the max value we have precalculated
			for(int i = MAX_PRECALCULATED_LOG_SUM + 1; i <= n; i++ ) {
				logSum += Math.log(i);
			}
		}
		return logSum;
	}
	
	/**
	 * returns the binomial coefficient given n and k. (n choose k)
	 * formula: (n choose k) = n!/(k! * (n-k)!)
	 * @return (n choose k)
	 */
	public static long binomialCoefficient(int n, int k) {
		double logCoefficient = logBinomialCoefficient(n, k);
		if( logCoefficient > Math.log( Long.MAX_VALUE ) ) {
			throw new MathException("Binomial Coefficient value exceeds return type long. Log value is " + logCoefficient );
		}
		return Math.round( Math.exp( logCoefficient ) );
	}

	/**
	 * returns the log of binomial coefficient given n and k. (log(n choose k))
	 * formula: log(n choose k) = log(n!/(k! * (n-k)!)) = log(n!) - log(k!) - log((n-k)!)
	 * @return log(n choose k)
	 */
	public static double logBinomialCoefficient(int n, int k) {
		if( n < k ) {
			throw new MathException("n must be greater than k to calculate (n choose k): n=" + n + ", k=" + k);
		}
		return logSum(n) - logSum(k) - logSum(n - k);	
	}
	
	/**
	 * Let k = sampleSucess, n = sampleSize, m = populationSuccess, N = populationSize.
	 * Formula: ( (m choose k) * ((N-m) choose (n-k)) ) / (N choose n)
	 * @param k	sampeSuccess
	 * @param n	sampleSize
	 * @param m	populationSuccess
	 * @param N	populationSize
	 * @return pValue	probability of getting exactly k successes given n m N
	 */
	public static double hypergeometricProbability(int sampleSuccess, int sampleSize, int populationSuccess, int populationSize) {
		double mCk = logBinomialCoefficient(populationSuccess, sampleSuccess); //m choose k
		double NmCnk = logBinomialCoefficient(populationSize - populationSuccess, sampleSize - sampleSuccess); //(N-m) choose (n-k)
		double NCn = logBinomialCoefficient(populationSize, sampleSize);
		return Math.exp(mCk - NCn + NmCnk);
	}
	
	/**
	 * returns cumulative probability of getting k or less successes in the given hypergeometric distribution.
	 * @param k	sampleSuccess
	 * @param n	sampleSize
	 * @param m	populationSuccess
	 * @param N	populationSize
	 * @return pValue	probability of getting k or less successes given n m N
	 */
	public static double cumulativeHypergeometricProbability(int sampleSuccess, int sampleSize, int populationSuccess, int populationSize) {
		double cumulativeP = 0.0d;
		for(int i = 0; i <= sampleSuccess; i++ ) {
			double p = hypergeometricProbability(i, sampleSize, populationSuccess, populationSize);
			cumulativeP += p;
		}
		return cumulativeP;
	}
	
	/**
	 * returns cumulative probability of getting k or _MORE_ successes in the given hypergeometric distribution.
	 * Normally you would calculate 1.0 - P(x<k) because it yields faster result if k is closer to 0 than to 
	 * sample size. However, this method offers at least 10 significant digits even when p is extremely small.
	 * 
	 * @param sampleSuccess
	 * @param sampleSize
	 * @param populationSuccess
	 * @param populationSize
	 * @return pValue	probability of getting k or _more_ successes given n m N
	 */
	public static double cumulativeHypergeometricProbabilityFromRight(int sampleSuccess, int sampleSize, int populationSuccess, int populationSize) {
		double cumulativeP = 0.0d;
		for(int i = sampleSize; i >= sampleSuccess; i-- ) {
			if( populationSuccess >= i ) { //if pop success is < i, probability is 0, no need to add
				double p = hypergeometricProbability(i, sampleSize, populationSuccess, populationSize);
				cumulativeP += p;
			}
		}
		return cumulativeP;
	}
	
	/**
	 * Let k = sampleSucess, n = sampleSize, m = populationSuccess, N = populationSize. This function returns
	 * the probability of observing k or more successes by chance given the hypergeometric dist with n, m, N.
	 * @param sampleSuccess
	 * @param sampleSize
	 * @param populationSuccess
	 * @param populationSize
	 * @return pValue	probability of observing k or more successes by chance.
	 */
	public static double richness(int sampleSuccess, int sampleSize, int populationSuccess, int populationSize) {
		double pValue = 1.0d;
		if( sampleSuccess != 0 ) {
			pValue = cumulativeHypergeometricProbabilityFromRight(sampleSuccess, sampleSize, populationSuccess, populationSize);
		}
		return pValue;
	}
	
	/**
	 * 	Calculate concordance for correct and contra counts.
	 * @param correctCount
	 * @param contraCount
	 * @return pValue
	 */
	public static double concordance(int correctCount, int contraCount) {
        double PROBABILITY_CORRECT = 0.5; //direction is either correct or wrong
        double pValue = 1.0d;
        
        if( correctCount < 0 || contraCount < 0 ) {
        	throw new MathException("correct count and contra count must be greater than 0: " + correctCount + ", " + contraCount);
        }
        try {
        	pValue = Beta.regularizedBeta(PROBABILITY_CORRECT, correctCount, contraCount + 1.0);
        } catch(Exception e) {
        	throw new MathException("Error calculating bionomial distribution: " + correctCount + ", " + contraCount, e);
        }
        return pValue; 
	}
}
