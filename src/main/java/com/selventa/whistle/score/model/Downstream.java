package com.selventa.whistle.score.model;

import org.openbel.framework.api.Kam.KamNode;

import com.selventa.whistle.data.enums.DirectionType;

/**
 * 
 * @author julianray
 *
 */
public class Downstream {
	
	private KamNode kamNode;
	private DirectionType directionType;

	public Downstream(final KamNode kamNode, final DirectionType directionType) {
		this.kamNode = kamNode;
		this.directionType = directionType;
	}

	public KamNode getKamNode() {
		return kamNode;
	}

	public DirectionType getDirectionType() {
		return directionType;
	}
	
	public void setDirectionType(DirectionType directionType) {
		this.directionType = directionType;
	}
}
