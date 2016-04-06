package utils;

/**
 * Primarily intended for computing standard deviation and variance in a
 * numerically sound manner.  Also does mean, min, and max.  Adapted from the
 * C++ class presented here:
 * 
 * 	http://www.johndcook.com/standard_deviation.html
 */
public class RunningStats {
	int m_n;
	double m_oldM, m_newM, m_oldS, m_newS;
	double min, max;

	public RunningStats() {
		clear();
	}

	public void clear() {
		m_n = 0;
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
	}

	public void push(double x) {
		m_n++;

		// See Knuth TAOCP vol 2, 3rd edition, page 232
		if (m_n == 1) {
			m_oldM = m_newM = x;
			m_oldS = 0.0;
		} else {
			m_newM = m_oldM + (x - m_oldM) / m_n;
			m_newS = m_oldS + (x - m_oldM) * (x - m_newM);

			// set up for next iteration
			m_oldM = m_newM;
			m_oldS = m_newS;
		}
		
		if (x < min) min = x;
		if (x > max) max = x;
	}

	public int getNumberOfValues() {
		return m_n;
	}

	public double getMean() {
		return (m_n > 0) ? m_newM : 0.0;
	}

	public double getVariance() {
		return ((m_n > 1) ? m_newS / (m_n - 1) : 0.0);
	}

	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
}
