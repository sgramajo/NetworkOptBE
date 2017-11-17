package model;

public class Rating {
	double rating; 
	String name;
	
	public Rating(double rating, String name) {
		this.rating = rating;
		this.name = name;
	}
	public double getRating() {
		return rating;
	}
	public void setRating(double rating) {
		this.rating = rating;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	} 
	
	
}
