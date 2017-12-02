package servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import model.Accuracy;
import model.GlobalItemItem;
import util.Collaborative;

/**
 * Servlet implementation class AccuracyResult
 */
@WebServlet("/AccuracyResult")
public class AccuracyResult extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AccuracyResult() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * This will get the recommended items
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		String userID = request.getParameter("userID");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		Collaborative items = new Collaborative(); 
		String json = gson.toJson(items.getRecommendationItems(userID));
		response.getWriter().write(json);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//doGet(request, response);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");//cross domain request/CORS
		Gson gson = new Gson(); 
		Collaborative items = new Collaborative(); 
		Accuracy result = items.testAccuracy(); 
		String json = gson.toJson(result);
		response.getWriter().write(json);
	}

}
