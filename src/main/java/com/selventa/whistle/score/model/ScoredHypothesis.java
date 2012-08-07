package com.selventa.whistle.score.model;

import com.selventa.whistle.score.service.Scorer.Prediction;

/**
 * 
 * @author julianray
 *
 */
public class ScoredHypothesis extends Hypothesis {
    private Prediction prediction;
	private Double richness;
	private Double concordance;
	private Integer possible;
	private Integer observed;

	public ScoredHypothesis(final Hypothesis hypothesis) {
		super(hypothesis.getKamNode(), hypothesis.getDirectionType(), hypothesis.getDepth());
	}

	public Integer getNumberCorrect() {
		return prediction == null ? 0 : prediction.getNumberCorrect();
	}

	public Integer getNumberContra() {
        return prediction == null ? 0 : prediction.getNumberContra();
    }
	
	public Integer getNumberAmbiguous() {
        return prediction == null ? 0 : prediction.getNumberAmbiguous();
    }
	
	public Prediction getPrediction() {
	    return prediction;
	}
	
	public void setPrediction(Prediction prediction) {
	    this.prediction = prediction;
	}
	
	public Double getRichness() {
		return richness;
	}

	public void setRichness(Double richness) {
		this.richness = richness;
	}

	public Double getConcordance() {
		return concordance;
	}

	public void setConcordance(Double concordance) {
		this.concordance = concordance;
	}

	public Integer getPossible() {
		return possible;
	}

	public void setPossible(Integer possible) {
		this.possible = possible;
	}

	public Integer getObserved() {
		return observed;
	}

	public void setObserved(Integer observed) {
		this.observed = observed;
	}
}
