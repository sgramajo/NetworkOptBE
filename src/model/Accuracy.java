package model;

import java.util.ArrayList;
import java.util.List;

public class Accuracy {
	List<List<Double>> missingData = new ArrayList<List<Double>>();
	List<List<Double>> globalResults = new ArrayList<List<Double>>();
	List<List<Double>> collaborativeResults = new ArrayList<List<Double>>();
	List<List<Double>> actualData = new ArrayList<List<Double>>();

	public Accuracy(List<List<Double>> actualData, List<List<Double>> missingData, List<List<Double>> globalResults, 
			List<List<Double>> collaborativeResults) {
		super();
		this.missingData = missingData;
		this.globalResults = globalResults;
		this.collaborativeResults = collaborativeResults; 
		this.actualData = actualData;
	}
	public List<List<Double>> getMissingData() {
		return missingData;
	}
	public void setMissingData(List<List<Double>> missingData) {
		this.missingData = missingData;
	}
	public List<List<Double>> getActualData() {
		return actualData;
	}
	public void setActualData(List<List<Double>> actualData) {
		this.actualData = actualData;
	}
	public List<List<Double>> getGlobalResults() {
		return globalResults;
	}
	public void setGlobalResults(List<List<Double>> globalResults) {
		this.globalResults = globalResults;
	}
	public List<List<Double>> getCollaborativeResults() {
		return collaborativeResults;
	}
	public void setCollaborativeResults(List<List<Double>> collaborativeResults) {
		this.collaborativeResults = collaborativeResults;
	}
}
