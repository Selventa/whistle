package com.selventa.whistle.score.model;

import java.util.HashSet;
import java.util.Set;

import org.openbel.framework.api.Kam.KamNode;

import com.selventa.whistle.data.enums.DirectionType;

/**
 * 
 * @author julianray
 *
 */
public class Hypothesis extends Downstream {

	private int depth;
	private Set<Downstream> downstreamNodeSet = new HashSet<Downstream>();
	
	public Hypothesis (final KamNode kamNode, final DirectionType directionType, final int depth) {
		super(kamNode, directionType);
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
	public Set<Downstream> getDownstreams() {
		return downstreamNodeSet;
	}
}
