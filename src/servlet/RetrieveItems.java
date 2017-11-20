package servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import model.Reviews;
import util.Collaborative;

/**
 * Servlet implementation class RetrieveItems
 */
//@WebServlet("/RetrieveItems")
public class RetrieveItems /*extends HttpServlet*/ {
	private static final long serialVersionUID = 1L;
	Collaborative items = new Collaborative(); 
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RetrieveItems() {
        super();
        // TODO Auto-generated constructor stub
    }
/*
	*//**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * The function will retrieve the items in the json data that has a few reviews
	 *//*
	@SuppressWarnings("static-access")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		String json = gson.toJson(items.createArray());
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		response.getWriter().write(json);
	}

	*//**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * This function will retrieve all of the users
	 *//*
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//doGet(request, response);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		String json = gson.toJson(items.users);
		response.getWriter().write(json);
	}*/

}
