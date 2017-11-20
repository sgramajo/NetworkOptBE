package util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import model.HomeKitchen;
import model.ItemInfo;
import model.Reviews;

/* This class deals with the collaborative filtering
 * in the data sets*/
public class Collaborative {
	
	public static List<String> users = new ArrayList<String>(); 
	public static Map<String, List<Reviews>>reviewMap = new HashMap<String, List<Reviews>>(); 
	public static Map<String, HomeKitchen> map = new HashMap<String, HomeKitchen>(); 
	
	public Collaborative(){
		users.clear();
		map.clear();
		readInput(); 
	}

	//read the Json files and stores them into models
	public static void readInput() {
		GsonBuilder gsonBuild = new GsonBuilder().serializeNulls().setPrettyPrinting();
		Gson gson = gsonBuild.create(); 
		List<HomeKitchen> data = new ArrayList<HomeKitchen>(); 
		List<Reviews> reviewData = new ArrayList<Reviews>();
		String basepath = new File("").getAbsolutePath();

		String url1 = basepath + "/src/resources/HomeKitchen.json";
		String url2 = basepath + "/src/resources/partial.json";
		//this can be used to map the items to the reviews
		try(Reader reader = new FileReader(url1)){
			//Json -> Object
			Type listType = new TypeToken<ArrayList<HomeKitchen>>(){}.getType(); 
			data = gson.fromJson(reader, listType); 
			//put it into a map
			for(HomeKitchen i: data) map.put(i.getAsin(), i);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//read the reviews/ratings for the items
		try(Reader reader = new FileReader(url2)){
			//Json -> Object
			Type listType = new TypeToken<ArrayList<Reviews>>(){}.getType();
			reviewData = gson.fromJson(reader, listType); 
			//see which ones are in the map
			for(Reviews i: reviewData){
				if(map.containsKey(i.getAsin())){
					if(reviewMap.containsKey(i.getAsin())){
						List<Reviews> reviews = reviewMap.get(i.getAsin());
						reviews.add(i); 
						reviewMap.put(i.getAsin(), reviews); 
						users.add(i.getReviewerName()); 
					}else{
						List<Reviews> reviews = new ArrayList<Reviews>(); 
						reviews.add(i); 
						reviewMap.put(i.getAsin(), reviews); 
						users.add(i.getReviewerName()); 
					}
				}
			}//end of for loop
			//printMatrix(reviewMap); 
		}catch(IOException e){
			e.printStackTrace();
		} 
	}

	//Retrieve one item's information 
	public static ItemInfo retrieveOneItem(String itemID){
		List<Reviews> list = reviewMap.get(itemID);
		HomeKitchen item = map.get(itemID); 
		ItemInfo returnItem = new ItemInfo(item, list);
		return returnItem; 
	}
	
	//this contains all of the items information that has x reviews
	public static List<HomeKitchen> createArray(){
		List<HomeKitchen> simpleArrayOfLists = new ArrayList<HomeKitchen>();
		reviewMap.forEach((x,y) -> {
			simpleArrayOfLists.add(map.get(x)); 
		});
		return simpleArrayOfLists; 
	}
	//print out matrix
	private static void printMatrix(Map<String, List<Reviews>> map) {
		//initializing variables that is useful to set up the matrix
		Set<String> names = new HashSet<String>(); 
		//The line below is used for debugging
		map.forEach((x,y) -> {
			System.out.println(x + " has " + y.size() + " reviews"); 
			//y.forEach(x -> ); 
		});

	}
	
	private static void printJson(List<HomeKitchen> data){
		data.forEach(x -> System.out.println(x.toString()));
	}

	// Class to implement collaborative filter
	public static void calculateRatings(){

		//printMatrix(reviewMap);

		// Row and column variables
		int itemCount = reviewMap.size();
		int userCount;

		// Create arraylist matrix of items and their reviews by users
		HashMap<String, HashMap<String, Double>> ratings = new HashMap<>(itemCount);

		// Retrieve list of users for all items
		ArrayList<String> userList = new ArrayList<>();
		reviewMap.forEach((itemID, reviewLst) -> {
			reviewLst.forEach(review -> {
				if(!userList.contains(review.getReviewerID())){
					userList.add(review.getReviewerID());
				}
			});
		});
		userCount = userList.size();

		// Initialilze ratings matrix, aka set all reviews to 0.0
		// Then retrive ratigs for all reviews for each item
		reviewMap.forEach((itemID, reviewLst) -> {
			ratings.put(itemID, new HashMap<String, Double>(){{
				userList.forEach(user -> put(user, 0.0));
			}});
			reviewLst.forEach(review -> {
				ratings.get(itemID).put(review.getReviewerID(), review.getOverall());
			});
		});

		// Debugger print
//		ratings.forEach((itemID, reviewHash) -> {
//			reviewHash.forEach((userID, rating) -> {
//				if(rating > 0.0)
//					System.out.print(rating + " ");
//			});
//			System.out.println();
//		});

		// Begin calculations to estimate missing ratings

		// Step 1: normalize ratings
		HashMap<String, HashMap<String, Double>> normalizedRatings = normalizeRatings(ratings, itemCount, userCount, userList);

	}

	// Normalize ratings by subtracting average item rating for each item (row mean)
	// Makes 0 the average rating for an item
	private static HashMap<String, HashMap<String, Double>> normalizeRatings(HashMap<String, HashMap<String, Double>> ratings, int itemCount, int userCount, ArrayList<String> userList){

		HashMap<String, HashMap<String, Double>> normalizedRatings = new HashMap<>(itemCount);
		normalizedRatings.forEach((itemID, reviewLst) ->{
			ratings.put(itemID, new HashMap<String, Double>(){{
				userList.forEach(user -> put(user, 0.0));
			}});
		});

		// Calculate row mean for all items
		HashMap<String, Double> itemRatingMean = new HashMap<>();

		double ratingMean = 0.0;

		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			ratingMean = 0.0;
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){
				ratingMean += userRatingEntry.getValue();
			}

			ratingMean /= reviewMap.get(itemRatingEntry.getKey()).size();
			itemRatingMean.put(itemRatingEntry.getKey(), ratingMean);
		}

		// Subtract row mean from ratinfs to normalize
		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){
				if(userRatingEntry.getValue() != 0.0)
					normalizedRatings.get(itemRatingEntry.getKey()).put(userRatingEntry.getKey(), userRatingEntry.getValue() - itemRatingMean.get(itemRatingEntry.getKey()));
			}
		}

		return normalizedRatings;
	}

}
