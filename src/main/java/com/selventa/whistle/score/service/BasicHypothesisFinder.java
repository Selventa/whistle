package com.selventa.whistle.score.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbel.framework.api.EdgeDirectionType;
import org.openbel.framework.api.EdgeFilter;
import org.openbel.framework.api.FunctionTypeFilterCriteria;
import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamEdge;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.NodeFilter;
import org.openbel.framework.api.RelationshipTypeFilterCriteria;
import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;

import com.selventa.whistle.data.enums.DirectionType;
import com.selventa.whistle.score.model.Downstream;
import com.selventa.whistle.score.model.Hypothesis;

/**
 *
 * @author julianray
 *
 */
public class BasicHypothesisFinder implements HypothesisFinder {


	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<Hypothesis> findAll(Kam kam, int maxDepth) {
		return findAll(kam, maxDepth, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<Hypothesis> findAll(Kam kam, int maxDepth, NodeFilter nodeFilter) {

    	List<Hypothesis> hypothesisList = new ArrayList<Hypothesis>();
		EdgeFilter edgeFilter = getEdgeFilter(kam);

		Map<KamNode, DirectionType> downstreams = new HashMap<KamNode, DirectionType>();
    	for (KamNode sourceNode : kam.getNodes(nodeFilter)) {
    		// Search for different depths. The search for the sourceNode terminates
    		// when a hypothesis is found at the nth depth
    		for (int currentDepth = 1; currentDepth < maxDepth; currentDepth++) {
	    		// Clear any existing data
	    		downstreams.clear();
	    		// recursively find all downstream RNA expression nodes that are connected within currentDepth steps
	    		find(kam, sourceNode, 1, downstreams, edgeFilter, currentDepth);
	    		// Check for a valid hypothesis
	    		if (downstreams.size() > 3) {
					// Create a new hypothesis from this source node. All hyps are assumed to be upregulated by default
	    			// Note: for convention we still use Depth = 2 for directly connected hyps
					Hypothesis hypothesis = new Hypothesis(sourceNode, DirectionType.UP, currentDepth + 1);
					// Add the downstreams for the hypothesis
					for (KamNode kamNode : downstreams.keySet()) {
						// Add this downstream to the hypothesis
						hypothesis.getDownstreams().add(new Downstream(kamNode, downstreams.get(kamNode)));
					}
					hypothesisList.add(hypothesis);
					// No need to evaluate for any other levels for this hyp.
					break;
	    		}
    		}
    	}

    	return hypothesisList;
    }
	/**
	 * Recursive Depth-First Search to evaluate a Kam node
	 *
	 * @param kam
	 * @param sourceNode
	 * @param currentDepth
	 * @param downstreams
	 * @param edgeFilter
	 * @param maxDepth
	 */
	protected void find(Kam kam, KamNode sourceNode, int currentDepth, Map<KamNode, DirectionType> downstreams, EdgeFilter edgeFilter, int maxDepth) {

		for (KamEdge kamEdge : kam.getAdjacentEdges(sourceNode, EdgeDirectionType.FORWARD, edgeFilter)) {

			KamNode targetNode = kamEdge.getTargetNode();
			DirectionType direction = getDirectionType(kamEdge.getRelationshipType());

			// If the targetNode is a RNA abundance node, keep it
			if (targetNode.getFunctionType().equals(FunctionEnum.RNA_ABUNDANCE)) {
				// See if we have this node already
				if (downstreams.containsKey(targetNode)) {
					// Check the direction
					DirectionType influenceDirection;
					if (currentDepth == 1) {
						influenceDirection = downstreams.get(targetNode).evaluate(getDirectionType(kamEdge.getRelationshipType()));
					} else {
						influenceDirection = downstreams.get(targetNode).compound(getDirectionType(kamEdge.getRelationshipType()));
					}
					// Update the map
					downstreams.put(targetNode, influenceDirection);
				} else {
					// add this new downstream
					downstreams.put(targetNode, direction);
				}
			} else {
				// If it is not a RNA node, keep searching
				if (currentDepth < maxDepth) {
					find(kam, targetNode, currentDepth + 1, downstreams, edgeFilter, maxDepth);
				}
			}
		}
	}

	/**
	 *
	 * @param relationshipType
	 * @return
	 */
	protected DirectionType getDirectionType(RelationshipType relationshipType) {
		if (relationshipType.isIncreasing()) {
			return DirectionType.UP;
		} else if (relationshipType.isDecreasing()) {
			return DirectionType.DOWN;
		} else {
			return DirectionType.AMBIG;
		}
	}
	/**
	 *
	 * @return
	 */
	protected EdgeFilter getEdgeFilter(Kam kam) {

		RelationshipTypeFilterCriteria filter = new RelationshipTypeFilterCriteria();
		filter.setInclude(true);
		filter.getValues().add(RelationshipType.DECREASES);
		filter.getValues().add(RelationshipType.DIRECTLY_DECREASES);
		filter.getValues().add(RelationshipType.INCREASES);
		filter.getValues().add(RelationshipType.DIRECTLY_INCREASES);
//		filter.getValues().add(RelationshipType.NEGATIVE_CORRELATION);
//		filter.getValues().add(RelationshipType.POSITIVE_CORRELATION);
		filter.getValues().add(RelationshipType.RATE_LIMITING_STEP_OF);

		EdgeFilter edgeFilter = kam.createEdgeFilter();
		edgeFilter.add(filter);

		return edgeFilter;
	}
	/**
	 *
	 * @return
	 */
    protected NodeFilter getNodeFilter(Kam kam) {
		NodeFilter nodeFilter = kam.createNodeFilter();

		// We only want to consider RNA downstreams at this time
		nodeFilter.add(new FunctionTypeFilterCriteria().add(FunctionEnum.RNA_ABUNDANCE));
		return nodeFilter;
    }
}
