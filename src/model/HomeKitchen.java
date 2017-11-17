package model;

import java.util.List;

public class HomeKitchen {
	private String asin; //id 
	private String description; 
	private String title; 
	private String imUrl; 
	private Object salesRank; 
	private List<String> categories;
	private double price; 
	private Related related; 
	private String brand; 
	
	public HomeKitchen(String asin, String description, String title, String imUrl, Object salesRank,
			List<String> categories, double price, Related related, String brand) {
		this.asin = asin;
		this.description = description;
		this.title = title;
		this.imUrl = imUrl;
		this.salesRank = salesRank;
		this.categories = categories;
		this.price = price; 
		this.related = related; 
		this.brand = brand; 
	}
	public String getAsin() {
		return asin;
	}
	public void setAsin(String asin) {
		this.asin = asin;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getImUrl() {
		return imUrl;
	}
	public void setImUrl(String imUrl) {
		this.imUrl = imUrl;
	}
	public Object getSalesRank() {
		return salesRank;
	}
	public void setSalesRank(Object salesRank) {
		this.salesRank = salesRank;
	}
	public List<String> getCategories() {
		return categories;
	}
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public Object getRelated() {
		return related;
	}
	public void setRelated(Related related) {
		this.related = related;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	@Override
	public String toString() {
		return "HomeKitchen [asin=" + asin + ", description=" + description + ", title=" + title + ", imUrl=" + imUrl
				+ ", salesRank=" + salesRank + ", categories=" + categories + ", price=" + price + ", related="
				+ related + ", brand=" + brand + "]";
	}
}
