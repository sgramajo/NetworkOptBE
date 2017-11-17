package model;

import java.util.List;

public class Related {
	private List<String> also_bought;
	private List<String> also_viewed; 
	private List<String> bought_together;
	private List<String> buy_after_viewing; 
	public Related(List<String> also_bought, List<String> also_viewed, List<String> bought_together, List<String> buy_after_viewing) {
		this.also_bought = also_bought;
		this.also_viewed = also_viewed;
		this.bought_together = bought_together;
		this.buy_after_viewing = buy_after_viewing; 
	}
	
	public List<String> getAlso_bought() {
		return also_bought;
	}
	public void setAlso_bought(List<String> also_bought) {
		this.also_bought = also_bought;
	}
	public List<String> getAlso_viewed() {
		return also_viewed;
	}
	public void setAlso_viewed(List<String> also_viewed) {
		this.also_viewed = also_viewed;
	}
	public List<String> getBought_together() {
		return bought_together;
	}
	public void setBought_together(List<String> bought_together) {
		this.bought_together = bought_together;
	}
	
	public List<String> getBuy_after_viewing() {
		return buy_after_viewing;
	}

	public void setBuy_after_viewing(List<String> buy_after_viewing) {
		this.buy_after_viewing = buy_after_viewing;
	}
	@Override
	public String toString() {
		return "[also_bought=" + also_bought + ", also_viewed=" + also_viewed + ", bought_together="
				+ bought_together + ", buy_after_viewing=" + buy_after_viewing + "]";
	}
}
