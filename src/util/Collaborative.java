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
	public static HashMap<String, HashMap<String, Double>> calculateRatings(){

		// Row and column variables
		int itemCount = reviewMap.size();

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

		// Begin calculations to estimate missing ratings

		// Step 1: normalize ratings
		HashMap<String, HashMap<String, Double>> normalizedRatings = normalizeRatings(ratings, itemCount, userList);

		// Step 2: calculate centered cosine similarity
		HashMap<String, HashMap<String, Double>> similarityMatrix = calculateSimilarity(normalizedRatings, itemCount, userList);

		// Step 3: estimate missing ratings
		item_itemCollaborativeFilter(ratings, similarityMatrix);

		return ratings;

	}

	// Normalize ratings by subtracting average item rating for each item (row mean)
	// Makes 0 the average rating for an item
	private static HashMap<String, HashMap<String, Double>> normalizeRatings(HashMap<String, HashMap<String, Double>> ratings, int itemCount, ArrayList<String> userList){

		HashMap<String, HashMap<String, Double>> normalizedRatings = new HashMap<>(itemCount);
		ratings.forEach((itemID, reviewLst) ->{
			normalizedRatings.put(itemID, new HashMap<String, Double>(){{
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

	// Calculate the centered cosine similarity for all items
	private static HashMap<String, HashMap<String, Double>> calculateSimilarity(HashMap<String, HashMap<String, Double>> normalizedRatings, int itemCount, ArrayList<String> userLst){

		HashMap<String, HashMap<String, Double>> similarityMatrix = new HashMap<>(itemCount);

		// Initialize all similarities to 0.0
		normalizedRatings.forEach((itemID, reviewLst) ->{
			similarityMatrix.put(itemID, new HashMap<String, Double>(){{
				normalizedRatings.forEach((item, list) -> put(item, 0.0));
			}});
		});

		for(Map.Entry<String, HashMap<String, Double>> item1RatingEntry : normalizedRatings.entrySet()){
			for(Map.Entry<String, HashMap<String, Double>> item2RatingEntry : normalizedRatings.entrySet()){

				double similarity, dotProduct, item1Magnitude, item2Magnitude;
				dotProduct = item1Magnitude = item2Magnitude = 0.0;

				for(String user : userLst){
					// dot product is almost always 0 ...
					dotProduct += normalizedRatings.get(item1RatingEntry.getKey()).get(user) * normalizedRatings.get(item2RatingEntry.getKey()).get(user);
					item1Magnitude += normalizedRatings.get(item1RatingEntry.getKey()).get(user) * normalizedRatings.get(item1RatingEntry.getKey()).get(user);
					item2Magnitude += normalizedRatings.get(item2RatingEntry.getKey()).get(user) * normalizedRatings.get(item2RatingEntry.getKey()).get(user);
				}

				double denominator = Math.sqrt(item1Magnitude) * Math.sqrt(item2Magnitude);
				similarity = (denominator != 0 ? dotProduct / denominator : 0.0);
				similarityMatrix.get(item1RatingEntry.getKey()).put(item2RatingEntry.getKey(), similarity);

			}
		}

		return similarityMatrix;
	}

	// Estimate the missing ratings using an item-item collaborative filter
	private static void item_itemCollaborativeFilter(HashMap<String, HashMap<String, Double>> ratings, HashMap<String, HashMap<String, Double>> similarityMatrix){

		// Get item neighborhood for all items in descending order based on similarity
		HashMap<String, ArrayList<Item>> itemNeighborhood = new HashMap<>();
		for(Map.Entry<String, HashMap<String, Double>> itemSimilarityEntry : similarityMatrix.entrySet()){

			ArrayList<Item> neighborhood = new ArrayList<>();

			for(Map.Entry<String, Double> item2SimilarityEntry : itemSimilarityEntry.getValue().entrySet()){
				if(itemSimilarityEntry.getKey() != item2SimilarityEntry.getKey()){ // dont add item to its own neighborhood
					neighborhood.add(new Item(item2SimilarityEntry.getKey(), item2SimilarityEntry.getValue()));
				}
			}

			// Sort the item neighborhood
			Collections.sort(neighborhood, new CustomComparator());
			Collections.reverse(neighborhood);
			itemNeighborhood.put(itemSimilarityEntry.getKey(), neighborhood);
		}

		// summations of similarity
		double predictedRating, similarityByRating, similarity;

		// Traverse ratings matric and estimate missing ratings
		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){

				// If we have a missing rating
				if(userRatingEntry.getValue() == 0.0){

					// reset summations of similarity
					similarityByRating = similarity = 0.0;

					// Pick the top N most similar items to the current item that have been rated by the current user
					int n = 0;
					for(Item neighbor : itemNeighborhood.get(itemRatingEntry.getKey())){
						if(ratings.get(itemRatingEntry.getKey()).get(userRatingEntry.getKey()) != 0.0){
							similarityByRating += neighbor.similarity * ratings.get(itemRatingEntry.getKey()).get(userRatingEntry.getKey());
							similarity += neighbor.similarity;
							n++;
						}
						if(n == N) break;
					}

					predictedRating = (similarity != 0 ? similarityByRating / similarity : 0.0);
					ratings.get(itemRatingEntry.getKey()).put(userRatingEntry.getKey(), (predictedRating < 1.0 ? 1.0 : (predictedRating > 5.0 ? 5.0 : predictedRating)));
				}
			}
		}
	}
}

class Item{
	Double similarity;
	String item;

	public Item(String item, double similarity){
		this.item = item;
		this.similarity = similarity;
	}
}

class CustomComparator implements Comparator<Item>{

	@Override
	public int compare(Item o1, Item o2) {
		return o1.similarity.compareTo(o2.similarity);
	}
}
