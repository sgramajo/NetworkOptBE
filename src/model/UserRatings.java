package model;

public class UserRatings {
	public HomeKitchen itemInfo; 
	public Double rating;
	public UserRatings(HomeKitchen itemInfo, Double rating) {
		super();
		this.itemInfo = itemInfo;
		this.rating = rating;
	}
	public HomeKitchen getItemInfo() {
		return itemInfo;
	}
	public void setItemInfo(HomeKitchen itemInfo) {
		this.itemInfo = itemInfo;
	}
	public Double getRating() {
		return rating;
	}
	public void setRating(Double rating) {
		this.rating = rating;
	} 
	
}
