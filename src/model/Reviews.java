package model;

import java.util.List;

public class Reviews {
	private String reviewerID;
	private String asin; 
	private String reviewerName; 
	private List<Integer> helpful; 
	private String reviewText; 
	private String summary; 
	private String reviewTime;
	private double overall; 
	private long unixReviewTime;
	
	
	public Reviews(String reviewerID, String asin, String reviewerName, List<Integer> helpful, String reviewText,
			String summary, String reviewTime, double overall, long unixReviewTime) {
		this.reviewerID = reviewerID;
		this.asin = asin;
		this.reviewerName = reviewerName;
		this.helpful = helpful;
		this.reviewText = reviewText;
		this.summary = summary;
		this.reviewTime = reviewTime;
		this.overall = overall;
		this.unixReviewTime = unixReviewTime;
	}
	public double getOverall() {
		return overall;
	}
	public void setOverall(double overall) {
		this.overall = overall;
	}
	public String getReviewerID() {
		return reviewerID;
	}
	public void setReviewerID(String reviewerID) {
		this.reviewerID = reviewerID;
	}
	public String getAsin() {
		return asin;
	}
	public void setAsin(String asin) {
		this.asin = asin;
	}
	public String getReviewerName() {
		return reviewerName;
	}
	public void setReviewerName(String reviewerName) {
		this.reviewerName = reviewerName;
	}
	public List<Integer> getHelpful() {
		return helpful;
	}
	public void setHelpful(List<Integer> helpful) {
		this.helpful = helpful;
	}
	public String getReviewText() {
		return reviewText;
	}
	public void setReviewText(String reviewText) {
		this.reviewText = reviewText;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getReviewTime() {
		return reviewTime;
	}
	public void setReviewTime(String reviewTime) {
		this.reviewTime = reviewTime;
	}
	public long getUnixReviewTime() {
		return unixReviewTime;
	}
	public void setUnixReviewTime(long unixReviewTime) {
		this.unixReviewTime = unixReviewTime;
	} 
	@Override
	public String toString() {
		return "Reviews [reviewerID=" + reviewerID + ", asin=" + asin + ", reviewerName=" + reviewerName + ", helpful="
				+ helpful + ", reviewText=" + reviewText + ", summary=" + summary + ", reviewTime=" + reviewTime
				+ ", overall=" + overall + ", unixReviewTime=" + unixReviewTime + "]";
	}

}
