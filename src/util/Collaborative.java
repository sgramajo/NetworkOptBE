package util;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		String url1 = "C:/Users/stacygramajo03/workspace/NetworkBackEnd/src/resources/HomeKitchen.json"; 
		String url2 = "C:/Users/stacygramajo03/workspace/NetworkBackEnd/src/resources/partial.json"; 
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

}
