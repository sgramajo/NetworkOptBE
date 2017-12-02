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

	// Item neighborhood size; the N most similar items we want to use in the collaborative filter
	public static int N = 10;

	// Global baseline variables
	public static double globalMeanItemRating = 0.0; // r_GI
	public static double globalMeanUserRating = 0.0; // r_GX
	public static HashMap<String, Double> meanItemRatings = new HashMap<>(); // r_i for all items
	public static HashMap<String, Double> meanUserRatings = new HashMap<>(); // r_x for all users

	// Accuracy test variables
	public static double collaborativeFilterRMSE = 0.0;
	public static double globalBaselineMSE = 0.0;
	public static HashMap<String, HashMap<String, Double>> actualRatings;
	public static HashMap<String, HashMap<String, Double>> missingRatings;
	public static HashMap<String, HashMap<String, Double>> collaborativeFilterRatings;
	public static HashMap<String, HashMap<String, Double>> globalBaselineRatings;

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

	// Get ratings matrix
	private static HashMap<String, HashMap<String, Double>> getRatings(ArrayList<String> userList){

		// Create arraylist matrix of items and their reviews by users
		HashMap<String, HashMap<String, Double>> ratings = new HashMap<>();

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

		return ratings;
	}

	// Get user list
	private static ArrayList<String> getUsers(){
		// Retrieve list of users for all items
		ArrayList<String> userList = new ArrayList<>();
		reviewMap.forEach((itemID, reviewLst) -> {
			reviewLst.forEach(review -> {
				if(!userList.contains(review.getReviewerID())){
					userList.add(review.getReviewerID());
				}
			});
		});

		return userList;
	}

	// Class to implement item-item collaborative filter method
	public static HashMap<String, HashMap<String, Double>> calculateRatingsWithItem_ItemFilter(){

		// Row and column variables
		int itemCount = reviewMap.size();

		// Create arraylist matrix of items and their reviews by users
		ArrayList<String> userList = getUsers();
		HashMap<String, HashMap<String, Double>> ratings = getRatings(userList);
		HashMap<String, HashMap<String, Double>> estimatedRatings = getRatings(userList);

		// Begin calculations to estimate missing ratings

		// Calculate general statistics of ratings: mean item ratings, mean user ratings, global mean item rating, and global mean user rating
		calculateMeanVariables(ratings, userList);

		// Step 1: normalize ratings
		HashMap<String, HashMap<String, Double>> normalizedRatings = normalizeRatings(ratings, itemCount, userList);

		// Step 2: calculate centered cosine similarity
		HashMap<String, HashMap<String, Double>> similarityMatrix = calculateSimilarity(normalizedRatings, itemCount, userList);

		// Step 3: estimate missing ratings
		item_itemCollaborativeFilter(ratings, estimatedRatings, similarityMatrix);

		return estimatedRatings;
	}

	// Class to implement global baseline method
	public static HashMap<String, HashMap<String, Double>> calculateRatingsWithGlobalBaseline(){

		// Create arraylist matrix of items and their reviews by users
		ArrayList<String> userList = getUsers();
		HashMap<String, HashMap<String, Double>> ratings = getRatings(userList);
		HashMap<String, HashMap<String, Double>> estimatedRatings = getRatings(userList);

		// Begin calculations to estimate missing ratings

		// Calculate general statistics of ratings: mean item ratings, mean user ratings, global mean item rating, and global mean user rating
		calculateMeanVariables(ratings, userList);

		// Estimate ratings using global baseline estimate
		globalBaselineEstimate(ratings, estimatedRatings);

		return estimatedRatings;
	}

	public static HashMap<String, HashMap<String, Double>> calculateRatingsWithGlobalBaseline(HashMap<String, HashMap<String, Double>> ratings, HashMap<String, HashMap<String, Double>> estimatedRatings, ArrayList<String> userList){

		// Begin calculations to estimate missing ratings

		// Calculate general statistics of ratings: mean item ratings, mean user ratings, global mean item rating, and global mean user rating
		calculateMeanVariables(ratings, userList);

		// Estimate ratings using global baseline estimate
		globalBaselineEstimate(ratings, estimatedRatings);

		return estimatedRatings;
	}

	public static HashMap<String, HashMap<String, Double>> calculateRatingsWithItem_ItemFilter(HashMap<String, HashMap<String, Double>> ratings, HashMap<String, HashMap<String, Double>> estimatedRatings, ArrayList<String> userList){

		int itemCount = ratings.size();

		// Begin calculations to estimate missing ratings

		// Calculate general statistics of ratings: mean item ratings, mean user ratings, global mean item rating, and global mean user rating
		calculateMeanVariables(ratings, userList);

		// Step 1: normalize ratings
		HashMap<String, HashMap<String, Double>> normalizedRatings = normalizeRatings(ratings, itemCount, userList);

		// Step 2: calculate centered cosine similarity
		HashMap<String, HashMap<String, Double>> similarityMatrix = calculateSimilarity(normalizedRatings, itemCount, userList);

		// Step 3: estimate missing ratings
		item_itemCollaborativeFilter(ratings, estimatedRatings, similarityMatrix);

		return estimatedRatings;
	}


	// Accuracy test for recommender system on a toy input based on reviews on different movies
	public static void testAccuracy(){

		actualRatings = new HashMap<String, HashMap<String, Double>>() {{
			put("Mulan", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 5.0);put("Melissa", 3.0);put("Ashton", 4.5);put("Jessica", 4.8);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 3.5); }});
			put("Moana", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.0);put("Melissa", 5.0);put("Ashton", 3.5);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Hercules", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 5.0);put("Jessica", 3.0);put("Pom", 5.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 3.5); }});
			put("Pocahontas", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.0);put("Melissa", 2.0);put("Ashton", 3.0);put("Jessica", 2.8);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});
			put("Zootopia", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 4.0);put("Jessica", 3.0);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 2.0); }});
			put("Meet the Robinsons", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.9);put("Jessica", 5.0);put("Pom", 3.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("The Jungle Book", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 5.0);put("Jessica", 3.5);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 4.5); }});
			put("Atlantis The Lost Empire", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 4.1);put("Jessica", 2.8);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 2.5); }});
			put("Alice Through the Looking Glass", new HashMap<String, Double>() {{ put("Rebeca", 3.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 2.4);put("Jessica", 2.3);put("Pom", 1.0);put("Minoska", 1.0);put("Luis", 3.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Chicken Little", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 3.7);put("Jessica", 2.8);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 2.0); }});
			put("Holes", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.3);put("Jessica", 1.5);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 3.2);put("Ezequiel", 3.5); }});
			put("Stitch the Movie", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 2.5);put("Ashton", 4.4);put("Jessica", 4.5);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Tarzan", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 4.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 2.5); }});
			put("The Fox and The Hound", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.0);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.0);put("Pom", 3.5);put("Minoska", 3.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("The Lion King", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 4.5);put("Ezequiel", 4.0); }});
			put("Frozen", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 4.5);put("Melissa", 5.0);put("Ashton", 2.8);put("Jessica", 1.4);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 2.0);put("Martha", 4.5);put("Ezequiel", 2.5); }});
			put("Beauty and the Beast", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 2.9);put("Jessica", 5.0);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Snow White and the Seven Dwarves", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.5);put("Jessica", 2.4);put("Pom", 4.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.0);put("Ezequiel", 3.0); }});
			put("Tangled", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 3.5);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Bambi", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 2.7);put("Jessica", 3.4);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Pinocchio", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 2.6);put("Jessica", 2.7);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 3.5); }});
			put("Toy Story", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.5);put("Melissa", 4.0);put("Ashton", 4.2);put("Jessica", 4.2);put("Pom", 5.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 4.0); }});
			put("Sleeping Beauty", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.0);put("Jessica", 2.6);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 3.5); }});
			put("Dumbo", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 1.0);put("Melissa", 4.0);put("Ashton", 1.0);put("Jessica", 3.3);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});

		}};
		ArrayList<String> userList = new ArrayList<>(Arrays.asList("Rebeca", "Stacy", "Melissa", "Ashton", "Jessica", "Pom", "Minoska", "Luis", "Martha", "Ezequiel"));

		// Ratings with a missing test data set of size 40
		int testDataSize = 40;
		missingRatings = new HashMap<String, HashMap<String, Double>>() {{
			put("Mulan", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 4.8);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 3.5); }});
			put("Moana", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Hercules", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 5.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 3.5); }});
			put("Pocahontas", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.8);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});
			put("Zootopia", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 2.0); }});
			put("Meet the Robinsons", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 5.0);put("Pom", 3.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("The Jungle Book", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.5);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 4.5); }});
			put("Atlantis The Lost Empire", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.8);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 2.5); }});
			put("Alice Through the Looking Glass", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.3);put("Pom", 1.0);put("Minoska", 1.0);put("Luis", 3.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Chicken Little", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.8);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 2.0); }});
			put("Holes", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.3);put("Jessica", 1.5);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 3.2);put("Ezequiel", 3.5); }});
			put("Stitch the Movie", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 2.5);put("Ashton", 4.4);put("Jessica", 4.5);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Tarzan", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 4.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 2.5); }});
			put("The Fox and The Hound", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.0);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.0);put("Pom", 3.5);put("Minoska", 3.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("The Lion King", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 4.5);put("Ezequiel", 4.0); }});
			put("Frozen", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 4.5);put("Melissa", 5.0);put("Ashton", 2.8);put("Jessica", 1.4);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 2.0);put("Martha", 4.5);put("Ezequiel", 2.5); }});
			put("Beauty and the Beast", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 2.9);put("Jessica", 5.0);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Snow White and the Seven Dwarves", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.5);put("Jessica", 2.4);put("Pom", 4.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.0);put("Ezequiel", 3.0); }});
			put("Tangled", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 3.5);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Bambi", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 2.7);put("Jessica", 3.4);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Pinocchio", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 2.6);put("Jessica", 2.7);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 3.5); }});
			put("Toy Story", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.5);put("Melissa", 4.0);put("Ashton", 4.2);put("Jessica", 4.2);put("Pom", 5.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 4.0); }});
			put("Sleeping Beauty", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.0);put("Jessica", 2.6);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 3.5); }});
			put("Dumbo", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 1.0);put("Melissa", 4.0);put("Ashton", 1.0);put("Jessica", 3.3);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});

		}};
		HashMap<String, HashMap<String, Double>> estimatedRatingsCF = new HashMap<String, HashMap<String, Double>>() {{
			put("Mulan", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 4.8);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 3.5); }});
			put("Moana", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Hercules", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 5.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 3.5); }});
			put("Pocahontas", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.8);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});
			put("Zootopia", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 2.0); }});
			put("Meet the Robinsons", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.9);put("Jessica", 5.0);put("Pom", 3.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("The Jungle Book", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 5.0);put("Jessica", 3.5);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 4.5); }});
			put("Atlantis The Lost Empire", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 4.1);put("Jessica", 2.8);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 2.5); }});
			put("Alice Through the Looking Glass", new HashMap<String, Double>() {{ put("Rebeca", 3.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 2.4);put("Jessica", 2.3);put("Pom", 1.0);put("Minoska", 1.0);put("Luis", 3.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Chicken Little", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 3.7);put("Jessica", 2.8);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 2.0); }});
			put("Holes", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.3);put("Jessica", 1.5);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 3.2);put("Ezequiel", 3.5); }});
			put("Stitch the Movie", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 2.5);put("Ashton", 4.4);put("Jessica", 4.5);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Tarzan", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 4.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 2.5); }});
			put("The Fox and The Hound", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.0);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.0);put("Pom", 3.5);put("Minoska", 3.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("The Lion King", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 4.5);put("Ezequiel", 4.0); }});
			put("Frozen", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 4.5);put("Melissa", 5.0);put("Ashton", 2.8);put("Jessica", 1.4);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 2.0);put("Martha", 4.5);put("Ezequiel", 2.5); }});
			put("Beauty and the Beast", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 2.9);put("Jessica", 5.0);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Snow White and the Seven Dwarves", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.5);put("Jessica", 2.4);put("Pom", 4.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.0);put("Ezequiel", 3.0); }});
			put("Tangled", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 3.5);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Bambi", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 2.7);put("Jessica", 3.4);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Pinocchio", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 2.6);put("Jessica", 2.7);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 3.5); }});
			put("Toy Story", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.5);put("Melissa", 4.0);put("Ashton", 4.2);put("Jessica", 4.2);put("Pom", 5.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 4.0); }});
			put("Sleeping Beauty", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.0);put("Jessica", 2.6);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 3.5); }});
			put("Dumbo", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 1.0);put("Melissa", 4.0);put("Ashton", 1.0);put("Jessica", 3.3);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});

		}};
		HashMap<String, HashMap<String, Double>> estimatedRatingsGB = new HashMap<String, HashMap<String, Double>>() {{
			put("Mulan", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 4.8);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 3.5); }});
			put("Moana", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Hercules", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 5.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 3.5); }});
			put("Pocahontas", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 2.8);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});
			put("Zootopia", new HashMap<String, Double>() {{ put("Rebeca", 0.0);put("Stacy", 0.0);put("Melissa", 0.0);put("Ashton", 0.0);put("Jessica", 3.0);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 2.0); }});
			put("Meet the Robinsons", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.9);put("Jessica", 5.0);put("Pom", 3.0);put("Minoska", 5.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("The Jungle Book", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 5.0);put("Jessica", 3.5);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 4.5); }});
			put("Atlantis The Lost Empire", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 4.1);put("Jessica", 2.8);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 4.0);put("Ezequiel", 2.5); }});
			put("Alice Through the Looking Glass", new HashMap<String, Double>() {{ put("Rebeca", 3.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 2.4);put("Jessica", 2.3);put("Pom", 1.0);put("Minoska", 1.0);put("Luis", 3.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Chicken Little", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 3.7);put("Jessica", 2.8);put("Pom", 2.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 2.0); }});
			put("Holes", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 3.0);put("Melissa", 5.0);put("Ashton", 3.3);put("Jessica", 1.5);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 3.2);put("Ezequiel", 3.5); }});
			put("Stitch the Movie", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 2.5);put("Ashton", 4.4);put("Jessica", 4.5);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 2.0);put("Ezequiel", 3.0); }});
			put("Tarzan", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 4.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 2.5); }});
			put("The Fox and The Hound", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.0);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.0);put("Pom", 3.5);put("Minoska", 3.0);put("Luis", 5.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("The Lion King", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 4.5);put("Ashton", 4.0);put("Jessica", 3.7);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 4.5);put("Ezequiel", 4.0); }});
			put("Frozen", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 4.5);put("Melissa", 5.0);put("Ashton", 2.8);put("Jessica", 1.4);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 2.0);put("Martha", 4.5);put("Ezequiel", 2.5); }});
			put("Beauty and the Beast", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 5.0);put("Melissa", 5.0);put("Ashton", 2.9);put("Jessica", 5.0);put("Pom", 5.0);put("Minoska", 5.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Snow White and the Seven Dwarves", new HashMap<String, Double>() {{ put("Rebeca", 2.0);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.5);put("Jessica", 2.4);put("Pom", 4.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 4.0);put("Ezequiel", 3.0); }});
			put("Tangled", new HashMap<String, Double>() {{ put("Rebeca", 4.5);put("Stacy", 3.5);put("Melissa", 2.0);put("Ashton", 2.0);put("Jessica", 3.2);put("Pom", 4.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 4.5);put("Ezequiel", 3.5); }});
			put("Bambi", new HashMap<String, Double>() {{ put("Rebeca", 4.0);put("Stacy", 4.5);put("Melissa", 3.0);put("Ashton", 2.7);put("Jessica", 3.4);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 3.0); }});
			put("Pinocchio", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 2.0);put("Melissa", 1.0);put("Ashton", 2.6);put("Jessica", 2.7);put("Pom", 3.0);put("Minoska", 4.0);put("Luis", 3.0);put("Martha", 3.0);put("Ezequiel", 3.5); }});
			put("Toy Story", new HashMap<String, Double>() {{ put("Rebeca", 5.0);put("Stacy", 4.5);put("Melissa", 4.0);put("Ashton", 4.2);put("Jessica", 4.2);put("Pom", 5.0);put("Minoska", 3.0);put("Luis", 4.0);put("Martha", 4.8);put("Ezequiel", 4.0); }});
			put("Sleeping Beauty", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 3.5);put("Melissa", 3.0);put("Ashton", 3.0);put("Jessica", 2.6);put("Pom", 3.0);put("Minoska", 3.0);put("Luis", 3.0);put("Martha", 3.5);put("Ezequiel", 3.5); }});
			put("Dumbo", new HashMap<String, Double>() {{ put("Rebeca", 3.5);put("Stacy", 1.0);put("Melissa", 4.0);put("Ashton", 1.0);put("Jessica", 3.3);put("Pom", 2.0);put("Minoska", 4.0);put("Luis", 4.0);put("Martha", 3.0);put("Ezequiel", 4.0); }});

		}};
		ArrayList<String> missingUsers = new ArrayList<>(Arrays.asList("Rebeca", "Stacy", "Melissa", "Ashton"));
		ArrayList<String> missingItems = new ArrayList<>(Arrays.asList("Mulan", "Moana", "Hercules", "Pocahontas", "Zootopia", "Meet the Robinsons", "The Jungle Book", "Atlantis The Lost Empire", "Alice Through the Looking Glass", "Chicken Little"));

		// Estimate the ratings with the two implemented methods
		collaborativeFilterRatings = calculateRatingsWithItem_ItemFilter(missingRatings, estimatedRatingsCF, userList);
		globalBaselineRatings = calculateRatingsWithGlobalBaseline(missingRatings, estimatedRatingsGB, userList);

		// Use root mean square error (RMSE) to evaluate the predictions
		double cfMeanSquareSummation = 0.0;
		double gbMeanSquareSummation = 0.0;

		for(String item : missingItems){
			for(String user : missingUsers){
				cfMeanSquareSummation += (collaborativeFilterRatings.get(item).get(user) - actualRatings.get(item).get(user)) * (collaborativeFilterRatings.get(item).get(user) - actualRatings.get(item).get(user));
				gbMeanSquareSummation += (globalBaselineRatings.get(item).get(user) - actualRatings.get(item).get(user)) * (globalBaselineRatings.get(item).get(user) - actualRatings.get(item).get(user));
			}
		}

		collaborativeFilterRMSE = Math.sqrt(cfMeanSquareSummation / testDataSize);
		globalBaselineMSE = Math.sqrt(gbMeanSquareSummation / testDataSize);
	}

	// Calculate the global baseline variables: row mean, column mean, global row mean, and global column mean
	private static void calculateMeanVariables(HashMap<String, HashMap<String, Double>> ratings, ArrayList<String> userList){

		meanItemRatings.clear();
		meanUserRatings.clear();
		globalMeanUserRating = 0.0;
		globalMeanItemRating = 0.0;

		// Calculate the row mean, or the average rating for every item
		double itemRatingMean;
		int usersRated;

		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			itemRatingMean = 0.0;
			usersRated = 0;
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){
				itemRatingMean += userRatingEntry.getValue();
				usersRated = (userRatingEntry.getValue() == 0) ? usersRated : usersRated + 1;
			}
			itemRatingMean /= usersRated;
			meanItemRatings.put(itemRatingEntry.getKey(), itemRatingMean);
		}


		// Calculate the global row mean, or the global average rating of all items
		double itemRatingSummation = 0.0;

		for(Map.Entry<String, Double> itemAvgRating : meanItemRatings.entrySet()){
			itemRatingSummation += itemAvgRating.getValue();
		}

		globalMeanItemRating = itemRatingSummation / meanItemRatings.size();


		// Calculate the column mean, or the average rating given by every user
		double userRatingMean;
		int itemsRated;

		for(String user : userList){
			userRatingMean = 0.0;
			itemsRated = 0;
			for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
				userRatingMean += ratings.get(itemRatingEntry.getKey()).get(user);
				itemsRated = (ratings.get(itemRatingEntry.getKey()).get(user) == 0) ? itemsRated : itemsRated + 1;
			}
			userRatingMean /= itemsRated;
			meanUserRatings.put(user, userRatingMean);
		}

		// Calculate global column mean, or the global average rating given by all users
		double userRatingSummation = 0.0;

		for(Map.Entry<String, Double> userAvgRating : meanUserRatings.entrySet()){
			userRatingSummation += userAvgRating.getValue();
		}

		globalMeanUserRating = userRatingSummation / meanUserRatings.size();
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

		// Subtract row mean from ratings to normalize
		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){
				if(userRatingEntry.getValue() != 0.0)
					normalizedRatings.get(itemRatingEntry.getKey()).put(userRatingEntry.getKey(), userRatingEntry.getValue() - meanItemRatings.get(itemRatingEntry.getKey()));
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
	private static HashMap<String, HashMap<String, Double>> item_itemCollaborativeFilter(HashMap<String, HashMap<String, Double>> ratings, HashMap<String, HashMap<String, Double>> estimatedRatings, HashMap<String, HashMap<String, Double>> similarityMatrix){

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

		// Traverse ratings matrix and estimate missing ratings
		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			for(Map.Entry<String, Double> userRatingEntry : itemRatingEntry.getValue().entrySet()){

				// If we have a missing rating
				if(userRatingEntry.getValue() == 0.0){

					// reset summations of similarity
					similarityByRating = similarity = 0.0;

					// Pick the top N most similar items to the current item that have been rated by the current user
					int n = 0;
					for(Item neighbor : itemNeighborhood.get(itemRatingEntry.getKey())){
						if(ratings.get(neighbor.item).get(userRatingEntry.getKey()) != 0.0){
							similarityByRating += neighbor.similarity * ratings.get(neighbor.item).get(userRatingEntry.getKey());
							similarity += neighbor.similarity;
							n++;
						}
						if(n == N) break;
					}

					predictedRating = (similarity != 0 ? similarityByRating / similarity : 0.0);
					estimatedRatings.get(itemRatingEntry.getKey()).put(userRatingEntry.getKey(), (predictedRating < 1.0 ? 1.0 : (predictedRating > 5.0 ? 5.0 : predictedRating)));
				}
			}
		}

		return estimatedRatings;
	}

	// Estimate the missing ratings using a global baseline
	private static HashMap<String, HashMap<String, Double>> globalBaselineEstimate(HashMap<String, HashMap<String, Double>> ratings, HashMap<String, HashMap<String, Double>> estimatedRatings){

		double predictedRating;

		for(Map.Entry<String, HashMap<String, Double>> itemRatingEntry : ratings.entrySet()){
			for(Map.Entry<String, Double> userRatingEntry : ratings.get(itemRatingEntry.getKey()).entrySet()){

				if(userRatingEntry.getValue() == 0){

					predictedRating = globalMeanItemRating + (meanItemRatings.get(itemRatingEntry.getKey()) - globalMeanItemRating) + (meanUserRatings.get(userRatingEntry.getKey()) - globalMeanUserRating);
					estimatedRatings.get(itemRatingEntry.getKey()).put(userRatingEntry.getKey(), (predictedRating < 1.0 ? 1.0 : (predictedRating > 5.0 ? 5.0 : predictedRating)));
				}
			}
		}
		return estimatedRatings;
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