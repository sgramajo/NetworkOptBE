package model;

import java.util.List;

public class ItemInfo {
	public HomeKitchen info; 
	public List<Reviews> reviews; 

	public ItemInfo(HomeKitchen info, List<Reviews> reviews){
		this.info = info; 
		this.reviews = reviews;
	}
	public HomeKitchen getInfo() {
		return info;
	}
	public List<Reviews> getReviews() {
		return reviews;
	}
	public void setInfo(HomeKitchen info) {
		this.info = info;
	}
	public void setReviews(List<Reviews> reviews) {
		this.reviews = reviews;
	}
}
