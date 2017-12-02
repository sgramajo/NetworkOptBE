package model;

public class GlobalItemItem {
	public String globalScore; 
	public String collaborativeFiltering;
	
	public GlobalItemItem(String globalScore, String collaborativeFiltering) {
		super();
		this.globalScore = globalScore;
		this.collaborativeFiltering = collaborativeFiltering;
	}
	public String getGlobalScore() {
		return globalScore;
	}
	public void setGlobalScore(String globalScore) {
		this.globalScore = globalScore;
	}
	public String getCollaborativeFiltering() {
		return collaborativeFiltering;
	}
	public void setCollaborativeFiltering(String collaborativeFiltering) {
		this.collaborativeFiltering = collaborativeFiltering;
	} 
	
}
